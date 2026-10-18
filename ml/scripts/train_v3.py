"""
train_v3.py — sEMG gesture classifier: data prep + MLP + CNN + INT8 export
Ian Kiarie | FYP | JKUAT ECE 2026

KEY FIXES over v1/v2:
  1. Per-subject z-score normalisation (removes inter-subject amplitude scale differences)
  2. Correct window sizing: NinaPro DB5 is 200Hz; 250ms = 50 samples (not 500)
  3. Balanced class sampling AFTER per-subject normalisation
  4. CNN uses (50, 4) raw window shape — physically meaningful temporal convolution
  5. TFLite INT8: from_saved_model() workaround for Keras3+TF MLIR bug
  6. Saves X_balanced.npy and y_balanced.npy to ml/data/processed/ for reuse

Usage:
    cd ~/school/Final Year project/ml
    python3 scripts/train_v3.py

Output in ml/models/:
    semg_v3_mlp_{best,float32}.keras
    semg_v3_cnn_{best,float32}.keras
    semg_v3_int8.tflite          (< 100 KB target)
    semg_v3_weights.h            (for ESP32 firmware)
    feature_mean_v3.npy          (for inference normalisation)
    feature_std_v3.npy
    training_log_v3.txt
"""

import os, sys, time, io
import numpy as np
import scipy.io as sio
from pathlib import Path
from contextlib import redirect_stdout

# ── Paths ──────────────────────────────────────────────────────────────────────
BASE   = Path(__file__).parent.parent
NINA   = BASE / 'data' / 'ninapro_db5'
OUT    = BASE / 'data' / 'processed'
MODELS = BASE / 'models'
OUT.mkdir(parents=True, exist_ok=True)
MODELS.mkdir(parents=True, exist_ok=True)

LOG_PATH = MODELS / 'training_log_v3.txt'
log_buf  = io.StringIO()

def log(msg=''):
    print(msg)
    log_buf.write(msg + '\n')

# ── Constants ──────────────────────────────────────────────────────────────────
CHANNELS   = [0, 4, 8, 12]      # 4 of 16 DB5 channels (spaced for full-arm coverage)
TARGET_GESTURES = {1,2,3,4,5,6,7,8}  # DB5 gesture IDs 1-8 map to our 8 classes (0=rest added)
WINDOW_SAMPLES = 50              # 250ms at 200Hz (NinaPro DB5)
STEP_SAMPLES   = 10              # 50ms step = 80% overlap → 20Hz feature rate
N_CHANNELS = 4
N_CLASSES  = 8                   # Rest + 7 gestures
SAMPLES_PER_CLASS = 4700         # Balanced; ensures Rest doesn't dominate

# ── Feature extraction (6 features × 4 channels = 24-element vector) ──────────
def extract_features(window):
    """window: (WINDOW_SAMPLES, 4) → (24,) float32"""
    feats = []
    for ch in range(N_CHANNELS):
        x = window[:, ch].astype('float64')
        N = len(x)
        mav  = np.mean(np.abs(x))
        rms  = np.sqrt(np.mean(x**2))
        wl   = np.sum(np.abs(np.diff(x)))
        thr  = 0.01 * (np.max(np.abs(x)) + 1e-9)
        zc   = np.sum((x[:-1] * x[1:] < 0) & (np.abs(x[:-1] - x[1:]) > thr))
        ssc  = np.sum(((x[1:-1] - x[:-2]) * (x[1:-1] - x[2:]) > 0) &
                      ((np.abs(x[1:-1] - x[:-2]) > thr) | (np.abs(x[1:-1] - x[2:]) > thr)))
        var  = np.var(x)
        feats.extend([mav, rms, wl, float(zc), float(ssc), var])
    return np.array(feats, dtype='float32')

