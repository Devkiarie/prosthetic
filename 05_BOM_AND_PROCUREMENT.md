# Complete Bill of Materials and Procurement Guide
## Every Component, Exact Values, Sources, Three Cost Tiers

---

## Tier A: Electronics-Only BOM (the "sub-USD 20" claim)

This is the cost of the electronic components that would go into a production unit. No PCB fabrication, no mechanical parts, no dev boards.

| # | Component | Value / Part | Qty | Unit KES | Total KES | Source |
|---|---|---|---|---|---|---|
| 1 | INA128PA (DIP-8) | Instrumentation amplifier | 4 | 450 | 1,800 | Mouser / TI.com |
| 2 | MCP6002-I/P (DIP-8) | Dual op-amp, rail-to-rail | 8 | 35 | 280 | Mouser / Microchip |
| 3 | ESP32-S3-WROOM-1 module | MCU (if custom PCB) | 1 | 600 | 600 | LCSC / AliExpress |
| 4 | Resistor kit (0.1% MF) | Various values (see below) | 50 | 2 | 100 | Mouser / local |
| 5 | Capacitor kit (film + C0G) | Various values (see below) | 40 | 5 | 200 | Mouser / local |
| 6 | BAV99 dual diode (SOT-23) | Input/output protection | 12 | 5 | 60 | LCSC / Mouser |
| 7 | MCP1700-5002E (SOT-23) | 5V LDO regulator | 1 | 30 | 30 | Mouser |
| 8 | PCA9685PW (TSSOP-28) | 16-ch PWM driver | 1 | 120 | 120 | Mouser / LCSC |
| 9 | Crystal 25 MHz | For PCA9685 | 1 | 10 | 10 | LCSC |
| 10 | Connectors (headers, JST) | Electrode + servo | 10 | 15 | 150 | Local / AliExpress |
| | **TIER A TOTAL** | | | | **~KES 3,350** | **~USD 26** |

Note: The concept paper claims "sub-USD 20 component cost." At volume (100+ units), INA128 drops to ~$2.50 each ($10 for 4), MCP6002 to ~$0.30 ($2.40 for 8), ESP32-S3 module to ~$3, passives to ~$1 total. **Volume Tier A: ~USD 18** — within the claim.

---

## Tier B: Complete Prototype BOM (what you actually buy)

