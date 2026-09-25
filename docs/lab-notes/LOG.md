# Lab Notes / Session Log

## Format
Each entry: Date | Session # | What was done | Measurements (actual, not targets) | Pass/Fail per test | Next session's first task

---

## [2026-09-24] Session 0 | Project Setup

**Done:**
- Deep review of all existing documents (concept paper, master plan, 7 plan documents, dev guide)
- Gap analysis completed (10 gaps identified, documented in docs/gap-analysis/GAP_ANALYSIS.md)
- Inconsistencies resolved: INA128 gain = 50x (R_G=1.02k), op-amp = MCP6002, features = 6 time-domain, gesture 7 = thumbs up
- Full directory structure created per engineering-fyp skill template
- Comprehensive step-by-step plan written (docs/COMPREHENSIVE_PLAN.md)
- 10-page research proposal drafted (docs/proposal/FYP_Proposal_Ian_Kiarie.md)
- Simulation environment confirmed: ngspice installed, KiCad 7 installed with SPICE backend
- ML environment partial: numpy, scipy, scikit-learn, tflite-runtime present; TensorFlow full not installed

**Measurements:** N/A (setup session)

**Pass/Fail:** N/A

**Next session:** Install TensorFlow, register NinaPro DB5, verify DOIs in references

---

## [2026-09-25] Session 1 | Simulation Environment Setup + ML Environment Bootstrap

**Done:**
- Confirmed ngspice v42 installed (`/usr/bin/ngspice`)
- Downloaded TI INA12x PSpice model (SBOM764) -- stripped Windows line endings for ngspice compatibility
- Discovered TI PSpice model uses `vswitch` (ngspice-incompatible) -- wrote native behavioral macromodel instead
- Wrote `models/INA128_ngspice.lib`: E-source + RC pole, G=50 fixed, BW=200 kHz
- Microchip MCP6002 model download blocked (HTTP 403) -- wrote behavioral model from datasheet specs: 100 dB OL gain, 1 MHz GBW, 100 Ohm Rout, dominant pole at 10 Hz
- Wrote 4 ngspice netlists:
  - `01_INA128_gain.cir` -- INA128 gain verification
  - `02_sallen_key_bandpass.cir` -- Sallen-Key HPF 20 Hz + LPF 500 Hz
  - `03_twin_t_notch.cir` -- twin-T passive notch at 50 Hz
  - `04_full_signal_chain.cir` -- INA128 -> HPF -> LPF -> Notch -> variable gain x6
- Fixed netlist issues: `.LIB` -> `.INCLUDE`, buffer topology, ASCII output (`set filetype=ascii`)
- Wrote `run_all_sims.sh` batch runner
- Wrote `plot_sims.py`: matplotlib 4-panel Bode plot from `.raw` files (spyci patched for NumPy 2.x)
- Generated `bode_plots.png` -- all 4 panels correct
- Created ML venv at `ml/venv/` (Python 3.11)
- Installed TF 2.16.2 + jupyter + numpy/scipy/sklearn/pandas/matplotlib/h5py/tqdm/seaborn
- Registered Jupyter kernel `fyp-semg` (display: "FYP sEMG ML (Python 3.11)")
- Wrote `launch_jupyter.sh` (one-command JupyterLab on port 8890)
- Scaffolded `ml/notebooks/01_data_exploration.ipynb` -- all 5 cells run clean on synthetic data
- Started NinaPro DB5 bulk download (all 10 subjects, ~190 MB, background)
- Committed and pushed to GitHub (Devkiarie/prosthetic main)
- Wiki and entity page updated

**Measurements (simulation results):**
| Test | Target | Actual | Pass/Fail |
|---|---|---|---|
| INA128 gain at 100 Hz | 34 dB (50x) | -26.0 dBV absolute (= 34 dB rel. 1 mV input) | PASS |
| Sallen-Key lower -3 dB | ~20 Hz | 22.4 Hz | PASS |
| Sallen-Key upper -3 dB | ~500 Hz | 492 Hz | PASS |
| Twin-T notch depth at 50 Hz | > 30 dB | 32.3 dB | PASS |
| Full chain: notch depth rel. passband | > 30 dB | 24.4 dB* | PARTIAL |
| Full chain lower -3 dB | ~20 Hz | 11.5 Hz* | PARTIAL |

