# Module 3: DSP and Feature Extraction Firmware

**Status:** SCAFFOLD COMPLETE — awaiting ESP32-S3 hardware to flash
**Phase:** [[docs/COMPREHENSIVE_PLAN#D3|Block D3 -- ESP32 ADC]] and [[docs/COMPREHENSIVE_PLAN#C6|Block C6 -- C Implementation]]
**Build:** SUCCESS — RAM 47.7% (156KB/320KB), Flash 32.6% (1.00MB/3.07MB)

---

## Architecture

```
ADC1 GPIO1-4 (8kHz round-robin)
     |
     v  Core 0 (highest priority)
Ring Buffer [1024 x 4ch x int16]
     | (every 500 samples = 250ms window, 100-sample step = 50ms)
     v
Feature Extraction (features.h)
  - MAV, RMS, WL, ZC, SSC, VAR per channel
  - Output: float32[24]
     |
     v
Normalisation: (x - mean) / std (from feature_mean_v3.npy)
     |
     v  Core 1
TFLite Micro Inference
  - INT8 slim MLP (24 -> 128 -> 64 -> 8)
  - Tensor arena: 96KB
  - Confidence threshold: 0.65 -> Rest if below
     |
     v
PCA9685 I2C (GPIO8=SDA, GPIO9=SCL) -> SG90 x5 + MG996R x1
     |
     v
BLE GATT notifications at 20Hz (throttled)
```

## Features (per 250ms window, per channel)

1. MAV -- Mean Absolute Value
2. RMS -- Root Mean Square
3. WL -- Waveform Length
4. ZC -- Zero Crossings (thresholded at 1% max amplitude)
5. SSC -- Slope Sign Changes (thresholded)
6. VAR -- Variance

See [[docs/decisions/DECISIONS#Features]] for why no FFT.

## Files

| File | Description |
|---|---|
| `firmware/src/main.cpp` | Complete firmware |
| `firmware/include/config.h` | All constants + 8-gesture posture table |
| `firmware/include/features.h` | C++ time-domain feature extraction |
| `firmware/model/semg_v3_int8.tflite` | INT8 model (38.2 KB) |
| `firmware/platformio.ini` | PlatformIO (ESP32-S3-DevKitC-1) |

## Pass/Fail Criteria

| Metric | Target | Status |
|---|---|---|
| Sampling rate | 2000 Hz +/- 1 Hz | Test when ESP32-S3 arrives |
| ADC noise floor | < 5 LSB RMS | Test on hardware |
| Feature error (C vs Python) | < 1% | Test on hardware |
| Feature extraction time | < 25 ms per window | ~5 ms estimated |
| CPU utilization | < 60% | Test on hardware |
| Continuous operation | Zero drops in 10 min | Test on hardware |
| Firmware build | PASS | **PASS** |

## ADC Pitfalls (ESP32-S3)
- **ONLY ADC1** (GPIO1-10) -- ADC2 disabled when BLE is active (ESP32-S3 errata)
- Usable input range: 0.15-2.45V (not full 0-3.3V)
- Oversample x4 to reduce BLE noise

## Log

- 2026-10-18: Firmware scaffold complete. Build PASS. Flash when hardware arrives.
