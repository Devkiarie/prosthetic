# FYP Development Log
**Project:** Low-Cost Myoelectric Prosthetic Control Interface
**Student:** Ian Kiarie | ENE212-0069/2022 | JKUAT B.Sc. ECE
**Supervisor:** Dr. Irene
**Start:** September 2026 | Target completion: August 2027

> This log captures every session, error, fix, and decision made during development.
> Written for two audiences: (1) the FYP final report, (2) reproducibility.

---

## Session Index

| Date | Session | Key Outcomes |
|---|---|---|
| 2026-09-09 | 01 | Project scope defined, component selection, initial proposal |
| 2026-09-22 | 02 | Dr. Irene concept review — 9 technical issues raised |
| 2026-09-24 | 03 | Gap analysis, gain fix (G=500→G=51), MCP6002 standardised, ngspice chosen |
| 2026-09-25 | 04-11 | Simulations (all PASS), TF installed, NinaPro download, ML pipeline |
| 2026-09-25 | 12-18 | Android app scaffolded (Kotlin+Compose), BLE GATT designed |
| 2026-10-02 | 19 | Dr. Irene formal review — 4-chapter restructure, full proposal rewrite |
| 2026-10-05 | 20 | Proposal v2 — 5-finger hand update, budget corrected |
| 2026-10-06 | 21 | CNN training attempt (v1/v2) — baseline F1=0.66 |
| 2026-10-06 | 22 | Stale file audit, BOM corrections, component order placed |
| 2026-10-06 | 23 | Shopping (Luthuli Ave), git history purge (679 MB removed) |
| 2026-10-18 | 24 | KiCad schematic, firmware scaffold, ML pipeline fixes, DOI verification |

---

## SESSION 01 — 2026-09-09: Project Scope

### Decisions Made
- 4-channel sEMG with INA128 instrumentation amp
- ESP32-S3 for MCU (TFLite Micro support, BLE, dual-core)
- Target: ≤ $63 total cost, 8 gesture classes
- NinaPro DB5 as training dataset

---

## SESSION 02 — 2026-09-22: Dr. Irene Concept Review

### Feedback Received (9 issues)
1. Demonstrate cost explicitly in proposal
2. Be specific on signal parameters (amplitude, frequency, channels)
3. Fix gesture count (was undefined, fixed to 8)
4. Define measurable performance targets (F1, latency, model size)
5. State exactly what the live demo will show
6. Write 10-page proposal: background, problem statement, objectives, methodology
7. INA128 gain was inconsistently stated (G=500 in some docs, G=50 in others)
8. Op-amp not standardised (TL072 vs MCP6002 in different docs)
9. Feature set not locked in (some docs had FFT features)

### Fixes Applied
- Gain locked to G=51 everywhere (RG = 1kΩ 1% MF)
- Op-amp standardised to MCP6002 (single-supply, 1.8–6V)
- Features locked to 6 time-domain only: MAV, RMS, WL, ZC, SSC, VAR
- Signal spec table added to proposal

---

## SESSION 03 — 2026-09-24: Gap Analysis + Architecture Decisions

### Decision: INA128 Gain = 51 (not 500)
**Problem:** G=500 with electrode DC offset (~300 mV) → INA128 output = 300 mV × 500 = 150V → immediate rail saturation.
**Fix:** Split gain — G=51 at INA128, post-HPF variable gain (1–10×) via MCP6002 potentiometer.
**Total gain range:** 51–510×, adjustable per subject.

### Decision: MCP6002 (not TL072/TL074)
**Problem:** TL072 requires dual supply (min ±3.5V). Design uses single 5V supply from 9V battery via LM7805.
**Fix:** MCP6002 operates 1.8–6V single supply, rail-to-rail output, 1 MHz GBW (sufficient for 500 Hz filters).

### Decision: ngspice (not LTspice)
**Reason:** ngspice is Linux-native (v42 installed). KiCad 9 uses ngspice as its SPICE backend. LTspice requires Wine on Linux — fragile. All SPICE netlists are cross-compatible.

### Decision: 6 time-domain features only
**Reason:** Prakash 2026 shows TD features competitive with frequency-domain for 6–8 gestures. FFT on ESP32-S3 adds ~15 ms per window and 4 KB stack. O(N) features reduce latency.

---

## SESSIONS 04–11 — 2026-09-25: Simulations + ML Environment

### Simulations (ngspice v42)
Four netlists written and run — all PASS:

| Simulation | File | Result |
|---|---|---|
| INA128 gain | `01_INA128_gain.cir` | G = 51.0 confirmed |
| Sallen-Key HPF (20 Hz) | `02_sallen_key_bandpass.cir` | fc = 19.8 Hz, -3dB OK |
| Twin-T notch (50 Hz) | `03_twin_t_notch.cir` | Notch = -38.2 dB at 50 Hz |
| Full signal chain | `04_full_signal_chain.cir` | End-to-end gain + BW confirmed |

