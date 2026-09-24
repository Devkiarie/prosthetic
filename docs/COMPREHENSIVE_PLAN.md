# FYP Comprehensive Step-by-Step Plan
## Low-Cost Multichannel sEMG Control Interface for Upper-Limb Prosthetic Rehabilitation
## Ian Kiarie | ENE212-0069/2022 | JKUAT ECE

Every step below is modular, independently testable, and has a clear pass/fail gate.
No step depends on hardware you do not yet have unless marked.

---

## BLOCK A: FOUNDATIONS (Do first, no hardware needed)

### A1. Write and Submit 10-Page Proposal
- **Input:** Dr Irene's 6 feedback points, existing concept paper, literature
- **Output:** `docs/proposal/FYP_Proposal_Ian_Kiarie.pdf`
- **Steps:**
  1. Write Section 1: Background (~2 pages)
     - sEMG physiology, prosthetic control pipeline
     - Cost barrier: commercial ($10k-40k) vs this project (<$20 BOM)
     - Three enablers: ESP32-S3, TFLite Micro, recent 2025-2026 publications
  2. Write Section 2: Problem Statement (~1 page)
     - 30,000+ Kenyans needing upper-limb devices, <200 fitted/year at KNH
     - Research question: "Can a complete multichannel myoelectric control system be built for under $20 component cost?"
  3. Write Section 3: Main Objective (~0.5 page)
     - One sentence: design, implement, validate a 4-channel sEMG system on ESP32-S3, 8 gestures, >80% accuracy, <$20 BOM
  4. Write Section 4: Specific Objectives (5 items, ~1 page)
     - SO1: Design 4-channel analog front-end (INA128, BPF, notch) on custom PCB
     - SO2: Implement real-time DSP pipeline on ESP32-S3
     - SO3: Train, quantize, deploy 1D-CNN on TFLite Micro
     - SO4: Implement BLE calibration protocol (<3 min)
     - SO5: Integrate with 2-DOF gripper and validate with 5 subjects
  5. Write Section 5: Related Studies (~3 pages)
     - Verify ALL DOIs by web-searching title+authors before including
     - Cover: Salgado 2025, Mekruksavanich 2026, Lee 2025, Molinari 2026, Furtado 2026, Prakash 2026
     - Gap table: show what each study lacks that this project addresses
  6. Write Section 6: Methodology (~2 pages)
     - Phase diagram with 6 modules
     - Signal specification table (4-ch, 20-500Hz, 2kHz, 24 features)
     - 8 gesture table with functional relevance
     - Performance targets table
     - End demonstration description
  7. Generate PDF (reportlab or pandoc)
  8. Submit to Dr Irene
- **Verify:** PDF is 10 pages +/- 1, all DOIs checked, all 6 feedback points addressed

### A2. Standardize All Documents
- **Steps:**
  1. Fix INA128 gain: G=50 (R_G=1.02k) everywhere
  2. Fix op-amp: MCP6002 everywhere
  3. Fix features: MAV, RMS, WL, ZC, SSC, VAR (6 time-domain) everywhere
  4. Fix gesture 7: "Thumbs up" everywhere
  5. Replace all LTspice references with ngspice/KiCad SPICE
  6. Update wiki entity page
- **Verify:** grep for "TL072", "TL074", "G=500", "G = 500", "LTspice" should return zero hits

### A3. Set Up Project Folder Structure
- **Steps:**
  1. Create the full directory tree in `/home/kiarie/school/Final Year project/`:
     ```
     hardware/kicad/
     hardware/simulation/
     hardware/gerbers/
     hardware/bom/
     firmware/src/
     firmware/model/
     ml/notebooks/
     ml/data/ninapro_db5/
     ml/data/processed/
     ml/models/
     ml/scripts/
     android/
     mechanical/gripper_stl/
     mechanical/assembly/
     tests/afe_measurements/
     tests/filter_sweeps/
     tests/crosstalk/
     tests/ml_metrics/
     tests/subject_data/
     docs/proposal/
     docs/thesis/
     docs/figures/
     docs/lab-notes/
     docs/paper/
     docs/decisions/
     ```
  2. Initialize LOG.md in docs/lab-notes/
  3. Initialize DECISIONS.md in docs/decisions/
  4. Initialize PAPER_DRAFT.md in docs/paper/
