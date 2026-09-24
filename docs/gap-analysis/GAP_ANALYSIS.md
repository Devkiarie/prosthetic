# FYP Gap Analysis -- Deep Review
## Ian Kiarie | ENE212-0069/2022 | Sept 2026

After reviewing all existing documents in both `/school/Projects/` and `/school/Final Year project/`, the wiki entity, and Dr Irene's feedback, here are the gaps that need resolution before proceeding.

---

## 1. Critical Inconsistencies

### 1.1 INA128 Gain Setting (BLOCKING)

| Document | INA128 Gain | R_G |
|---|---|---|
| Wiki entity (fyp-semg-prosthetic.md) | 500x | 100 ohm |
| Concept paper (01_sEMG_Prosthetic_Control.md) | 500x | 100 ohm |
| Master plan (00_MASTER_PLAN.md) gain budget | **50x** | 1.02k ohm |
| Analog engineering (01_ANALOG_ENGINEERING.md) | **50x** | 1.02k ohm |
| Development guide (DEVELOPMENT_GUIDE.md) | 500x | 100 ohm |

**The master plan's gain budget analysis is correct.** G=500 at the INA128 will saturate from electrode DC offset (300mV x 500 = 150V). The split-gain approach (50x INA128 + variable 4-10x post-filter = 200-500x total) is the right engineering decision.

**Resolution:** Standardize on G=50 (R_G = 1.02k ohm) everywhere. Update the wiki entity, concept paper, and development guide.

### 1.2 Feature Set

| Document | Features |
|---|---|
| Wiki entity | MAV, RMS, WL, ZC, SSC, Variance (6 time-domain) |
| Concept paper section 8 | MAV, RMS, WL, ZC + MNF, MDF (4 time + 2 freq) |
| DSP code (02_DIGITAL_ENGINEERING.md) | MAV, RMS, WL, ZC, SSC, VAR (6 time-domain) |
| Block diagrams | "MNF, MDF, spectral moments" referenced |

**Resolution:** Use 6 time-domain features (MAV, RMS, WL, ZC, SSC, VAR). These avoid the cost of FFT on the ESP32 and are sufficient per literature (Prakash et al. 2026 confirms time-domain features remain competitive for 6-8 gestures). MNF/MDF can be explored as a stretch goal. Update concept paper and block diagrams.

### 1.3 Op-Amp Choice

| Document | Op-Amp |
|---|---|
| Concept paper | TL074 |
| Development guide | TL072 |
| Analog engineering plan | **MCP6002** |

**Resolution:** MCP6002 is correct for single-supply (3.3-5V) operation. TL072/TL074 need dual supply (minimum +-3.5V). Standardize on MCP6002 everywhere.

### 1.4 Gesture Class 7

| Document | Gesture 7 |
|---|---|
| Wiki entity | Thumbs up |
| Concept paper (section 8.4.2) | Co-contraction (toggle grip mode) |

**Resolution:** Use "Thumbs up" as defined in the wiki entity. It has clear functional relevance (social gesture / mode select) and is more demonstrable than co-contraction.

---

## 2. Simulation Environment (BLOCKING for Phase 3)

The plan references LTspice XVII. **LTspice does not run natively on Linux.**

**Available on this machine:**
- ngspice 42 (installed, command-line SPICE simulator)
- KiCad 7.0.11 with built-in SPICE simulation (uses ngspice backend)

**Resolution:** Replace all LTspice references with ngspice. Write SPICE netlists (.cir files) for each stage. Use KiCad's integrated SPICE simulator for schematic-based simulation. This is actually better since the simulation runs directly from the KiCad schematic.

**Simulation plan:**
1. INA128: download TI SPICE model, create ngspice testbench
2. Sallen-Key HPF/LPF: pure SPICE (no special models needed)
3. Twin-T notch: pure SPICE
4. Combined chain: ngspice AC analysis
5. Monte Carlo: ngspice `.param` with `.mc` or `.step` directives

---

## 3. ML Environment (BLOCKING for Phase 7)

