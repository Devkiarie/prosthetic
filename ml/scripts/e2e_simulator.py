"""
e2e_simulator.py — sEMG → Features → TFLite → Servo Angles
Ian Kiarie | FYP ENE212-0069/2022 | JKUAT ECE

Demonstrates the complete signal processing pipeline WITHOUT hardware:
  1. Generate synthetic sEMG burst (simulates forearm muscle activation)
  2. Run it through the analog front-end model (INA128 gain + filter response)
  3. Extract 24 time-domain features
  4. Normalise and run INT8 TFLite inference
  5. Map classifier output → servo angles (PCA9685 posture)
  6. Plot everything end-to-end

Run:
    cd "~/school/Final Year project"
    source ml/venv/bin/activate
    python3 ml/scripts/e2e_simulator.py

Output:
    ml/data/processed/e2e_simulation.png  — full pipeline figure
"""

import numpy as np
import matplotlib
matplotlib.use('Agg')  # headless
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from pathlib import Path
import tensorflow as tf

# ── Paths ─────────────────────────────────────────────────────────────────────
BASE   = Path(__file__).parent.parent
MODELS = BASE / 'models'
OUT    = BASE / 'data' / 'processed'
TFLITE = MODELS / 'semg_v3_int8.tflite'

# ── Constants ─────────────────────────────────────────────────────────────────
FS          = 2000          # Hz — ESP32-S3 sampling rate
WIN_SAMPLES = 500           # 250 ms window at 2 kHz
N_CH        = 4
N_CLASSES   = 8
GESTURE_NAMES = ['Rest', 'Open', 'Power Grasp', 'Pinch', 'Point',
                 'Wrist Flex', 'Wrist Ext', 'Thumbs Up']

# Servo angles [thumb, index, middle, ring, pinky, wrist] in degrees
GESTURE_POSTURES = [
    [0,   0,   0,   0,   0,  90],   # Rest
    [0,   0,   0,   0,   0,  90],   # Open
    [180, 180, 180, 180, 180, 90],  # Power grasp
    [160, 160, 180, 180, 180, 90],  # Pinch
    [180, 0,   180, 180, 180, 90],  # Point
    [0,   0,   0,   0,   0,  45],   # Wrist flex
    [0,   0,   0,   0,   0,  135],  # Wrist ext
    [0,   180, 180, 180, 180, 90],  # Thumbs up
]
SERVO_NAMES = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky', 'Wrist']

# Muscle channel names
CH_NAMES = ['FCR\n(Wrist flex)', 'ED\n(Finger ext)', 'FDS\n(Finger flex)', 'BR\n(Wrist sup)']

# ── Stage 1: Synthetic sEMG signal generator ──────────────────────────────────
def generate_semg(gesture_id: int, n_samples: int = WIN_SAMPLES,
                  fs: int = FS, noise_floor: float = 0.02) -> np.ndarray:
    """
    Generate plausible synthetic sEMG for a given gesture.

    Each gesture activates a characteristic subset of the 4 channels
    with different amplitude profiles based on known muscle function:
      CH0 FCR: wrist flexion, power grasp
      CH1 ED:  finger extension, open hand
      CH2 FDS: finger flexion, pinch, point
      CH3 BR:  wrist supination, elbow flex
    """
    t = np.linspace(0, n_samples / fs, n_samples)

    # Base activation amplitudes per channel per gesture
    activations = {
        0: [0.05, 0.05, 0.05, 0.05],   # Rest — minimal
        1: [0.10, 0.85, 0.10, 0.15],   # Open — ED dominant
        2: [0.80, 0.10, 0.75, 0.30],   # Power — FCR + FDS
        3: [0.30, 0.10, 0.85, 0.15],   # Pinch — FDS dominant
        4: [0.20, 0.20, 0.80, 0.20],   # Point — FDS + FCR
        5: [0.90, 0.10, 0.20, 0.40],   # Wrist flex — FCR dominant
        6: [0.10, 0.75, 0.10, 0.70],   # Wrist ext — ED + BR
        7: [0.10, 0.10, 0.30, 0.90],   # Thumbs up — BR dominant
    }
    amps = activations[gesture_id]

    rng = np.random.default_rng(42 + gesture_id)
    signal = np.zeros((n_samples, N_CH))

    for ch in range(N_CH):
        # sEMG = bandlimited noise burst (20-500 Hz) scaled by activation
        # Simulate as sum of motor unit action potential-like spikes
        base_freq = 80 + ch * 30   # dominant frequency per channel
        noise = rng.standard_normal(n_samples)
        # Bandpass-like shaping: multiply by amplitude modulation
        envelope = amps[ch] * (0.5 + 0.5 * np.sin(2 * np.pi * 4 * t))
        carrier = (noise * envelope +
                   0.3 * amps[ch] * np.sin(2 * np.pi * base_freq * t) +
                   0.2 * amps[ch] * np.sin(2 * np.pi * base_freq * 2.3 * t))
        # Scale to Myo Armband range (~[-1, 1] normalised)
        if carrier.std() > 0:
            carrier = carrier / carrier.std() * amps[ch]
        signal[:, ch] = carrier + noise_floor * rng.standard_normal(n_samples)

    return signal.astype('float32')


