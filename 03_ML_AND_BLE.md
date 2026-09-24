# Phases 7–9: Machine Learning, Calibration, BLE App
## Dataset, Model Training, Quantisation, User Calibration, Android App

---

## Phase 7 — Dataset and Machine Learning (Week 26–32)

### Week 26–27: Dataset Preparation

**Day 101–102: Download NinaPro DB5**

1. Register at https://ninapro.hevs.ch/ (free academic account)
2. Download Database 5 (DB5) — 10 intact subjects, 52 finger movements
3. Data format: MATLAB .mat files per subject per exercise
4. Key variables in each file:
   - `emg` — shape (N, 16) — 16 sEMG channels sampled at 200 Hz
   - `stimulus` — shape (N, 1) — gesture label per sample (0 = rest)
   - `repetition` — shape (N, 1) — repetition number (1–6)

**Important:** NinaPro DB5 is sampled at 200 Hz with 16 channels. Your hardware samples at 2 kHz with 4 channels. The feature extraction (MAV, RMS, etc.) normalises this difference — features computed on 200 Hz windows and 2 kHz windows represent the same physical quantities, just at different resolutions. Train on NinaPro features, then validate on your own hardware data.

**Day 103–104: Select gesture classes**

From NinaPro DB5 Exercise 1 (basic finger movements), select 8 gestures that map to useful prosthetic functions:

| Class | NinaPro Label | Gesture | Prosthetic Function |
|---|---|---|---|
| 0 | 0 | Rest | Neutral position |
| 1 | 1 | Index flexion | Point |
| 2 | 2 | Index + middle flexion | Pinch |
| 3 | 6 | Thumb up | Thumbs up |
| 4 | 9 | Fist (all fingers) | Power grip |
| 5 | 12 | Open hand | Release |
| 6 | 13 | Wrist flexion | Wrist down |
| 7 | 14 | Wrist extension | Wrist up |

Use 4 of the 16 channels: channels [0, 4, 8, 12] — spatially distributed across the electrode array, closest to a 4-channel forearm placement.

**Day 105–108: Feature extraction from NinaPro**

```python
# 01_prepare_dataset.py
import scipy.io as sio
import numpy as np
import os, glob

NINAPRO_DIR = 'ml/data/ninapro_db5'
OUTPUT_DIR  = 'ml/data/processed'
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Gesture map: NinaPro label → our 0-7 class
GESTURE_MAP = {0: 0, 1: 1, 2: 2, 6: 3, 9: 4, 12: 5, 13: 6, 14: 7}
CHANNELS    = [0, 4, 8, 12]   # 4 of 16 channels
WINDOW_SIZE = 50               # 250 ms at 200 Hz
STEP_SIZE   = 10               # 50 ms step

def compute_features(window):
    """Compute 6 features per channel, return 24-dim vector."""
    features = []
    for ch in range(window.shape[1]):
        x = window[:, ch].astype(np.float64)
        N = len(x)
        max_abs = np.max(np.abs(x))
        thr = 0.01 * max_abs if max_abs > 0 else 1e-9

        mav = np.mean(np.abs(x))
        rms = np.sqrt(np.mean(x**2))
        wl  = np.sum(np.abs(np.diff(x)))

        zc = 0
        for i in range(1, N):
            if (x[i] > 0 and x[i-1] < 0) or (x[i] < 0 and x[i-1] > 0):
                if abs(x[i] - x[i-1]) > thr:
                    zc += 1

        ssc = 0
        for i in range(2, N):
            d1, d2 = x[i] - x[i-1], x[i-1] - x[i-2]
            if d1 * d2 < 0 and (abs(d1) > thr or abs(d2) > thr):
                ssc += 1

        var = np.var(x)
        features.extend([mav, rms, wl, zc/N, ssc/N, var])
    return np.array(features, dtype=np.float32)

all_X, all_y, all_subj = [], [], []

for subj_id in range(1, 11):    # 10 subjects
    fname = os.path.join(NINAPRO_DIR, f'S{subj_id}_E1_A1.mat')
    if not os.path.exists(fname):
        print(f'  Skipping S{subj_id} — file not found')
        continue

    mat = sio.loadmat(fname)
    emg   = mat['emg'][:, CHANNELS]       # (N, 4)
    stim  = mat['stimulus'].flatten()      # (N,)
    repet = mat['repetition'].flatten()    # (N,)

    # Sliding window extraction
    for start in range(0, len(emg) - WINDOW_SIZE, STEP_SIZE):
        end   = start + WINDOW_SIZE
        label = int(stim[start + WINDOW_SIZE // 2])   # centre label

        if label not in GESTURE_MAP:
            continue

        window = emg[start:end]            # (50, 4)
        feats  = compute_features(window)  # (24,)

        all_X.append(feats)
        all_y.append(GESTURE_MAP[label])
        all_subj.append(subj_id)

    print(f'  S{subj_id}: {len([s for s in all_subj if s == subj_id])} windows')

X    = np.array(all_X,   dtype=np.float32)
y    = np.array(all_y,   dtype=np.int32)
subj = np.array(all_subj, dtype=np.int32)

np.save(os.path.join(OUTPUT_DIR, 'X_features.npy'), X)
np.save(os.path.join(OUTPUT_DIR, 'y_labels.npy'),   y)
np.save(os.path.join(OUTPUT_DIR, 'subjects.npy'),    subj)

print(f'\nTotal: {len(X)} windows, {len(np.unique(y))} classes')
print(f'Class distribution: {np.bincount(y)}')
print(f'Feature shape: {X.shape}')
```