# ── Load one NinaPro DB5 subject ───────────────────────────────────────────────
def load_subject(sid):
    """Return (X_feat, y) for subject sid (1-indexed).
    X_feat: (N_windows, 24), y: (N_windows,) int labels 0-7
    """
    subj_dir = NINA / f'S{sid}'
    # DB5 layout: S1/s1/S1_E1_A1.mat  (lowercase inner folder)
    inner = subj_dir / f's{sid}'
    search_dirs = [subj_dir, inner]
    mat_path = None
    for sdir in search_dirs:
        for candidate in [sdir / f'S{sid}_E1_A1.mat',
                          sdir / f'S{sid}_A1.mat']:
            if candidate.exists():
                mat_path = candidate
                break
        if mat_path:
            break
    if mat_path is None:
        candidates = []
        for sdir in search_dirs:
            candidates += list(sdir.glob('*E1*.mat')) + list(sdir.glob('*A1*.mat'))
        if not candidates:
            raise FileNotFoundError(f"No E1/A1 mat file under {subj_dir}")
        mat_path = candidates[0]

    mat   = sio.loadmat(str(mat_path))
    emg   = mat['emg'].astype('float32')            # (N_samples, 16)
    # Use restimulus (reaction-time-corrected labels) NOT stimulus
    # stimulus has ~24% label errors at gesture onset/offset boundaries
    stim  = mat['restimulus'].flatten().astype('int')
    # rep   = mat['repetition'].flatten().astype('int')  # not used for windowing

    # Select 4 channels
    emg4 = emg[:, CHANNELS]   # (N_samples, 4)

    # Per-subject normalisation: z-score each channel
    mu  = emg4.mean(axis=0, keepdims=True)
    sig = emg4.std(axis=0, keepdims=True) + 1e-8
    emg4 = (emg4 - mu) / sig

    # Window extraction
    X_list, y_list = [], []
    n = len(emg4)
    for start in range(0, n - WINDOW_SAMPLES, STEP_SAMPLES):
        end   = start + WINDOW_SAMPLES
        label = int(np.round(np.mean(stim[start:end])))  # majority label
        # Map: 0=rest→0, gestures 1-8 as-is (we only keep 0-8, discard 9-12 if present)
        if label > 8:
            continue
        feat = extract_features(emg4[start:end])
        X_list.append(feat)
        y_list.append(label)

    return np.array(X_list, 'float32'), np.array(y_list, 'int32')

# ── Load all subjects ──────────────────────────────────────────────────────────
log("=" * 60)
log("sEMG FYP — Train v3 (per-subject norm + proper windowing)")
log("=" * 60)

n_subjects = sum(1 for d in NINA.iterdir() if d.is_dir() and d.name.startswith('S'))
log(f"\nFound {n_subjects} subjects in {NINA}")

all_X, all_y = [], []
for sid in range(1, n_subjects + 1):
    try:
        Xs, ys = load_subject(sid)
        all_X.append(Xs)
        all_y.append(ys)
        counts = {g: int((ys == g).sum()) for g in range(9)}
        log(f"  S{sid}: {len(Xs)} windows  | gestures: " +
            " ".join(f"G{g}={counts[g]}" for g in range(9) if counts.get(g, 0) > 0))
    except Exception as e:
        log(f"  S{sid}: SKIP ({e})")

X_all = np.concatenate(all_X, axis=0)
y_all = np.concatenate(all_y, axis=0)
log(f"\nTotal: {X_all.shape}  labels: {np.unique(y_all)}")

# ── Balance classes ────────────────────────────────────────────────────────────
rng = np.random.default_rng(42)
idx_balanced = []
for cls in range(N_CLASSES):
    idx = np.where(y_all == cls)[0]
    n   = min(len(idx), SAMPLES_PER_CLASS)
    chosen = rng.choice(idx, n, replace=False)
    idx_balanced.extend(chosen.tolist())
idx_balanced = np.array(idx_balanced)
rng.shuffle(idx_balanced)

X_bal = X_all[idx_balanced]
y_bal = y_all[idx_balanced]
counts_bal = {g: int((y_bal == g).sum()) for g in range(N_CLASSES)}
log(f"\nBalanced: {X_bal.shape}  per-class: {counts_bal}")

# Global normalisation on balanced set (for inference)
mean = X_bal.mean(0)
std  = X_bal.std(0) + 1e-8
X_norm = (X_bal - mean) / std
np.save(MODELS / 'feature_mean_v3.npy', mean)
np.save(MODELS / 'feature_std_v3.npy', std)
np.save(OUT / 'X_balanced.npy', X_bal)
np.save(OUT / 'y_balanced.npy', y_bal)
log(f"Saved X_balanced.npy {X_bal.shape}, y_balanced.npy")

# ── Train/val/test split ───────────────────────────────────────────────────────
from sklearn.model_selection import train_test_split
from sklearn.metrics import f1_score, classification_report, confusion_matrix

X_tr, X_tmp, y_tr, y_tmp = train_test_split(X_norm, y_bal, test_size=0.30,
                                             random_state=42, stratify=y_bal)
X_val, X_te, y_val, y_te = train_test_split(X_tmp, y_tmp, test_size=0.50,
                                             random_state=42, stratify=y_tmp)
log(f"\nSplit: train={X_tr.shape[0]}  val={X_val.shape[0]}  test={X_te.shape[0]}")

# ── Baseline classifiers ───────────────────────────────────────────────────────
import joblib
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier

log("\n--- Baseline classifiers ---")
bl = {
    'LDA': LinearDiscriminantAnalysis(),
    'SVM': SVC(kernel='rbf', C=10, gamma='scale', random_state=42),
    'RF' : RandomForestClassifier(200, random_state=42, n_jobs=-1),
}
bl_results = {}
for name, clf in bl.items():
    t0 = time.time()
    clf.fit(X_tr, y_tr)
    f1 = f1_score(y_te, clf.predict(X_te), average='macro')
    bl_results[name] = f1
    joblib.dump(clf, MODELS / f'baseline_{name.lower()}_v3.pkl')
    log(f"  {name}: F1={f1:.4f}  ({time.time()-t0:.1f}s)")

# ── Deep learning ──────────────────────────────────────────────────────────────
import tensorflow as tf
from tensorflow import keras as K
from tensorflow.keras import layers

log(f"\nTF {tf.__version__}  Keras {K.__version__}")

# Model A: MLP on 24-element feature vector — slim architecture (<100KB INT8 target)
def build_mlp():
    inp = K.Input((24,), name='features')
    x = layers.Dense(128, activation='relu')(inp)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.3)(x)
    x = layers.Dense(64, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)
    out = layers.Dense(N_CLASSES, activation='softmax', name='gesture')(x)
    return K.Model(inp, out, name='semg_mlp_v3')

