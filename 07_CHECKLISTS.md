# Master Checklists
## Phase-by-Phase Pass/Fail Verification

Print this document. Check off each item with a pen as you complete it. Do not proceed to the next phase until ALL items in the current phase are checked.

---

## Phase 0 — Requirements & Safety (Week 1–2)

- [ ] Requirements document written and frozen (no changes after this without supervisor approval)
- [ ] Safety analysis written: battery-only rule, input protection, isolation
- [ ] Input protection circuit designed: 10 kΩ series + BAV99 per input
- [ ] Risk register created with 10+ risks, mitigations assigned
- [ ] Components ordered from Mouser (INA128, MCP6002, precision resistors, film caps)
- [ ] Local components purchased (ESP32-S3, breadboard, wire, batteries)
- [ ] KiCad 8 installed
- [ ] LTspice installed + INA128/MCP6002 SPICE models downloaded
- [ ] PlatformIO installed in VS Code
- [ ] Python 3.10+ with numpy, scipy, scikit-learn, tensorflow, matplotlib

---

## Phase 1 — Single-Channel Proof (Week 3–8)

### INA128 Bring-Up
- [ ] INA128 powered: Vcc = 5V, output = Vref ±50 mV with inputs shorted
- [ ] R_G = 1.02 kΩ installed (G = 50)
- [ ] Signal generator test: 100 Hz / 1 mV input → ~50 mV output (gain verified)
- [ ] CMRR test: 50 Hz common-mode 500 mV → output < 5 mV

### Filter Stages
- [ ] HPF assembled: Sallen-Key, C=470 nF, R1=12 kΩ, R2=24 kΩ
- [ ] HPF verified: -3 dB at 20 Hz ±15%
- [ ] LPF assembled: Sallen-Key, R=10 kΩ, C1=22 nF, C2=47 nF
- [ ] LPF verified: -3 dB at 500 Hz ±15%
- [ ] Twin-T notch assembled: R=10 kΩ, C=330 nF, 2C=680 nF, R/2=5 kΩ trimmer
- [ ] Notch trimmed: 50 Hz rejection > 30 dB
- [ ] Variable gain stage assembled: MCP6002 + 100 kΩ trimmer
- [ ] ADC divider + protection assembled

### First Real sEMG
- [ ] Pre-flight safety check passed (battery only, no USB during human testing)
- [ ] Electrodes placed on forearm (Channel 1: flexor carpi radialis)
- [ ] Skin prepped: alcohol + abrasion + 2 min stabilisation
- [ ] sEMG visible on oscilloscope: clear difference between rest and contraction
- [ ] **SNR measured: ______ dB (target: > 20 dB)** → PASS / FAIL
- [ ] **Bandpass -3 dB points: ______ Hz and ______ Hz (target: 20 and 500)** → PASS / FAIL
- [ ] **50 Hz rejection: ______ dB (target: > 30 dB)** → PASS / FAIL
- [ ] Oscilloscope captures saved (rest, contraction, frequency sweep, notch)
- [ ] Phase 1 results documented in lab notebook

---

## Phase 2–3 — AFE Design & Simulation (Week 6–12)

### Simulation
- [ ] INA128 simulation: gain = 50 ±1%, CMRR > 60 dB at 50 Hz
- [ ] HPF simulation: -3 dB at 20 Hz, -40 dB/decade below
- [ ] LPF simulation: -3 dB at 500 Hz, -40 dB/decade above
- [ ] Notch simulation: rejection > 30 dB at 50 Hz
- [ ] Combined Bode plot: bandpass 20–500 Hz with 50 Hz null
- [ ] Monte Carlo analysis: 100 runs, spread of cutoff frequencies documented
- [ ] All .asc simulation files saved in `hardware/simulation/`
- [ ] Bode plots exported as PNG for thesis

### Schematic
- [ ] Complete single-channel schematic in KiCad (all stages)
- [ ] Schematic replicated ×4 as hierarchical sheets
- [ ] Power supply schematic (LDO, Vref, decoupling)
- [ ] ESP32-S3 pin assignment sheet
- [ ] PCA9685 + servo connector sheet
- [ ] ERC (Electrical Rules Check) passes with zero errors
- [ ] Schematic exported as PDF

---

## Phase 4 — 4-Channel PCB (Week 13–18)

### Layout
- [ ] 4-layer stackup defined (signal / GND / power / digital)
- [ ] Component placement in zones: input | analog | digital | power
- [ ] Channel layouts are symmetric (matched trace lengths)
- [ ] INA128 input traces: short, guarded, differential pair
- [ ] All decoupling caps within 3 mm of IC power pins
- [ ] No right-angle bends on signal traces
- [ ] 10 mm antenna keep-out zone on ESP32-S3
- [ ] Test points labeled and accessible
- [ ] DRC passes with zero errors
- [ ] 3D view reviewed for component clearances

