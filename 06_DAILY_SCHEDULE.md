# Day-by-Day Schedule — 12 Months
## October 2026 → September 2027

Legend: **[D]** = deliverable due | **[O]** = order placed | **[T]** = test/measurement | **[R]** = review with supervisor

---

## Month 1: October 2026 — Requirements, Simulation, Orders

| Day | Week | Task | Phase |
|---|---|---|---|
| 1 Mon | W1 | Write requirements document (freeze specs) | P0 |
| 2 Tue | W1 | Write safety analysis, input protection design | P0 |
| 3 Wed | W1 | Create risk register spreadsheet | P0 |
| 4 Thu | W1 | **[O]** Order INA128, MCP6002, precision resistors from Mouser | P0 |
| 5 Fri | W1 | **[O]** Buy ESP32-S3-DevKitC, breadboard, wire from CBD | P0 |
| | | | |
| 6 Mon | W2 | Install LTspice, download INA128 + MCP6002 SPICE models | P3 |
| 7 Tue | W2 | Draw INA128 gain stage in LTspice, run DC + AC analysis | P3 |
| 8 Wed | W2 | Draw Sallen-Key HPF (20 Hz), run AC analysis, plot Bode | P3 |
| 9 Thu | W2 | Draw Sallen-Key LPF (500 Hz), run AC analysis, plot Bode | P3 |
| 10 Fri | W2 | Draw Twin-T notch (50 Hz), run AC analysis, measure depth | P3 |
| | | | |
| 11 Mon | W3 | Connect full signal chain in LTspice, run combined Bode | P3 |
| 12 Tue | W3 | Monte Carlo analysis: 100 runs with 5% tolerance | P3 |
| 13 Wed | W3 | **[D]** Export all Bode plots, save simulation files | P3 |
| 14 Thu | W3 | Install PlatformIO, create firmware project, blink LED test | P5 |
| 15 Fri | W3 | ESP32-S3 ADC basic read: GPIO1 → serial print raw values | P5 |
| | | | |
| 16 Mon | W4 | Download NinaPro DB5, extract .mat files | P7 |
| 17 Tue | W4 | Write Python data loader, explore DB5 in Jupyter | P7 |
| 18 Wed | W4 | Visualise raw sEMG from NinaPro: time-domain plots per gesture | P7 |
| 19 Thu | W4 | Select 8 gesture classes, map to NinaPro labels | P7 |
| 20 Fri | W4 | **[R]** Week 4 supervisor meeting: show simulations + data plan | — |

---

## Month 2: November 2026 — Single-Channel Breadboard

| Day | Week | Task | Phase |
|---|---|---|---|
| 21 Mon | W5 | Receive Mouser order, inventory check | P1 |
| 22 Tue | W5 | INA128 on breadboard: power only, measure DC output | P1 |
| 23 Wed | W5 | Add R_G (1.02 kΩ), signal generator test at 100 Hz / 1 mV | P1 |
| 24 Thu | W5 | CMRR test: common-mode 50 Hz, 500 mV on both inputs | P1 |
| 25 Fri | W5 | Document INA128 results: gain, CMRR, noise floor | P1 |
| | | | |
| 26 Mon | W6 | Build Sallen-Key HPF on breadboard (after INA128 output) | P1 |
| 27 Tue | W6 | Test HPF: frequency sweep 5 Hz – 100 Hz, verify -3 dB at 20 Hz | P1 |
| 28 Wed | W6 | Build Sallen-Key LPF (after HPF output) | P1 |
| 29 Thu | W6 | Test LPF: frequency sweep 100 Hz – 2 kHz, verify -3 dB at 500 Hz | P1 |
| 30 Fri | W6 | Full bandpass test: combined HPF + LPF sweep 5 Hz – 2 kHz | P1 |
| | | | |
| 31 Mon | W7 | Build Twin-T notch filter | P1 |
| 32 Tue | W7 | Tune notch with trimmer: inject 50 Hz, minimize output | P1 |
| 33 Wed | W7 | **[T]** Measure notch depth (target > 30 dB) | P1 |
| 34 Thu | W7 | Add variable gain stage (MCP6002 + 100 kΩ trimmer) | P1 |
| 35 Fri | W7 | Add ADC divider + protection. Full signal chain assembled | P1 |
| | | | |
| 36 Mon | W8 | Electrode placement: prep skin, apply Ag/AgCl, connect | P1 |
| 37 Tue | W8 | **First real sEMG recording** — oscilloscope, rest vs contraction | P1 |
| 38 Wed | W8 | Debug interference: try battery-only power, shielded cables | P1 |
| 39 Thu | W8 | **[T]** Quantitative SNR measurement (target > 20 dB) | P1 |
| 40 Fri | W8 | **[D]** Phase 1 complete. Document results + oscilloscope captures | P1 |

