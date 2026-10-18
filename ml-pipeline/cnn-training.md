# ML: Model Training (MLP Gesture Classifier)

**Status:** DONE -- 2026-10-18 (train_v3.py slim MLP)
**Script:** `ml/scripts/train_v3.py`

## Input

- `ml/data/processed/X_balanced.npy`  shape (37,600, 24)  float32
- `ml/data/processed/y_balanced.npy`  shape (37,600,)     int32
- 8 classes, ~4,700 windows each (class-balanced)
- Split: 70% train / 15% val / 15% test (stratified)
- Labels: `restimulus` (reaction-time-corrected -- NOT `stimulus`)
- Window: 50 samples at 200 Hz = 250 ms (NinaPro DB5 native rate)

## Architecture (deployed)

**slim MLP** (24 -> 128 -> 64 -> 8)

```
Input(24)
  -> Dense(128, ReLU) -> BatchNormalization -> Dropout(0.3)
  -> Dense(64, ReLU)  -> BatchNormalization -> Dropout(0.2)
  -> Dense(8, Softmax)
```

## Training History

| Run | F1 | Config | Notes |
|---|---|---|---|
| train_cnn_v2 (old) | 0.659 (INFLATED) | stimulus labels, no cross-subj split | Wrong |
| train_v3 r1 | 0.437 | restimulus, correct split, but 500-sample window | Bug in windowing |
| train_v3 r2 | 0.544 | restimulus, 50-sample window | Correct |
| train_v3 r3 (512-unit) | 0.565 | larger model | 472 KB -- FAIL size |
| **train_v3 slim (final)** | **0.601** | 128->64, 150 epochs | **38.2 KB PASS** |

## Key Bugs Found

1. **`stimulus` vs `restimulus`**: `stimulus` has ~24% mislabelled windows at gesture
   boundaries. Use `mat['restimulus']`. Impact: +10.7 F1 points.

2. **Wrong window size**: NinaPro DB5 is 200 Hz. 250 ms = 50 samples (not 500).
   500 samples is correct for the ESP32 running at 2 kHz.

3. **Data path**: Mat files are at `S{N}/s{N}/S{N}_E1_A1.mat` (lowercase inner dir).

## Hyperparameters (final)

- Optimizer: Adam lr=1e-3
- LR schedule: ReduceLROnPlateau factor=0.5, patience=10, min=1e-5
- Early stopping: patience=20, restore_best_weights=True
- Batch size: 64
- Max epochs: 150

## Log

- 2026-09-25: First data exploration, 01_data_exploration.ipynb
- 2026-10-06: train_cnn_v2.py -- F1=0.659 (known inflated, stimulus bug)
- 2026-10-18: train_v3.py -- all bugs fixed, slim model final
