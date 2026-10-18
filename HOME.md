# FYP — Low-Cost sEMG Prosthetic Control

**Ian Kiarie** | ENE212-0069/2022 | JKUAT ECE | 2022-2027
**Supervisor:** Dr Irene
**Last updated:** 2026-10-18

---

## Quick Links

| Document | Purpose |
|---|---|
| [[docs/COMPREHENSIVE_PLAN]] | Master plan — 8 blocks, every step laid out |
| [[docs/lab-notes/DEVELOPMENT_LOG]] | Full development log — all sessions, errors, fixes |
| [[docs/lab-notes/LOG]] | Lab notebook — every session logged |
| [[docs/decisions/DECISIONS]] | Design rationale — why things are the way they are |
| [[docs/proposal/ENE212-0069-2022_PROJECT_PROPOSAL_SEP-2026_REVISED_v2]] | Current proposal (4-chapter, Dr. Irene reviewed) |
| [[references/doi_verification_log]] | All 8 references — DOIs verified |
| [[hardware/kicad/semg_afe.kicad_sch]] | KiCad schematic — full 4-channel AFE |

---

## Modules

| # | Module | Status | Notes |
|---|---|---|---|
| 1 | [[modules/analog-front-end]] | **Simulations PASS — awaiting components** | INA128 G=51, BPF 20-100Hz, notch 50Hz. Schematic done. |
| 2 | [[modules/custom-pcb]] | Not started — after breadboard validation | KiCad schematic ready, PCB layout pending |
| 3 | [[modules/dsp-firmware]] | **Scaffold COMPLETE — awaiting ESP32-S3** | main.cpp, features.h, config.h done. Build PASS. |
| 4 | [[modules/gesture-classifier]] | **DONE — INT8 model deployed** | slim MLP 38.2 KB, F1=0.601 cross-subject verified |
| 5 | [[modules/actuator-hand]] | **STL ready — awaiting PCA9685/servos** | 5-finger tendon-driven hand, servo pockets cut |
| 6 | [[modules/ble-calibration]] | **Android app scaffolded — awaiting hardware** | BLE GATT, screens, gesture illustrations done |

---

## Simulation

| Simulation | Tool | Status |
|---|---|---|
| [[simulations/ina128-gain]] | ngspice v42 | **PASS** — G=51 confirmed |
| [[simulations/sallen-key-hpf]] | ngspice | **PASS** — fc=20 Hz confirmed |
| [[simulations/sallen-key-lpf]] | ngspice | **PASS** — fc=100 Hz confirmed |
| [[simulations/twin-t-notch]] | ngspice | **PASS** — >30 dB at 50 Hz |
| [[simulations/full-chain]] | ngspice | **PASS** — end-to-end gain + BW |
| [[simulations/monte-carlo]] | ngspice | Not started — post-breadboard |

---

## ML Pipeline

| Step | Status |
|---|---|
| [[ml-pipeline/ninapro-exploration]] | **DONE** — all 10 subjects, restimulus labels |
| [[ml-pipeline/feature-extraction]] | **DONE** — 6 TD features × 4 channels = 24 |
| [[ml-pipeline/baseline-classifiers]] | **DONE** — LDA F1=0.375, SVM=0.514, RF=0.586 |
| [[ml-pipeline/cnn-training]] | **DONE** — slim MLP 128→64, F1=0.601 cross-subject |
| [[ml-pipeline/quantization]] | **DONE** — INT8 38.2 KB, drop=0.011, PASS |

---

## References

| # | Citation | DOI Verified |
|---|---|---|
| 1 | [[references/salgado-2025]] — Single-ch sEMG on ESP32 | **YES** — 10.1109/ACCESS.2025.3598649 |
| 2 | Pizzolato et al. 2017 — NinaPro DB5 | **YES** — 10.1371/journal.pone.0186132 |
| 3 | Mattick et al. 2023 — 1.5M amputations/yr | **YES** — 10.1080/09638288.2022.2152875 |
| 4 | Hammond et al. 2025 — Cost barrier SSA | **YES** — 10.1016/j.strmo.2024.002236 |
| 5 | Hudgins et al. 1993 — Original TD features | **YES** — 10.1109/10.204774 |
| 6 | Li, Shi, Yu 2021 — DL survey sEMG | **YES** — PMC8107289 |
| 7 | Atzori et al. 2012 — NinaPro concept | **YES** — 10.1109/BioRob.2012.6290287 |
| 8 | [[references/molinari-2026]] — HD-EMG ceiling | **YES** — 10.1109/ACCESS.2026.3670500 |

See [[references/doi_verification_log]] for full verification notes.

---

## Key Specs (frozen)

| Parameter | Value |
|---|---|
| Channels | 4 differential sEMG (forearm) |
| Gestures | 8 classes |
| Sampling rate | 2 kHz per channel (ESP32 ADC1) |
| Signal band | 20–100 Hz (after anti-alias LPF) |
| Features | 6 time-domain × 4 channels = 24 |
| Classifier | INT8 slim MLP (128→64), 38.2 KB |
| Cross-subject F1 | 0.601 (base model, NinaPro DB5) |
| Target F1 post-calibration | ≥ 0.85 (subject-specific fine-tuning) |
| Target latency | < 50 ms inference |
| Target cost | < $20 electronics BOM |
| INA128 gain | G=51 (RG=1kΩ 1% MF) |
| Op-amp | MCP6002 (single 5V supply) |
| Actuator | 5-finger tendon-driven hand — 5× SG90 + 1× MG996R |

---

## Project Structure

```
Final Year project/
├── HOME.md                          ← this file
├── docs/
│   ├── lab-notes/DEVELOPMENT_LOG.md ← full dev log (all sessions, all errors)
│   ├── lab-notes/LOG.md             ← session log
│   ├── decisions/DECISIONS.md       ← architectural decisions
│   ├── proposal/*_REVISED_v2.docx   ← current proposal
│   └── thesis/                      ← (placeholder)
├── hardware/
│   ├── kicad/semg_afe.kicad_sch     ← schematic (KiCad 9)
│   ├── simulation/*.cir             ← ngspice netlists (all PASS)
│   └── hand_stl/modified/           ← servo-pocketed palm STL
├── firmware/
│   ├── src/main.cpp                 ← ESP32-S3 firmware
│   ├── include/config.h             ← constants + gesture postures
│   ├── include/features.h           ← C++ feature extraction
│   └── model/semg_v3_int8.tflite    ← INT8 model (38.2KB)
├── ml/
│   ├── scripts/train_v3.py          ← canonical training script
│   └── notebooks/                   ← data exploration
├── android/MyoControl/              ← BLE app (Kotlin + Compose)
├── modules/                         ← one note per hardware module
├── simulations/                     ← one note per simulation
├── ml-pipeline/                     ← one note per ML step
└── references/                      ← paper notes + DOI log
```