---

## Month 3: December 2026 — AFE Schematic, Continue ML

| Day | Week | Task | Phase |
|---|---|---|---|
| 41 Mon | W9 | **[R]** Supervisor meeting: show sEMG oscilloscope captures | — |
| 42 Tue | W9 | Start KiCad: create project, set up libraries | P4 |
| 43 Wed | W9 | Draw Channel 1 schematic (INA128 → HPF → LPF → notch → gain → ADC) | P4 |
| 44 Thu | W9 | Draw power supply schematic (LDO, Vref, decoupling) | P4 |
| 45 Fri | W9 | Draw ESP32-S3 header connections, pin assignments | P4 |
| | | | |
| 46 Mon | W10 | Replicate Channel 1 as hierarchical sheet × 4 | P4 |
| 47 Tue | W10 | Draw PCA9685 + servo connector schematic | P4 |
| 48 Wed | W10 | Run ERC (Electrical Rules Check), fix all errors | P4 |
| 49 Thu | W10 | **[D]** Complete schematic — export PDF for review | P4 |
| 50 Fri | W10 | Python: implement feature extraction (MAV, RMS, WL, ZC, SSC, VAR) | P7 |
| | | | |
| 51 Mon | W11 | Python: extract features from all 10 NinaPro subjects | P7 |
| 52 Tue | W11 | Python: run LDA baseline (LOSO cross-validation) | P7 |
| 53 Wed | W11 | Python: run SVM baseline (LOSO) | P7 |
| 54 Thu | W11 | Python: run Random Forest baseline (LOSO) | P7 |
| 55 Fri | W11 | **[D]** Baseline results table: LDA/SVM/RF F1 scores | P7 |
| | | | |
| 56 Mon | W12 | Start PCB layout: board outline, layer stackup, placement zones | P4 |
| 57 Tue | W12 | Place all Channel 1 components, route critical traces | P4 |
| 58 Wed | W12 | Place Channels 2–4, power section, connectors | P4 |
| 59 Thu | W12 | Route all traces, add ground planes, via stitching | P4 |
| 60 Fri | W12 | **[R]** Supervisor meeting: show baselines + PCB layout draft | — |

---

## Month 4: January 2027 — PCB Order, CNN Training

| Day | Week | Task | Phase |
|---|---|---|---|
| 61 Mon | W13 | PCB layout: final cleanup, silkscreen labels, test points | P4 |
| 62 Tue | W13 | Run DRC, fix all violations | P4 |
| 63 Wed | W13 | Generate Gerber files, review in online viewer | P4 |
| 64 Thu | W13 | **[O]** Order PCB from JLCPCB (5 pcs, 4-layer, DHL) | P4 |
| 65 Fri | W13 | **[D]** PCB design frozen. Export 3D view for documentation | P4 |
| | | | |
| 66 Mon | W14 | Python: build 1D-CNN model architecture | P7 |
| 67 Tue | W14 | Python: train CNN with LOSO (10-fold), log per-fold F1 | P7 |
| 68 Wed | W14 | Python: hyperparameter tuning (learning rate, dropout, filters) | P7 |
| 69 Thu | W14 | Python: generate confusion matrix, classification report | P7 |
| 70 Fri | W14 | Python: compare CNN vs baselines — write comparison table | P7 |
| | | | |
| 71 Mon | W15 | ESP32: implement timer-based ADC sampling at 2 kHz | P5 |
| 72 Tue | W15 | ESP32: implement 4-channel DMA ring buffer | P5 |
| 73 Wed | W15 | ESP32: verify sampling rate (timestamp analysis) | P5 |
| 74 Thu | W15 | ESP32: implement raw data UART streamer | P5 |
| 75 Fri | W15 | ESP32: record raw data from signal generator, verify in Python | P5 |
| | | | |
| 76 Mon | W16 | Python: train FINAL model on all 10 subjects | P7 |
| 77 Tue | W16 | Python: INT8 quantisation, verify accuracy drop < 3% | P7 |
| 78 Wed | W16 | Python: export to TFLite, convert to C array (xxd) | P7 |
| 79 Thu | W16 | Python: export scaler parameters to C header | P7 |
| 80 Fri | W16 | **[D]** ML model complete: INT8 TFLite < 100 KB, F1 > 0.85 | P7 |

