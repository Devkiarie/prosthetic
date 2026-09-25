# ML Step: NinaPro DB5 Exploration

**Status:** In progress -- download running (2026-09-25)
**Notebook:** `ml/notebooks/01_data_exploration.ipynb`
**Kernel:** FYP sEMG ML (Python 3.11)

## Dataset Facts
- 10 subjects, 52 gestures + rest, Exercise 1-3
- 2x Myo Armband = 16 channels @ 200 Hz, 6 reps per gesture
- Direct download (no login): `https://ninapro.hevs.ch/files/DB5_Preproc/sN.zip`
- ~19 MB per subject, ~190 MB total
- Extracting to: `ml/data/ninapro_db5/S{N}/`

## Channel Selection
Using 4 of 16 channels: indices [0, 4, 8, 12] -- evenly spaced around forearm from first Myo

## Gesture Mapping (DB5 label -> our class)
| DB5 | Gesture | Our class |
|---|---|---|
| 0 | Rest | 0 |
| 3 | Fist | 2 (Power grasp) |
| 4 | Thumb up | 7 |
| 5 | Wrist flexion | 5 |
| 6 | Wrist extension | 6 |
| 11 | Pinch | 3 |
| 12 | Hand open | 1 |
| 1 | Index extension | 4 (Point) |

## Window Parameters (DB5 rate, 200 Hz)
- Window: 50 samples = 250 ms
- Step: 10 samples = 50 ms (80% overlap)
- NOTE: Hardware uses 500 samples at 2000 Hz -- same 250 ms window, different sample count

## Status
- [ ] Download complete (all 10 subjects)
- [ ] Load subject 1, verify shape (N, 16)
- [ ] Select 4 channels, remap labels
- [ ] Visualise 1 gesture per channel (time-domain plot)
- [ ] Windowed feature extraction on all subjects
- [ ] Save processed `X_features.npy`, `y_labels.npy`

## Last Updated
2026-09-25 -- notebook scaffolded, download in progress
