# Module 1: Analog Front-End

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#D2|Block D2 -- Single-Channel Breadboard Build]]
**Simulation:** [[simulations/ina128-gain]], [[simulations/sallen-key-hpf]], [[simulations/sallen-key-lpf]], [[simulations/twin-t-notch]], [[simulations/full-chain]]

---

## Architecture (per channel)

```
Electrode(+) --[10k protection]-- INA128 IN+
Electrode(-) --[10k protection]-- INA128 IN-
                                    |
                              INA128 (G=50, R_G=1.02k)
                                    |
                              Sallen-Key HPF (fc=20Hz)
                              C=470nF, R1=12k, R2=24k
                                    |
                              Sallen-Key LPF (fc=500Hz)
                              R=10k, C1=22nF, C2=47nF
                                    |
                              Twin-T Notch (50Hz)
                              R=10k, C=330nF, R/2=5k trim, 2C=680nF
                                    |
                              Variable Gain (MCP6002, x1-x10)
                                    |
                              Divider (3.3k/5.6k) + BAV99 clamp
                                    |
                              ESP32-S3 ADC1
```

## Key Components

| Part | Value | Why |
|---|---|---|
| INA128PA | G=50 via R_G=1.02k | Split gain avoids DC offset saturation. See [[docs/decisions/DECISIONS#INA128 Gain]] |
| MCP6002 | Single-supply op-amp | Works 1.8-6V, rail-to-rail. See [[docs/decisions/DECISIONS#Op-Amp]] |
| Film capacitors | 470nF, 330nF, 680nF | Stable, no voltage-dependent drift (unlike X7R ceramic) |
| C0G/NP0 ceramic | 22nF, 47nF | Low tolerance for LPF accuracy |

## Pass/Fail Criteria

| Metric | Target | How to measure |
|---|---|---|
| SNR | > 20 dB | Oscilloscope: RMS(contraction) / RMS(rest) |
| Bandpass -3dB | 20 Hz and 500 Hz | Signal generator frequency sweep |
| 50 Hz rejection | > 30 dB | 50 Hz injection, measure attenuation |
| Noise floor | < 50 uV RMS (input-referred) | Input shorted, measure output |

## Log

_Entries added as work progresses._
