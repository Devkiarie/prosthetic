# sEMG Prosthetic Control — Master Project Plan
## Ian Kiarie | B.Sc. ECE | JKUAT | FYP 2022-2027

---

## Project Title

Design and Implementation of a Low-Cost Multichannel Surface Electromyography Control Interface for Upper-Limb Prosthetic Rehabilitation

---

## Document Map

| Document | Contents |
|---|---|
| **00_MASTER_PLAN.md** | This file — timeline, phase overview, success criteria |
| **01_ANALOG_ENGINEERING.md** | Phases 0–3: Requirements, single channel, AFE design, simulation |
| **02_DIGITAL_ENGINEERING.md** | Phases 4–6: 4-channel PCB, ESP32-S3 ADC, DSP pipeline |
| **03_ML_AND_BLE.md** | Phases 7–9: Dataset, ML training, calibration, BLE app |
| **04_INTEGRATION_AND_VALIDATION.md** | Phases 10–13: Actuator, decision logic, validation, 5-subject experiment |
| **05_BOM_AND_PROCUREMENT.md** | Complete BOM with exact values, part numbers, sources, 3 cost tiers |
| **06_DAILY_SCHEDULE.md** | Day-by-day breakdown across 12 months |
| **07_CHECKLISTS.md** | Phase-by-phase pass/fail checklists |

---

## System Architecture — Signal Chain

```
Ag/AgCl Electrodes (4 channels + 1 reference)
        |
        v
Input Protection (series R + ESD clamp)
        |
        v
INA128 Instrumentation Amplifier (G = 50, per channel)
        |
        v
2nd-Order Sallen-Key HPF (fc = 20 Hz, Butterworth)
        |
        v
2nd-Order Sallen-Key LPF (fc = 500 Hz, Butterworth)
        |
        v
Twin-T Notch Filter (f_notch = 50 Hz, > 30 dB rejection)
        |
        v
Variable Gain Stage (x10–x50, adjustable per subject)
        |
        v
DC Bias + Level Shift (center at Vref/2 = 1.65 V)
        |
        v
ESP32-S3 ADC1 (12-bit, 2 kHz/channel, DMA)
        |
        v
FreeRTOS DSP Task: 250 ms window, 24-feature extraction
        |
        v
TFLite Micro INT8 1D-CNN: 8-class gesture classification
        |
        v
Confidence Filter + Debounce Logic
        |
        v
PCA9685 PWM → 2x MG996R Servos → 3D-Printed 2-DOF Gripper
        |
        v
BLE GATT → Android Calibration App (live monitoring + transfer learning)
```

---

## Gain Budget — End to End

The gain must be designed so the maximum expected sEMG signal fills the ADC range without clipping, while the minimum expected signal is above the ADC noise floor.

| Parameter | Value |
|---|---|
| Maximum expected sEMG differential | 5 mV peak |
| Minimum expected sEMG differential | 50 µV peak |
| ESP32-S3 ADC range | 0 – 3.3 V |
| DC bias (mid-rail) | 1.65 V |
| Available swing (single-sided) | 1.65 V |
| ADC noise floor (12-bit, 3.3V range) | ~0.8 mV (1 LSB) |
| Target: max signal at ADC | < 1.5 V peak (90% of available swing) |
| Target: min signal at ADC | > 10 mV peak (well above noise floor) |

**Gain calculation:**
- For 5 mV max input: Total gain = 1.5 V / 5 mV = **300**
- For 50 µV min input with 10 mV target: Total gain = 10 mV / 50 µV = **200**
- Target total gain range: **200 – 300** (fixed), adjustable up to **500** for weak signals

**Gain distribution (recommended split):**
| Stage | Gain | Rationale |
|---|---|---|
| INA128 | 50× | Moderate front-end gain; avoids rail saturation from electrode DC offset |
| Post-filter variable stage | 4× – 10× | Adjustable via trimmer; compensates for subject-to-subject amplitude variation |
| **Total** | **200× – 500×** | Covers full expected signal range |

Why not put all gain at the INA128?
- Electrode DC offset of 300 mV × gain 500 = 150 V — instant saturation
- With gain 50: 300 mV × 50 = 15 V — still saturates on ±5V supply, but the HPF after the INA128 removes the DC component before further amplification
- The split-gain approach lets the HPF reject DC offset before the remaining gain is applied

---

