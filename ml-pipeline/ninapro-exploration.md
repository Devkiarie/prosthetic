# ML Step: NinaPro DB5 Exploration

**Status:** COMPLETE -- data verified 2026-09-25
**Notebook:** `ml/notebooks/01_data_exploration.ipynb`
**Script:** `ml/scripts/explore_ninapro.py`
**Kernel:** FYP sEMG ML (Python 3.11)

## Dataset Facts (confirmed from real data)
- 10 subjects, Exercise 1-3 downloaded, 30 .mat files total
- Path pattern: `ml/data/ninapro_db5/S{N}/s{N}/S{N}_E1_A1.mat`
- EMG: 8-bit signed integers [-128, 127] (Myo armband quantisation, not raw mV)
- Subject 1: 130,267 samples = 651 s @ 200 Hz
- 4 selected channels: indices [0, 4, 8, 12]

## Gesture Mapping (DB5 label -> our class) -- all 8 confirmed present
| DB5 | Gesture | Our class | Windows (S1) |
|---|---|---|---|
| 0 | Rest | 0 | 8204 |
| 1 | Index extension | 4 (Point) | 505 |
| 3 | Fist | 2 (Power grasp) | 437 |
| 4 | Thumb up | 7 | 424 |
| 5 | Wrist flexion | 5 | 416 |
| 6 | Wrist extension | 6 | 443 |
| 11 | Pinch | 3 | 394 |
| 12 | Hand open | 1 | 382 |

## Window Parameters (DB5 rate, 200 Hz)
- Window: 50 samples = 250 ms at 200 Hz
- Step: 10 samples = 50 ms (80% overlap)
- Features per window: 24 (6 per channel x 4 channels)
- Subject 1 total windows: 11,205

## Feature Extraction (Subject 1)
- Feature matrix shape: (11,205 x 24)
- All 8 classes present
- Class imbalance: Rest >> other gestures (~73% Rest)
  - MITIGATION: undersample Rest to match gesture class counts before training

## Outputs
- `ml/data/processed/S1_X_features.npy`  shape (11205, 24)
- `ml/data/processed/S1_y_labels.npy`    shape (11205,)
- `ml/data/processed/real_semg_session.png`  -- 60 s overview
- `ml/data/processed/gesture_grid.png`  -- 1 rep per gesture

## Next Steps
- [ ] Run explore_ninapro.py for all 10 subjects
- [ ] Combine into full X (all subjects), y dataset
- [ ] Undersample/balance Rest class
- [ ] Train/test split: subjects 9+10 as held-out test set
- [ ] Baseline SVM classifier (quick validation)
- [ ] Proceed to CNN training (notebook 04)

## Last Updated
2026-09-25 -- Subject 1 fully explored, pipeline verified