# ── Stage 2: Analog front-end model (simplified, frequency-domain) ────────────
def apply_afe_model(raw: np.ndarray, fs: int = FS) -> np.ndarray:
    """
    Approximate the INA128 + Sallen-Key BPF + Twin-T notch response
    using FFT-based filtering (models the transfer function).

    Not a real SPICE simulation, but shows the frequency shaping effect.
    """
    from scipy import signal as sp
    filtered = np.zeros_like(raw)

    for ch in range(N_CH):
        sig = raw[:, ch].copy()
        # HPF at 20 Hz (removes DC and motion artifact)
        b_hp, a_hp = sp.butter(2, 20 / (fs / 2), btype='high')
        sig = sp.filtfilt(b_hp, a_hp, sig)
        # LPF at 100 Hz (anti-alias)
        b_lp, a_lp = sp.butter(2, 100 / (fs / 2), btype='low')
        sig = sp.filtfilt(b_lp, a_lp, sig)
        # Notch at 50 Hz
        b_n, a_n = sp.iirnotch(50, Q=30, fs=fs)
        sig = sp.filtfilt(b_n, a_n, sig)
        filtered[:, ch] = sig

    return filtered


# ── Stage 3: Feature extraction (matches firmware features.h exactly) ─────────
def extract_features(window: np.ndarray) -> np.ndarray:
    """window: (WIN_SAMPLES, 4) → (24,) float32"""
    feats = []
    for ch in range(N_CH):
        x = window[:, ch].astype('float64')
        N = len(x)
        mav  = np.mean(np.abs(x))
        rms  = np.sqrt(np.mean(x**2))
        wl   = np.sum(np.abs(np.diff(x)))
        thr  = 0.01 * (np.max(np.abs(x)) + 1e-9)
        zc   = np.sum((x[:-1] * x[1:] < 0) & (np.abs(x[:-1] - x[1:]) > thr))
        ssc  = np.sum(((x[1:-1] - x[:-2]) * (x[1:-1] - x[2:]) > 0) &
                      ((np.abs(x[1:-1] - x[:-2]) > thr) |
                       (np.abs(x[1:-1] - x[2:]) > thr)))
        var  = np.var(x)
        feats.extend([mav, rms, wl, float(zc), float(ssc), var])
    return np.array(feats, dtype='float32')


# ── Stage 4: TFLite INT8 inference ────────────────────────────────────────────
def run_inference(feats: np.ndarray) -> tuple[int, np.ndarray]:
    """Returns (predicted_class, probabilities[8])"""
    # Load normalisation params
    mean = np.load(MODELS / 'feature_mean_v3.npy')
    std  = np.load(MODELS / 'feature_std_v3.npy')
    feats_norm = (feats - mean) / (std + 1e-8)

    interp = tf.lite.Interpreter(model_path=str(TFLITE))
    interp.allocate_tensors()
    inp_d = interp.get_input_details()[0]
    out_d = interp.get_output_details()[0]
    i_sc, i_zp = inp_d['quantization']
    o_sc, o_zp = out_d['quantization']

    xq = np.clip(np.round(feats_norm / i_sc + i_zp), -128, 127).astype('int8')[np.newaxis]
    interp.set_tensor(inp_d['index'], xq)
    interp.invoke()
    oq = interp.get_tensor(out_d['index'])[0]
    probs = (oq.astype('float32') - o_zp) * o_sc
    # Softmax normalise (INT8 dequantised values are approximately probabilities)
    probs = np.exp(probs - probs.max())
    probs = probs / probs.sum()
    return int(np.argmax(probs)), probs


# ── Stage 5: Servo angles ─────────────────────────────────────────────────────
def gesture_to_servos(gesture_id: int) -> list[int]:
    return GESTURE_POSTURES[gesture_id]