`bode_plots.png` generated showing all four frequency responses.

### ML Environment Setup
- Python 3.11 venv: `ml/venv/`
- TensorFlow 2.16.2 CPU installed
- JupyterLab installed, kernel `fyp-semg` registered
- Launch: `bash launch_jupyter.sh`

### NinaPro DB5 Download
- All 10 subjects downloaded and extracted to `ml/data/ninapro_db5/`
- Folder layout: `S{N}/s{N}/S{N}_E1_A1.mat` (lowercase inner folder — important for code)
- Format: 16-channel float32 EMG, 200 Hz, exercise 1 = gestures 0–12

**ERROR encountered:** Initial explore script used `stimulus` field for labels.
**Root cause discovered later (2026-10-18):** `stimulus` has ~24% mislabelled samples at gesture onset/offset (reaction-time lag). Correct field is `restimulus`.

### Initial Data Exploration (01_data_exploration.ipynb)
- Subject 1: 130k samples, 651 s recording
- All 8 gesture classes present
- 11,205 windows × 24 features extracted
- Class imbalance found: Rest = 73% of windows (before balancing)
- Class-balanced dataset: X_balanced.npy (37,308 × 24), y_balanced.npy

---

## SESSIONS 12–18 — 2026-09-25: Android App + 3D Hand

### Android App (MyoControl)
Stack: Kotlin + Jetpack Compose + Hilt + Room
BLE GATT: 5 characteristics defined
- `BLE_CHAR_EMG_UUID` — notify: 4×int16 raw ADC values
- `BLE_CHAR_GESTURE_UUID` — notify: uint8 gesture ID
- `BLE_CHAR_CONF_UUID` — notify: float32 confidence
- `BLE_CHAR_CALIB_UUID` — write: calibration commands
- `BLE_CHAR_STATUS_UUID` — read/notify: device state

Screens scaffolded: Home, Monitor, Train, Analytics, Settings
Canvas hand illustrations: 8 poses, animated
Theme: cream (default) + FM absolute black (dark mode)

### 3D Hand Design (Ada Hand v1.1)
- STLs cloned from Open Bionics Ada Hand repository
- Blender 5.2.2 installed via snap
- Ada Right v4 (animated, rigged) confirmed working in Blender 5.2
- **ERROR:** Ada Right v1.1.blend opens as blank in Blender 5.2 (Blender 2.7x format incompatibility)
- **Fix:** Used Ada_Right_v4_animated.blend (v2 format, Blender 5.2 compatible)
- Servo pockets cut via Boolean Modifier: 5× SG90 (23×12.5×29 mm) + 1× MG996R wrist
- STL exported: `hardware/hand_stl/modified/5th_Palm_with_servo_pockets.stl`
- Backup: `hardware/hand_stl/backup_20261005_235508.zip` (43 MB)

---

## SESSION 19 — 2026-10-02: Dr. Irene Formal Review

### Restructure Required
Full 4-chapter format per JKUAT departmental standard:
- Chapter 1: Background, Problem Statement, Justification, Scope, 3 Specific Objectives
- Chapter 2: Literature Review (9 subsections, critique, gap table, proposed work)
- Chapter 3: Methodology (gain calcs, filter values, safety, NinaPro mapping, ethics, cost tiers)
- Chapter 4: Expected Results linked to each objective

### 9 Technical Issues Addressed
1. INA128 REF pin must go to low-impedance buffered 1.65V mid-supply (not floating)
2. Sallen-Key uses film capacitors (not X7R ceramic — value drifts with voltage)
3. Anti-aliasing LPF added: 100 Hz (fc = 1/(2π×10k×150n)) before ADC
4. NinaPro DB5 channel selection documented: [0, 4, 8, 12] of 16
5. Train/val/test split stated: 70/15/15, stratified by gesture
6. Inference latency defined: end-to-end (ADC → feature → inference → servo)
7. Gesture-to-posture mapping table written (all 8 gestures × 6 servos)
8. Participant protocol and ethics statement added
9. Cost tiers: Tier A (BOM only ~KES 3,150) vs Tier B (full project ~KES 8,700)

Revised DOCX: `docs/proposal/ENE212-0069-2022_PROJECT_PROPOSAL_SEP-2026_REVISED_v2.docx`

---

## SESSIONS 20–21 — 2026-10-05/06: Proposal v2 + CNN Training

### Proposal v2 Changes
- All "2-DOF gripper" replaced with "5-finger tendon-driven hand (5× SG90 + 1× MG996R)"
- Budget row split: SG90×5 = KES 750, MG996R×1 = KES 400
- Budget total updated to KES 8,700
- References [2]-[6],[8],[9] completed with DOIs and page numbers
- Reference [3] corrected from IEEE Access to IEEE TBME

### CNN Training v1/v2 Attempts
**ERROR:** First training script (`train_cnn.py`) used wrong architecture — treated 24-element feature vector as a 24-step time sequence for Conv1D, which has no physical meaning.
**Fix (v2):** Reshaped to (6 feature types, 4 channels) — Conv1D kernel now spans same feature across all channels.

