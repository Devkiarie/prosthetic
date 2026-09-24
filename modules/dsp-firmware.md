# Module 3: DSP and Feature Extraction Firmware

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#D3|Block D3 -- ESP32 ADC]] and [[docs/COMPREHENSIVE_PLAN#C6|Block C6 -- C Implementation]]

---

## Architecture

```
Timer ISR (2 kHz) --> ADC DMA (4 channels) --> Ring Buffer
                                                    |
                                              Feature Extraction Task (Core 0)
                                              6 features x 4 channels = 24
                                                    |
                                              Feature Queue --> Inference Task (Core 1)
```

## Features (per 250ms window, per channel)

1. MAV -- Mean Absolute Value
2. RMS -- Root Mean Square
3. WL -- Waveform Length
4. ZC -- Zero Crossings (thresholded)
5. SSC -- Slope Sign Changes (thresholded)
6. VAR -- Variance

See [[docs/decisions/DECISIONS#Features]] for why no FFT.

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| Sampling rate | 2000 Hz +/- 1 Hz |
| ADC noise floor | < 5 LSB RMS |
| Feature error (C vs Python) | < 1% |
| Feature extraction time | < 25 ms per window |
| CPU utilization | < 60% |
| Continuous operation | Zero drops in 10 min |

## Log

_Entries added as work progresses._