---

## Month 5: February 2027 — PCB Assembly, DSP Pipeline

| Day | Week | Task | Phase |
|---|---|---|---|
| 81 Mon | W17 | Receive PCB from JLCPCB — visual inspection | P4 |
| 82 Tue | W17 | Solder power section: LDO, Vref, bulk caps | P4 |
| 83 Wed | W17 | Power test: measure all rails with multimeter | P4 |
| 84 Thu | W17 | Solder Channel 1: INA128, filters, gain, protection | P4 |
| 85 Fri | W17 | **[T]** Test Channel 1 with signal generator (same as Phase 1 tests) | P4 |
| | | | |
| 86 Mon | W18 | Solder Channels 2, 3, 4 | P4 |
| 87 Tue | W18 | **[T]** Test each channel individually: gain, bandpass, notch | P4 |
| 88 Wed | W18 | **[T]** Crosstalk test: drive Ch1, measure Ch2/3/4 (target < -40 dB) | P4 |
| 89 Thu | W18 | **[T]** Supply ripple test (target < 10 mV) | P4 |
| 90 Fri | W18 | **[D]** PCB validated. Solder ESP32 header + PCA9685 | P4 |
| | | | |
| 91 Mon | W19 | ESP32: implement dsp_features.c (6 features × 4 channels) | P6 |
| 92 Tue | W19 | ESP32: test features on known waveform (50 Hz sine from generator) | P6 |
| 93 Wed | W19 | ESP32: stream features via UART, compare with Python | P6 |
| 94 Thu | W19 | **[T]** Feature validation: < 1% error on all 24 features vs Python | P6 |
| 95 Fri | W19 | **[T]** Performance: feature extraction < 25 ms per window | P6 |
| | | | |
| 96 Mon | W20 | **[R]** Supervisor meeting: show PCB, demonstrate signal chain | — |
| 97 Tue | W20 | ESP32: integrate TFLite Micro, load INT8 model | P6 |
| 98 Wed | W20 | ESP32: run inference on test feature vectors, verify predictions | P6 |
| 99 Thu | W20 | ESP32: measure inference latency (target < 50 ms) | P6 |
| 100 Fri | W20 | **[D]** Full pipeline working: ADC → features → inference → serial output | P6 |

---

## Month 6: March 2027 — First Real Electrode-to-Prediction Test

| Day | Week | Task | Phase |
|---|---|---|---|
| 101 Mon | W21 | Connect electrodes to PCB, first live classification test | P6 |
| 102 Tue | W21 | Debug: adjust gain trimmers per channel for each gesture | P6 |
| 103 Wed | W21 | Record 5 minutes of live classified gestures, compute accuracy | P6 |
| 104 Thu | W21 | Identify worst-performing gestures, analyse confusion patterns | P6 |
| 105 Fri | W21 | **[D]** First live demo: gesture → serial prediction (no actuator yet) | P6 |
| | | | |
| 106 Mon | W22 | ESP32: implement BLE GATT service (NimBLE stack) | P9 |
| 107 Tue | W22 | ESP32: add gesture + confidence notify characteristics | P9 |
| 108 Wed | W22 | ESP32: test BLE with nRF Connect app (Android) | P9 |
| 109 Thu | W22 | ESP32: add signal quality characteristic (RMS per channel) | P9 |
| 110 Fri | W22 | ESP32: add control characteristic (calibration commands) | P9 |
| | | | |
| 111 Mon | W23 | PCA9685 + servo bring-up: I2C scan, PWM generation test | P10 |
| 112 Tue | W23 | Define gesture → servo angle mapping table | P10 |
| 113 Wed | W23 | Implement actuator.c: gesture command → servo movement | P10 |
| 114 Thu | W23 | 3D print gripper (start print — 4-6 hours) | P10 |
| 115 Fri | W23 | Assemble gripper, attach servos, test mechanical movement | P10 |
| | | | |
| 116 Mon | W24 | Connect everything: electrodes → PCB → ESP32 → servos → gripper | P10 |
| 117 Tue | W24 | **First end-to-end demo: muscle signal → gripper movement** | P10 |
| 118 Wed | W24 | Debug: adjust confidence threshold, debounce parameters | P11 |
| 119 Thu | W24 | Implement decision_logic.c: threshold + debounce + timeout | P11 |
| 120 Fri | W24 | **[R]** Supervisor meeting: LIVE DEMO — gesture → gripper | — |