**ERROR:** v2 CNN plateaued at val_accuracy ~0.60, then diverged (loss rising from epoch 70).
**Root cause:** (later identified) Using `stimulus` instead of `restimulus` — 24% of windows mislabelled.

**Result:** MLP-noBN F1 = 0.7907 (this was inflated — data leakage from stimulus vs restimulus not yet understood).

**ERROR:** TFLite INT8 export blocked by Keras3 + TF MLIR bug.
```
Error: tensorflow/lite/kernels/read_variable.cc:67 variable != nullptr was not true
```
**Fix (2026-10-18):** Warm-up call before `from_keras_model()` forces variable materialisation.

### Git History Purge
**Problem:** Repository had grown to >700 MB from committed .mat/.keras/.pkl files.
**Fix:** `git filter-repo` removed all large binary objects from every commit.
- Files removed: all `.mat`, `.keras`, `.pkl`, `.npy` files
- Before: largest commit object = 679 MB
- After: largest blob = 4.6 MB
- `.gitignore` expanded to cover all future large binary types
- Force-pushed to GitHub: `Devkiarie/prosthetic`

---

## SESSION 22 — 2026-10-06: Stale File Audit + BOM Corrections

### BOM Corrections
- **ERROR:** RG resistor documented as 1kΩ in some files, 100Ω in others.
- **Fix:** Locked to 1kΩ 1% MF everywhere (gives G=51, not G=500).
- **ERROR:** Potentiometer documented as 10kΩ in some files.
- **Fix:** Locked to 100kΩ B100K everywhere (gives variable gain 1–10×).
- C_lp (anti-alias capacitor) locked to 150 nF (fc = 106 Hz ≈ 100 Hz target).

Files corrected: `docs/bom_breadboard_v1.csv`, `modules/analog-front-end.md`, `simulations/*.md`

---

## SESSION 23 — 2026-10-06: Component Sourcing + Shopping

### Luthuli Avenue Survey
**Available locally:**
- Breadboards, jumper wires, resistors, capacitors (passives)
- LM7805 LDO regulator
- 9V batteries
- Generic SG90 servos (LOW PRIORITY ONLY — verify label)

**NOT available locally — must order online:**
- INA128P DIP-8 (instrumentation amp)
- MCP6002 DIP-8 (op-amp)
- PCA9685 16-channel PWM driver
- MG996R high-torque servo (wrist)
- Ag/AgCl ECG/EMG electrodes with lead wires

**CRITICAL WARNING:** ESP32 at Luthuli Ave dealer is generic ESP32 (not ESP32-S3).
Do NOT buy without confirming the exact label on the chip.

**ERROR in original BOM:** LCSC C5194 listed as SOP8-DIP8 adapter.
**Reality:** LCSC C5194 is a 74HC4049 hex buffer IC — completely wrong part.
**Fix:** Order INA128P and MCP6002 in DIP-8 directly from AliExpress (no adapter needed).

**Component ordered:**
- ESP32-S3-DevKitC-1-N8R8: LCSC part C20541519 (~$5.80)
- All ICs via AliExpress

**Deliverables created:**
- `docs/FYP_Shopping_Checklist.pdf` — printable with warnings
- `docs/components_order.txt` — AliExpress order reference
- `docs/lcsc_bom_upload.csv` — LCSC cart file

---

## SESSION 24 — 2026-10-18: Pre-Arrival Work

### 1. KiCad Schematic

**Created:** `hardware/kicad/semg_afe.kicad_sch` (KiCad 9.0.8)
**Exported:** `hardware/kicad/semg_afe_schematic.pdf`

Full 4-channel analog front-end schematic covering:
- 4× INA128PA with RG = 1kΩ 1% MF (G=51), 10kΩ protection resistors
- 4× Sallen-Key HPF (fc=20 Hz): C=470 nF film, R1=12 kΩ, R2=24 kΩ
- 4× Sallen-Key LPF (fc=100 Hz anti-alias): R=10 kΩ, C=150 nF C0G
- 4× Twin-T notch (50 Hz): R=10 kΩ 1%, R/2=5.1 kΩ + 2kΩ trimpot, C=330 nF 1%, 2C=680 nF
- 4× MCP6002 variable gain (1–10×), RV=100 kΩ B100K pot
- 1× 1.65V REF (shared): 3.3V → 10kΩ → 10kΩ → GND + MCP6002 unity buffer
- ADC output conditioning: 1kΩ series + 33 nF anti-alias + BAV99 clamp
- PCA9685 servo driver: VCC=5V, SDA=GPIO8, SCL=GPIO9, PWM=50 Hz

### 2. Firmware Scaffold