### Week 28–29: Baseline Classifiers

**Day 109–111: Classical ML baselines**

Before building the CNN, build 3 classical models. This gives you (a) an academic comparison table and (b) a fallback if the CNN does not train well.

```python
# 02_baseline_classifiers.py
import numpy as np
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import f1_score, classification_report
import warnings; warnings.filterwarnings('ignore')

X    = np.load('ml/data/processed/X_features.npy')
y    = np.load('ml/data/processed/y_labels.npy')
subj = np.load('ml/data/processed/subjects.npy')

# Leave-One-Subject-Out Cross-Validation (LOSO)
# This is the gold standard for sEMG: train on 9 subjects, test on 1, rotate
logo = LeaveOneGroupOut()

classifiers = {
    'LDA':           LinearDiscriminantAnalysis(),
    'SVM (RBF)':     SVC(kernel='rbf', C=10, gamma='scale'),
    'Random Forest':  RandomForestClassifier(n_estimators=200, max_depth=20),
}

for name, clf in classifiers.items():
    f1_scores = []
    for train_idx, test_idx in logo.split(X, y, groups=subj):
        scaler = StandardScaler()
        X_train = scaler.fit_transform(X[train_idx])
        X_test  = scaler.transform(X[test_idx])

        clf.fit(X_train, y[train_idx])
        y_pred = clf.predict(X_test)
        f1 = f1_score(y[test_idx], y_pred, average='macro')
        f1_scores.append(f1)

    print(f'{name}: LOSO F1 = {np.mean(f1_scores):.4f} +/- {np.std(f1_scores):.4f}')
```

**Expected results:**
| Classifier | Expected LOSO F1 |
|---|---|
| LDA | 0.60 – 0.70 |
| SVM (RBF) | 0.70 – 0.80 |
| Random Forest | 0.72 – 0.82 |
| 1D-CNN (target) | 0.80 – 0.90 |

### Week 29–31: 1D-CNN Training

**Day 112–115: Model architecture**