- **Verify:** `find . -type d | wc -l` matches expected count

### A4. Set Up ML Python Environment
- **Steps:**
  1. Create venv: `python3 -m venv ml/venv`
  2. Install: `pip install tensorflow numpy scipy scikit-learn matplotlib jupyter pandas`
  3. Verify: `python3 -c "import tensorflow; print(tensorflow.__version__)"`
- **Verify:** All imports succeed, TF version >= 2.13

### A5. Download NinaPro DB5
- **Steps:**
  1. Register at https://ninapro.hevs.ch/ (free)
  2. Download DB5 Exercise 1 for all 10 subjects
  3. Store in `ml/data/ninapro_db5/`
  4. Verify: `ls ml/data/ninapro_db5/*.mat | wc -l` should be >= 10
- **Verify:** Can load with scipy.io.loadmat, print shapes

### A6. Download SPICE Models
- **Steps:**
  1. Download INA128 SPICE model from TI: https://www.ti.com/product/INA128
  2. Download MCP6002 SPICE model from Microchip
  3. Store in `hardware/simulation/models/`
  4. Test: `ngspice -b -r test.raw hardware/simulation/test_ina128.cir`
- **Verify:** ngspice loads models without errors

---

## BLOCK B: SIMULATION (Computer only, no hardware)

### B1. Simulate INA128 Gain Stage
- **Tool:** ngspice or KiCad SPICE
- **Steps:**
  1. Create netlist: INA128 with R_G=1.02k, single 5V supply, Vref=2.5V
  2. AC analysis 1Hz-100kHz: verify flat gain of ~34dB (50x)
  3. Differential test: 1mV 100Hz input, verify ~50mV output
  4. CMRR test: 500mV common-mode 50Hz, verify output < 0.5mV
  5. Save Bode plot as PNG
- **Pass:** Gain = 50 +/-1%, CMRR > 60dB at 50Hz
- **Simulatable:** YES

### B2. Simulate Sallen-Key HPF (20 Hz)
- **Steps:**
  1. Netlist: MCP6002, C1=C2=470nF, R1=12k, R2=24k
  2. AC analysis 0.1Hz-10kHz
  3. Record -3dB frequency
  4. Save Bode plot
- **Pass:** -3dB at 20Hz +/-15%, -40dB/decade below
- **Simulatable:** YES

### B3. Simulate Sallen-Key LPF (500 Hz)
- **Steps:**
  1. Netlist: MCP6002, R1=R2=10k, C1=22nF, C2=47nF
  2. AC analysis 10Hz-100kHz
  3. Record -3dB frequency
  4. Save Bode plot
- **Pass:** -3dB at 495Hz +/-10%, -40dB/decade above
- **Simulatable:** YES

### B4. Simulate Twin-T Notch (50 Hz)
- **Steps:**
  1. Netlist: R=10k, C=330nF, R/2=5k, 2C=680nF
  2. AC analysis 10Hz-200Hz (narrow range)
  3. Measure rejection depth at 50Hz
  4. Measure notch bandwidth
  5. Save Bode plot
- **Pass:** Rejection > 30dB at 50Hz, signals at 40Hz and 60Hz < 1dB attenuation
- **Simulatable:** YES

### B5. Simulate Complete Single-Channel Chain
- **Steps:**
  1. Cascade: INA128 -> HPF -> LPF -> Notch -> Variable Gain -> Divider
  2. AC analysis 1Hz-10kHz
  3. Combined Bode plot (magnitude + phase)
  4. Transient analysis: inject 1mV 100Hz + 500mV 50Hz, verify output
- **Pass:** Bandpass 20-500Hz, 50Hz null, signal fills ADC range
- **Simulatable:** YES

### B6. Monte Carlo Tolerance Analysis
- **Steps:**
  1. Add 5% tolerance on all capacitors, 1% on resistors
  2. Run 100 iterations
  3. Plot spread of HPF fc, LPF fc, notch fc
  4. Identify which components need tighter tolerance
- **Pass:** All 100 runs stay within spec (HPF 17-23Hz, LPF 450-550Hz, notch 47-53Hz)
- **Simulatable:** YES