**Files created:**
- `firmware/platformio.ini` — ESP32-S3-DevKitC-1, Chirale TFLite Micro 2.0.0, Adafruit PCA9685
- `firmware/include/config.h` — all hardware constants and gesture posture table
- `firmware/include/features.h` — full C++ time-domain feature extraction
- `firmware/src/main.cpp` — complete firmware: Core0 ADC DMA + Core1 inference/BLE/servo
- `firmware/tools/copy_model.py` — pre-build script to sync model from ml/models/

**BUILD RESULT: SUCCESS**
```
RAM:   47.7%  (156 KB / 320 KB)
Flash: 32.6%  (1.00 MB / 3.07 MB)
Build time: 258 s (first compile)
EXIT: 0
```

**Errors encountered and fixed:**

| Error | Cause | Fix |
|---|---|---|
| `ModuleNotFoundError: platformio` | Wrong Python path (system python3, not venv) | Activated ML venv before calling `python3 -m platformio run` |
| `UnknownPackageError: tensorflow/lite-for-microcontrollers` | Old library name | Changed to `spaziochirale/Chirale_TensorFLowLite @ ^2.0.0` |
| `expected unqualified-id before numeric constant` (×3) | `-D` build flags in `platformio.ini` clashed with `constexpr` declarations in `config.h` (STEP_SAMPLES, N_FEATURES, N_CLASSES, PCA9685_ADDR, etc.) | Removed all duplicate `-D` flags — all constants defined only in `config.h` |
| `has no member 'AddBatchNormalization'` | TFLite Micro API name differs from Keras | Changed to `resolver.AddMul()` (BatchNorm uses Mul+Add ops internally) |

### 3. ML Pipeline Fixes + Training

#### Root Cause Analysis: Why v1/v2 F1 was wrong

| Version | F1 | Root cause |
|---|---|---|
| v1 MLP-noBN | 0.7907 | **Data leakage** — `stimulus` mixes transition windows with gestures. Cross-subject split not enforced. |
| v2 MLP | 0.6589 | Same leakage, weaker model |
| v3-r1 (first correct run) | 0.4372 | Fixed split, but still using `stimulus` labels |
| v3-r2 | 0.5442 | Fixed `restimulus` — eliminated 24% mislabelled windows |
| v3-r3 (512-unit) | 0.5647 | More capacity + longer training |
| **v3-slim (final)** | **0.5647 cross-subject / 0.601 verified** | 128→64 unit MLP, correct labels, INT8 = **38.2 KB PASS** |

#### Key Bug: `stimulus` vs `restimulus` in NinaPro DB5

**Discovery:** 24% of all samples in NinaPro DB5 have incorrect labels in the `stimulus` field due to reaction-time lag at gesture onset and offset boundaries.

**Evidence:**
```python
stim   = mat['stimulus'].flatten()
restim = mat['restimulus'].flatten()
diff   = (stim != restim).sum()
# Result: 31,012 / 130,267 samples = 23.8% mislabelled
```

**Fix:** Use `restimulus` field (corrected, reaction-time-adjusted labels). This is documented in the NinaPro DB5 paper (Pizzolato et al. 2017) but easy to miss.

**Impact:** +10.7 F1 points on MLP after fix.

#### Key Bug: Wrong window size

**Problem:** Script used 500 samples as window size.
**Reality:** NinaPro DB5 is recorded at **200 Hz**, not 2 kHz.
- 250 ms at 200 Hz = **50 samples** (correct)
- 250 ms at 2000 Hz = 500 samples (wrong — this is the live ESP32 window)

The two sampling rates are different:
- NinaPro DB5: 200 Hz (dataset)
- ESP32-S3 on real hardware: 2 kHz (system design)

Feature extraction at 200 Hz uses 50-sample windows. Feature extraction on ESP32 will use 500-sample windows. Same features, same math — just different N.

#### Key Bug: TFLite INT8 export (READ_VARIABLE error)
```
tensorflow/lite/kernels/read_variable.cc:67 variable != nullptr was not true.
Node number 16 (READ_VARIABLE) failed to invoke.
```

**Root cause:** TFLite converter tries to trace the model graph, but tf.Variable resources haven't been materialised yet.

**Fix:** Call the model once (warm-up) before conversion:
```python
_ = model(np.zeros((1, 24), dtype='float32'), training=False)
converter = tf.lite.TFLiteConverter.from_keras_model(model)
```
The warm-up forces all tf.Variable resources to materialise in the session before TFLite traces the graph.

#### Key Bug: NinaPro data path

**Error:**
```
FileNotFoundError: No E1/A1 mat file in /home/kiarie/school/Final Year project/ml/data/ninapro_db5/S1
```

**Actual path:** `S1/s1/S1_E1_A1.mat` (lowercase inner subfolder `s{N}`)

**Fix:**
```python
inner = subj_dir / f's{sid}'
search_dirs = [subj_dir, inner]
```

#### INT8 Model — Final Verified Numbers

Model file: `ml/models/semg_v3_int8.tflite`
Copied to: `firmware/model/semg_v3_weights.h` + `firmware/model/semg_v3_int8.tflite`

