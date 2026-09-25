# ML Step: NinaPro DB5 Exploration

**Status:** COMPLETE -- 2026-09-25
**Notebook:** `ml/notebooks/01_data_exploration.ipynb` (cells 1-9 all passing)
**Script:** `ml/scripts/explore_ninapro.py`
**Kernel:** FYP sEMG ML (Python 3.11)

## Dataset Facts (confirmed from real data)
- 10 subjects, Exercise 1 used (gestures 1-12 + rest)
- Path: `ml/data/ninapro_db5/S{N}/s{N}/S{N}_E1_A1.mat`
- EMG: 8-bit signed integers [-128, 127] (Myo Armband quantisation)
- ~130,000 samples per subject @ 200 Hz = ~650 s per subject

## Channel Selection
Channels [0, 4, 8, 12] -- evenly spaced from first Myo armband

## Window Parameters
- Window: 50 samples = 250 ms at 200 Hz
- Step: 10 samples = 50 ms step (80% overlap)
- NOTE: Hardware uses 500 samples at 2000 Hz -- same 250 ms, different count

## Gesture Mapping (DB5 -> our class)
| DB5 | Gesture | Our class |
|---|---|---|
| 0 | Rest | 0 |
| 12 | Hand open | 1 |
| 3 | Fist | 2 (Power grasp) |
| 11 | Pinch | 3 |
| 1 | Index extension | 4 (Point) |
| 5 | Wrist flexion | 5 |
| 6 | Wrist extension | 6 |
| 4 | Thumb up | 7 |

## Final Dataset (all 10 subjects, balanced)
| File | Shape | Description |
|---|---|---|
| `X_all_subjects.npy` | (~112k, 24) | All windows, imbalanced |
| `y_all_subjects.npy` | (~112k,) | Labels for above |
| `X_balanced.npy` | (37308, 24) | Balanced, ready for training |
| `y_balanced.npy` | (37308,) | Labels for balanced set |

## Class Distribution (X_balanced)
| Class | Gesture | Windows |
|---|---|---|
| 0 | Rest | 4,663 |
| 1 | Open hand | 4,239 |
| 2 | Power grasp | 5,307 |
| 3 | Pinch | 3,766 |
| 4 | Point | 5,785 |
| 5 | Wrist flex | 4,671 |
| 6 | Wrist ext | 4,751 |
| 7 | Thumbs up | 4,126 |
| **Total** | | **37,308** |

## Status
- [x] Download complete (all 10 subjects)
- [x] Load + inspect Subject 1
- [x] Select 4 channels, remap labels
- [x] Session overview plot + gesture grid plot
- [x] Windowed feature extraction (all 10 subjects)
- [x] Balance Rest class
- [x] Save X_balanced.npy + y_balanced.npy

## Next
Move to `ml-pipeline/cnn-training.md` and `ml/notebooks/02_cnn_training.ipynb`

## Last Updated
2026-09-25 -- all cells 1-9 run, dataset complete
