# FYP -- Low-Cost sEMG Prosthetic Control

**Ian Kiarie** | ENE212-0069/2022 | JKUAT ECE | 2026-2027
**Supervisor:** Dr Irene

---

## Quick Links

| Document | Purpose |
|---|---|
| [[docs/COMPREHENSIVE_PLAN]] | Master plan -- 8 blocks, every step laid out |
| [[docs/gap-analysis/GAP_ANALYSIS]] | Gaps found during deep review (Sep 24) |
| [[docs/lab-notes/LOG]] | Lab notebook -- every session logged |
| [[docs/decisions/DECISIONS]] | Design rationale -- why things are the way they are |
| [[docs/paper/PAPER_DRAFT]] | Live thesis draft -- fills as work progresses |
| [[docs/proposal/FYP_Proposal_Ian_Kiarie]] | 10-page proposal for Dr Irene |

---

## Modules

Each module is independently testable. Click through for details, test criteria, and status.

| # | Module | Status | Notes |
|---|---|---|---|
| 1 | [[modules/analog-front-end]] | Not started | INA128 G=50, BPF 20-500Hz, notch 50Hz |
| 2 | [[modules/custom-pcb]] | Not started | 4-layer, KiCad, JLCPCB |
| 3 | [[modules/dsp-firmware]] | Not started | ESP32-S3, FreeRTOS, 24 features |
| 4 | [[modules/gesture-classifier]] | Not started | 1D-CNN, INT8, TFLite Micro |
| 5 | [[modules/actuator-gripper]] | Not started | PCA9685, MG996R, 2-DOF gripper |
| 6 | [[modules/ble-calibration]] | Not started | BLE GATT, transfer learning, mobile app |

---

## Simulation

| Simulation | Tool | Status |
|---|---|---|
| [[simulations/ina128-gain]] | ngspice / KiCad SPICE | Not started |
| [[simulations/sallen-key-hpf]] | ngspice | Not started |
| [[simulations/sallen-key-lpf]] | ngspice | Not started |
| [[simulations/twin-t-notch]] | ngspice | Not started |
| [[simulations/full-chain]] | ngspice | Not started |
| [[simulations/monte-carlo]] | ngspice | Not started |

---

## ML Pipeline

| Step | Status |
|---|---|
| [[ml-pipeline/ninapro-exploration]] | Not started |
| [[ml-pipeline/feature-extraction]] | Not started |
| [[ml-pipeline/baseline-classifiers]] | Not started |
| [[ml-pipeline/cnn-training]] | Not started |
| [[ml-pipeline/quantization]] | Not started |

---

## References

| # | Citation | Verified DOI? |
|---|---|---|
| 1 | [[references/salgado-2025]] -- Single-ch sEMG on ESP32 | No |
| 2 | [[references/mekruksavanich-2026]] -- Deep residual + channel attention | No |
| 3 | [[references/lee-2025]] -- FedEMG transfer learning | No |
| 4 | [[references/molinari-2026]] -- HD-EMG wearable platform | No |
| 5 | [[references/furtado-2026]] -- Modular low-cost prosthetic | No |
| 6 | [[references/prakash-2026]] -- Comprehensive EMG review | No |
| 7 | [[references/abdikenov-2025]] -- Robotic prostheses review | No |

---

## Key Specs (frozen)

| Parameter | Value |
|---|---|
| Channels | 4 differential sEMG |
| Gestures | 8 classes |
| Sampling rate | 2 kHz per channel |
| Signal band | 20-500 Hz |
| Features | 6 time-domain x 4 channels = 24 |
| Classifier | INT8 1D-CNN < 100 KB |
| Target accuracy | > 80% (5 subjects, real-time) |
| Target F1 | > 0.85 (NinaPro DB5, offline) |
| Target latency | < 50 ms inference |
| Target cost | < $20 electronics BOM |

---

## Project Structure

```
Final Year project/
+-- HOME.md                          <-- this file
+-- 00_MASTER_PLAN.md                <-- timeline overview
+-- 01-04_*.md                       <-- phase detail docs
+-- 05_BOM_AND_PROCUREMENT.md
+-- 06_DAILY_SCHEDULE.md
+-- 07_CHECKLISTS.md
+-- modules/                         <-- one note per module
+-- simulations/                     <-- one note per simulation
+-- ml-pipeline/                     <-- one note per ML step
+-- references/                      <-- one note per paper
+-- hardware/                        <-- KiCad, SPICE, gerbers, BOM
+-- firmware/                        <-- ESP32 code
+-- ml/                              <-- notebooks, data, models
+-- android/                         <-- BLE app
+-- mechanical/                      <-- 3D print files
+-- tests/                           <-- measurement data
+-- docs/                            <-- proposal, thesis, figures, logs
+-- resources/                       <-- papers, datasheets
```