# ── Main simulation loop ───────────────────────────────────────────────────────
def run_simulation(gesture_id: int = 2):
    print(f"\n{'='*60}")
    print(f"  sEMG End-to-End Simulator — {GESTURE_NAMES[gesture_id]}")
    print(f"{'='*60}")

    # Stage 1
    raw = generate_semg(gesture_id)
    print(f"[1] Raw sEMG:       shape={raw.shape}  range=[{raw.min():.3f}, {raw.max():.3f}]")

    # Stage 2
    try:
        filtered = apply_afe_model(raw)
        print(f"[2] AFE filtered:   INA128 G=51 → BPF 20-100Hz → Notch 50Hz")
    except ImportError:
        filtered = raw
        print("[2] AFE model:      scipy not available — using raw signal")

    # Stage 3
    feats = extract_features(filtered)
    print(f"[3] Features (24):  MAV={feats[0]:.4f}  RMS={feats[1]:.4f}  WL={feats[2]:.4f}  ...")

    # Stage 4
    pred_class, probs = run_inference(feats)
    pred_name = GESTURE_NAMES[pred_class]
    confidence = float(probs[pred_class])
    correct = (pred_class == gesture_id)
    print(f"[4] TFLite INT8:    Predicted={pred_name} ({confidence*100:.1f}%) "
          f"{'✓ CORRECT' if correct else f'✗ expected {GESTURE_NAMES[gesture_id]}'}")
    print(f"    Top-3: " + " | ".join(
        f"{GESTURE_NAMES[i]}={probs[i]*100:.1f}%"
        for i in np.argsort(probs)[-3:][::-1]))

    # Stage 5
    angles = gesture_to_servos(pred_class)
    print(f"[5] Servo angles:   " + "  ".join(
        f"{SERVO_NAMES[i]}={angles[i]}°" for i in range(6)))

    return raw, filtered, feats, probs, pred_class, angles