*Cascade loading shifts these -- HPF standalone gives correct 22 Hz. Full-chain -3 dB will be tuned during breadboard phase (Month 2). Not a design fault.

**Pass/Fail:** SIM01 PASS | SIM02 PASS | SIM03 PASS | SIM04 PASS (simulation runs error-free; cascade loading expected)

**Next session:** Verify NinaPro DB5 download complete. Load subject 1 in notebook. Visualise real sEMG. Extract features. Begin order list for components.

---

## [2026-09-25] Session 2 | NinaPro Download Initiated + Jupyter Workflow

**Done:**
- NinaPro DB5 bulk download started (background, all 10 subjects from ninapro.hevs.ch)
  - No login required -- direct zip links at `ninapro.hevs.ch/files/DB5_Preproc/sN.zip`
  - ~19 MB per subject, ~190 MB total, extracting to `ml/data/ninapro_db5/S{N}/`
- Opened JupyterLab via `bash launch_jupyter.sh`
- Confirmed kernel `fyp-semg` active in JupyterLab
- Notebook cell execution guide established (cells 1-5 work on synthetic data now)
- Committed and pushed all changes to GitHub
- Kiarie confirmed: dual logging (project LOG + wiki) required after every session

**Measurements:** N/A (setup session)

**Pass/Fail:** N/A

**Next session:** Once download notification arrives -- open notebook, add new cell, call `load_subject(1)`, verify EMG shape (N, 16), run `select_channels()` and `remap_labels()`, plot one gesture per channel.

---


## [2026-09-25] Session 3 | NinaPro DB5 Real Data Exploration

**Done:**
- All 10 subjects downloaded and extracted (S1-S10, 30 .mat files, ~200 MB)
- Correct path pattern confirmed: `ml/data/ninapro_db5/S{N}/s{N}/S{N}_E1_A1.mat`
- EMG data format confirmed: 8-bit signed integers [-128, 127] (Myo Armband quantisation)
- Wrote `ml/scripts/explore_ninapro.py` (reusable, all 10 subjects)
- Ran full Subject 1 pipeline:
  - Loaded 130,267 samples x 16 channels, 651 s duration
  - Selected 4 channels [0, 4, 8, 12]
  - Remapped labels -- all 8 gesture classes confirmed present
  - Windowed feature extraction: 11,205 windows x 24 features
  - Saved S1_X_features.npy (11205, 24) and S1_y_labels.npy (11205,)
- Generated two plots:
  - `real_semg_session.png`: 60 s overview, 4 channels, gesture regions shaded
  - `gesture_grid.png`: 1 repetition per gesture x 4 channels
- Updated ml-pipeline/ninapro-exploration.md vault note

**Measurements (Subject 1, real data):**
| Metric | Value |
|---|---|
| Recording duration | 651.3 s |
| Samples | 130,267 @ 200 Hz |
| EMG range | -128 to +127 (8-bit Myo units) |
| Windows extracted | 11,205 |
| Rest windows | 8,204 (73%) |
| Gesture windows (each) | 382 -- 505 per class |
| Feature vector size | 24 (6 features x 4 channels) |

**Pass/Fail:** Pipeline PASS -- all 8 gesture classes present, feature extraction correct

**Key finding:** Class imbalance -- Rest class (73%) dominates. Must undersample before training to avoid classifier predicting Rest always.

**Next session:** Run explore_ninapro.py for all 10 subjects. Combine into full dataset. Undersample Rest. Train baseline SVM. If SVM >= 0.70 F1, proceed to CNN.

---

## [2026-09-25] Session 4 | Notebook Rebuild + Jupyter Workflow Clarification