**Currently installed:** Python 3.11, numpy, scipy, scikit-learn, tflite-runtime

**Missing:** Full TensorFlow (needed for model training and quantization). tflite-runtime only runs inference.

**Resolution:**
```bash
pip3 install tensorflow --user   # or use a venv
# Verify: python3 -c "import tensorflow; print(tensorflow.__version__)"
```

Also missing: matplotlib (needed for confusion matrices and plots).

---

## 4. NinaPro DB5 Dataset (BLOCKING for Phase 7)

Not downloaded. Need to:
1. Register at https://ninapro.hevs.ch/ (free academic account)
2. Download DB5 (10 subjects, .mat files)
3. Store in `ml/data/ninapro_db5/`

---

## 5. Android Development Environment

**Not installed:** Android Studio, Android SDK, JDK

**Resolution options (ranked by FYP simplicity):**
1. **Web Bluetooth (recommended for FYP):** Write a Progressive Web App using Web Bluetooth API. Works on Chrome for Android. No Android Studio needed. HTML/JS only.
2. **Kotlin + Jetpack Compose:** Standard Android. Requires Android Studio (~2GB).
3. **Flutter:** Cross-platform. Requires Flutter SDK + Android SDK.

**Decision needed before Phase 9.**

---

## 6. 10-Page Proposal (Dr Irene's requirement)

The existing `01_sEMG_Prosthetic_Control.md` is a 40+ page technical design document, not a 10-page academic proposal. The proposal needs to follow this specific structure:

1. Background
2. Problem Statement
3. Main Objective
4. Specific Objectives
5. Related Studies
6. Methodology

And address Dr Irene's 6 points:
1. Demonstrate low-cost explicitly
2. Be specific on signals captured
3. State gesture count clearly
4. Define performance expectations
5. Describe the end demonstration
6. Follow the above structure

---

## 7. DOI Verification

The engineering-fyp skill warns: "AI-generated DOIs are often wrong." All 10 references in the concept paper need verification via web search before inclusion in the proposal.

---

## 8. Missing Infrastructure

| Item | Status | Action |
|---|---|---|
| Project folder structure (firmware/, hardware/, ml/, etc.) | Not created | Create per master plan |
| Lab notebook (LOG.md) | Template exists, no entries | Start on first work session |
| Paper draft (PAPER_DRAFT.md) | Template exists, mostly empty | Fill as phases complete |
| Decisions log (DECISIONS.md) | Template exists, empty | Record gain decision immediately |
| Python virtual environment for ML | Not set up | Create with TF, numpy, scipy, sklearn, matplotlib |
| SPICE model library | Not downloaded | Get INA128, MCP6002 SPICE models |

---

## 9. BOM / Procurement Readiness

The BOM in `05_BOM_AND_PROCUREMENT.md` is thorough but:
- No actual orders have been placed
- INA128 availability in Kenya needs verification
- Lead times for Mouser/JLCPCB need to be factored into sequence

---

## 10. Scope Risk: 3D Printing Access

The plan assumes access to a 3D printer for the gripper. Need to confirm:
- Does JKUAT have accessible 3D printers?
- If not, identify a printing service in Nairobi
- Gripper is NOT core contribution; can be substituted with an off-the-shelf robotic gripper

---

## Summary Priority Matrix

| Gap | Severity | Fix Effort |
|---|---|---|
| INA128 gain inconsistency | HIGH | Low (update docs) |
| LTspice to ngspice | HIGH | Medium (rewrite sim steps) |
| TensorFlow not installed | HIGH | Low (pip install) |
| 10-page proposal not written | HIGH | High (write now) |
| NinaPro DB5 not downloaded | MEDIUM | Low (register + download) |
| DOI verification | MEDIUM | Medium (web search each) |
| Feature set inconsistency | MEDIUM | Low (standardize) |
| Op-amp inconsistency | MEDIUM | Low (update docs) |
| Project folder structure | LOW | Low (mkdir) |
| Android dev environment | LOW | Defer to Phase 9 |