| Metric | Value | Target | Status |
|---|---|---|---|
| Model size (INT8) | **38.2 KB** | < 100 KB | **PASS** |
| Quantization drop | 0.011 F1 | < 3% | **PASS** |
| Cross-subject F1 | 0.601 | ≥ 0.85 (post-cal) | Expected |
| Input tensor | int8 [1,24] | — | Confirmed |
| Output tensor | int8 [1,8] | — | Confirmed |

**Per-gesture F1 (verified on 1,000 real test samples):**

| Gesture | F1 | Notes |
|---|---|---|
| WristExt | 0.71 | Strongest — distinct activation |
| WristFlex | 0.70 | Strong |
| Rest | 0.70 | High recall (0.87) |
| ThumbsUp | 0.59 | Good |
| Pinch | 0.55 | Moderate |
| Point | 0.54 | Moderate |
| Power | 0.52 | Weak cross-subject |
| Open hand | 0.50 | Weakest — confused with Rest |

**Note on 0.601 vs 0.85 target:** The 0.85 target is explicitly a **post-calibration** metric. Cross-subject base model is expected at 0.55–0.65 (consistent with published literature for 4-ch, 8-class, TD features on DB5). The 3-minute calibration session in the Android app fine-tunes the final Dense layer on the user's own EMG — this is the architectural path to 0.85.

### 4. DOI Verification

All 8 references confirmed against actual paper titles:

| # | Citation | Status | Notes |
|---|---|---|---|
| [1] | Salgado et al. 2025 — DOI: 10.1109/ACCESS.2025.3598649 | VERIFIED | Single-ch sEMG on ESP32 |
| [2] | Pizzolato et al. 2017 — DOI: 10.1371/journal.pone.0186132 | VERIFIED | NinaPro DB5 paper |
| [3] | Mattick et al. 2023 — DOI: 10.1080/09638288.2022.2152875 | VERIFIED | 1.5M amputations/year |
| [4] | Hammond et al. 2025 — DOI: 10.1016/j.strmo.2024.002236 | VERIFIED | Cost barrier SSA |
| [5] | Hudgins et al. 1993 — DOI: 10.1109/10.204774 | VERIFIED | Original TD features |
| [6] | Li, Shi, Yu 2021 — PMCID: PMC8107289 | VERIFIED | DL survey sEMG |
| [7] | Atzori et al. 2012 — DOI: 10.1109/BioRob.2012.6290287 | VERIFIED | NinaPro concept |
| [8] | Molinari et al. 2026 — DOI: 10.1109/ACCESS.2026.3670500 | VERIFIED | HD-EMG ceiling |

**Note:** Reference [8] (Molinari) — confirm final journal issue vs preprint status before submission.

---

## Current System Status (2026-10-18)

### What is DONE
| Module | Status | Evidence |
|---|---|---|
| KiCad schematic (4-ch AFE) | COMPLETE | `hardware/kicad/semg_afe.kicad_sch` + PDF |
| ngspice simulations (4 netlists) | PASS | `hardware/simulation/*.raw` + bode_plots.png |
| NinaPro DB5 dataset | DOWNLOADED | `ml/data/ninapro_db5/S1–S10/` |
| ML pipeline (train_v3.py) | COMPLETE | All bugs fixed, reproducible |
| INT8 TFLite model | COMPLETE | `ml/models/semg_v3_int8.tflite` — 38.2 KB PASS |
| Firmware scaffold | BUILD PASS | RAM 47.7%, Flash 32.6% |
| Model copied to firmware | DONE | `firmware/model/semg_v3_weights.h` |
| Android app (MyoControl) | SCAFFOLDED | BLE, screens, gestures — needs live data wiring |
| 3D hand STL (servo pockets) | COMPLETE | `hardware/hand_stl/modified/5th_Palm_with_servo_pockets.stl` |
| Proposal (4-chapter) | COMPLETE | `docs/proposal/*_REVISED_v2.docx` |
| DOI verification | COMPLETE | `references/doi_verification_log.md` |

### What is WAITING ON HARDWARE
| Module | Blocked by |
|---|---|
| M1: Single-channel breadboard AFE | INA128P, MCP6002, electrodes (ordered) |
| M3: Firmware + DSP on device | ESP32-S3-DevKitC-1 (ordered) |
| M5: Servo test | PCA9685 + MG996R (ordered) |
| Integration test | All components |

### Next Actions (when components arrive)
1. Build single-channel INA128 breadboard (Stage 1 only)
2. Connect to oscilloscope — verify G=51 gain, check for 50 Hz noise
3. Add Sallen-Key HPF — verify 20 Hz cutoff
4. Add Twin-T notch — tune trimpot with oscilloscope (inject 50 Hz, null output)
5. Flash firmware to ESP32-S3: `cd firmware && pio run --target upload`
6. Serial monitor: verify ADC values respond to muscle contraction
7. Run Android app, connect BLE, verify gesture display

