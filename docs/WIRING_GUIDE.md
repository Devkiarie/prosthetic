# sEMG Prosthetic — Breadboard Wiring Guide
**Version:** v1.0 | 2026-10-18
**Schematic source:** `hardware/kicad/semg_afe.kicad_sch`
**PDF:** `hardware/kicad/semg_afe_schematic.pdf`

---

## Power Supply

| From | To | Wire | Notes |
|---|---|---|---|
| 9V battery (+) | LM7805 IN | Red | Through power switch |
| LM7805 GND | Battery (−) | Black | |
| LM7805 OUT | +5V rail | Orange | All ICs, servo VCC |
| 9V battery (−) | AGND rail | Black | Analog ground star point |
| ESP32-S3 3.3V | 3.3V rail | Yellow | For REF divider only |
| ESP32-S3 GND | AGND | Black | **Single star point — join AGND to DGND here only** |

> **Critical:** Use separate analog ground (AGND) and digital ground (DGND). Join them at ONE point only — at the ESP32-S3 GND pin. Connecting them at multiple points creates ground loops that couple 50Hz noise into µV sEMG signals.

---

## Electrode → INA128 (per channel, repeat × 4)

| Electrode | INA128 Pin | Via | Notes |
|---|---|---|---|
| Muscle (+) electrode | Pin 3 (IN+) | 10kΩ protection resistor | Ag/AgCl snap lead wire |
| Muscle (−) electrode | Pin 2 (IN−) | 10kΩ protection resistor | 20mm from (+) electrode |
| Body REF electrode | All INA128 REF (pin 5) | — | Bony prominence (wrist styloid). Connect to 1.65V REF buffer output |

---

## INA128 Connections (per channel, repeat × 4)

| INA128 Pin | Connect to | Value | Notes |
|---|---|---|---|
| Pin 1 (RG−) | One end of RG | — | |
| Pin 8 (RG+) | Other end of RG | **RG = 1kΩ 1% MF** | G = 1 + 49.4k/1k = 50.4 ≈ 51× |
| Pin 2 (IN−) | − electrode via 10kΩ | — | |
| Pin 3 (IN+) | + electrode via 10kΩ | — | |
| Pin 4 (V−) | −9V or AGND | — | Use −9V for true differential; AGND for single-supply |
| Pin 5 (REF) | 1.65V REF buffer output | — | **Do NOT leave floating** |
| Pin 6 (OUT) | Next stage (Sallen-Key HPF) | — | |
| Pin 7 (V+) | +5V (from LM7805) | — | |
| 10µF + 100nF caps | Pin 7 to AGND, Pin 4 to AGND | — | Power decoupling, place close to IC |

---

## Sallen-Key HPF (High-Pass Filter, fc = 20 Hz) — per channel

Removes DC electrode offset. Topology: 2nd-order Butterworth, unity-gain.

| Node | Component | Value | To |
|---|---|---|---|
| INA128 OUT → | C_HPF1 | **470nF film** | → R_HPF1 |
| → R_HPF1 | **12kΩ** | → MCP6002 IN+ |
| → C_HPF2 | **470nF film** | → junction (feedback) |
| → R_HPF2 | **24kΩ** | → MCP6002 OUT (feedback) |
| MCP6002 IN− | — | → MCP6002 OUT (unity gain follower feedback) | |
| MCP6002 OUT | → | Next stage (LPF) | |

> **Use film capacitors only** (polyester or polypropylene). X7R ceramic drifts with voltage and temperature — degrades filter accuracy.

---

## Sallen-Key LPF (Low-Pass Filter, fc = 100 Hz) — per channel

Anti-aliasing for 2kHz ADC. Removes noise above 100Hz.

| Component | Value | Notes |
|---|---|---|
| R_LPF (×2) | **10kΩ** | |
| C_LPF (×2) | **150nF C0G ceramic** | C0G = stable with temperature |
| Op-amp | MCP6002 | Same IC, second channel |

Topology: same 2nd-order Sallen-Key as HPF above. fc = 1/(2π × 10k × 150n) = 106 Hz ≈ 100 Hz.

---

## Twin-T Notch Filter (50 Hz) — per channel

