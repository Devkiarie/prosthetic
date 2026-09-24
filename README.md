# Low-Cost Multichannel sEMG Control Interface for Upper-Limb Prosthetic Rehabilitation

**Final Year Project** -- B.Sc. Electronics & Computer Engineering, JKUAT (2022-2027)

A 4-channel surface EMG acquisition and classification system for myoelectric prosthetic control, built for under $20 in component cost.

## What This Does

1. **Acquires** sEMG signals from 4 forearm electrodes via a custom analog front-end (INA128, bandpass filters, 50 Hz notch)
2. **Classifies** 8 hand gestures in real-time using a quantized 1D-CNN on an ESP32-S3 microcontroller
3. **Drives** a 2-DOF 3D-printed prosthetic gripper based on the classified gesture
4. **Calibrates** to new users in under 3 minutes via a BLE mobile app

## Specs

| Parameter | Value |
|---|---|
| Channels | 4 differential sEMG |
| Gestures | 8 classes |
| Sampling rate | 2 kHz per channel |
| Signal band | 20-500 Hz |
| Classifier | INT8 1D-CNN, TFLite Micro |
| Target accuracy | > 80% (real-time, 5 subjects) |
| Target F1 | > 0.85 (NinaPro DB5, offline) |
| Inference latency | < 50 ms |
| Electronics BOM | < $20 |

## Repository Structure

```
hardware/           KiCad schematics, PCB layout, SPICE simulations, BOM
firmware/           ESP32-S3 firmware (PlatformIO, FreeRTOS)
ml/                 ML notebooks, training scripts, models
android/            BLE calibration app
mechanical/         3D print files for gripper
tests/              Measurement data, subject experiment results
docs/               Proposal, thesis, figures, lab notes, decisions
modules/            Obsidian-linked module notes
simulations/        Obsidian-linked simulation notes
ml-pipeline/        Obsidian-linked ML step notes
references/         Obsidian-linked paper notes
```

## Tools

- **EDA:** KiCad 7, ngspice
- **Firmware:** PlatformIO (ESP-IDF + FreeRTOS)
- **ML:** Python 3.11, TensorFlow, scikit-learn
- **Dataset:** NinaPro DB5 (not included, download from https://ninapro.hevs.ch/)

## License

Academic use. Full project report available upon completion.
