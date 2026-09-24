# Module 4: Gesture Classifier

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#C4|Block C4 -- Train CNN]] and [[docs/COMPREHENSIVE_PLAN#C5|Block C5 -- Quantization]]

---

## Network Architecture

```
Input: (24, 1)
  |
Conv1D(32, kernel=5, ReLU) --> BN --> MaxPool(2) --> Dropout(0.25)
  |
Conv1D(64, kernel=3, ReLU) --> BN --> MaxPool(2) --> Dropout(0.25)
  |
Conv1D(32, kernel=3, ReLU) --> BN --> GlobalAvgPool
  |
Dense(64, ReLU) --> Dropout(0.3) --> Dense(8, Softmax)
```

## 8 Gesture Classes

| Class | Gesture | Gripper Action |
|---|---|---|
| 0 | Rest | Hold position |
| 1 | Open hand | Release |
| 2 | Power grasp | Full grip |
| 3 | Pinch grip | Fine grip |
| 4 | Point | Index extension |
| 5 | Wrist flexion | Rotate inward |
| 6 | Wrist extension | Rotate outward |
| 7 | Thumbs up | Mode select |

## Training Data

- NinaPro DB5: 10 subjects, 4-channel subset, 8 gestures
- Validation: Leave-One-Subject-Out (LOSO)
- See [[ml-pipeline/ninapro-exploration]] and [[ml-pipeline/cnn-training]]

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| LOSO macro F1 | > 0.85 |
| Model size (INT8) | < 100 KB |
| Quantization accuracy drop | < 3% |
| Inference latency | < 50 ms |

## Log

_Entries added as work progresses._
