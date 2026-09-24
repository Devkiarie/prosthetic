# Design Decisions Log

## Format
Each entry: Date | Decision | Alternatives Considered | Rationale | Risk

---

## [2026-09-24] INA128 Gain = 50x (R_G = 1.02k ohm)

**Alternatives:** G=500 (R_G=100 ohm) as in original concept paper

**Rationale:** Electrode DC offset of ~300mV multiplied by G=500 produces 150V at INA128 output, causing immediate saturation. Split-gain approach: G=50 at INA128, then HPF removes DC before post-filter variable gain (4x to 10x) brings total to 200-500x. This keeps the INA128 output within rails while still achieving needed total gain.

**Risk:** Variable gain stage adds one more adjustment per channel. Mitigated by multi-turn trimmer potentiometer.

---

## [2026-09-24] Op-Amp = MCP6002 (not TL072/TL074)

**Alternatives:** TL072 (dual, common), TL074 (quad, common)

**Rationale:** TL072/TL074 require dual supply (minimum +-3.5V = 7V total). The design uses a single 5V analog supply from a 9V battery via LDO. MCP6002 operates from 1.8V to 6V single supply, has rail-to-rail output, 1 MHz GBW (sufficient for 500 Hz filters), and 8.5 nV/sqrt(Hz) noise.

**Risk:** MCP6002 has lower GBW (1 MHz vs 3 MHz for TL072). Not a problem since highest filter frequency is 500 Hz.

---

## [2026-09-24] Features = 6 Time-Domain Only (no FFT)

**Alternatives:** Include MNF and MDF (frequency-domain, require FFT)

**Rationale:** Literature consensus (Prakash 2026): time-domain features (MAV, RMS, WL, ZC, SSC, VAR) remain competitive with deep-learning features for 6-8 gestures. FFT on ESP32-S3 adds ~15ms per window and 4KB stack. Keeping to time-domain simplifies the DSP pipeline, reduces latency, and avoids ESP32 memory pressure. MNF/MDF can be added as a stretch goal if F1 < 0.85.

**Risk:** If time-domain features alone do not achieve F1 > 0.85, FFT features can be added later.

---

## [2026-09-24] Simulation Tool = ngspice (not LTspice)

**Alternatives:** LTspice XVII (Windows only, could use Wine)

**Rationale:** ngspice is natively installed on this Linux machine (v42). KiCad 7 uses ngspice as its SPICE backend, so simulations can run directly from KiCad schematics. LTspice on Wine is fragile and adds complexity. SPICE netlists are fully portable between simulators.

**Risk:** ngspice lacks LTspice's graphical ease. Mitigated by using KiCad's integrated SPICE simulation GUI.