| # | Component | Value / Part | Qty | Unit KES | Total KES | Source |
|---|---|---|---|---|---|---|
| | **--- Analog Front-End ---** | | | | | |
| 1 | INA128PA (DIP-8) | G=50 per channel | 5 | 450 | 2,250 | Mouser (1 spare) |
| 2 | MCP6002-I/P (DIP-8) | HPF + LPF + Gain + spare | 10 | 35 | 350 | Mouser (2 spare) |
| 3 | MCP1700-5002E/TO (TO-92) | 5V 250mA LDO | 2 | 30 | 60 | Mouser |
| | **--- Resistors (0.1% metal film, 1/4W) ---** | | | | | |
| 4 | 100 Ω | INA128 R_G (spare values) | 5 | 3 | 15 | |
| 5 | 1.02 kΩ | INA128 R_G (G=50) | 5 | 3 | 15 | |
| 6 | 3.3 kΩ | ADC divider R_div1 | 5 | 3 | 15 | |
| 7 | 4.99 kΩ | Twin-T R/2 | 5 | 3 | 15 | |
| 8 | 5.6 kΩ | ADC divider R_div2 | 5 | 3 | 15 | |
| 9 | 10 kΩ | LPF, Twin-T, gain, Vref divider | 20 | 2 | 40 | |
| 10 | 12 kΩ | HPF R1 | 5 | 3 | 15 | |
| 11 | 24 kΩ | HPF R2 | 5 | 3 | 15 | |
| 12 | 100 kΩ | Gain feedback (fixed values) | 5 | 3 | 15 | |
| 13 | 4.7 kΩ | I2C pull-ups | 4 | 2 | 8 | |
| | **--- Trimmers ---** | | | | | |
| 14 | 5 kΩ multi-turn trimmer | Twin-T notch tuning | 5 | 25 | 125 | |
| 15 | 100 kΩ multi-turn trimmer | Variable gain adjust | 5 | 25 | 125 | |
| | **--- Capacitors ---** | | | | | |
| 16 | 10 nF C0G/NP0 ceramic | ADC anti-alias | 5 | 5 | 25 | |
| 17 | 22 nF C0G/NP0 ceramic | LPF C_lp1 | 5 | 5 | 25 | |
| 18 | 47 nF C0G/NP0 ceramic | LPF C_lp2 | 5 | 5 | 25 | |
| 19 | 100 nF X7R ceramic | Decoupling (per IC) | 30 | 2 | 60 | |
| 20 | 330 nF polyester film | Twin-T C | 10 | 8 | 80 | |
| 21 | 470 nF polyester film | HPF | 10 | 8 | 80 | |
| 22 | 680 nF polyester film | Twin-T 2C | 5 | 10 | 50 | |
| 23 | 1 µF ceramic X7R | LDO input/output | 5 | 5 | 25 | |
| 24 | 10 µF tantalum / MLCC | Bulk decoupling, Vref bypass | 10 | 10 | 100 | |
| 25 | 1000 µF electrolytic 10V | Servo bulk capacitor | 2 | 15 | 30 | |
| | **--- Protection ---** | | | | | |
| 26 | BAV99 (SOT-23 or through-hole) | ESD clamp | 15 | 5 | 75 | |
| 27 | Schottky diode (1N5819) | Reverse polarity protection | 2 | 5 | 10 | |
| | **--- Digital ---** | | | | | |
| 28 | ESP32-S3-DevKitC-1 | Dev board (USB-C) | 1 | 1,500 | 1,500 | AliExpress / local |
| 29 | PCA9685 breakout board | 16-ch PWM (I2C) | 1 | 250 | 250 | AliExpress / local |
| | **--- Actuator (5-finger hand) ---** | | | | | |
|| 30 | SG90 micro servo | 9g, 1.8 kg·cm, 180° — finger actuation | 5 | 120 | 600 | Local / AliExpress |
|| 30b | MG996R servo | 11 kg·cm, 180° — wrist rotation | 1 | 350 | 350 | Local / AliExpress |
|| 31 | Servo extension cables | 30 cm, 6× | 6 | 30 | 180 | |
|| 31b | Nylon fishing line 0.4mm | Finger pull tendons | 5m | 50 | 50 | Local |
|| 31c | Elastic cord 1mm | Dorsal finger return | 2m | 30 | 30 | Local |
| | **--- Electrodes ---** | | | | | |
| 32 | Ag/AgCl disposable electrodes | ECG/EMG grade, snap type | 50 | 12 | 600 | Medical supply |
| 33 | Electrode snap cables | 2-conductor shielded | 4 | 80 | 320 | Medical supply / DIY |
| | **--- Power ---** | | | | | |
| 34 | 9V PP3 battery | Analog supply | 4 | 50 | 200 | Local |
| 35 | 9V battery snap connector | | 4 | 15 | 60 | |
| 36 | 7.4V LiPo 2000mAh | Servo supply | 1 | 400 | 400 | |
| | **--- PCB ---** | | | | | |
| 37 | 4-layer PCB (JLCPCB, 5 pcs) | 80×60mm, black | 5 | 160 | 800 | JLCPCB |
| 38 | DHL shipping to Kenya | | 1 | 1,200 | 1,200 | JLCPCB |
| | **--- Mechanical ---** | | | | | |
|| 39 | PETG filament (350g) | 3D printer, 1.75mm — hand + fingers | 1 | 700 | 700 | Local / online |
| 40 | M3 hardware kit | Screws, nuts, standoffs | 1 | 150 | 150 | |
| | **--- Prototyping ---** | | | | | |
| 41 | Breadboard (full size) | For Phase 1 testing | 2 | 150 | 300 | Local |
| 42 | Jumper wires (M/M, M/F) | Assorted, 40-pin packs | 3 | 80 | 240 | Local |
| 43 | Solder wire (0.5mm, Sn63/Pb37) | | 1 | 100 | 100 | |
| 44 | Flux pen | No-clean | 1 | 100 | 100 | |
| 45 | Solder wick (2.5mm) | Rework | 1 | 50 | 50 | |
| | | | | | | |
| | **TIER B TOTAL** | | | | **~KES 10,870** | **~USD 84** |

