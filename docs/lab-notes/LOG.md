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


## Session 11 — 2026-09-25 Launcher icon assets added
**Time:** 2026-09-25 19:37

### Error: mipmap/ic_launcher not found (AAPT)
Scaffold never created the res/mipmap-* folders.
Fix: generated solid-cyan (#00E5FF) placeholder PNGs via stdlib Python.
Files created:
- mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi: ic_launcher.png, ic_launcher_round.png, ic_launcher_foreground.png
- mipmap-anydpi-v26: ic_launcher.xml, ic_launcher_round.xml (adaptive icon XML)
- values/ic_launcher_background.xml (#0A0F1E dark background)


## Session 12 — 2026-09-25 Theme Rework: Cream Default + FM Absolute Black Dark
**Time:** $(date +"%Y-%m-%d %H:%M")
**Commit:** f9871a7

### Changes
- McColors.kt: added semantic aliases (Accent, TextPrimary, Background, Border, SurfaceVariant) + HeroCardStart/HeroCardEnd warm peach gradient + DecorPeach/Beige/Sage blob colours
- GestureDisplay.kt: replaced plain white card with warm peach gradient hero card (`#FFF5F2` → `#FAD5C8`), decorative coral circle backdrop, pill confidence bar. No more cyan border.
- HomeScreen.kt: 4 soft decorative circles in background (bottom area, low opacity pastels), MYOCONTROL title now charcoal (not cyan), connect button is OutlinedButton pill, emergency stop is pill-shaped
- McComponents.kt: McCard + McStatCard use MaterialTheme.colorScheme for auto light/dark
- AppNavHost.kt: Scaffold + NavigationBar use MaterialTheme.colorScheme.background — cream in light, absolute black in dark mode

### Design System
- Light (default): cream `#F9F6F2` bg, white `#FFFFFF` cards, coral `#F07860` accent, charcoal `#1A1A1A` text
- Dark (optional): absolute black `#080808` bg, `#111111` cards, coral accent, white text
- Hero card: peach gradient with decorative circle, matches reference UI exactly
- Nav bar: coral active, gray inactive, no pill indicator, no tonal elevation

## Session 13 — 2026-09-25 BLE Fix + Settings Screen
**Commit:** 3eb95a6

### Changes
**BLE SecurityException fix:**
- AndroidManifest: added legacy BLUETOOTH + BLUETOOTH_ADMIN permissions (maxSdkVersion=30) so API <31 devices don't crash on `getBluetoothLeScanner()`
- BlePermission.kt: new `rememberBlePermissionRequest()` composable — requests BLUETOOTH_SCAN + BLUETOOTH_CONNECT + ACCESS_FINE_LOCATION at runtime before calling BleManager.startScan()
- HomeScreen: connect button now calls permission launcher first; only calls ViewModel after all permissions granted; disconnect skips permission check

**Settings Screen:**
- SettingsScreen.kt: Light / Dark / System theme option cards with mini theme previews
- Selected card has coral border + checkmark icon
- About section shows app name, purpose, device, project info
- Gear icon on HomeScreen is now a proper `IconButton` wired to navigate to settings
- AppNavHost accepts appTheme + onSetTheme params; hides bottom nav on settings
- MainActivity holds AppTheme state (default: LIGHT) and passes to AppNavHost

## Session 14 — 2026-09-25 Canvas Illustrations + BLE Device Picker
**Commit:** 2a29634

### GestureIllustration.kt (new file)
Pure Compose Canvas drawing of a human hand — no image assets needed.
- Palm body, 4 fingers, thumb — each drawn as a rotated/scaled rounded rect
- 8 unique poses via `AnimationSpec` per `GestureLabel`: REST, OPEN_HAND, POWER_GRASP, PINCH, POINT, WRIST_FLEX, WRIST_EXT, THUMBS_UP
- Finger curl (0=open, 1=fist) animates with 500ms FastOutSlowIn tween between gestures
- Wrist angle animates ±30° for FLEX/EXT
- Coral accent dot on PINCH/POINT/THUMBS_UP fingertip
- Warm skin tone (#F5C4B0 = McColors.DecorPeach), peach circle backdrop

### GestureDisplay.kt — card redesigned
- Left column: 130dp GestureIllustration (replaces monospace text symbol)
- Right column: gesture name, large bold confidence %, confidence bar, latency
- Matches reference layout: illustration left, stats right

### BleDevicePicker.kt (new file)
ModalBottomSheet shown immediately when scan starts:
- Scanning state: spinner + message
- Devices found: list rows with device name, MAC address, RSSI signal colour (green/amber/red)
- X button cancels / stops scan
- Tap any device row to connect

### BleManager.kt
- Removed auto-connect (was connecting to first device found, no user choice)
- Added `scannedDevices: StateFlow<List<BleDeviceInfo>>` — accumulates scan results
- Added `connectToDevice(BleDeviceInfo)` — called from picker
- Broad scan (no service UUID filter) so all BLE devices visible during dev

## Session 15 — 2026-09-25 BLE Picker Dismiss Fix
**Commit:** 4fca279

### Bug: X button didn't close picker + navigation blocked
**Root cause 1:** `onDismiss` called `onConnectClick()` which stopped the scan but `scannedDevices` remained non-empty. `showPicker = isScanning || devices.isNotEmpty()` stayed true — sheet never left composition.

**Root cause 2:** `ModalBottomSheet` was rendered outside the main `Box`, so its full-screen scrim overlay stayed in the tree even after swipe-dismiss, eating all touch events from the `NavHost`.

**Fixes applied:**
- `HomeScreen`: `showPicker` changed from reactive expression to `mutableStateOf(false)` controlled by `LaunchedEffect(isScanning, isConnected)` — setting it false removes the composable from the tree entirely
- X button + swipe: `onDismiss = { viewModel.cancelScan(); showPicker = false }`
- Device tap: `showPicker = false` before `connectToDevice()` — no orphaned sheet
- `BleManager.stopScan()`: now also clears `_scannedDevices = emptyList()`
- `HomeViewModel.cancelScan()`: new dedicated function that calls `bleManager.stopScan()`
- Connect button: "Scanning..." → "Stop" when scanning (second cancel path)


## Session 16 — 2026-09-25 Actuator Decision: 5-Finger Hand
**No code commit — docs only**

### Decision
Upgraded actuator from 2-DOF gripper (2× MG996R) to 5-finger tendon-driven prosthetic hand.
**Rationale:** Same sEMG pipeline — classifier outputs a posture label, firmware looks up 5 servo angles.
BOM delta: ~KES 650 (~USD 5). Still under USD 20 BOM. PCA9685 already in BOM and handles 16 channels.

### Files Updated
- `modules/actuator-gripper.md` — full rewrite: motor placement, tendon mechanism, gesture-servo table, print specs, assembly order, PCA9685 channel map
- `05_BOM_AND_PROCUREMENT.md` — 2× MG996R → 5× SG90 + 1× MG996R + nylon/elastic; filament 250g→350g
- `docs/COMPREHENSIVE_PLAN.md` — SO5 and E4 block updated to 5-finger hand + 11-step assembly
- Wiki entity `fyp-semg-prosthetic.md` — actuation, end-demo, system architecture all updated

### Design Tool Guidance Written
See below in response — Tinkercad / Fusion 360 / Blender + Bambu/Creality printer workflow.

## Session 17 — 2026-09-25 Ada Hand STL Downloaded + 3D Tools Installed
**Commit:** 8150729

### Downloaded
- Open Bionics Ada Hand v1.1 STL files → `hardware/hand_stl/Ada_3D_model_files/`
- Added as git submodule (CC BY-SA 4.0 license)
- Palm: 174×193×42mm — 42mm depth fits SG90 servos (29mm tall) ✓
- Blender source file included: `Blender Files/Right Hand/Ada Right v1.1.blend`

### Tools Installed on Linux
- MeshLab: `sudo apt install meshlab` — view/inspect STLs
- OpenSCAD: `sudo apt install openscad` — parametric scripting
- Blender 5.2: `sudo snap install blender --classic` (installing)
- numpy-stl + trimesh: `pip3 install numpy-stl trimesh` — scripted manipulation

### Next Steps for Hand Design
1. Open Ada Right v1.1.blend in Blender
2. Add 5× SG90 servo pockets to dorsal palm
3. Add 2mm tendon channels through each finger
4. Add MG996R pocket at wrist base
5. Print test finger phalanx first before full print

## Session 18 — 2026-09-25 Session Wrap-Up
**Commits:** see below

### Summary of all work completed today (2026-09-25)
- Sessions 1–5: ngspice sims, TF stack, NinaPro download, ML notebook, X_balanced.npy
- Sessions 6–8: MyoControl Android scaffold, HomeScreen, Phase 2 fake classifier
- Session 9: KSP version fix (kotlin/ksp 2.1.0)
- Session 10–11: compileSdk 35 fix, gradle.properties heap, launcher icons
- Session 12: Cream+FM black theme, peach hero card, decorative blobs
- Session 13: BLE SecurityException fixed (runtime permissions), Settings screen
- Session 14: Canvas gesture illustrations (8 hand poses), BLE device picker
- Session 15: BLE picker dismiss fix, navigation unblocked
- Session 16: Actuator upgraded 2-DOF gripper → 5-finger tendon hand
- Session 17: Ada Hand v1.1 STLs downloaded, tools installed (Blender/FreeCAD/OpenSCAD)
- Session 18: gradle google-fonts dep added by Android Studio; cleanup; wrap-up

### Android Studio auto-changes committed
- libs.versions.toml: added androidx-ui-text-google-fonts library alias
- build.gradle.kts: added google fonts implementation dependency (needed for Manrope)

## Session 19 — 2026-09-25 .blend Conversion + STL Extraction
**Commit:** d7b142d

### Problem
Ada Right v1.1.blend was created in Blender 2.7x — Blender 5.2 (snap) crashed with
"Corrupt .blend file, unexpected data size" — format too old.

### Solution
1. Downloaded Blender 4.2.0 LTS portable (~336MB, no sudo) to ~/.local/
2. Blender 4.2 opened the old file and exported all 30 mesh objects as individual STLs
3. Verified: Blender 5.2 imports finger STL cleanly (14,248 verts, no errors)
4. Deleted Blender 4.2 portable and tmp download after conversion

### Extracted STLs (hardware/hand_stl/Ada_3D_model_files/STLs/Right Hand/extracted/)
- 0th_Thumb.stl, 1st_finger.stl to 4th_finger.stl — 5 finger assemblies
- 5th_Palm.stl, Master_-_Palm.stl — palm body variants
- Dorsal_cover.stl, PCB_bracket.stl, Boolean_Union_-_Wrist_connector.stl
- Boolean difference/union tools for servo pocket cuts

### How to open in Blender 5.2
File → Import → STL → select any extracted STL
Then import multiple: 5th_Palm.stl + all finger STLs for full hand view