## 12-Month Phase Structure

| Phase | Name | Months | Weeks | Key Deliverable |
|---|---|---|---|---|
| 0 | Requirements & Safety | Month 1, Wk 1–2 | 2 | Frozen requirements doc, risk register |
| 1 | Single-Channel Proof | Month 1–2, Wk 3–8 | 6 | Clean sEMG on oscilloscope, SNR > 20 dB |
| 2 | AFE Design | Month 2–3, Wk 6–10 | 5 | Complete analog schematic with calculated values |
| 3 | Simulation | Month 3, Wk 10–12 | 3 | LTspice Bode plots, Monte Carlo results |
| 4 | 4-Channel PCB | Month 4–5, Wk 13–18 | 6 | Fabricated + assembled PCB, crosstalk < -40 dB |
| 5 | ADC + Acquisition | Month 5–6, Wk 17–22 | 6 | 4-ch DMA at 2 kHz, raw data streaming |
| 6 | DSP Pipeline | Month 6–7, Wk 22–26 | 5 | 24-feature extraction, < 1% error vs Python |
| 7 | ML Training | Month 7–8, Wk 26–32 | 7 | INT8 model, F1 > 0.85, < 100 KB |
| 8 | User Calibration | Month 8–9, Wk 32–36 | 4 | BLE calibration protocol, < 3 min |
| 9 | BLE App | Month 9, Wk 34–38 | 4 | Android app, live gesture display |
| 10 | Actuator + Gripper | Month 9–10, Wk 36–40 | 4 | PCA9685 + servos + gripper working |
| 11 | Decision Logic | Month 10, Wk 40–42 | 2 | Confidence filter, debounce, safe fallback |
| 12 | Validation | Month 10–11, Wk 42–46 | 4 | All electrical/performance metrics measured |
| 13 | 5-Subject Experiment | Month 11–12, Wk 46–50 | 4 | Per-subject accuracy, confusion matrices |
| — | Thesis Writing | Month 12, Wk 48–52 | 4 | Complete thesis document, defence prep |

Note: Phases overlap. PCB ordering has 2-week lead time used for firmware development. ML training starts before PCB arrives using NinaPro DB5 data.

---

## Engineering Targets — Pass/Fail

| # | Metric | Target | Method |
|---|---|---|---|
| E1 | Single-channel SNR | > 20 dB | Oscilloscope: signal RMS during contraction / noise RMS at rest |
| E2 | Bandpass response | 20–500 Hz, ±3 dB | Signal generator frequency sweep |
| E3 | 50 Hz rejection | > 30 dB | 50 Hz injection, measure attenuation |
| E4 | Inter-channel crosstalk | < -40 dB | Drive one channel, measure others |
| E5 | Analog supply ripple | < 10 mV | Oscilloscope on power rail under load |
| E6 | Sampling rate | 2 kHz/channel actual | Timestamp analysis of DMA buffers |
| E7 | Feature extraction error | < 1% vs Python | Automated comparison test |
| E8 | Feature extraction time | < 25 ms per window | ESP32 timer measurement |
| E9 | Offline classifier F1 | > 0.85 | NinaPro DB5 held-out test set |
| E10 | INT8 model size | < 100 KB | File size measurement |
| E11 | On-device inference latency | < 50 ms | ESP32 timer measurement |
| E12 | Real-time accuracy | > 80% | 5-subject live test |
| E13 | Calibration duration | < 3 minutes | Timed user study |
| E14 | Post-calibration improvement | > 10% accuracy gain | Before/after comparison |
| E15 | Servo response time | < 200 ms | Gesture command to position |
| E16 | Gripper endurance | > 200 cycles | Open/close cycle test |
| E17 | System continuous operation | > 60 minutes | Timed stability test |
| E18 | Total component cost | < KES 8,500 | BOM summation |

---