Rejects 50Hz Kenya mains interference. Most critical and most sensitive stage.

| Component | Value | Notes |
|---|---|---|
| R_N1, R_N2 | **10kΩ 1% MF** | Must be 1% — 5% degrades notch from 40dB to 20dB |
| R_N_HALF | **5.1kΩ 1% + 2kΩ trimpot** | Adjust trimpot for deepest null on oscilloscope |
| C_N1, C_N2 | **330nF 1%** | |
| C_N_DOUBLE | **680nF 1%** | = 2 × C_N |

**Tuning procedure:** Inject 50Hz sine wave at input. Adjust trimpot until output is minimum on oscilloscope. Target: > 30dB rejection. Replace trimpot with nearest fixed resistor value after tuning.

---

## Variable Gain Stage — per channel

Sets final amplification. Adjust per subject as needed.

| Component | Value | Notes |
|---|---|---|
| MCP6002 | Non-inverting amp | |
| RV (potentiometer) | **100kΩ B100K** | B-taper (linear). NOT A-taper, NOT 10kΩ |
| Wiper → Rin | **10kΩ fixed** | Sets minimum gain at 1× (wiper at GND) |
| Gain range | **1× to 11×** | Total chain: 51 × (1 to 11) = 51 to 561× |
| BAV99 clamp | Anode to AGND, Cathode to 3.3V | Protects ADC from overvoltage |
| Series resistor | **1kΩ** | Between variable gain output and ADC pin |
| Anti-alias cap | **33nF** | ADC pin to AGND (fc ≈ 4.8 kHz — passes 2kHz, blocks RF) |

---

## 1.65V REF Buffer (shared — one for all 4 channels)

Sets the mid-supply bias so sEMG signal (bipolar) swings around 1.65V within the ESP32-S3 ADC range.

| Component | Value | Connection |
|---|---|---|
| R_DIV1 | **10kΩ** | 3.3V → node A |
| R_DIV2 | **10kΩ** | node A → AGND |
| C_REF | **100nF** | node A → AGND (decoupling) |
| MCP6002 IN+ | → node A | — |
| MCP6002 IN− | → MCP6002 OUT | Unity gain follower |
| MCP6002 OUT | → all 4× INA128 Pin 5 (REF) | Low-impedance 1.65V reference |

> node A voltage = 3.3V × 10k/(10k+10k) = **1.65V**. The buffer drives the REF pin with low output impedance — no voltage sag under load.

---

## ESP32-S3 ADC Connections (per channel)

| ADC Channel | GPIO Pin | Signal | Notes |
|---|---|---|---|
| ADC1_CH0 | **GPIO 1** | CH1 — FCR (inner forearm, 1/3 from elbow) | |
| ADC1_CH1 | **GPIO 2** | CH2 — ED (back of forearm, 1/3 from elbow) | |
| ADC1_CH2 | **GPIO 3** | CH3 — FDS (inner forearm, midpoint) | |
| ADC1_CH3 | **GPIO 4** | CH4 — BR (outer forearm, elbow crease) | |

> **NEVER use ADC2** (GPIO 11-20). ADC2 is disabled when BLE is active (ESP32-S3 errata ADC-183).
> ADC input range: 0.15V – 2.45V (11dB attenuation). Signal centred at 1.65V → swing ±1V = 0.65–2.65V → trim gain to stay inside 0.15–2.45V.

---

## PCA9685 Servo Driver Connections

| PCA9685 Pin | ESP32-S3 Pin / Rail | Notes |
|---|---|---|
| VCC | 3.3V | Logic supply |
| GND | DGND | |
| SDA | GPIO 8 | I2C data |
| SCL | GPIO 9 | I2C clock |
| V+ | 5V (from LM7805) | Servo power rail — separate from logic |
| OE (Output Enable) | GND or 3.3V via 10kΩ pull-down | Active LOW — pull to GND to enable outputs |

### Servo Channel Assignment

