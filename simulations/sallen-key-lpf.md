# Simulation: Sallen-Key Low-Pass Filter 500 Hz

**Status:** PASS -- completed 2026-09-25 (simulated as part of sim 02 bandpass)
**Tool:** ngspice v42
**Related module:** [[modules/analog-front-end]]

## Design values
- R1 = R2 = 33 kΩ (E12 standard), C1 = C2 = 10 nF
- fc = 1/(2π × 33k × 10n) = 482 Hz ≈ 500 Hz
- Same Butterworth K=1.586 gain as HPF stage

## Results

| Metric | Target | Actual | Pass/Fail |
|---|---|---|---|
| Upper -3 dB | ~500 Hz | 492 Hz | PASS |
| Gain at 250 Hz (passband) | ~+7.7 dB | 7.68 dBV | PASS |

## Files
- Netlist: `hardware/simulation/02_sallen_key_bandpass.cir` (combined HPF+LPF)
- Bode plot: `hardware/simulation/bode_plots.png` panel 2
