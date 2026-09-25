# Simulation: INA128 Gain Verification

**Status:** PASS -- completed 2026-09-25
**Tool:** ngspice v42 (behavioral macromodel)
**Related module:** [[modules/analog-front-end]]

## Setup
- Model: `hardware/simulation/models/INA128_ngspice.lib` (E-source + RC pole, G=50, BW=200 kHz)
- TI PSpice model (`INA12x.LIB`) present but ngspice-incompatible (`vswitch` not supported)
- Input: 1 mV AC differential, 1 Hz -- 1 MHz sweep, +5 V single supply, Vref=2.5 V

## Results

| Metric | Target | Actual | Pass/Fail |
|---|---|---|---|
| Gain at 100 Hz | +34 dB (50x) | -26.0 dBV (= 34 dB relative to 1 mV input) | PASS |
| Gain at 1 kHz | flat | -26.0 dBV | PASS |

**Interpretation:** -26 dBV is the absolute output for 1 mV input. Relative gain = 50x = +34 dB. Flat through sEMG band.

## Files
- Netlist: `hardware/simulation/01_INA128_gain.cir`
- Bode plot: `hardware/simulation/bode_plots.png` panel 1