## Risk Register

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | 50 Hz mains dominates sEMG | High | High | Battery power, INA128 CMRR > 100 dB, twin-T notch, shielded cables |
| R2 | INA128 unavailable from suppliers | Medium | High | Order early, get spares, identify pin-compatible alternatives (AD620, INA118) |
| R3 | ESP32-S3 ADC too noisy | Medium | Medium | 4× oversampling, ESP-IDF calibration API; fallback: ADS1115 external ADC |
| R4 | Electrode DC offset saturates amplifier | High | Medium | Split gain (50× at INA, rest after HPF), HPF removes DC before further gain |
| R5 | PCB layout error requires re-spin | Medium | High | Thorough LTspice sim + DRC before ordering; order 5 boards |
| R6 | Cross-user accuracy < 80% | Medium | Medium | Transfer learning calibration; if still below, reduce to 6 gestures |
| R7 | INT8 quantisation drops accuracy > 5% | Low | Medium | Quantisation-aware training; keep float32 model as fallback |
| R8 | 3D-printed gripper mechanical failure | Low | Low | PETG filament; proven InMoov-based design; gripper is demonstrator not core contribution |
| R9 | JLCPCB delivery delayed | Low | Medium | Order early; do firmware + ML work during wait |
| R10 | Subject recruitment difficulty | Medium | Medium | Start recruiting in Month 9; need only 5 volunteers from JKUAT campus |

---

## Three BOM Tiers

The concept paper targets sub-USD 20 component cost. This is tracked across three tiers:

| Tier | What it includes | Target |
|---|---|---|
| A: Electronics-only BOM | ICs, passives, connectors — no PCB, no mechanical | < KES 2,600 (USD 20) |
| B: Prototype BOM | Tier A + PCB fabrication + 3D printing + battery + servos | < KES 8,500 (USD 65) |
| C: Scaled BOM (100+ units) | Tier A at volume pricing + amortised PCB + injection mould | < KES 2,000 (USD 15) |

Detailed BOM with exact values is in `05_BOM_AND_PROCUREMENT.md`.

---

## Repository Structure

```
Final Year project/
├── 00_MASTER_PLAN.md                       ← this file
├── 01_ANALOG_ENGINEERING.md                ← Phases 0–3
├── 02_DIGITAL_ENGINEERING.md               ← Phases 4–6
├── 03_ML_AND_BLE.md                        ← Phases 7–9
├── 04_INTEGRATION_AND_VALIDATION.md        ← Phases 10–13
├── 05_BOM_AND_PROCUREMENT.md               ← full BOM
├── 06_DAILY_SCHEDULE.md                    ← day-by-day for 12 months
├── 07_CHECKLISTS.md                        ← phase checklists
├── hardware/
│   ├── kicad/                              ← schematic + PCB project
│   ├── simulation/                         ← LTspice files
│   ├── gerbers/                            ← fabrication outputs
│   └── bom/                                ← BOM exports
├── firmware/
│   ├── platformio.ini
│   ├── src/
│   │   ├── main.cpp
│   │   ├── adc_sampler.cpp / .h
│   │   ├── dsp_features.cpp / .h
│   │   ├── inference.cpp / .h
│   │   ├── actuator.cpp / .h
│   │   ├── ble_service.cpp / .h
│   │   └── decision_logic.cpp / .h
│   └── model/
│       └── semg_model_int8.cc
├── ml/
│   ├── notebooks/
│   │   ├── 01_data_exploration.ipynb
│   │   ├── 02_feature_engineering.ipynb
│   │   ├── 03_baseline_classifiers.ipynb
│   │   ├── 04_cnn_training.ipynb
│   │   └── 05_quantisation.ipynb
│   ├── data/                               ← NinaPro DB5 downloads
│   ├── models/                             ← saved models
│   └── scripts/                            ← training scripts
├── android/                                ← BLE calibration app
├── mechanical/
│   ├── gripper_stl/                        ← 3D print files
│   └── assembly/                           ← assembly photos/instructions
├── tests/
│   ├── afe_measurements/                   ← oscilloscope captures
│   ├── filter_sweeps/                      ← frequency response data
│   ├── crosstalk/                          ← inter-channel measurements
│   ├── ml_metrics/                         ← confusion matrices, F1 reports
│   └── subject_data/                       ← 5-subject experiment results
└── docs/
    ├── concept_paper.pdf
    ├── thesis/
    └── figures/
```

---

## How to Use These Documents

1. Read `00_MASTER_PLAN.md` (this file) for the big picture
2. Open `06_DAILY_SCHEDULE.md` to see what to do TODAY
3. Open the relevant phase document (01–04) for the detailed how-to
4. Check off items in `07_CHECKLISTS.md` as you complete them
5. Reference `05_BOM_AND_PROCUREMENT.md` when ordering components

---

*Document version: 1.0 | Created: September 2026 | Project start: October 2026*