### B7. Generate KiCad Schematic (no PCB yet)
- **Steps:**
  1. Open KiCad, create project `semg_afe_v1`
  2. Draw single-channel schematic from verified simulation values
  3. Replicate x4 as hierarchical sheets
  4. Draw power supply sheet
  5. Draw ESP32-S3 pin assignment sheet
  6. Draw PCA9685 + servo sheet
  7. Run ERC, fix all errors
  8. Export schematic PDF
- **Pass:** ERC zero errors, all nets connected, all values annotated
- **Simulatable:** N/A (design task)

---

## BLOCK C: MACHINE LEARNING (Computer only, parallel with Block B)

### C1. Explore NinaPro DB5
- **Steps:**
  1. Open Jupyter, load S1_E1_A1.mat
  2. Print shapes: emg, stimulus, repetition
  3. Plot raw sEMG for each of the 8 target gestures
  4. Plot power spectral density (confirm 20-500Hz band)
  5. Document observations in notebook
- **Pass:** All 8 gesture classes identifiable visually
- **Simulatable:** YES (pure Python)

### C2. Feature Extraction Pipeline
- **Steps:**
  1. Write `features.py` reference implementation (6 features x 4 channels = 24)
  2. Process all 10 subjects
  3. Save X_features.npy, y_labels.npy, subjects.npy
  4. Print class distribution (check for imbalance)
  5. Visualize features with t-SNE or PCA (are classes separable?)
- **Pass:** Feature matrix shape (N, 24), 8 classes present, class distribution documented
- **Simulatable:** YES

### C3. Baseline Classifiers (LDA, SVM, RF)
- **Steps:**
  1. Implement Leave-One-Subject-Out cross-validation
  2. Train LDA, SVM(RBF), Random Forest
  3. Report LOSO macro F1 for each
  4. Save results for comparison table in thesis
- **Pass:** At least one baseline achieves F1 > 0.70
- **Simulatable:** YES

### C4. Train 1D-CNN
- **Steps:**
  1. Build model: Conv1D(32,5) -> Conv1D(64,3) -> Conv1D(32,3) -> GAP -> Dense(64) -> Dense(8)
  2. LOSO cross-validation (10 folds)
  3. Report per-fold and mean F1
  4. Generate confusion matrix (aggregate across folds)
  5. Generate classification report
- **Pass:** Mean LOSO F1 > 0.85
- **Simulatable:** YES

### C5. INT8 Quantization
- **Steps:**
  1. Train final model on ALL subjects
  2. Convert to INT8 TFLite using representative dataset
  3. Measure model size
  4. Verify quantized accuracy vs float32
  5. Export: .tflite file, C array, scaler parameters
- **Pass:** Model < 100KB, accuracy drop < 3%
- **Simulatable:** YES

### C6. Firmware Feature Extraction (C implementation)
- **Steps:**
  1. Write dsp_features.c matching Python reference exactly
  2. Test with recorded NinaPro windows injected via serial
  3. Compare C output vs Python output feature-by-feature
  4. Measure execution time on ESP32-S3
- **Pass:** Max error < 1%, execution time < 25ms
- **Requires:** ESP32-S3 dev board

---

## BLOCK D: HARDWARE BUILD (Requires components)

### D1. Component Procurement
- **Steps:**
  1. Order from Mouser: INA128 x5, MCP6002 x10, precision resistors, film caps
  2. Buy locally: ESP32-S3 DevKitC, breadboards, wires, batteries, electrodes
  3. Order JLCPCB: submit Gerbers after Block B7 is complete
- **Pass:** All components received, inventory checked against BOM
- **Requires:** Money + ordering lead time

### D2. Single-Channel Breadboard Build
- **Steps:**
  1. Build INA128 gain stage alone, verify with signal generator
  2. Add HPF, verify -3dB at 20Hz
  3. Add LPF, verify -3dB at 500Hz
  4. Add twin-T notch, trim for 50Hz rejection > 30dB
  5. Add variable gain stage
  6. Add divider + protection for ESP32 ADC
  7. Pre-flight safety check (battery only, no USB with electrodes)
  8. Record first real sEMG from forearm
  9. Measure SNR: 10s rest vs 10s contraction
