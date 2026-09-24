# Module 2: Custom 4-Channel PCB

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#D4|Block D4 -- PCB Design and Fabrication]]
**Depends on:** [[modules/analog-front-end]] (single-channel validated first)

---

## Specs

- 4-layer stackup: signal / analog GND / power / digital
- Board size: ~80mm x 60mm
- Tool: KiCad 7
- Fab: JLCPCB (5 pcs, DHL to Kenya)

## Layout Zones

| Zone | Contents |
|---|---|
| A: Input (left) | 4x electrode connectors, input protection |
| B: Analog (center) | 4x INA128, filters, gain stages, test points |
| C: Digital (right) | ESP32 header, PCA9685, servo connectors |
| D: Power (bottom) | Battery connector, LDO, Vref, bulk caps |

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| Crosstalk (all 6 pairs) | < -40 dB |
| Analog supply ripple | < 10 mV pk-pk |
| All 4 channels pass individual tests | Same as [[modules/analog-front-end]] |

## Log

_Entries added as work progresses._
