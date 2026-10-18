# ML: Baseline Classifiers

**Status:** DONE -- 2026-10-18 (train_v3.py, cross-subject evaluation)
**Script:** `ml/scripts/train_v3.py`

## Results

| Classifier | F1 (cross-subject) | Notes |
|---|---|---|
| LDA | 0.375 | Linear decision boundary -- too simple |
| SVM (RBF, C=10) | 0.514 | Good for within-subject, weaker cross-subject |
| RF (200 trees) | 0.586 | Best classical baseline |
| **slim MLP (deployed)** | **0.601** | Best overall -- used in firmware |

All results use `restimulus` labels, per-subject z-score normalisation, 70/15/15 split.

## Previous (inflated) results (2026-10-06)

Old training used `stimulus` (24% mislabelled) and no cross-subject split:
- RF: F1=0.704 (now known to be inflated ~0.12 due to data leakage)
- MLP: F1=0.790 (inflated)

These numbers are **not valid** and should not appear in the final report.

## Log

- 2026-10-06: First baselines run -- inflated due to stimulus bug
- 2026-10-18: Rerun with restimulus, correct split -- RF=0.586, MLP=0.601