- **Pass:** SNR > 20dB, bandpass 20-500Hz +/-3dB, 50Hz rejection > 30dB
- **Requires:** All analog components, oscilloscope (JKUAT lab)

### D3. ESP32-S3 ADC Bring-Up
- **Steps:**
  1. Flash PlatformIO firmware skeleton
  2. Configure ADC1 (4 channels, 12-bit, DMA)
  3. Timer-triggered sampling at 2kHz
  4. Verify sampling rate with timestamp analysis
  5. Measure ADC noise floor (inputs shorted to Vref)
  6. Stream raw data to PC via UART
  7. Run Python receiver, save CSV
  8. 10-minute continuous test for dropped samples
- **Pass:** Rate = 2000Hz +/-1Hz, noise < 5 LSB RMS, zero drops in 10 min
- **Requires:** ESP32-S3 DevKitC

### D4. PCB Design, Fabrication, Assembly
- **Steps:**
  1. Complete PCB layout in KiCad (from Block B7 schematic)
  2. Run DRC, fix all errors
  3. Generate Gerbers, review in online viewer
  4. Order from JLCPCB (4-layer, 5 pcs, DHL shipping)
  5. Receive and inspect PCB
  6. Solder power section first, test all rails
  7. Solder Channel 1, test (same as D2 tests)
  8. Solder Channels 2-4, test each
  9. Crosstalk test: all 6 channel pairs
  10. Supply ripple test under full load
- **Pass:** All 4 channels within spec, crosstalk < -40dB, ripple < 10mV
- **Requires:** PCB from JLCPCB (~2 weeks), soldering equipment

### D5. Integrated ADC + PCB Test
- **Steps:**
  1. Connect ESP32-S3 to PCB via header
  2. All 4 channels sampling at 2kHz
  3. Record 4-channel sEMG from forearm
  4. Python analysis: plot all 4 channels, verify independence
  5. Feature extraction on live data
- **Pass:** Clean 4-channel sEMG, features match Python reference
- **Requires:** Completed PCB + ESP32-S3

---

## BLOCK E: FIRMWARE INTEGRATION (Requires ESP32 + PCB)

### E1. Deploy TFLite Model on ESP32
- **Steps:**
  1. Include semg_model_data.cc and scaler_params.h in firmware
  2. Initialize TFLite Micro interpreter
  3. Feed test vectors, compare predictions to Python
  4. Measure inference latency
- **Pass:** Predictions match Python, latency < 50ms, no allocation errors

### E2. Full Pipeline: ADC -> DSP -> Inference -> Serial
- **Steps:**
  1. Wire FreeRTOS tasks: ADC (Core 0) -> DSP (Core 0) -> Inference (Core 1)
  2. Feature queue between DSP and inference
  3. Output gesture + confidence on serial
  4. 30-minute stability test
- **Pass:** Runs 30 min without crash, memory stable, predictions reasonable

### E3. BLE GATT Service
- **Steps:**
  1. Implement NimBLE GATT server with 6 characteristics
  2. Advertise as "sEMG-FYP"
  3. Test with nRF Connect app on phone
  4. Verify: gesture notify at 10Hz, signal quality at 1Hz
  5. Implement control commands (start/stop calibration)
- **Pass:** Pairing < 5s, update rate > 10Hz, packet loss < 1%

### E4. Actuator Integration
- **Steps:**
  1. Wire PCA9685 to ESP32 via I2C
  2. Wire 2x MG996R servos to PCA9685
  3. Test all 8 gesture positions via serial commands
  4. Measure servo response time
  5. 3D print gripper, attach to servos
  6. Grip force test (500g object)
  7. Endurance test (200 cycles)
- **Pass:** Response < 200ms, holds 500g, survives 200 cycles

### E5. Decision Logic Layer
- **Steps:**
  1. Implement confidence threshold (60%)
  2. Implement debounce (3 consecutive frames)
  3. Implement hold timeout (2s revert to rest)
  4. Test: system stable during rest (no random actuations)
  5. Test: clean transitions between gestures
- **Pass:** No false activations in 5 min of rest, transitions < 0.75s

---

## BLOCK F: CALIBRATION AND APP (Requires BLE working)

