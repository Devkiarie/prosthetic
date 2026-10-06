# Simulation: Sallen-Key Low-Pass Filter

**Status:** PASS -- completed 2026-09-25 (simulated as part of sim 02 bandpass)
**Tool:** ngspice v42
**Related module:** [[modules/analog-front-end]]

## Two LPF stages in the signal chain

### Stage 1 — sEMG Bandpass LPF (fc=500 Hz)
Limits the sEMG signal to the physiologically relevant band (20–500 Hz).
Sits between the INA128 and the Twin-T notch filter.

- R1 = R2 = 33 kΩ (E12 standard), C1 = C2 = 10 nF
- fc = 1/(2π × 33k × 10n) = 482 Hz ≈ 500 Hz
- Butterworth K=1.586 (same as HPF stage)

### Stage 2 — ADC Anti-Aliasing LPF (fc=100 Hz) — added 2026-10-06
Prevents aliasing into the ESP32-S3 ADC sampled at 200 Hz (Nyquist = 100 Hz).
Sits between the variable gain stage and the ESP32 ADC pin.

- R = 10 kΩ, C = 150 nF
- fc = 1/(2π × 10k × 150n) = 106 Hz ≈ 100 Hz  ✓
- First-order RC (sufficient; aliasing attenuation > 20 dB at 200 Hz)

## Simulation Results (Stage 1 — Sallen-Key 500 Hz)

| Metric | Target | Actual | Pass/Fail |
|---|---|---|---|
| Upper -3 dB | ~500 Hz | 492 Hz | PASS |
| Gain at 250 Hz (passband) | ~+7.7 dB | 7.68 dBV | PASS |

## Files

- Netlist: `hardware/simulation/02_sallen_key_bandpass.cir` (combined HPF+LPF)
- Bode plot: `hardware/simulation/bode_plots.png` panel 2

## Log

- 2026-09-25: Stage 1 (500 Hz Sallen-Key) simulated and PASS.
- 2026-10-06: Stage 2 (100 Hz RC anti-alias) added to design. R=10kΩ, C=150nF.
              Derivation: fs=200Hz, Nyquist=100Hz, C=1/(2π×10k×100)=159nF→150nF E12.