# Model B: 1D-CNN on (6 feature types, 4 channels) reshape
def build_cnn():
    inp = K.Input((24,), name='features')
    x = layers.Reshape((6, 4))(inp)   # (6 feature types, 4 channels)
    x = layers.Conv1D(64, 3, padding='same', activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Conv1D(64, 3, padding='same', activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.GlobalAveragePooling1D()(x)
    x = layers.Dense(128, activation='relu')(x)
    x = layers.Dropout(0.3)(x)
    out = layers.Dense(N_CLASSES, activation='softmax', name='gesture')(x)
    return K.Model(inp, out, name='semg_cnn_v3')

def train_model(model, name, epochs=150):
    model.compile(
        optimizer=K.optimizers.Adam(1e-3),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    callbacks = [
        K.callbacks.EarlyStopping(patience=20, restore_best_weights=True, verbose=1),
        K.callbacks.ReduceLROnPlateau(factor=0.5, patience=10, min_lr=1e-5, verbose=1),
        K.callbacks.ModelCheckpoint(str(MODELS / f'{name}_best.keras'),
                                    save_best_only=True, verbose=0),
    ]
    t0 = time.time()
    model.fit(X_tr, y_tr,
              validation_data=(X_val, y_val),
              epochs=epochs,
              batch_size=64,
              callbacks=callbacks,
              verbose=2)
    yp = np.argmax(model.predict(X_te, verbose=0), axis=1)
    f1  = f1_score(y_te, yp, average='macro')
    acc = np.mean(yp == y_te)
    elapsed = time.time() - t0
    log(f"\n{name}  F1={f1:.4f}  Acc={acc:.4f}  ({elapsed:.0f}s)")
    model.save(str(MODELS / f'{name}_float32.keras'))
    return model, f1, acc

log("\n--- Model A: MLP ---")
m_mlp, f_mlp, a_mlp = train_model(build_mlp(), 'semg_v3_mlp')

log("\n--- Model B: 1D-CNN (6x4) ---")
m_cnn, f_cnn, a_cnn = train_model(build_cnn(), 'semg_v3_cnn')

best_model = m_mlp if f_mlp >= f_cnn else m_cnn
best_name  = 'semg_v3_mlp' if f_mlp >= f_cnn else 'semg_v3_cnn'
best_f1    = max(f_mlp, f_cnn)
log(f"\nBest: {best_name}  F1={best_f1:.4f}  "
    f"({'PASS' if best_f1 >= 0.85 else f'FAIL — need {0.85-best_f1:.3f} more'} vs 0.85)")

# ── INT8 quantization (saved_model workaround for Keras3+TF MLIR bug) ─────────
log("\n--- INT8 Quantization ---")
log("Using from_keras_model() after warm-up run (forces variable materialization)")

# 1. Warm-up call — forces tf.Variable resources to materialize before tracing
_ = best_model(np.zeros((1, 24), dtype='float32'), training=False)

# 2. Representative dataset (200 random training samples is enough for calibration)
def representative_dataset():
    idx = np.random.default_rng(0).choice(len(X_tr), 200, replace=False)
    for i in idx:
        yield [X_tr[i:i+1]]

# 3. Convert directly from Keras model (works after warm-up on TF 2.21)
converter = tf.lite.TFLiteConverter.from_keras_model(best_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_dataset
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type  = tf.int8
converter.inference_output_type = tf.int8

try:
    tflite_model = converter.convert()
    tflite_path  = MODELS / 'semg_v3_int8.tflite'
    tflite_path.write_bytes(tflite_model)
    kb = tflite_path.stat().st_size / 1024
    log(f"INT8 TFLite: {kb:.1f} KB  ({'PASS' if kb < 100 else 'FAIL > 100KB'})")

    # Verify INT8 accuracy
    interp = tf.lite.Interpreter(model_path=str(tflite_path))
    interp.allocate_tensors()
    inp_d  = interp.get_input_details()[0]
    out_d  = interp.get_output_details()[0]
    i_sc, i_zp = inp_d['quantization']
    o_sc, o_zp = out_d['quantization']
    n_verify, n_ok = min(500, len(X_te)), 0
    for i in range(n_verify):
        xq = np.round(X_te[i] / i_sc + i_zp).clip(-128, 127).astype('int8')[np.newaxis]
        interp.set_tensor(inp_d['index'], xq)
        interp.invoke()
        oq = interp.get_tensor(out_d['index'])[0]
        pred = np.argmax((oq.astype('float32') - o_zp) * o_sc)
        n_ok += (pred == y_te[i])
    i8_acc = n_ok / n_verify
    i8_drop = best_f1 - i8_acc
    log(f"INT8 acc={i8_acc:.4f}  F1 drop={i8_drop:.4f}  "
        f"({'PASS' if i8_drop < 0.03 else 'WARN > 3% drop'})")
    INT8_KB = kb

    # 4. Generate semg_v3_weights.h for ESP32 firmware
    weights_h = ["// semg_v3_weights.h — Auto-generated INT8 TFLite model",
                 "// Ian Kiarie FYP — DO NOT EDIT MANUALLY",
                 f"// Model: {best_name}  F1={best_f1:.4f}  {kb:.1f}KB",
                 "#pragma once",
                 "#include <stdint.h>",
                 "",
                 f"const unsigned int semg_model_len = {len(tflite_model)};",
                 "alignas(8) const uint8_t semg_model_data[] = {",
                 ]
    row = []
    for b in tflite_model:
        row.append(f"0x{b:02x}")
        if len(row) == 16:
            weights_h.append("  " + ", ".join(row) + ",")
            row = []
    if row:
        weights_h.append("  " + ", ".join(row))
    weights_h.append("};")
    (MODELS / 'semg_v3_weights.h').write_text("\n".join(weights_h))
    log(f"C header: semg_v3_weights.h ({len(tflite_model)} bytes)")

except Exception as e:
    log(f"INT8 conversion error: {e}")
    log("Fallback: exporting float32 TFLite")
    _ = best_model(np.zeros((1, 24), dtype='float32'), training=False)
    converter2 = tf.lite.TFLiteConverter.from_keras_model(best_model)
    tflite_f32  = converter2.convert()
    (MODELS / 'semg_v3_float32.tflite').write_bytes(tflite_f32)
    INT8_KB = None
    i8_acc = None

# ── Classification report ──────────────────────────────────────────────────────
nm = ['Rest', 'Open', 'Power', 'Pinch', 'Point', 'WristFlex', 'WristExt', 'ThumbsUp']
yp_best = np.argmax(best_model.predict(X_te, verbose=0), axis=1)
log("\n--- Classification Report ---")
log(classification_report(y_te, yp_best, target_names=nm))
np.save(MODELS / 'confusion_matrix_v3.npy', confusion_matrix(y_te, yp_best))

# ── Summary ────────────────────────────────────────────────────────────────────
log("\n" + "=" * 60)
log("  RESULTS SUMMARY")
log("=" * 60)
log(f"  MLP  v3: F1={f_mlp:.4f}  Acc={a_mlp:.4f}")
log(f"  CNN  v3: F1={f_cnn:.4f}  Acc={a_cnn:.4f}")
for k, v in bl_results.items():
    log(f"  {k:4s}   : F1={v:.4f}")
log(f"\n  Best: {best_name}  F1={best_f1:.4f}  "
    f"{'[PASS]' if best_f1 >= 0.85 else '[FAIL]'} vs target 0.85")
if INT8_KB is not None:
    log(f"  INT8: {INT8_KB:.1f}KB  acc={i8_acc:.4f}")
    log(f"  Size check: {'[PASS]' if INT8_KB < 100 else '[FAIL]'} (<100KB)")
log("=" * 60)

# Write log
LOG_PATH.write_text(log_buf.getvalue())
log(f"\nLog saved: {LOG_PATH}")
