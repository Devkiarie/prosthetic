# Module 6: BLE Calibration Interface

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#E3|Block E3 -- BLE GATT]] and [[docs/COMPREHENSIVE_PLAN#F1|Block F1 -- Calibration]]

---

## BLE GATT Characteristics

| Characteristic | Properties | Payload |
|---|---|---|
| Gesture Output | Read, Notify | class (0-7) + confidence (0-100%) |
| Signal Quality | Read, Notify | RMS per channel (4 floats) |
| Control | Write | Command byte (start/stop cal) |
| Calibration Data | Notify | Feature chunks |
| Model Update | Write | Calibrated weight chunks |

## Calibration Protocol

1. App guides user through 8 gestures x 3 reps x 5 sec = ~3.2 min
2. ESP32 collects features + labels
3. Freeze all layers except final Dense(8)
4. Fine-tune last layer (20 epochs, SGD, lr=1e-4)
5. Re-quantize to INT8
6. Store in ESP32 NVS

## Mobile App (recommended: Web Bluetooth PWA)

Screens: Connect, Dashboard, Calibration Wizard, Signal View, Settings.
No Android Studio needed -- runs in Chrome on any Android phone.

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| BLE pairing time | < 5 seconds |
| Gesture update rate | > 10 Hz |
| Calibration time | < 3 minutes |
| Accuracy improvement | > 10 percentage points |

## Log

_Entries added as work progresses._