---

## Month 7: April 2027 — Calibration + BLE App

| Day | Week | Task | Phase |
|---|---|---|---|
| 121–125 | W25 | Implement calibration data collection on ESP32 (BLE stream) | P8 |
| 126–130 | W26 | Python: calibration fine-tuning script (last-layer transfer learning) | P8 |
| 131–135 | W27 | Test calibration: before/after accuracy on 2 people (self + friend) | P8 |
| 136–140 | W28 | **[D]** Calibration working: < 3 min, > 10% accuracy improvement | P8 |

---

## Month 8: May 2027 — Android App

| Day | Week | Task | Phase |
|---|---|---|---|
| 141–145 | W29 | Android Studio: project setup, BLE scanning + connection | P9 |
| 146–150 | W30 | App: live gesture display, confidence bar, signal quality | P9 |
| 151–155 | W31 | App: calibration wizard UI with countdown and progress | P9 |
| 156–160 | W32 | App: test on physical Android device, fix BLE edge cases | P9 |

---

## Month 9: June 2027 — Integration Polish

| Day | Week | Task | Phase |
|---|---|---|---|
| 161–165 | W33 | System-level stress test: run for 60 min continuous | P11 |
| 166–170 | W34 | Fix any crashes, memory leaks, BLE disconnections | P11 |
| 171–175 | W35 | Build enclosure (3D-printed or laser-cut acrylic) | P10 |
| 176–180 | W36 | **[R]** Supervisor meeting: show complete integrated system | — |

---

## Month 10: July 2027 — Validation Campaign

| Day | Week | Task | Phase |
|---|---|---|---|
| 181–185 | W37 | **[T]** E1–E6: Analog measurements (SNR, bandpass, notch, crosstalk, ripple, sampling rate) | P12 |
| 186–190 | W38 | **[T]** E7–E11: Digital measurements (feature error, feature time, F1, model size, latency) | P12 |
| 191–195 | W39 | **[T]** E12–E18: System measurements (accuracy, calibration, servo, endurance, stability, cost) | P12 |
| 196–200 | W40 | **[D]** Validation spreadsheet complete, all 18 metrics pass/fail documented | P12 |

---

## Month 11: August 2027 — 5-Subject Experiment

| Day | Week | Task | Phase |
|---|---|---|---|
| 201–205 | W41 | Write consent form, recruit 5 subjects from JKUAT | P13 |
| 206–210 | W42 | **Subject 1 and 2:** Full experiment protocol (setup, calibration, recording, demo) | P13 |
| 211–215 | W43 | **Subject 3 and 4:** Full experiment protocol | P13 |
| 216–220 | W44 | **Subject 5:** Full protocol + aggregate analysis, plot confusion matrices | P13 |

---

## Month 12: September 2027 — Thesis and Defence

| Day | Week | Task | Phase |
|---|---|---|---|
| 221–225 | W45 | Thesis: Chapter 1 (Introduction), Chapter 2 (Literature Review) | — |
| 226–230 | W46 | Thesis: Chapter 3 (System Design), Chapter 4 (Implementation) | — |
| 231–235 | W47 | Thesis: Chapter 5 (Results), Chapter 6 (Conclusion) | — |
| 236–240 | W48 | Thesis: References, Appendices, Abstract, Table of Contents | — |
| 241–245 | W49 | **[R]** Submit draft to supervisor, incorporate feedback | — |
| 246–250 | W50 | Final edits, print, bind | — |
| 251–252 | W51 | **Defence preparation:** rehearse 15-min presentation | — |
| 253 | W52 | **[D]** DEFENCE DAY | — |

---

## Summary Milestones

| Week | Milestone |
|---|---|
| W4 | Simulation complete, NinaPro data explored |
| W8 | Single-channel sEMG proven on oscilloscope |
| W13 | PCB ordered, CNN trained |
| W18 | PCB assembled and validated |
| W20 | Full firmware pipeline: ADC → features → inference |
| W24 | **First live demo: electrode → gripper** |
| W28 | Calibration working |
| W32 | Android app functional |
| W36 | Integrated system polished |
| W40 | All 18 validation metrics documented |
| W44 | 5-subject experiment complete |
| W50 | Thesis submitted |
| W52 | **Defence** |

---

*Schedule assumes 5 working days per week. Adjust for exam periods, holidays, and Ketha work commitments.*
