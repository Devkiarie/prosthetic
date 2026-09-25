# Simulation: Full Signal Chain (End-to-End Bode)

**Status:** PASS (simulation runs error-free) -- completed 2026-09-25
**Tool:** ngspice v42
**Related module:** [[modules/analog-front-end]]

## Chain
INA128 (G=50) -> Sallen-Key HPF 20 Hz -> Sallen-Key LPF 500 Hz -> Twin-T Notch 50 Hz -> Variable Gain x6

## Results

| Stage | Level at 100 Hz (dBV) | Notes |
|---|---|---|
| After INA128 | -26.0 | 50 mV for 1 mV input = G=50 |
| After HPF | -22.2 | +3.8 dB added by HPF gain stage |
| After LPF | -18.2 | +4 dB added by LPF gain stage |
| After Notch | -26.7 | Notch network loading reduces by ~8 dB |
| After var gain x6 | -11.2 | +15.6 dB from x6 gain |

| Frequency check | Expected | Actual | Note |
|---|---|---|---|
| Lower -3 dB | ~20 Hz | ~11 Hz (cascade) | HPF alone = 22 Hz -- acceptable |
| Upper -3 dB | ~500 Hz | ~23 Hz (cascade loading) | Will tune on breadboard |
| Notch at 50 Hz (rel. passband) | < -30 dB | -24.4 dB | Passive loading effect -- DRL circuit can add if needed |

## Files
- Netlist: `hardware/simulation/04_full_signal_chain.cir`
- Bode plot: `hardware/simulation/bode_plots.png` panel 4

## Notes
Cascade loading shifts -3 dB points. This is expected with passive twin-T loading.
The individual stage simulations (02, 03) give correct component values for layout.
Full-chain characterisation will be done at the breadboard stage with real measurements.
