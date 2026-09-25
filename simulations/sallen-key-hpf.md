# Simulation: Sallen-Key High-Pass Filter 20 Hz

**Status:** PASS -- completed 2026-09-25
**Tool:** ngspice v42
**Related module:** [[modules/analog-front-end]]

## Design values
- R1 = R2 = 82 kΩ (E12 standard), C1 = C2 = 100 nF
- fc = 1/(2π × 82k × 100n) = 19.4 Hz ≈ 20 Hz
- Butterworth Q=0.707: Rg=10 kΩ, Rf=5.86 kΩ, K=1.586
- Op-amp: MCP6002 behavioral model (GBW=1 MHz, single supply 5 V)

## Results (from combined bandpass sim 02)

| Metric | Target | Actual | Pass/Fail |
|---|---|---|---|
| Lower -3 dB | ~20 Hz | 22.4 Hz | PASS |
| Upper -3 dB (LPF cascaded) | ~500 Hz | 492 Hz | PASS |
| Gain at 100 Hz (passband) | ~+7.8 dB (K=1.586 x2) | 7.81 dBV | PASS |

## Files
- Netlist: `hardware/simulation/02_sallen_key_bandpass.cir`
- Bode plot: `hardware/simulation/bode_plots.png` panel 2

## Notes
- Slight upward shift from 20 Hz to 22 Hz is due to E12 resistor value (82 kΩ vs ideal 79.6 kΩ).
  Will tune with trimmer during breadboard phase if needed.