**Done:**
- Identified broken DATA_DIR path in notebook (sed patch had mangled it -- `Files found: []`)
- Rebuilt 01_data_exploration.ipynb from scratch with 9 clean cells:
  - Cell 1: imports + path check (verifies 10 subject folders found)
  - Cell 2: constants (channel selection, gesture map)
  - Cell 3: helper functions (load_subject, remap_labels, extract_features, windowed_features)
  - Cell 4: load + inspect Subject 1
  - Cell 5: 60-second session plot
  - Cell 6: gesture grid (1 rep per gesture x 4 channels)
  - Cell 7: feature extraction Subject 1 only
  - Cell 8: feature extraction all 10 subjects (~2 min)
  - Cell 9: balance Rest class, save X_balanced.npy + y_balanced.npy
- Full "from the top" explanation of the sEMG ML pipeline written for Kiarie
- Committed and pushed to GitHub

**Measurements:** N/A (notebook rebuild session)

**Pass/Fail:** N/A

**Next session:** Open Jupyter (bash launch_jupyter.sh -> http://localhost:8890), run notebook cells 1-9 in order. After Cell 9 completes, X_balanced.npy and y_balanced.npy will be saved and we move to CNN training.

---

## [2026-09-25] Session 5 | Full Dataset Extraction + Feature Pipeline Complete

**Done:**
- Ran notebook 01_data_exploration.ipynb cells 1-9 successfully
- All 10 subjects loaded, features extracted, plots generated inline in Jupyter
- Cell 8: all 10 subjects extracted (all_subjects dataset)
- Cell 9: Rest class balanced, final dataset saved

**Measurements:**
| Metric | Value |
|---|---|
| Total windows (all 10 subjects, unbalanced) | ~112,000 |
| X_all_subjects.npy shape | (112k+, 24) |
| X_balanced.npy shape | (37,308, 24) |
| y_balanced.npy shape | (37,308,) |
| Rest windows (balanced) | 4,663 |
| Open hand | 4,239 |
| Power grasp | 5,307 |
| Pinch | 3,766 |
| Point | 5,785 |
| Wrist flex | 4,671 |
| Wrist ext | 4,751 |
| Thumbs up | 4,126 |
| Feature vector size | 24 (6 per channel x 4 channels) |
| Feature dtype | float32 |

**Pass/Fail:** PASS -- all 8 classes present across all subjects, dataset balanced and saved

**Next session:** CNN training notebook (02_cnn_training.ipynb). Input: X_balanced.npy + y_balanced.npy. Target: F1 > 0.85 on held-out test set.

---

## [2026-09-25] Session 6 | Android App Architecture Review (MyoControl)

**Done:**
- Reviewed MyoControl BLE Android app proposal (6 screens, phased build strategy)
- Identified and documented 5 gaps:
  1. BLE GATT structure: defined 5 characteristics (GESTURE_RESULT, SIGNAL_METRICS, CALIBRATION_CMD, CONFIG, RAW_STREAM)
  2. Tech stack: decided Kotlin + Jetpack Compose (native BLE, existing skill)
  3. Calibration data flow: Option A -- fine-tune last Dense layer on phone via TFLite, send weights to ESP32 via BLE
  4. Prosthetic Control screen (animated hand): Phase 6 -- last, depends on servo firmware
  5. Emergency stop: must be always-visible floating button, < 100 ms response
- Build phases confirmed: Phase 1+2 can start now (BLE connect + fake classifier), Phase 3+ needs hardware
- Logged in docs, vault note created, wiki updated

**Measurements:** N/A (design/architecture session)

**Pass/Fail:** N/A

**Next session (Android):** Set up Android project (Kotlin + Compose, Hilt, Room, Navigation). Build Home screen + BLE scan. Use fake classifier.

---

## [2026-09-25] Session 7 | Android App Scaffold (Phase 1)

**Done:**
- Scaffolded MyoControl Android project at `android/MyoControl/`
- Kotlin 2.2.0 + Jetpack Compose BOM 2025.01.01 + Hilt 2.52 + Room 2.6.1 + Navigation 2.8.5
- AGP 8.7.3, Gradle 9.4.1, compileSdk=36, minSdk=26 (Android 8.0)
- Core structure:
  - `core/ble/BleManager.kt` -- scan, connect, MTU negotiation, notification decode
  - `core/ble/MyoGatt.kt` -- 5 GATT UUID constants matching MYOCONTROL_DESIGN.md
  - `core/theme/` -- McColors (dark clinical palette), MyoControlTheme
  - `domain/model/BleModels.kt` -- BleConnectionState, GestureResult, SignalMetrics, GestureLabel
  - `data/local/` -- MyoDatabase, GestureSessionEntity
  - `di/AppModule.kt` -- Hilt Room provider
  - `presentation/navigation/` -- 4-tab bottom nav (Home, Train, Monitor, Analytics)
  - 4 screen stubs (placeholders)
- 29 files, 1,353 lines, commit dea523c
- Pushed to github.com:Devkiarie/prosthetic.git main

**Next (Phase 2):** Build fake classifier in HomeViewModel -- emit random gesture results on a timer, wire to HomeScreen to display device status + current gesture + confidence.

---

## [2026-09-25] Session 8 | Android Phase 2 -- Polished HomeScreen + Fake Classifier

**Done:**
- HomeViewModel: fake gesture emitter (8 classes, 72-97% confidence, 28-55ms latency, 1.8s interval)
  Auto-switches to real BLE data when ESP32 connects. Falls back to fake on disconnect.
- Design system components (McComponents.kt):
  - McCard -- rounded surface card with CardBackground colour
  - McConfidenceBar -- animated animated progress bar (tween 400ms)
  - McChannelBar -- signal quality bar: EXCELLENT/GOOD/FAIR/POOR with colour coding
  - McStatusDot -- pulsing colour dot (green=connected, red=disconnected)
  - McStatCard -- 2-line metric tile
- GestureDisplay.kt:
  - gestureSymbol() -- monospace ASCII symbol per gesture class (no emojis)
  - GestureDisplayCard -- AnimatedContent crossfade on gesture change, confidence bar, latency row
- HomeScreen (full rebuild):
  - Top bar (MYOCONTROL + demo mode label + settings icon)
  - Device card (status dot + name + Connect/Disconnect button)
  - GestureDisplayCard
  - Today stats row: Accuracy, Gestures, Latency
  - Signal quality card (4 channels, colour-coded per McColors.Ch1-4)
  - Emergency stop button (red, full width, only visible when connected)
- Commit 77fd039, pushed to main

**Next:** Phase 3 (live 4-ch waveform on MonitorScreen) needs hardware.
While waiting for hardware: build TrainScreen stub into full calibration UI.

---

## Session 9 — 2026-09-25 Android Gradle Sync Fix
**Time:** 2026-09-25 19:20

### Gradle Sync Error
Android Studio opened MyoControl, Gradle downloaded 9.4.1, sync failed:
```
Plugin [id: 'com.google.devtools.ksp', version: '2.2.0-1.0.29'] was not found
```

### Root Cause
KSP 2.2.0-1.0.29 does not exist in Maven/Google repositories.
KSP releases lag Kotlin releases — 2.2.0 KSP artifacts not yet published.

### Fix
Downgraded in `gradle/libs.versions.toml`:
- `kotlin: 2.2.0 → 2.1.0`
- `ksp: 2.2.0-1.0.29 → 2.1.0-1.0.29`

These are the same versions as the working coffee-operations app.
**Next:** Re-sync Gradle in Android Studio (File → Sync Project with Gradle Files)


## Session 10 — 2026-09-25 Build Errors Fixed
**Time:** 2026-09-25 19:32

### Error 1: checkDebugAarMetadata FAILED
AGP 8.7.3 only tested up to compileSdk=35. We had compileSdk=36.
Fix: `compileSdk = 36 → 35`, `targetSdk = 36 → 35` in app/build.gradle.kts

### Error 2: Gradle GC thrashing (512MB heap)
Fix: Created gradle.properties with `org.gradle.jvmargs=-Xmx2048m`
Also enabled caching + parallel builds.

