"""
explore_ninapro.py
Run: cd ~/school/"Final Year project" && ml/venv/bin/python3 ml/scripts/explore_ninapro.py
"""
import numpy as np
import scipy.io as sio
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from pathlib import Path

DATA_DIR    = Path("ml/data/ninapro_db5")
OUT_DIR     = Path("ml/data/processed")
CHANNEL_IDX = [0, 4, 8, 12]
CHANNEL_NAMES = ['FCR (CH1)', 'ED (CH2)', 'FDS (CH3)', 'BR (CH4)']
FS = 200
GESTURE_MAP = {0:0, 3:2, 4:7, 5:5, 6:6, 11:3, 12:1, 1:4}
GESTURE_NAMES = {0:'Rest',1:'Open hand',2:'Power grasp',3:'Pinch',
                 4:'Point',5:'Wrist flex',6:'Wrist ext',7:'Thumbs up'}
WINDOW, STEP, ZC_THR = 50, 10, 0.05

OUT_DIR.mkdir(parents=True, exist_ok=True)


def load_subject(n):
    p = DATA_DIR / f"S{n}" / f"s{n}" / f"S{n}_E1_A1.mat"
    mat    = sio.loadmat(str(p))
    emg    = mat['emg'].astype(np.float32)
    labels = mat['restimulus'].flatten().astype(int)
    reps   = mat['rerepetition'].flatten().astype(int)
    return emg, labels, reps


def remap_labels(labels):
    mapped = np.full_like(labels, -1)
    for db5, proj in GESTURE_MAP.items():
        mapped[labels == db5] = proj
    return mapped


def extract_features(win):
    """(WINDOW, 4) -> (24,) feature vector"""
    f = []
    for c in range(win.shape[1]):
        x   = win[:, c]
        thr = ZC_THR * np.max(np.abs(x)) if np.max(np.abs(x)) > 0 else 1e-6
        dx  = np.diff(x)
        f.extend([
            np.mean(np.abs(x)),
            np.sqrt(np.mean(x**2)),
            np.sum(np.abs(dx)),
            float(np.sum((x[:-1]*x[1:]<0) & (np.abs(x[:-1]-x[1:])>=thr))),
            float(np.sum((dx[:-1]*dx[1:]<0) & ((np.abs(dx[:-1])+np.abs(dx[1:]))>=thr))),
            np.var(x),
        ])
    return np.array(f, dtype=np.float32)


def windowed_features(emg4, mapped):
    X, y = [], []
    N = len(emg4)
    for start in range(0, N - WINDOW, STEP):
        lbl_win = mapped[start:start+WINDOW]
        valid   = lbl_win[lbl_win >= 0]
        if len(valid) == 0:
            continue
        maj = np.argmax(np.bincount(valid + 1, minlength=10)) - 1
        if maj < 0:
            continue
        X.append(extract_features(emg4[start:start+WINDOW]))
        y.append(maj)
    return np.array(X), np.array(y)


# ── Load Subject 1 ──────────────────────────────────────────────────
print("Loading Subject 1...")
emg, labels, reps = load_subject(1)
emg4   = emg[:, CHANNEL_IDX]
mapped = remap_labels(labels)
t      = np.arange(len(emg4)) / FS

print(f"  EMG shape:   {emg.shape}")
print(f"  Duration:    {emg.shape[0]/FS:.1f} s")
print(f"  EMG range:   [{emg.min():.0f}, {emg.max():.0f}]  (8-bit Myo units)")
print(f"  Classes:     {np.unique(mapped[mapped>=0])}")

# ── Plot 1: 60-second overview ───────────────────────────────────────
fig, axes = plt.subplots(4, 1, figsize=(14, 8), sharex=True)
fig.suptitle('NinaPro DB5 -- Subject 1, Real sEMG (first 60 s)', fontsize=12, fontweight='bold')
mask = t <= 60
for i, (ax, name) in enumerate(zip(axes, CHANNEL_NAMES)):
    ax.plot(t[mask], emg4[mask, i], lw=0.4, color=f'C{i}')
    ax.set_ylabel(name, fontsize=8)
    ax.set_ylim(-130, 130)
    ax.grid(alpha=0.2)
for db5_lbl, proj_lbl in GESTURE_MAP.items():
    if db5_lbl == 0:
        continue
    idx = np.where((labels == db5_lbl) & mask)[0]
    if len(idx) == 0:
        continue
    for ax in axes:
        ax.axvspan(t[idx[0]], t[idx[-1]], alpha=0.1, color=f'C{proj_lbl}')
axes[-1].set_xlabel('Time (s)')
plt.tight_layout()
p1 = OUT_DIR / 'real_semg_session.png'
plt.savefig(str(p1), dpi=150, bbox_inches='tight')
print(f"Saved: {p1}")
plt.close()

# ── Plot 2: Gesture grid (1 rep each) ───────────────────────────────
fig, axes = plt.subplots(4, 8, figsize=(20, 8), sharey=True)
fig.suptitle('NinaPro DB5 -- Subject 1: One Repetition per Gesture', fontsize=11, fontweight='bold')
for g_proj in range(8):
    db5_lbl = next(k for k, v in GESTURE_MAP.items() if v == g_proj)
    idx     = np.where((labels == db5_lbl) & (reps == 1))[0]
    if len(idx) == 0:
        for ch in range(4):
            axes[ch, g_proj].set_visible(False)
        continue
    seg   = emg4[idx[0]:idx[-1]+1]
    t_ms  = np.arange(len(seg)) / FS * 1000
    for ch in range(4):
        axes[ch, g_proj].plot(t_ms, seg[:, ch], lw=0.6, color=f'C{ch}')
        axes[ch, g_proj].set_ylim(-130, 130)
        axes[ch, g_proj].grid(alpha=0.2)
        if g_proj == 0:
            axes[ch, g_proj].set_ylabel(CHANNEL_NAMES[ch], fontsize=7)
        if ch == 0:
            axes[ch, g_proj].set_title(GESTURE_NAMES[g_proj], fontsize=8)
        if ch == 3:
            axes[ch, g_proj].set_xlabel('ms', fontsize=7)
plt.tight_layout()
p2 = OUT_DIR / 'gesture_grid.png'
plt.savefig(str(p2), dpi=150, bbox_inches='tight')
print(f"Saved: {p2}")
plt.close()

# ── Feature extraction ───────────────────────────────────────────────
print("\nExtracting windowed features (Subject 1)...")
X, y = windowed_features(emg4, mapped)
print(f"  Feature matrix: {X.shape}   (windows x 24 features)")
print(f"  Labels:         {np.unique(y)}")
for c in range(8):
    print(f"    Class {c} ({GESTURE_NAMES[c]}): {np.sum(y==c)} windows")

np.save(str(OUT_DIR / 'S1_X_features.npy'), X)
np.save(str(OUT_DIR / 'S1_y_labels.npy'),   y)
print(f"\nSaved: S1_X_features.npy  {X.shape}")
print(f"Saved: S1_y_labels.npy    {y.shape}")
print("\nDone. Real NinaPro sEMG data pipeline working.")