### Fabrication
- [ ] Gerber files generated and reviewed in online viewer
- [ ] PCB ordered from JLCPCB (4-layer, 5 pcs)
- [ ] Tracking number received, estimated delivery date noted

### Assembly & Test
- [ ] PCB received and visually inspected
- [ ] Power section soldered and tested (all rails correct)
- [ ] Channel 1 soldered and tested (gain, bandpass, notch)
- [ ] Channel 2 soldered and tested
- [ ] Channel 3 soldered and tested
- [ ] Channel 4 soldered and tested
- [ ] **Crosstalk Ch1→Ch2: ______ dB (target: < -40 dB)** → PASS / FAIL
- [ ] **Crosstalk Ch1→Ch3: ______ dB** → PASS / FAIL
- [ ] **Crosstalk Ch1→Ch4: ______ dB** → PASS / FAIL
- [ ] **Crosstalk Ch2→Ch3: ______ dB** → PASS / FAIL
- [ ] **Crosstalk Ch2→Ch4: ______ dB** → PASS / FAIL
- [ ] **Crosstalk Ch3→Ch4: ______ dB** → PASS / FAIL
- [ ] **Supply ripple: ______ mV p-p (target: < 10 mV)** → PASS / FAIL
- [ ] ESP32 header soldered, ADC reads from PCB output
- [ ] PCB photos saved for thesis

---

## Phase 5 — ADC Acquisition (Week 17–22)

- [ ] ADC configured: 4 channels, ADC1, 12-bit, attenuation DB_12
- [ ] Hardware timer running at 2 kHz (500 µs period)
- [ ] All 4 channels sampled per timer tick
- [ ] **Actual sampling rate: ______ Hz (target: 2000 ±1)** → PASS / FAIL
- [ ] **ADC noise floor (inputs to Vref): ______ LSB RMS (target: < 5)** → PASS / FAIL
- [ ] Raw data streams to PC via UART
- [ ] Python receiver script captures and saves CSV
- [ ] **Zero dropped samples over 10 minutes (2400 windows)** → PASS / FAIL
- [ ] ADC calibration enabled (ESP-IDF line fitting)

---

## Phase 6 — DSP Pipeline (Week 22–26)

- [ ] Python reference implementation written and tested on NinaPro data
- [ ] C implementation compiles on ESP32-S3
- [ ] **Feature error (C vs Python): ______ % max (target: < 1%)** → PASS / FAIL
- [ ] **Feature extraction time: ______ ms (target: < 25 ms)** → PASS / FAIL
- [ ] **CPU utilisation (sampling + features): ______ % (target: < 60%)** → PASS / FAIL
- [ ] TFLite Micro model loads without allocation errors
- [ ] **Inference latency: ______ ms (target: < 50 ms)** → PASS / FAIL
- [ ] Predictions match Python on test vectors
- [ ] Full pipeline running: ADC → features → inference → serial output
- [ ] Runs 30 minutes without crash or memory leak

---

## Phase 7 — ML Training (Week 26–32)

- [ ] NinaPro DB5 downloaded (10 subjects)
- [ ] 8 gesture classes selected and mapped
- [ ] Feature matrix shape: (N, 24) — verified
- [ ] LDA LOSO F1: ______ (record for comparison table)
- [ ] SVM LOSO F1: ______ (record)
- [ ] Random Forest LOSO F1: ______ (record)
- [ ] 1D-CNN LOSO F1: ______ (target: > 0.85)
- [ ] Confusion matrix plotted and saved
- [ ] Classification report generated (per-class precision, recall, F1)
- [ ] Final model trained on ALL subjects
- [ ] **INT8 model size: ______ KB (target: < 100 KB)** → PASS / FAIL
- [ ] **Quantisation accuracy drop: ______ % (target: < 3%)** → PASS / FAIL
- [ ] TFLite model converted to C array
- [ ] Scaler parameters exported to C header

---

## Phase 8 — Calibration (Week 32–36)

- [ ] Calibration protocol defined: 8 gestures × 3 reps × 5 sec
- [ ] **Calibration total time: ______ seconds (target: < 180)** → PASS / FAIL
- [ ] Fine-tuning code tested on held-out NinaPro subject
- [ ] **Pre-calibration accuracy on new user: ______ %**
- [ ] **Post-calibration accuracy on same user: ______ %**
- [ ] **Improvement: ______ % (target: > 10%)** → PASS / FAIL
- [ ] Calibrated model re-quantised and verified

---

## Phase 9 — BLE App (Week 34–38)

- [ ] ESP32 BLE GATT service running (6 characteristics)
- [ ] ESP32 advertises as "sEMG-FYP"
- [ ] **BLE pairing time: ______ seconds (target: < 5)** → PASS / FAIL
- [ ] Android app connects and displays live gesture
- [ ] **Gesture update rate: ______ Hz (target: > 10)** → PASS / FAIL
- [ ] Confidence percentage displayed
- [ ] Signal quality bars show per-channel status
- [ ] Calibration wizard functional (guided session)
- [ ] App handles disconnection (auto-reconnect)
- [ ] Tested on physical Android device (not just emulator)