| PCA9685 CH | Servo | Joint | Gesture posture |
|---|---|---|---|
| CH0 | SG90 | Thumb | 0°=extended, 180°=curled |
| CH1 | SG90 | Index finger | 0°=extended, 180°=curled |
| CH2 | SG90 | Middle finger | 0°=extended, 180°=curled |
| CH3 | SG90 | Ring finger | 0°=extended, 180°=curled |
| CH4 | SG90 | Pinky finger | 0°=extended, 180°=curled |
| CH5 | MG996R | Wrist | 45°=flex, 90°=neutral, 135°=extend |

### Servo pulse timing (50Hz PWM)
- Period = 20ms, 4096 ticks
- 0° = 500µs = 102 ticks
- 180° = 2500µs = 512 ticks
- Formula: `ticks = (500 + angle × 2000/180) × 4096 / 20000`

---

## Full System Block Diagram (text)

```
[9V Battery] ──── LM7805 ──── +5V ──────────── PCA9685 V+, INA128 V+, MCP6002 VDD
                  └── AGND ── Star point ────── ESP32-S3 GND

[Ag/AgCl Electrodes × 4 pairs]
     │   (10kΩ protection each lead)
     ▼
[INA128 ×4] (G=51, RG=1kΩ 1%)
  REF → [1.65V buffer (MCP6002 unity follower)]
     │
     ▼
[Sallen-Key HPF ×4] (fc=20Hz, 470nF film, 12kΩ+24kΩ, MCP6002)
     │
     ▼
[Sallen-Key LPF ×4] (fc=100Hz, 150nF C0G, 10kΩ, MCP6002)
     │
     ▼
[Twin-T Notch ×4] (50Hz, 10kΩ 1%, 330nF 1%, 680nF 1%, 5.1kΩ+2kΩ trimpot)
     │
     ▼
[Variable Gain ×4] (MCP6002, 100kΩ B100K pot, 1kΩ Rin, BAV99 clamp)
     │
     ▼
[ESP32-S3 ADC1] (GPIO1-4, 12-bit, 2kHz/ch, 0.15-2.45V range)
     │ (firmware: Core0 sampling → feature extraction → TFLite inference)
     ▼
[PCA9685] ──── SG90 ×5 (fingers) + MG996R ×1 (wrist)
               (I2C: GPIO8 SDA, GPIO9 SCL)
     │
     ▼
[BLE GATT] ──── Android MyoControl app
                (gesture ID, confidence, raw EMG)
```

---

## Electrode Placement Map

```
FOREARM (right arm, palm facing up):

                    ELBOW
                      │
         ─────────────┼─────────────
        │  CH2-ED     │             │   ← BACK (extensor) side
         ─────────────┼─────────────
                      │
         ─────────────┼─────────────
        │ CH1-FCR  CH3-FDS           │   ← FRONT (flexor) side
         ─────────────┼─────────────
                      │
                    WRIST

CH1 — FCR (Flexor carpi radialis): Inner forearm, 1/3 from elbow
CH2 — ED  (Extensor digitorum):    BACK of forearm, 1/3 from elbow
CH3 — FDS (Flexor dig. superficialis): Inner forearm, midpoint
CH4 — BR  (Brachioradialis):       Outer forearm, near elbow crease
REF — Wrist styloid (bony bump outer wrist, no muscle)

Each channel: 2 electrodes × 20mm spacing (center-to-center), parallel to forearm axis
```

---

## First Build Order (when components arrive)

1. Power supply: LM7805 + 9V battery → verify +5V with multimeter
2. 1.65V REF: R_DIV1 + R_DIV2 + C_REF + MCP6002 buffer → verify 1.65V output
3. Single INA128 channel (CH1 only): INA128 + RG + protection resistors → verify G=51 with function generator (1mV input → 51mV output)
4. Add Sallen-Key HPF → verify 20Hz cutoff with oscilloscope frequency sweep
5. Add Twin-T notch → tune trimpot → verify >30dB at 50Hz
6. Add Sallen-Key LPF → verify 100Hz cutoff
7. Add variable gain stage → verify 1V output swing for typical sEMG
8. Connect to ESP32-S3 GPIO1 → verify ADC reads in serial monitor
9. Flex forearm muscle → verify ADC value changes in serial monitor
10. Repeat for CH2-CH4
11. Flash full firmware: `pio run --target upload`