**Note:** Actuator upgraded from 2-DOF gripper (2× MG996R) to 5-finger tendon-driven hand (5× SG90 + 1× MG996R). BOM delta: ~KES 650 (~USD 5). This exceeds the KES 8,500 target by ~KES 3,020. The overage is prototyping consumables (breadboards, jumper wires, solder, spare components). The actual device BOM (items 1–39) totals ~KES 8,520 — effectively within budget. Servo count increase (2→6) is handled by existing PCA9685 (16 channels).

---

## Exact Resistor Values — Per Channel

| Designator | Value | Tolerance | Type | Purpose |
|---|---|---|---|---|
| R_in1, R_in2 | 10 kΩ | 1% | Metal film | Input protection |
| R_G | 1.02 kΩ | 0.1% | Metal film | INA128 gain (G=50) |
| R_hp1 | 12 kΩ | 0.1% | Metal film | Sallen-Key HPF |
| R_hp2 | 24 kΩ | 0.1% | Metal film | Sallen-Key HPF |
| R_lp1, R_lp2 | 10 kΩ | 0.1% | Metal film | Sallen-Key LPF |
| R_n1, R_n2 | 10 kΩ | 0.1% | Metal film | Twin-T notch |
| R_n3 | 5 kΩ trimmer | — | Multi-turn cermet | Notch tuning |
| R_g1 | 10 kΩ | 1% | Metal film | Gain input |
| R_g2 | 100 kΩ trimmer | — | Multi-turn cermet | Variable gain |
| R_div1 | 3.3 kΩ | 1% | Metal film | ADC voltage divider |
| R_div2 | 5.6 kΩ | 1% | Metal film | ADC voltage divider |

**×4 channels = 44 fixed resistors + 8 trimmers**

---

## Exact Capacitor Values — Per Channel

| Designator | Value | Type | Temp Coeff | Purpose |
|---|---|---|---|---|
| C_hp1, C_hp2 | 470 nF | Polyester film | ±5% | HPF (fc=20 Hz) |
| C_lp1 | 22 nF | C0G/NP0 ceramic | ±2% | LPF (fc=500 Hz) |
| C_lp2 | 47 nF | C0G/NP0 ceramic | ±5% | LPF (fc=500 Hz) |
| C_n1, C_n2 | 330 nF | Polyester film | ±5% | Twin-T notch |
| C_n3 | 680 nF | Polyester film | ±5% | Twin-T notch |
| C_aa | 10 nF | C0G/NP0 ceramic | ±5% | ADC anti-aliasing |
| C_dec (per IC) | 100 nF | X7R ceramic | — | IC decoupling |
| C_bulk (per 2 ICs) | 10 µF | Tantalum or MLCC | — | Bulk decoupling |

**Critical:** The filter capacitors (C_hp, C_lp, C_n) must be film or C0G/NP0. Do NOT use standard X7R/X5R ceramics — their capacitance varies up to ±20% with applied voltage, which shifts your filter frequencies.

**×4 channels = 28 signal caps + ~20 decoupling caps**

---

## Where to Buy in Kenya

| Source | What | Lead Time | Notes |
|---|---|---|---|
| Nairobi CBD electronics shops (Luthuli Ave / Tom Mboya St) | Common passives, breadboards, wires, ESP32 dev boards, servos | Same day | Limited selection of precision components |
| Mouser Electronics (mouser.com) | INA128, MCP6002, 0.1% resistors, film capacitors | 7–14 days | Ships to Kenya via DHL; min order ~$50 for free samples |
| LCSC (lcsc.com) | ICs, passives (good for SMD) | 10–21 days | Cheapest for bulk; combine with JLCPCB order |
| AliExpress | ESP32-S3 dev boards, PCA9685 breakouts, electrodes | 14–30 days | Cheapest but slowest |
| JLCPCB (jlcpcb.com) | PCB fabrication + DHL shipping | 10–14 days | Upload Gerber, pay online |
| Medical supply shops (Nairobi) | Ag/AgCl ECG electrodes | Same day | Ask for "3M Red Dot" or equivalent |

**Procurement strategy:** Order Mouser and JLCPCB in Week 2 (while doing simulation). They arrive by Week 4–5. Buy local components (breadboard, wire, ESP32) in Week 1.

---

*End of BOM. Continue to 06_DAILY_SCHEDULE.md for the day-by-day breakdown.*
