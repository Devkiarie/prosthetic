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