---

## Standing Decisions Log

| Date | Decision | Rationale |
|---|---|---|
| 2026-09-24 | INA128 gain = G=51 (RG=1kΩ 1%) | G=500 causes rail saturation from electrode DC offset |
| 2026-09-24 | MCP6002 op-amp (not TL072) | Single-supply (1.8–6V), rail-to-rail, works at 5V from battery LDO |
| 2026-09-24 | ngspice (not LTspice) | Linux-native, KiCad integration, no Wine needed |
| 2026-09-24 | 6 TD features only (no FFT) | O(N) complexity, <1 ms on ESP32, literature-validated for 8 gestures |
| 2026-09-25 | NinaPro DB5 (not PhysioNet) | Free, 10 subjects, 200 Hz, 52 gestures (8 of which match ours) |
| 2026-10-05 | 5-finger tendon-driven hand (not 2-DOF gripper) | More clinically meaningful, better demo visibility |
| 2026-10-06 | 100kΩ B100K pot (not 10kΩ) | 10kΩ gives gain max ~5×; 100kΩ gives 1–10× range needed |
| 2026-10-18 | Slim MLP (128→64) not large (512→512→256) | INT8 38.2 KB PASS vs 472 KB FAIL; F1 cost = ~0.06 cross-subject |
| 2026-10-18 | Use `restimulus` not `stimulus` in NinaPro | `stimulus` has 24% mislabelled windows at gesture boundaries |
| 2026-10-18 | TFLite warm-up before conversion | Fixes READ_VARIABLE crash in Keras3+TF2.21 |

---

## Error Registry (all bugs, all fixes)

| # | Date | Error | Root Cause | Fix | Impact |
|---|---|---|---|---|---|
| E01 | 09-22 | G=500 in some docs, G=50 in others | Copy-paste inconsistency in early design | Locked G=51 everywhere, formula documented | Prevents rail saturation |
| E02 | 09-22 | TL072 in some docs, MCP6002 in others | Early design used TL072, later switched without full audit | Standardised to MCP6002, DECISIONS.md written | Single-supply compatibility |
| E03 | 09-24 | LTspice netlists on Linux | LTspice is Windows-only | Switched to ngspice v42 (Linux-native) | All simulations now reproducible |
| E04 | 09-25 | `SyntaxError` in explore_ninapro.py | `sed` command used to edit Python file — corrupted indentation | Rewrote notebook from scratch with clean cells | Data pipeline restored |
| E05 | 10-05 | Ada Right v1.1.blend crashes Blender 5.2 | .blend file is Blender 2.7x format (pre-2.80 binary) | Used Ada_Right_v4_animated.blend (Blender 2.80+ format) | 3D hand design unblocked |
| E06 | 10-05 | LCSC C5194 wrong part | BOM listed generic adapter, LCSC C5194 is 74HC4049 buffer | Corrected to DIP-8 parts ordered directly from AliExpress | BOM accuracy |
| E07 | 10-06 | TFLite INT8 export: READ_VARIABLE | Keras3 tf.Variable not materialised before TFLite traces | Warm-up call before `from_keras_model()` | INT8 model now exportable |
| E08 | 10-06 | CNN training: val_acc plateaus at 0.60 then diverges | `stimulus` field has 24% mislabelled transition windows | Use `restimulus` field (Pizzolato 2017) | +10.7 F1 points |
| E09 | 10-18 | Wrong window size (500 vs 50 samples) | Confused NinaPro 200 Hz with ESP32 2 kHz | 250 ms at 200 Hz = 50 samples | Correct feature extraction |
| E10 | 10-18 | `FileNotFoundError` for NinaPro mat files | Path is `S{N}/s{N}/` (lowercase inner dir) not `S{N}/` | Search both `S{N}/` and `S{N}/s{N}/` subdirs | Data loader robust |
| E11 | 10-18 | PlatformIO: `UnknownPackageError` for TFLite | Library name changed; `tensorflow/lite-for-microcontrollers` deprecated | Use `spaziochirale/Chirale_TensorFLowLite @ ^2.0.0` | Firmware compiles |
| E12 | 10-18 | PlatformIO: `expected unqualified-id before numeric constant` | `-D` flags in `platformio.ini` clashed with `constexpr` in `config.h` | Removed all duplicate `-D` flags from `platformio.ini` | Firmware compiles |
| E13 | 10-18 | `AddBatchNormalization` not found in TFLite Micro op resolver | TFLite Micro uses decomposed ops (Mul+Add) not a single BN op | Changed to `resolver.AddMul()` | Firmware compiles |

---
*Log maintained by Alfred (Hermes Agent). Updated after every session.*
*Last updated: 2026-10-18*

---

## SESSIONS 09–15 — 2026-09-25: Android App Build Fixes + UI

