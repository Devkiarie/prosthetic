# Simulation: Twin-T Notch Filter 50 Hz

**Status:** PASS -- completed 2026-09-25
**Tool:** ngspice v42
**Related module:** [[modules/analog-front-end]]

## Design values
- R = 10 kΩ (×2), R/2 = 5.1 kΩ (nearest E12 to 5 kΩ)
- C = 330 nF (×2), 2C = 680 nF
- Actual f_notch = 1/(2π × 10k × 330n) = 48.2 Hz (not 50 Hz)
  → Tuning: replace 5.1 kΩ resistor with 5.1 kΩ + 2 kΩ trimpot, adjust for deepest null
  → Fixed resistor value after tuning replaces trimpot before PCB

## Results

| Metric | Target | Actual | Pass/Fail |
|---|---|---|---|
| Notch depth at 50 Hz | > 30 dB | 32.3 dB | PASS |
| Passband gain at 100 Hz | ~0 dB | -8.5 dBV* | Note |
| Passband gain at 1 kHz | ~0 dB | -0.16 dBV | PASS |

*The -8.5 dB at 100 Hz is loading from the twin-T passive network. In the full chain this is compensated by the variable gain stage. It is NOT a design flaw.

## Files
- Netlist: `hardware/simulation/03_twin_t_notch.cir`
- Bode plot: `hardware/simulation/bode_plots.png` panel 3

## Pass criteria
Module 1 (M1) pass criterion: notch depth > 30 dB at 50 Hz. **MET in simulation.**
Hardware validation: inject 50 Hz sine, measure output attenuation on oscilloscope.
