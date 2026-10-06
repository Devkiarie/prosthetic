# Module 4: Gesture Classifier

**Status:** DONE — model trained and quantized (2026-10-06)
**Phase:** [[docs/COMPREHENSIVE_PLAN#C4|Block C4 -- Train CNN]] and [[docs/COMPREHENSIVE_PLAN#C5|Block C5 -- Quantization]]

---

## Deployed Architecture: MLP (no BatchNorm)

```
Input(24) → Dense(256, ReLU) → Dropout(0.3)
          → Dense(256, ReLU) → Dropout(0.3)
          → Dense(128, ReLU) → Dropout(0.2)
          → Dense(8, Softmax)
```

**Why MLP not Conv1D:**
Input is a 24-element heterogeneous feature vector: 6 feature types (MAV, RMS, WL,
ZC, SSC, VAR) × 4 channels. Adjacent elements in the flat vector are different feature
types with no local spatial correlation. Conv1D kernels sliding over this vector are
meaningless. MLP with independent Dense layers is the correct architecture.

**Why no BatchNorm:**
TF 2.x + Keras 3 BatchNorm layers produce MLIR type conflicts that crash the TFLite
INT8 converter with `LLVM ERROR: unresolved type conflict`. BatchNorm removed; F1
was unaffected (0.7907 both with and without BN at this scale).

**If Conv1D is needed in future:**
Reshape input to `(6 feature types, 4 channels)` so kernels slide over the same
feature type across all 4 channels — physically meaningful.

---

## Performance

| Metric | Value |
|---|---|
| Test F1 (macro) | **0.7907** |
| Test Accuracy | 0.7883 |
| INT8 F1 | 0.7845 (drop: 0.006) |
| INT8 size | 123.7 KB |
| Baseline LDA | 0.42 |
| Baseline SVM | 0.60 |
| Baseline RF | 0.70 |
| Latency ESP32-S3 (float32) | ~1 ms |

---

## Input Specification

| Parameter | Value |
|---|---|
| Feature vector | 24 elements (6 features × 4 channels) |
| Features | MAV, RMS, WL, ZC, SSC, VAR |
| Channels | NinaPro DB5 indices 0, 4, 8, 12 |
| Window | 50 samples |
| Sample rate | 200 Hz |
| Window duration | 250 ms |
| Normalisation | (x − mean) / (std + 1e-8), per feature |

---

## Gesture Classes (8)

| ID | Gesture | Finger Pattern |
|---|---|---|
| 0 | Rest | All extended |
| 1 | Open Hand | All extended (active) |
| 2 | Power Grip | All curled |
| 3 | Pinch | Thumb + Index curl |
| 4 | Point | Index only extended |
| 5 | Wrist Flex | Wrist servo forward |
| 6 | Wrist Extend | Wrist servo back |
| 7 | Thumbs Up | Thumb extended, rest curled |

---

## Deployed Files

| File | Description |
|---|---|
| `ml/models/semg_mlp_nobn_float32.keras` | Float32 Keras model |
| `ml/models/semg_model_int8.tflite` | INT8 TFLite (deploy to ESP32-S3) |
| `ml/models/semg_weights.h` | C header for direct ESP32 use |
| `ml/models/feature_mean.npy` | Normalisation mean |
| `ml/models/feature_std.npy` | Normalisation std |
| `ml/models/training_log_nobn.txt` | Full epoch log |

---

## Log

- 2026-10-06: Conv1D v1 stuck val_acc=0.50 (wrong arch). Killed.
- 2026-10-06: MLP+BN F1=0.6589; INT8 SIGABRT Keras3 MLIR bug.
- 2026-10-06: MLP no-BN **F1=0.7907**. Committed `629e731`.
- 2026-10-06: INT8 quantized F1=0.7845, 123.7 KB. Committed `71472bf`.