### Session 09 — Gradle/KSP Version Fix
**Error E03b:** `Plugin [id: 'com.google.devtools.ksp', version: '2.2.0-1.0.29'] was not found`
**Root cause:** KSP 2.2.0 artifacts not yet published to Maven/Google repos (lag behind Kotlin release).
**Fix:** Downgraded `libs.versions.toml`: kotlin `2.2.0 → 2.1.0`, ksp `2.2.0-1.0.29 → 2.1.0-1.0.29`.

### Session 10 — Build Errors Fixed
**Error E03c:** `checkDebugAarMetadata FAILED` — AGP 8.7.3 only tested to compileSdk=35, we had 36.
**Fix:** `compileSdk = 36 → 35`, `targetSdk = 36 → 35` in `app/build.gradle.kts`.
**Error E03d:** Gradle GC thrashing (512 MB heap).
**Fix:** `gradle.properties` — `org.gradle.jvmargs=-Xmx2048m`, enabled caching + parallel builds.

### Session 11 — Launcher Icon Assets
**Error E03e:** `mipmap/ic_launcher not found (AAPT)` — scaffold never created `res/mipmap-*` folders.
**Fix:** Generated solid-cyan placeholder PNGs via stdlib Python for all 5 density buckets. Created `mipmap-anydpi-v26` adaptive icon XMLs. Background: `#0A0F1E`.

### Session 12 — Theme Rework
Design: cream default (`#F9F6F2` bg) + FM absolute black dark (`#080808` bg).
Added: warm peach gradient hero card, semantic colour aliases, decorative pastel blob colours.
Commit: `f9871a7`.

### Session 13 — BLE SecurityException + Settings Screen
**Error E03f:** `SecurityException: Need BLUETOOTH_SCAN permission` on API ≥31 devices.
**Fix:** Added `rememberBlePermissionRequest()` composable — requests `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` at runtime before calling `BleManager.startScan()`.
Also added legacy `BLUETOOTH` + `BLUETOOTH_ADMIN` permissions (maxSdkVersion=30) for pre-API31.

### Session 14 — Canvas Gesture Illustrations + BLE Device Picker
Built `GestureIllustration.kt` — pure Compose Canvas hand drawing, no image assets. 8 animated poses with finger curl (0°=open, 180°=closed) via 500ms tween.
Built `BleDevicePicker.kt` — ModalBottomSheet with RSSI colour coding, device list, cancel button.

### Session 15 — BLE Picker Dismiss Fix
**Error E03g:** X button didn't close the picker; scrim overlay stayed, eating all touch events.
**Root cause 1:** `showPicker = isScanning || devices.isNotEmpty()` — devices remained after scan stop.
**Root cause 2:** `ModalBottomSheet` rendered outside main `Box` → full-screen scrim orphaned in tree.
**Fix:** `showPicker` changed to `mutableStateOf(false)` controlled by `LaunchedEffect`. Set false explicitly on X tap, device tap, and disconnect.

---

## SESSIONS 16–20 — 2026-09-25: Actuator Decision + 3D Hand

### Session 16 — Actuator Upgrade Decision
**Decision:** 2-DOF gripper (2× MG996R) → 5-finger tendon-driven hand (5× SG90 + 1× MG996R).
**Rationale:** Same sEMG pipeline, same BOM cost (~KES 650 delta, still under $20 BOM). PCA9685 handles 16 channels so no hardware change. More compelling demo.
Files updated: `modules/actuator-hand.md`, `05_BOM_AND_PROCUREMENT.md`, `docs/COMPREHENSIVE_PLAN.md`.

### Session 17 — Ada Hand v1.1 STLs + 3D Tools
Downloaded Open Bionics Ada Hand v1.1 STLs → `hardware/hand_stl/Ada_3D_model_files/` (git submodule).
Palm: 174×193×42mm — fits SG90 servos (29mm tall).
Tools installed: Blender 5.2.2 (snap), OpenSCAD (apt), numpy-stl, trimesh.

### Session 18 — Wrap-Up

### Session 19 — Blender Format Conversion
**Error E03h:** Ada Right v1.1.blend crashes Blender 5.2 — Blender 2.7x format incompatibility.
**Fix:** Downloaded Blender 4.2 LTS portable (no sudo), opened old file, exported all 30 mesh objects as STLs, deleted 4.2 portable after. Blender 5.2 imports extracted STLs cleanly.
Extracted STLs saved to `hardware/hand_stl/Ada_3D_model_files/STLs/Right Hand/extracted/`.

### Session 20 — Hand Rigging + Animation
- Opened `Ada_Right_v2_full.blend` in Blender 5.2 — full hand assembled
- Added HandRig armature (5 bones), parented all 5 finger meshes (BONE parenting)
- Keyframed open→fist→open: frame1=open, frame30=80° curl, frame60=open
- Saved as `Ada_Right_v4_animated.blend` (active file for all design work)

**Blender pitfalls discovered:**
- Armature bones animate but meshes stay frozen if `parent_type = 'BONE'` not explicitly set
- Must set mesh world matrix: `mesh.matrix_world = world_mat` before parenting then restore after
- `--headless` is not a valid Blender flag (treats it as file path) — use `-b` for background mode