```python
# 03_cnn_training.py
import numpy as np
import tensorflow as tf
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import f1_score, confusion_matrix, classification_report
import matplotlib.pyplot as plt

X    = np.load('ml/data/processed/X_features.npy')   # (N, 24)
y    = np.load('ml/data/processed/y_labels.npy')      # (N,)
subj = np.load('ml/data/processed/subjects.npy')      # (N,)

N_FEATURES = X.shape[1]   # 24
N_CLASSES  = len(np.unique(y))  # 8

def build_model():
    inputs = tf.keras.Input(shape=(N_FEATURES, 1))

    # Block 1
    x = tf.keras.layers.Conv1D(32, 5, padding='same', activation='relu')(inputs)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.MaxPooling1D(2)(x)
    x = tf.keras.layers.Dropout(0.25)(x)

    # Block 2
    x = tf.keras.layers.Conv1D(64, 3, padding='same', activation='relu')(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.MaxPooling1D(2)(x)
    x = tf.keras.layers.Dropout(0.25)(x)

    # Block 3
    x = tf.keras.layers.Conv1D(32, 3, padding='same', activation='relu')(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.GlobalAveragePooling1D()(x)

    # Dense head (this is the layer we fine-tune during calibration)
    x = tf.keras.layers.Dense(64, activation='relu', name='dense_head')(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(N_CLASSES, activation='softmax', name='classifier')(x)

    model = tf.keras.Model(inputs, outputs)
    return model

# Full LOSO evaluation
logo = LeaveOneGroupOut()
all_f1 = []
all_y_true, all_y_pred = [], []

for fold, (train_idx, test_idx) in enumerate(logo.split(X, y, groups=subj)):
    test_subj = subj[test_idx][0]
    print(f'\nFold {fold+1}/10: Test subject S{test_subj}')

    # Scale
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X[train_idx]).reshape(-1, N_FEATURES, 1)
    X_test  = scaler.transform(X[test_idx]).reshape(-1, N_FEATURES, 1)

    # Train
    model = build_model()
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])

    model.fit(X_train, y[train_idx], epochs=50, batch_size=64,
              validation_split=0.1, verbose=0,
              callbacks=[tf.keras.callbacks.EarlyStopping(
                  patience=10, restore_best_weights=True)])

    y_pred = model.predict(X_test, verbose=0).argmax(axis=1)
    f1 = f1_score(y[test_idx], y_pred, average='macro')
    all_f1.append(f1)
    all_y_true.extend(y[test_idx])
    all_y_pred.extend(y_pred)
    print(f'  F1 = {f1:.4f}')

print(f'\n=== LOSO Results ===')
print(f'Mean F1: {np.mean(all_f1):.4f} +/- {np.std(all_f1):.4f}')
print(classification_report(all_y_true, all_y_pred,
      target_names=['Rest','Index','Pinch','ThumbUp','Fist','Open','WristFlex','WristExt']))

# Save confusion matrix
cm = confusion_matrix(all_y_true, all_y_pred)
fig, ax = plt.subplots(figsize=(8, 8))
ax.imshow(cm, cmap='Blues')
for i in range(N_CLASSES):
    for j in range(N_CLASSES):
        ax.text(j, i, str(cm[i, j]), ha='center', va='center', fontsize=9)
ax.set_xlabel('Predicted'); ax.set_ylabel('True')
ax.set_title(f'LOSO Confusion Matrix (Mean F1={np.mean(all_f1):.3f})')
labels = ['Rest','Idx','Pinch','Thumb','Fist','Open','WFlex','WExt']
ax.set_xticks(range(8)); ax.set_xticklabels(labels, rotation=45)
ax.set_yticks(range(8)); ax.set_yticklabels(labels)
plt.tight_layout()
plt.savefig('ml/models/confusion_matrix_loso.png', dpi=150)
plt.close()
```

### Week 31–32: INT8 Quantisation and Deployment

**Day 120–122: Train final model on ALL subjects**

```python
# 04_quantize_and_export.py
import numpy as np, tensorflow as tf

X = np.load('ml/data/processed/X_features.npy')
y = np.load('ml/data/processed/y_labels.npy')

from sklearn.preprocessing import StandardScaler
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X).reshape(-1, 24, 1).astype(np.float32)

# Save scaler params for embedded use
np.save('ml/models/scaler_mean.npy', scaler.mean_.astype(np.float32))
np.save('ml/models/scaler_std.npy',  scaler.scale_.astype(np.float32))

# Build and train final model on ALL data
model = build_model()   # from previous script
model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(X_scaled, y, epochs=80, batch_size=64, validation_split=0.1,
          callbacks=[tf.keras.callbacks.EarlyStopping(patience=15, restore_best_weights=True)])

# Save float32 model
model.save('ml/models/semg_float32.keras')
print(f'Float32 model params: {model.count_params()}')

# === INT8 Quantisation ===
def representative_data_gen():
    for i in range(min(1000, len(X_scaled))):
        yield [X_scaled[i:i+1]]

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type  = tf.int8
converter.inference_output_type = tf.int8

tflite_model = converter.convert()

with open('ml/models/semg_int8.tflite', 'wb') as f:
    f.write(tflite_model)

size_kb = len(tflite_model) / 1024
print(f'INT8 model size: {size_kb:.1f} KB (target: < 100 KB)')

# Verify quantised accuracy
interpreter = tf.lite.Interpreter(model_content=tflite_model)
interpreter.allocate_tensors()
inp = interpreter.get_input_details()[0]
out = interpreter.get_output_details()[0]

correct = 0
for i in range(len(X_scaled)):
    # Quantise input
    scale, zp = inp['quantization']
    x_q = (X_scaled[i:i+1] / scale + zp).astype(np.int8)
    interpreter.set_tensor(inp['index'], x_q)
    interpreter.invoke()
    pred = interpreter.get_tensor(out['index']).argmax()
    if pred == y[i]:
        correct += 1

q_acc = correct / len(y)
print(f'Quantised accuracy: {q_acc:.4f}')
print(f'Float accuracy: {model.evaluate(X_scaled, y, verbose=0)[1]:.4f}')
print(f'Accuracy drop: {model.evaluate(X_scaled, y, verbose=0)[1] - q_acc:.4f}')
```

