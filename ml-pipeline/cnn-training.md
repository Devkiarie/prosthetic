# ML Step: 1D-CNN Training

**Status:** Not started -- input data ready (2026-09-25)
**Notebook:** `ml/notebooks/02_cnn_training.ipynb` (to be created)
**Kernel:** FYP sEMG ML (Python 3.11)

## Input (ready)
- `ml/data/processed/X_balanced.npy`  shape (37308, 24)  float32
- `ml/data/processed/y_balanced.npy`  shape (37308,)     int64
- 8 classes, ~4,000-5,800 windows each

## Train/Test Split Plan
- Subjects 1-8: training set (~80%)
- Subjects 9-10: held-out test set (~20%)
- NOTE: current balanced dataset mixes all subjects -- need subject-aware split
  OR use the per-subject arrays and split before combining

## Model Architecture (target)
```
Input: (24,) feature vector
Dense(128, relu)
Dropout(0.3)
Dense(64, relu)
Dropout(0.3)
Dense(8, softmax)
```
Simple dense network first (faster to train, good baseline).
Then upgrade to 1D-CNN on raw (500, 4) windows if F1 target not met.

## Performance Targets
| Metric | Target |
|---|---|
| Offline F1 (macro) | >= 0.85 |
| Model size (INT8 TFLite) | < 100 KB |
| Inference latency (ESP32-S3) | < 50 ms |

## Status
- [ ] Create 02_cnn_training.ipynb
- [ ] Train/test split (subject-aware)
- [ ] Baseline dense network
- [ ] Evaluate F1 per class
- [ ] If F1 >= 0.85: proceed to quantisation
- [ ] If F1 < 0.85: upgrade to 1D-CNN on raw windows

## Last Updated
2026-09-25 -- input data ready, notebook not yet created