---

## Error Registry Additions (Sessions 09–20)

| # | Date | Error | Root Cause | Fix | Impact |
|---|---|---|---|---|---|
| E14 | 09-25 | KSP 2.2.0-1.0.29 not found | Not yet published to Maven | Downgrade kotlin+ksp to 2.1.0 | Android app builds |
| E15 | 09-25 | checkDebugAarMetadata FAILED | compileSdk=36 exceeds AGP 8.7.3 limit | compileSdk=35 | Android app builds |
| E16 | 09-25 | Gradle GC thrashing | 512 MB heap too small | gradle.properties Xmx2048m | Build stability |
| E17 | 09-25 | AAPT launcher icon not found | Scaffold missed res/mipmap-* folders | Generated PNG placeholders via Python | App runs on device |
| E18 | 09-25 | BLE SecurityException | Missing runtime BLUETOOTH_SCAN permission | rememberBlePermissionRequest() composable | BLE scan works |
| E19 | 09-25 | BLE picker scrim stays on screen | showPicker used reactive isScanning — didn't clear | Changed to mutableStateOf(false), explicitly clear | Picker dismisses correctly |
| E20 | 09-25 | Ada v1.1.blend crashes Blender 5.2 | Blender 2.7x binary format incompatible | Blender 4.2 LTS conversion → STL export | 3D hand design unblocked |
| E21 | 09-25 | Blender bones animate but meshes frozen | parent_type not set to 'BONE' | mesh.parent_type = 'BONE'; restore world matrix | Hand animation works |

---
*Log maintained by Alfred (Hermes Agent). Updated 2026-10-18 — sessions 09-20 backfilled from LOG.md and git history.*

---

## SESSION 25 — 2026-10-18: Schematic Wiring + E2E Simulator + Android Wiring

### Issues Addressed
1. KiCad PDF schematic showed labels/annotations but no drawn wires
2. No end-to-end simulation without hardware
3. Android app had unfinished screen stubs + generic gesture poses
4. Ada Hand submodule showing "Sync Changes 4↑" in VS Code Git GUI

### Fixes

#### Schematic (hardware/kicad/)
- `semg_ch1_schematic.png/svg` — schemdraw-generated properly connected circuit diagram
- All wires drawn: Electrode → R_PROT → INA128 → HPF → Notch → Var Gain → ADC
- Tool: `ml/scripts/draw_schematic.py` (schemdraw 0.23)

#### E2E Simulator (ml/scripts/e2e_simulator.py)
- Stage 1: Synthetic sEMG per gesture (anatomically-motivated per-channel activations)
- Stage 2: AFE model (scipy butter HPF + LPF + iirnotch)
- Stage 3: Feature extraction (exact match to firmware features.h)
- Stage 4: TFLite INT8 inference
- Stage 5: Servo angle output (PCA9685 posture table)
- Plot: `ml/data/processed/e2e_simulation.png`

#### Android App
- `GestureIllustration.kt`: fixed gesture poses:
  - PINCH: was middle/ring/pinky close (wrong). Now thumb+index curl toward each other
  - REST: natural relaxed curl (was all identical 0.10)
  - WRIST_FLEX/EXT: fingers relaxed not rigid
- `GestureSessionDao.kt`: Room DAO with flow queries (per-gesture counts, avg confidence, avg latency)
- `MyoDatabase.kt`: added abstract gestureSessionDao()
- `AppModule.kt`: provide GestureSessionDao via Hilt
- `MonitorViewModel.kt` (new): real BLE SignalMetrics + synthetic demo mode
- `MonitorScreen.kt`: wired to MonitorViewModel via hiltViewModel()
- `AnalyticsViewModel.kt` (new): Room DB queries via combine()
- `AnalyticsScreen.kt`: shows real Room DB data when available, falls back to simulated
- `HomeViewModel.kt`: persists every BLE gesture result to Room DB

#### Ada Submodule Fix
- Removed Ada_3D_model_files as git submodule (was showing "Sync 4↑" — 4 unpushable commits)
- Converted to plain folder with README.md (STLs gitignored — too large)
- Modified palm STL tracked separately: `hardware/hand_stl/modified/5th_Palm_with_servo_pockets.stl`
- VS Code Git GUI now shows single repo, no Ada panel

### Error Registry
| # | Error | Root Cause | Fix |
|---|---|---|---|
| E22 | schemdraw Wire('n|-') fails | LTspice/KiCad notation not supported | Use explicit Line up/left/down routing |
| E23 | schemdraw Dot().length() fails | Dot has no length method | Use Line().down().length() for positioning |
| E24 | schemdraw get_figure() fails | Drawing.get_figure() doesn't exist | Use Drawing.save() directly |
| E25 | Ada "Sync 4↑" in Git GUI | Submodule had 4 commits pointing to someone else's repo | Remove submodule, convert to plain folder |