### F1. Calibration Protocol
- **Steps:**
  1. Define guided session: 8 gestures x 3 reps x 5s = ~3.2 min
  2. Implement on ESP32: calibration mode collects features + labels
  3. Fine-tune last Dense layer (freeze rest)
  4. Measure accuracy before/after calibration on held-out NinaPro subject
- **Pass:** Calibration < 3 min, accuracy improvement > 10%

### F2. Mobile App (BLE interface)
- **Steps:**
  1. Choose platform (recommend Web Bluetooth PWA for simplicity)
  2. Implement: scan, connect, display live gesture + confidence
  3. Signal quality bars (4 channels)
  4. Calibration wizard with countdown and progress
  5. Test on physical Android phone
- **Pass:** Connects, displays gestures in real-time, calibration wizard works

---

## BLOCK G: VALIDATION (Requires complete integrated system)

### G1. Full Electrical Validation (18 metrics)
- **Steps:**
  1. Measure all 18 engineering metrics from the master plan
  2. Document each: setup, procedure, raw data, result, pass/fail
  3. Generate Bode plot, crosstalk matrix, performance table
  4. All results feed directly into thesis Chapter 5
- **Pass:** 16+ of 18 metrics pass (critical ones: SNR, F1, latency, cost)

### G2. Five-Subject Experiment
- **Steps:**
  1. Write informed consent form, get supervisor approval
  2. Recruit 5 JKUAT students
  3. Per subject: setup (10min) -> calibrate (3min) -> record (20min) -> demo (5min) -> teardown (5min)
  4. Collect: raw data, features, predictions, confusion matrices, latency, video
  5. Analyze: per-subject accuracy/F1/latency, aggregate confusion matrix
  6. Generate thesis tables and figures
- **Pass:** Mean accuracy > 80%, mean F1 > 0.80, mean latency < 50ms

### G3. Cost Demonstration
- **Steps:**
  1. Photograph all components with prices
  2. Create Tier A (electronics only) cost breakdown
  3. Create Tier C (volume 100+) cost projection
  4. Compare against literature (table: this project vs Salgado vs Molinari vs commercial)
- **Pass:** Tier A < $20, Tier C < $15, clearly cheaper than all literature

---

## BLOCK H: DOCUMENTATION (Parallel throughout)

### H1. Thesis Writing (fill as you go)
- Chapter 1: Introduction (after A1)
- Chapter 2: Literature Review (after A1)
- Chapter 3: System Design (after B7)
- Chapter 4: Implementation (after E5)
- Chapter 5: Results (after G2)
- Chapter 6: Conclusion (after G2)

### H2. Conference Paper Draft
- Target: IEEE AFRICON 2027 or IEEE EMBC 2027
- Draft after G2 results are in

---

## Dependency Graph (what blocks what)

```
A1 (Proposal) ---------> Submit to Dr Irene (unblocks everything else)
A2-A6 (Setup) ---------> All subsequent blocks

B1-B6 (Simulation) ----> B7 (Schematic) ----> D4 (PCB order)

C1-C5 (ML training) ---> C6 (C firmware) ---> E1 (Deploy model)

D1 (Procurement) ------> D2 (Breadboard) ---> D4 (PCB) ---> D5 (Integrated)
D1 -----> D3 (ESP32 ADC)

D5 + E1 + E2 -----> E3 (BLE) + E4 (Actuator) + E5 (Decision)

E3 + E5 -----> F1 (Calibration) + F2 (App)

All E + F -----> G1 (Validation) + G2 (Experiment)

G1 + G2 -----> H1 (Thesis) + H2 (Paper)
```

## What Can Be Simulated on Linux

| Task | Tool | Installed? |
|---|---|---|
| Analog circuit simulation | ngspice | YES |
| Schematic + simulation | KiCad 7 SPICE | YES |
| ML model training | TensorFlow/Python | NEEDS TF INSTALL |
| Feature extraction validation | Python + numpy | YES |
| DSP C code compile test | GCC (cross-compile) | YES (PlatformIO) |
| PCB design + DRC | KiCad 7 | YES |
| BLE testing | nRF Connect (phone) | Need phone |
| Android app dev | Android Studio | NOT INSTALLED (use Web BT instead) |