**Day 123–124: Convert to C array**

```bash
xxd -i ml/models/semg_int8.tflite > firmware/src/model/semg_model_data.cc
```

This generates:
```c
const unsigned char semg_int8_tflite[] = { 0x20, 0x00, 0x00, 0x00, ... };
const unsigned int semg_int8_tflite_len = 45678;
```

Also export scaler parameters as C arrays:
```python
mean = np.load('ml/models/scaler_mean.npy')
std  = np.load('ml/models/scaler_std.npy')

with open('firmware/src/model/scaler_params.h', 'w') as f:
    f.write('#ifndef SCALER_PARAMS_H\n#define SCALER_PARAMS_H\n\n')
    f.write(f'static const float scaler_mean[24] = {{\n  ')
    f.write(', '.join(f'{v:.8f}f' for v in mean))
    f.write('\n};\n\n')
    f.write(f'static const float scaler_std[24] = {{\n  ')
    f.write(', '.join(f'{v:.8f}f' for v in std))
    f.write('\n};\n\n#endif\n')
```

**Phase 7 Exit Checklist:**
- [ ] NinaPro DB5 downloaded and processed (10 subjects)
- [ ] 8 gesture classes selected and mapped
- [ ] Feature extraction produces (N, 24) feature matrix
- [ ] LDA/SVM/RF baselines computed with LOSO
- [ ] 1D-CNN trained with LOSO — report mean F1 and per-fold F1
- [ ] Confusion matrix plotted and saved
- [ ] Final model trained on all subjects
- [ ] INT8 quantisation complete — model size < 100 KB
- [ ] Quantisation accuracy drop < 3%
- [ ] Model converted to C array for ESP32
- [ ] Scaler parameters exported as C header
- [ ] All notebooks committed to `ml/notebooks/`

---

## Phase 8 — User Calibration (Week 32–36)

### Calibration Concept

The pre-trained model works reasonably well on a new user (LOSO showed > 80% on unseen subjects). But personalised calibration improves accuracy by 10–15 percentage points. The idea:

1. Freeze all CNN layers except the last Dense(64 → 8) "classifier" layer
2. Collect 3 repetitions of each gesture from the new user (~20 seconds per gesture)
3. Extract features from these recordings
4. Fine-tune only the last layer on this small dataset
5. Quantise the updated model
6. Flash the updated weights to the ESP32 (or transfer via BLE)

### Calibration Protocol

**Guided session — controlled by the BLE app:**

```
App: "We will calibrate the system to your muscles. Follow the on-screen instructions."
App: "1/8: OPEN HAND — hold for 5 seconds" → [countdown 5..4..3..2..1]
App: "Relax" → [3 second pause]
App: "1/8: OPEN HAND — hold for 5 seconds" → [countdown] (rep 2)
App: "Relax"
App: "1/8: OPEN HAND — hold for 5 seconds" → [countdown] (rep 3)
App: "Relax"
→ Repeat for gestures 2/8 through 8/8
App: "Calibration complete! Updating model..."
```

Total time: 8 gestures × 3 reps × (5 sec hold + 3 sec rest) = **192 seconds ≈ 3.2 minutes**

This is close to the 3-minute target. If needed, reduce to 2 reps (128 seconds).

### Fine-Tuning Code

```python
# calibrate.py — runs on phone or PC connected via BLE
import numpy as np, tensorflow as tf

# Load the pre-trained model
model = tf.keras.models.load_model('ml/models/semg_float32.keras')

# Freeze everything except the last layer
for layer in model.layers[:-1]:
    layer.trainable = False
model.layers[-1].trainable = True   # 'classifier' layer

# Compile with lower learning rate
model.compile(optimizer=tf.keras.optimizers.Adam(1e-4),
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])

# Load calibration data collected via BLE
# X_cal: (N_cal, 24, 1) — features from the calibration session
# y_cal: (N_cal,) — gesture labels
X_cal = np.load('calibration_features.npy').reshape(-1, 24, 1)
y_cal = np.load('calibration_labels.npy')

# Scale with the original scaler
scaler_mean = np.load('ml/models/scaler_mean.npy')
scaler_std  = np.load('ml/models/scaler_std.npy')
X_cal_scaled = ((X_cal.reshape(-1, 24) - scaler_mean) / scaler_std).reshape(-1, 24, 1)

# Fine-tune (few epochs — small data, risk of overfitting)
model.fit(X_cal_scaled, y_cal, epochs=20, batch_size=16, verbose=1)

# Evaluate before vs after
print("Pre-calibration accuracy:", pre_accuracy)
print("Post-calibration accuracy:", model.evaluate(X_cal_scaled, y_cal)[1])

# Re-quantise
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
# ... same quantisation as before
tflite_cal = converter.convert()

# Save or transfer via BLE
with open('semg_calibrated_int8.tflite', 'wb') as f:
    f.write(tflite_cal)
```

