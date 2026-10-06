# ML Step: MLP Training (sEMG Gesture Classifier)

**Status:** DONE — 2026-10-06
**Script:** `ml/scripts/train_cnn_nobn.py` (via `/tmp/quantize_fix.py`)
**Kernel:** FYP sEMG ML (Python 3.11, ml/venv)

## Input

- `ml/data/processed/X_balanced.npy`  shape (37308, 24)  float32
- `ml/data/processed/y_balanced.npy`  shape (37308,)     int64
- 8 classes, ~4,000–5,800 windows each
- Split: 70% train / 15% val / 15% test (stratified)
- NinaPro DB5 channels: 0, 4, 8, 12 (4-channel hardware mapping)
- Window: 50 samples at 200 Hz = 250 ms

## Architecture (deployed)

**MLP — no BatchNorm** (flat 24-vector input)

```
Input(24) → Dense(256, ReLU) → Dropout(0.3)
          → Dense(256, ReLU) → Dropout(0.3)
          → Dense(128, ReLU) → Dropout(0.2)
          → Dense(8, Softmax)
```

Why MLP not Conv1D: The 24-element input vector is heterogeneous (6 feature types ×
4 channels interleaved). Conv1D would apply kernels across adjacent elements that have
no local correlation (e.g. MAV_ch1 adjacent to RMS_ch1 are different feature types,
not spatially related). MLP treats all 24 inputs independently, which is correct.

Why no BatchNorm: TF+Keras3 BatchNorm ops produce MLIR type conflicts that SIGABRT
the TFLite INT8 converter. Removed BatchNorm; F1 unchanged at 0.7907.

## Results

| Metric | Value |
|---|---|
| Test F1 (macro) | **0.7907** |
| Test Accuracy | 0.7883 |
| Baseline LDA | 0.42 |
| Baseline SVM | 0.60 |
| Baseline RF | 0.70 |
| Target (pre-calibration) | 0.85 (post user calibration) |

## Gesture–Finger Mapping (8 classes)

| Class | Gesture | Finger Posture |
|---|---|---|
| 0 | Rest | All fingers extended |
| 1 | Open Hand | All fingers extended (active) |
| 2 | Power Grip | All fingers curled |
| 3 | Pinch | Thumb + Index curl |
| 4 | Point | Index extended, rest curled |
| 5 | Wrist Flex | Wrist servo forward |
| 6 | Wrist Extend | Wrist servo back |
| 7 | Thumbs Up | Thumb extended, rest curled |

## Files

- Model: `ml/models/semg_mlp_nobn_float32.keras`
- INT8:  `ml/models/semg_model_int8.tflite`
- Header:`ml/models/semg_weights.h`
- Stats: `ml/models/feature_mean.npy`, `ml/models/feature_std.npy`
- Log:   `ml/models/training_log_nobn.txt`

## Log

- 2026-09-25: NinaPro DB5 data downloaded; X_balanced.npy ready (37308×24)
- 2026-10-06: Conv1D v1 trained -- stuck at val_acc=0.50 (wrong arch, heterogeneous vector)
- 2026-10-06: MLP v2 with BatchNorm -- F1=0.6589; INT8 SIGABRT
- 2026-10-06: MLP no-BatchNorm -- **F1=0.7907**. Committed `629e731`.
- 2026-10-06: INT8 quantized -- F1=0.7845, 123.7 KB. Committed `71472bf`.