# ── Plotting ──────────────────────────────────────────────────────────────────
def plot_pipeline(gesture_id: int = 2):
    raw, filtered, feats, probs, pred_class, angles = run_simulation(gesture_id)
    t = np.linspace(0, WIN_SAMPLES / FS * 1000, WIN_SAMPLES)  # ms

    fig = plt.figure(figsize=(18, 12), facecolor='#0f172a')
    fig.suptitle(
        f'sEMG End-to-End Simulator  —  Input: {GESTURE_NAMES[gesture_id]}'
        f'  →  Predicted: {GESTURE_NAMES[pred_class]} ({probs[pred_class]*100:.0f}%)',
        color='white', fontsize=14, fontweight='bold', y=0.98)

    colors = ['#22d3ee', '#34d399', '#fbbf24', '#a78bfa']
    gs = gridspec.GridSpec(4, 3, figure=fig, hspace=0.55, wspace=0.35)

    # Row 1: Raw sEMG (4 channels)
    for ch in range(N_CH):
        ax = fig.add_subplot(gs[0, ch // 2 * 1 + ch % 2])  # 2×2 grid in row 0
    # Easier: use 4 cols for top row
    gs2 = gridspec.GridSpec(5, 4, figure=fig, hspace=0.6, wspace=0.35,
                             top=0.93, bottom=0.06)

    # Raw signal (top row)
    for ch in range(N_CH):
        ax = fig.add_subplot(gs2[0, ch])
        ax.set_facecolor('#1e293b')
        ax.plot(t, raw[:, ch], color=colors[ch], linewidth=0.8, alpha=0.9)
        ax.set_title(f'CH{ch+1} {CH_NAMES[ch].split(chr(10))[0]}', color='white', fontsize=9)
        ax.set_xlabel('ms', color='#94a3b8', fontsize=7)
        ax.tick_params(colors='#94a3b8', labelsize=7)
        for spine in ax.spines.values():
            spine.set_color('#334155')
        if ch == 0:
            ax.set_ylabel('Amplitude', color='#94a3b8', fontsize=7)

    # Filtered signal (row 2)
    for ch in range(N_CH):
        ax = fig.add_subplot(gs2[1, ch])
        ax.set_facecolor('#1e293b')
        ax.plot(t, filtered[:, ch], color=colors[ch], linewidth=0.8, alpha=0.9)
        ax.set_title(f'After AFE (20-100Hz BPF + 50Hz notch)', color='#94a3b8', fontsize=8)
        ax.tick_params(colors='#94a3b8', labelsize=7)
        for spine in ax.spines.values():
            spine.set_color('#334155')

    # Features bar chart (row 3, span 2 cols)
    ax_feat = fig.add_subplot(gs2[2, :2])
    ax_feat.set_facecolor('#1e293b')
    feat_names = []
    for ch in range(N_CH):
        feat_names += [f'{f}\nCH{ch+1}' for f in ['MAV','RMS','WL','ZC','SSC','VAR']]
    bar_colors = [colors[ch // 6 % 4] for ch in range(24)]
    ax_feat.bar(range(24), feats, color=bar_colors, alpha=0.85, width=0.7)
    ax_feat.set_xticks(range(24))
    ax_feat.set_xticklabels([f.split('\n')[0] for f in feat_names],
                              rotation=90, fontsize=6, color='#94a3b8')
    ax_feat.set_title('Feature Vector (24 elements)', color='white', fontsize=9)
    ax_feat.tick_params(colors='#94a3b8', labelsize=7)
    for spine in ax_feat.spines.values():
        spine.set_color('#334155')

    # Class probabilities (row 3, span 2 cols)
    ax_prob = fig.add_subplot(gs2[2, 2:])
    ax_prob.set_facecolor('#1e293b')
    bar_c = ['#22d3ee' if i == pred_class else '#334155' for i in range(N_CLASSES)]
    ax_prob.barh(GESTURE_NAMES, probs * 100, color=bar_c, alpha=0.9)
    ax_prob.set_xlabel('Probability (%)', color='#94a3b8', fontsize=8)
    ax_prob.set_title(f'INT8 TFLite Output\n→ {GESTURE_NAMES[pred_class]}', color='white', fontsize=9)
    ax_prob.tick_params(colors='#94a3b8', labelsize=8)
    for spine in ax_prob.spines.values():
        spine.set_color('#334155')
    ax_prob.axvline(65, color='#fbbf24', linewidth=1, linestyle='--', alpha=0.6)
    ax_prob.text(66, -0.4, 'threshold', color='#fbbf24', fontsize=6)

    # Servo angles (row 4, span all)
    ax_servo = fig.add_subplot(gs2[3, :])
    ax_servo.set_facecolor('#1e293b')
    servo_colors = ['#f97316', '#34d399', '#34d399', '#34d399', '#34d399', '#a78bfa']
    bars = ax_servo.bar(SERVO_NAMES, angles, color=servo_colors, alpha=0.85, width=0.6)
    ax_servo.set_ylim(0, 200)
    ax_servo.axhline(90, color='#475569', linewidth=0.8, linestyle='--')
    ax_servo.set_ylabel('Angle (°)', color='#94a3b8', fontsize=8)
    ax_servo.set_title(f'PCA9685 Servo Angles → Gesture: {GESTURE_NAMES[pred_class]}',
                       color='white', fontsize=9)
    ax_servo.tick_params(colors='#94a3b8', labelsize=9)
    for spine in ax_servo.spines.values():
        spine.set_color('#334155')
    for bar, angle in zip(bars, angles):
        ax_servo.text(bar.get_x() + bar.get_width()/2, angle + 3, f'{angle}°',
                      ha='center', va='bottom', color='white', fontsize=9, fontweight='bold')

    # Pipeline arrow legend (row 4 bottom)
    ax_legend = fig.add_subplot(gs2[4, :])
    ax_legend.set_facecolor('#0f172a')
    ax_legend.axis('off')
    stages = [
        'Ag/AgCl\nElectrodes', '→', 'INA128\nG=51',
        '→', 'BPF+Notch\n20-100Hz,50Hz',
        '→', 'TFLite INT8\n24→128→64→8',
        '→', 'PCA9685\nServo Driver',
        '→', 'ESP32-S3\nBLE notify',
        '→', 'Android\nMyoControl App'
    ]
    stage_colors = {
        'Ag/AgCl\nElectrodes': '#fb7185',
        'INA128\nG=51': '#22d3ee',
        'BPF+Notch\n20-100Hz,50Hz': '#fbbf24',
        'TFLite INT8\n24→128→64→8': '#34d399',
        'PCA9685\nServo Driver': '#a78bfa',
        'ESP32-S3\nBLE notify': '#22d3ee',
        'Android\nMyoControl App': '#fb923c',
    }
    x = 0.01
    for s in stages:
        if s == '→':
            ax_legend.text(x, 0.5, '→', color='#475569', fontsize=14,
                          va='center', transform=ax_legend.transAxes)
            x += 0.025
        else:
            c = stage_colors.get(s, 'white')
            ax_legend.text(x, 0.5, s, color=c, fontsize=7.5, fontweight='bold',
                          va='center', transform=ax_legend.transAxes,
                          bbox=dict(boxstyle='round,pad=0.3', facecolor='#1e293b',
                                    edgecolor=c, linewidth=1.2, alpha=0.9))
            x += 0.095

    out_path = OUT / 'e2e_simulation.png'
    plt.savefig(str(out_path), dpi=150, bbox_inches='tight',
                facecolor='#0f172a', edgecolor='none')
    plt.close()
    print(f"\n[→] Saved: {out_path}")
    return str(out_path)


# ── Run all 8 gestures ────────────────────────────────────────────────────────
if __name__ == '__main__':
    print("Running all 8 gestures through the pipeline...\n")
    correct = 0
    for g in range(N_CLASSES):
        _, _, _, probs, pred, _ = run_simulation(g)
        if pred == g:
            correct += 1

    print(f"\n{'='*60}")
    print(f"  Simulation accuracy: {correct}/{N_CLASSES} gestures correctly predicted")
    print(f"{'='*60}")

    print("\nGenerating pipeline plot for Power Grasp (gesture 2)...")
    out = plot_pipeline(gesture_id=2)
    print(f"Plot saved to: {out}")