---

## Phase 10 — Actuator (Week 36–40)

- [ ] PCA9685 detected on I2C bus (addr 0x40)
- [ ] Both servos move to all 8 gesture positions
- [ ] **Servo response time: ______ ms (target: < 200 ms)** → PASS / FAIL
- [ ] Gripper 3D printed, assembled, attached to servos
- [ ] **Gripper holds 500g object: YES / NO** → PASS / FAIL
- [ ] **Gripper survives 200 open/close cycles: YES / NO** → PASS / FAIL
- [ ] No ADC noise increase when servos active (separate power verified)
- [ ] Emergency stop function tested and working

---

## Phase 11 — Decision Logic (Week 40–42)

- [ ] Confidence threshold prevents firing during rest (< 60% → hold)
- [ ] Debounce requires 3 consecutive same-class frames
- [ ] Hold timeout reverts to rest after 2s of low confidence
- [ ] System never actuates on < 60% confidence (tested over 5 minutes)
- [ ] Gesture transitions take < 0.75 seconds
- [ ] Emergency stop accessible via BLE command

---

## Phase 12 — Validation (Week 42–46)

| # | Metric | Target | Measured | Pass/Fail |
|---|---|---|---|---|
| E1 | Single-channel SNR | > 20 dB | ______ | |
| E2 | Bandpass response | 20–500 Hz ±3 dB | ______ | |
| E3 | 50 Hz rejection | > 30 dB | ______ | |
| E4 | Inter-channel crosstalk | < -40 dB | ______ | |
| E5 | Analog supply ripple | < 10 mV | ______ | |
| E6 | Sampling rate | 2000 Hz ±1 | ______ | |
| E7 | Feature error vs Python | < 1% | ______ | |
| E8 | Feature extraction time | < 25 ms | ______ | |
| E9 | Offline classifier F1 | > 0.85 | ______ | |
| E10 | INT8 model size | < 100 KB | ______ | |
| E11 | Inference latency | < 50 ms | ______ | |
| E12 | Real-time accuracy (5 subj) | > 80% | ______ | |
| E13 | Calibration duration | < 3 min | ______ | |
| E14 | Post-cal improvement | > 10% | ______ | |
| E15 | Servo response time | < 200 ms | ______ | |
| E16 | Gripper endurance | > 200 cycles | ______ | |
| E17 | Continuous operation | > 60 min | ______ | |
| E18 | Total component cost | < KES 8,500 | ______ | |

**Total passed: ______ / 18**

---

## Phase 13 — 5-Subject Experiment (Week 46–50)

- [ ] Consent form written and approved by supervisor
- [ ] Subject 1: consent signed, experiment completed, data saved
- [ ] Subject 2: consent signed, experiment completed, data saved
- [ ] Subject 3: consent signed, experiment completed, data saved
- [ ] Subject 4: consent signed, experiment completed, data saved
- [ ] Subject 5: consent signed, experiment completed, data saved
- [ ] Per-subject confusion matrices generated (5 PNGs)
- [ ] Aggregate confusion matrix generated
- [ ] Per-subject results table complete (accuracy, F1, latency)
- [ ] **Mean accuracy across 5 subjects: ______ % (target: > 80%)** → PASS / FAIL
- [ ] **Mean F1 across 5 subjects: ______ (target: > 0.80)** → PASS / FAIL
- [ ] **Mean latency: ______ ms (target: < 50 ms)** → PASS / FAIL
- [ ] Video demonstration recorded (at least 2 subjects)
- [ ] All raw data committed to `tests/subject_data/`

---

## Thesis (Week 45–52)

- [ ] Chapter 1: Introduction (problem, objectives, scope)
- [ ] Chapter 2: Literature Review (15+ IEEE references)
- [ ] Chapter 3: System Design (all schematics, block diagrams)
- [ ] Chapter 4: Implementation (PCB, firmware, ML, app)
- [ ] Chapter 5: Results (all 18 metrics, confusion matrices, subject data)
- [ ] Chapter 6: Conclusion (achievements, limitations, future work)
- [ ] Abstract (250 words)
- [ ] References (IEEE format)
- [ ] Appendices (full schematics, BOM, code listings, raw data)
- [ ] Table of Contents generated
- [ ] Figures: Bode plot, PCB layout, oscilloscope traces, confusion matrices, prototype photo
- [ ] Draft submitted to supervisor
- [ ] Supervisor feedback incorporated
- [ ] Final version printed and bound
- [ ] 15-minute defence presentation prepared
- [ ] Defence rehearsed (at least twice, timed)

---

*Print this document at project start. Pin it on your wall. Check off items with a pen as you go.*