**Phase 8 Exit Checklist:**
- [ ] Calibration protocol defined: 8 gestures × 3 reps × 5 sec
- [ ] Total calibration time measured: < 3 minutes
- [ ] Fine-tuning code tested on held-out NinaPro subject
- [ ] Accuracy improvement measured: > 10% target
- [ ] Calibrated model re-quantised and verified
- [ ] Calibration data format documented

---

## Phase 9 — BLE Android Application (Week 34–38)

### BLE GATT Service Design

**Service UUID:** `12345678-1234-1234-1234-123456789abc` (generate your own)

| Characteristic | UUID suffix | Properties | Payload | Purpose |
|---|---|---|---|---|
| Gesture Output | `0001` | Read, Notify | 1 byte: class (0–7) | Live gesture classification |
| Confidence | `0002` | Read, Notify | 1 byte: 0–100 (%) | Classifier confidence |
| Signal Quality | `0003` | Read, Notify | 4 bytes: RMS per channel | Electrode check |
| Control | `0010` | Write | 1 byte: command code | Start/stop calibration |
| Calibration Data | `0011` | Notify | 100 bytes: feature chunk | Stream cal features |
| Model Update | `0012` | Write | chunks | Upload calibrated weights |

**Command codes for Control characteristic:**
| Code | Command |
|---|---|
| 0x01 | Start calibration mode |
| 0x02 | Next gesture (app signals ESP32 to record) |
| 0x03 | Stop recording |
| 0x04 | Cancel calibration |
| 0x10 | Start live inference mode |
| 0x11 | Stop inference |
| 0xFF | Reset to factory model |

### ESP32 BLE Implementation

```c
// ble_service.h
#ifndef BLE_SERVICE_H
#define BLE_SERVICE_H

#include <stdint.h>

void ble_service_init(void);
void ble_notify_gesture(uint8_t gesture, uint8_t confidence);
void ble_notify_signal_quality(float rms[4]);

// Callback when app sends a command
typedef void (*ble_command_cb_t)(uint8_t command);
void ble_set_command_callback(ble_command_cb_t cb);

#endif
```

Use the ESP-IDF NimBLE stack (lighter than Bluedroid):
- Add `CONFIG_BT_NIMBLE_ENABLED=y` in `sdkconfig.defaults`
- Implement GATT server with the characteristics above
- Notify at 10 Hz (every 100 ms) for gesture and confidence
- Notify at 1 Hz for signal quality

### Android App (Kotlin)

**Technology choice:** Android BLE API with Jetpack Compose UI.

**App screens:**
1. **Scan & Connect** — list nearby BLE devices, filter by service UUID
2. **Dashboard** — live gesture display, confidence bar, signal quality per channel
3. **Calibration** — guided wizard with countdown, progress bar, gesture illustrations
4. **Settings** — electrode mapping, gesture labels, model info

**Key libraries:**
- `no.nordicsemi.android:ble-ktx:2.7.0` — simplified BLE scanning and connection
- Jetpack Compose — modern Android UI
- Kotlin Coroutines — async BLE operations

**Minimum viable app (for FYP demo):**
1. Connect to ESP32 by name ("sEMG-FYP")
2. Display live gesture as large text ("FIST" / "OPEN" / "REST")
3. Display confidence percentage
4. Show 4 signal-quality bars (one per channel, green = good contact, red = poor)
5. Calibration wizard that sends control commands and receives feature data

**Phase 9 Exit Checklist:**
- [ ] BLE GATT service running on ESP32 with all characteristics
- [ ] ESP32 advertises and pairs within 5 seconds
- [ ] Android app connects and displays live gesture at > 10 Hz
- [ ] Confidence percentage displayed in real time
- [ ] Signal quality indicators show electrode contact status
- [ ] Calibration wizard functional: guided session, data collection, model update
- [ ] App handles BLE disconnection gracefully (auto-reconnect)
- [ ] App tested on physical Android device (not just emulator)

---

*End of Phases 7–9. Continue to 04_INTEGRATION_AND_VALIDATION.md for actuator, decision logic, validation, and the 5-subject experiment.*
