# Phases 0–3: Analog Engineering
## Requirements, Single-Channel Proof, AFE Design, Simulation

---

## Phase 0 — Freeze Requirements and Safety (Week 1–2)

### Day 1–2: Requirements Document

Create a one-page requirements sheet. Every engineering decision traces back to this.

**Frozen Requirements:**

| Requirement | Value | Source |
|---|---|---|
| Channels | 4 differential sEMG | Concept paper |
| Gesture classes | 6 minimum, 8 target | Concept paper |
| Sampling rate | 2,000 Hz per channel | Concept paper |
| Signal bandwidth | 20–500 Hz | sEMG standard |
| 50 Hz rejection | > 30 dB | Concept paper |
| Single-channel SNR | > 20 dB | Concept paper |
| Inter-channel crosstalk | < -40 dB | Concept paper |
| Analog supply ripple | < 10 mV | Concept paper |
| Offline classifier F1 | > 0.85 | Concept paper |
| Inference latency | < 50 ms | Concept paper |
| Real-time accuracy | > 80% (5 subjects) | Concept paper |
| Calibration time | < 3 minutes | Concept paper |
| Total prototype BOM | < KES 8,500 | Budget target |
| Gripper DOF | 2 | Concept paper |
| Validation subjects | 5 | Concept paper |
| Repetitions per gesture | 10 | Concept paper |

### Day 3–4: Safety Analysis

**Electrical safety rules — non-negotiable:**

1. ALL body-connected testing uses battery power ONLY. No mains-derived supply touches the analog front-end during human testing.
2. Maximum DC current through electrodes: < 10 µA (leakage). The INA128 input bias current is typically 5 nA — safe.
3. Input protection on every channel: 10 kΩ series resistor at each INA128 input pin + BAV99 dual diode clamp to supply rails. This limits fault current to < 0.5 mA even if the INA128 fails short.
4. Never power the ESP32-S3 from USB while electrodes are attached to a person. USB ground connects to mains earth through the laptop charger, creating a ground loop path through the subject's body.
5. Servo power supply is physically separate from analog/MCU supply. Servos draw 1–2 A stall current — a shared supply will droop and corrupt ADC readings.

**Input protection circuit (per channel):**
```
Electrode+ ──[10kΩ]──┬── INA128 IN+
                      │
                   [BAV99]
                      │
                   Vcc / GND
```
The 10 kΩ resistor limits current. The BAV99 clamps voltage to within one diode drop of the supply rails. Total added noise from 10 kΩ: 4 nV/√Hz × √(500 Hz bandwidth) ≈ 90 nV RMS — negligible compared to sEMG amplitude.

### Day 5: Risk Register

Create a spreadsheet with columns: Risk ID, Description, Likelihood (H/M/L), Impact (H/M/L), Mitigation, Owner, Status. Populate from the master plan risk register. Print it and pin it above your workbench. Review weekly.

### Day 6–7: Order Initial Components

Order these FIRST (long lead items):
- INA128PA (DIP-8) × 5 from Mouser/DigiKey/TI.com (check stock)
- ESP32-S3-DevKitC-1 × 1
- Ag/AgCl disposable EMG electrodes × 50 (medical supply shop or online)
- MCP6002 op-amp (DIP-8) × 10 (single-supply, rail-to-rail, low noise — for filters)
- Resistor kit: 0.1% metal film E96 series, 10Ω to 1MΩ
- Capacitor kit: C0G/NP0 (pF range) + film caps (nF range) + X7R (µF range)
- 9V PP3 batteries × 4 + battery clips
- Breadboard (high quality, short bus rails) or protoboard
- Shielded cable (2-conductor + shield) × 2 meters

**Why MCP6002 for filters (not TL072):**
The TL072 is NOT single-supply and has a minimum supply of ±3.5V (7V total). If you plan to run the analog section from a single 3.3V or 5V rail, the TL072 will not work. The MCP6002 operates from 1.8V to 6V single supply, has rail-to-rail output, 1 MHz GBW (sufficient for 500 Hz filters), and 8.5 nV/√Hz noise. If you use ±5V split supply (two 9V batteries + regulators), the TL072 is fine.

**Supply decision — make this now:**

| Option | Pros | Cons |
|---|---|---|
| Single 3.3V (from ESP32 regulator) | Simple, one battery | Very limited headroom, 1.65V swing only |
| Single 5V (LDO from 9V battery) | Good headroom, simple | Need rail-to-rail op-amps |
| Split ±5V (two 9V batteries + LM7805/LM7905) | Maximum headroom, TL072 works | More components, heavier |

**Recommended: Single 5V analog supply** from a low-noise LDO (MCP1700-5002E or similar) fed by a 9V battery. Use MCP6002 or OPA2340 for filter stages. Virtual ground at 2.5V (resistor divider + buffer) provides the mid-rail reference.

---

## Phase 1 — Single-Channel Proof of Concept (Week 3–8)

### Week 3 — INA128 Bring-Up

**Day 8: INA128 on breadboard — power only**

1. Insert INA128PA into breadboard (DIP-8 package)
2. Connect pin 7 (V+) to +5V, pin 4 (V-) to GND (single supply for now)
3. Add 100 nF ceramic capacitor between pin 7 and pin 4, as close to the IC as possible
4. Add 10 µF electrolytic between pin 7 and pin 4 (bulk decoupling)
5. Connect pin 5 (Vref) to 2.5V (resistor divider: two 10 kΩ from 5V to GND, junction = 2.5V, buffered through MCP6002 voltage follower)
6. Leave pins 1, 2, 3, 6, 8 unconnected
7. Power on. Measure with multimeter:
   - Pin 7: should read +5V ± 0.1V
   - Pin 4: should read 0V
   - Pin 5: should read +2.5V ± 0.05V
   - Pin 6 (output): should read approximately 2.5V (equal to Vref with no input)

**If output is not ~2.5V:** Check all connections. Check the decoupling cap is not shorted. Check the Vref divider is producing 2.5V.

**Day 9: INA128 gain resistor**

The INA128 gain equation: G = 1 + 50,000 / R_G

| Desired Gain | R_G Value | Nearest 0.1% Resistor |
|---|---|---|
| 10 | 5,556 Ω | 5.6 kΩ |
| 20 | 2,632 Ω | 2.61 kΩ |
| 50 | 1,020 Ω | 1.02 kΩ |
| 100 | 505 Ω | 510 Ω |
| 200 | 251 Ω | 249 Ω |
| 500 | 100 Ω | 100 Ω |

**Start with G = 50 (R_G = 1.02 kΩ)**. This is a safe moderate gain that will not saturate from electrode DC offset.

Connect R_G between pins 1 and 8 of the INA128.

**Day 10: Signal generator test**

1. Set signal generator to: 100 Hz sine, 1 mV peak-to-peak amplitude
2. Connect signal generator output to INA128 pin 3 (IN+)
3. Connect signal generator ground to INA128 pin 2 (IN-) through a 10 kΩ resistor (simulating electrode impedance)
4. Connect oscilloscope Channel 1 to INA128 pin 6 (output)
5. Connect oscilloscope Channel 2 to signal generator output (reference)

**Expected result:**
- Output: 100 Hz sine wave, ~50 mV peak-to-peak (1 mV × 50 = 50 mV), centered at 2.5V
- Phase: approximately in-phase with input

**Measure and record:**
- Actual gain: Vout_pp / Vin_pp (should be ~50 ± 2)
- Output DC offset: should be 2.5V ± 50 mV
- Output noise at rest (short IN+ to IN-): should be < 5 mV RMS

**Day 11: CMRR test**

1. Connect BOTH IN+ and IN- to the same signal generator output (common-mode configuration)
2. Set signal generator to: 50 Hz sine, 500 mV peak-to-peak
3. Measure output amplitude

**Expected:** Output should be < 0.5 mV peak-to-peak (CMRR > 60 dB at 50 Hz)
If output is > 5 mV: check that R_G connections are clean, decoupling is adequate, and there is no resistive asymmetry in the input paths.

### Week 4 — Filter Stages

**Day 12–13: High-Pass Filter (fc = 20 Hz)**

Purpose: Remove electrode DC offset and motion artifact (below 20 Hz).

**Circuit: 2nd-order Sallen-Key HPF, Butterworth (unity gain)**

Using MCP6002 (single-supply, rail-to-rail):

```
INA128 OUT ──[C1]──┬──[C2]──┬── MCP6002 IN+
                    │         │
                  [R1]      [R2]
                    │         │
                   Vref     Vref (2.5V)
                              │
                        MCP6002 OUT ── (to LPF)
                              │
                        MCP6002 IN- ── MCP6002 OUT (unity gain feedback)
```

**Component calculation for fc = 20 Hz, Butterworth (Q = 0.707):**

Choose equal capacitors: C1 = C2 = C = 1 µF (film capacitor, NOT ceramic)

R1 = 1 / (4π × fc × C × Q) = 1 / (4π × 20 × 1×10⁻⁶ × 0.707)
R1 = **5,627 Ω → use 5.62 kΩ (0.1%)**

R2 = Q / (π × fc × C) = 0.707 / (π × 20 × 1×10⁻⁶)
R2 = **11,254 Ω → use 11.3 kΩ (0.1%)**

**Verification:** fc = 1 / (2π × √(R1 × R2 × C1 × C2))
= 1 / (2π × √(5,620 × 11,300 × 1µ × 1µ))
= 1 / (2π × √(0.0635))
= 1 / (2π × 0.252)
= **1 / 1.583 = 0.632 → wait, this gives 0.632 Hz, not 20 Hz**

Let me recalculate. The Sallen-Key HPF transfer function has a different topology. For a unity-gain Sallen-Key HPF with equal capacitors:

Actually, let me use the standard form. For a 2nd-order HPF Sallen-Key:

```
          s²
H(s) = ─────────────
        s² + s(ω₀/Q) + ω₀²
```

With C1 = C2 = C, the design equations are:
- R2 = 1 / (2 × Q × ω₀ × C)
- R1 = 1 / (2 × Q × ω₀ × C × (2Q²)) ... this depends on the exact topology

**Simplified approach — use the TI FilterPro values:**

For 2nd-order Butterworth HPF at 20 Hz, unity gain:
- C1 = C2 = 470 nF
- R1 = 12 kΩ
- R2 = 24 kΩ

Verification: fc = 1/(2π × C × √(R1×R2)) = 1/(2π × 470n × √(12k × 24k))
= 1/(2π × 470n × 16,970) = 1/(2π × 0.007976) = 1/0.05012 = **19.95 Hz** ✓

**Use these values:**
| Component | Value | Type |
|---|---|---|
| C1 | 470 nF | Film (Polyester or Polypropylene) |
| C2 | 470 nF | Film |
| R1 | 12 kΩ | 0.1% metal film |
| R2 | 24 kΩ | 0.1% metal film |

**Day 14–15: Low-Pass Filter (fc = 500 Hz)**

Purpose: Remove high-frequency noise above the useful sEMG band.

**Circuit: 2nd-order Sallen-Key LPF, Butterworth (unity gain)**

```
HPF OUT ──[R3]──┬──[R4]──┬── MCP6002 IN+
                 │         │
               [C4]      [C3]
                 │         │
               Vref      Vref (2.5V)
                           │
                     MCP6002 OUT ── (to notch)
                           │
                     MCP6002 IN- ── MCP6002 OUT
```

For equal resistors R3 = R4 = R = 10 kΩ:

C3 = 1 / (4π × fc × R × Q) = 1 / (4π × 500 × 10,000 × 0.707)
C3 = **22.5 nF → use 22 nF (C0G/NP0)**

C4 = Q / (π × fc × R) = 0.707 / (π × 500 × 10,000)
C4 = **45.0 nF → use 47 nF (C0G/NP0)**

Verification: fc = 1 / (2π × R × √(C3 × C4))
= 1 / (2π × 10,000 × √(22n × 47n))
= 1 / (2π × 10,000 × 32.15n)
= 1 / (2π × 3.215×10⁻⁴)
= 1 / 0.002020 = **495 Hz** ✓ (close enough to 500 Hz)

**Use these values:**
| Component | Value | Type |
|---|---|---|
| R3 | 10 kΩ | 0.1% metal film |
| R4 | 10 kΩ | 0.1% metal film |
| C3 | 22 nF | C0G/NP0 ceramic |
| C4 | 47 nF | C0G/NP0 ceramic |

**Day 16–17: Twin-T Notch Filter (f_notch = 50 Hz)**

Purpose: Reject 50 Hz mains interference with > 30 dB attenuation at exactly 50 Hz.

**Circuit: Passive Twin-T with active boost**

```
            R5          R5
IN ──┬──[10kΩ]──┬──[10kΩ]──┬── OUT
     │          │           │
   [C5]       [R6]       [C5]
  [330nF]   [5.1kΩ]    [330nF]
     │          │           │
    Vref      [C6]        Vref
             [680nF]
               │
             Vref (2.5V)
```

**Component values for 50 Hz:**

f_notch = 1 / (2π × R × C)

Choose R5 = 10 kΩ:
C5 = 1 / (2π × 50 × 10,000) = 318.3 nF → **use 330 nF (film)**

R6 = R5/2 = 5,000 Ω → **use 4.99 kΩ (0.1%) or a 5 kΩ trimmer potentiometer**

C6 = 2 × C5 = 660 nF → **use 680 nF (film)**

The trimmer for R6 is CRITICAL. Component tolerances shift the notch frequency. During calibration:
1. Inject a 50 Hz sine wave at the input
2. Monitor the output on oscilloscope
3. Adjust the trimmer until the output is minimized
4. Measure the trimmer value
5. Optionally replace with a fixed 0.1% resistor of that value

**Expected performance:**
- Rejection at 50 Hz: > 30 dB (typically 35–45 dB with trimming)
- Bandwidth of notch: approximately 48–52 Hz at -3 dB
- Signals at 40 Hz and 60 Hz: < 1 dB attenuation

**Enhanced version with active Q-boost:**
The passive twin-T has a moderately narrow notch. To deepen it, add a MCP6002 in positive feedback:

```
Twin-T output ──[R7: 100kΩ]──┬── MCP6002 IN+
                               │
                          [R8: 10kΩ]
                               │
                         MCP6002 OUT ── back to Twin-T junction (between R5 pair)
                               │
                         MCP6002 IN- ── MCP6002 OUT
```

This boosts Q (narrower, deeper notch) at the expense of slight ringing at nearby frequencies. Try without it first; add only if passive notch does not achieve > 30 dB.

**Use these values:**
| Component | Value | Type | Notes |
|---|---|---|---|
| R5 (×2) | 10 kΩ | 0.1% metal film | Matched pair |
| R6 | 5 kΩ trimmer or 4.99 kΩ | Trimmer + 0.1% backup | Calibrate for deepest null |
| C5 (×2) | 330 nF | Film (polyester) | Matched pair, NOT ceramic |
| C6 | 680 nF | Film (polyester) | 2× C5 |

### Week 5 — Variable Gain + Level Shift

**Day 18–19: Variable gain stage**

Purpose: Adjust total system gain between 200× and 500× to compensate for subject-to-subject amplitude variation.

**Circuit: MCP6002 inverting amplifier with adjustable gain**

```
Notch OUT ──[R9: 10kΩ]──┬── MCP6002 IN-
                          │
                        [R10: potentiometer 100kΩ]
                          │
                    MCP6002 OUT
                          │
                    MCP6002 IN+ ── Vref (2.5V)
```

Gain = -R10/R9 = -(10 kΩ to 100 kΩ) / 10 kΩ = **-1 to -10**

Combined with INA128 gain of 50: total gain = 50 × (1 to 10) = **50 to 500**

The inverting configuration inverts the signal polarity — this does not matter for feature extraction (MAV, RMS, ZC all use absolute values or squared values). If phase matters for your analysis, use a non-inverting configuration instead.

**Practical: use a 100 kΩ multi-turn potentiometer** (10-turn cermet). This gives fine adjustment. Set it to approximately 50 kΩ (gain = 5, total gain = 250) as a starting point.

**Day 20: Level shift and ADC protection**

The ESP32-S3 ADC expects 0–3.3V input. The analog front-end outputs a signal centered at 2.5V (Vref) with swing ±1–2V.

Two options:
1. **If analog supply is 5V:** Add a resistive divider (2:3) to scale 0–5V to 0–3.3V
2. **If analog supply is 3.3V:** No divider needed, but ensure the signal never exceeds 3.3V

**Recommended: resistive divider + protection**

```
Variable Gain OUT ──[R11: 3.3kΩ]──┬── ESP32-S3 ADC pin (GPIO1)
                                    │
                                  [R12: 5.6kΩ]
                                    │
                                   GND
                                    │
                                  [C7: 10nF to GND] (anti-aliasing)
                                    │
                                  [BAV99 clamp to 3.3V and GND]
```

Divider ratio: R12 / (R11 + R12) = 5.6k / (3.3k + 5.6k) = **0.629**

So 5V analog supply → 3.15V at ADC (safe)
2.5V (Vref center) → 1.57V at ADC (near mid-range — good)

The 10 nF capacitor with the divider resistance forms a low-pass at:
fc = 1 / (2π × (R11 ‖ R12) × C7) = 1 / (2π × 2.08kΩ × 10nF) = **7.65 kHz**
This provides anti-aliasing above the Nyquist frequency (1 kHz at 2 kHz sample rate).

### Week 6–7 — First Real sEMG Recording

**Day 21: Complete single-channel assembly**

Assemble the full single-channel chain on breadboard:
```
Electrode+ → 10kΩ protection → INA128 IN+
Electrode- → 10kΩ protection → INA128 IN-
Reference electrode → Vref (2.5V)
INA128 OUT → HPF → LPF → Notch → Variable Gain → Divider → ESP32 ADC
```

**Pre-flight checklist before attaching electrodes to a person:**
- [ ] All powered from 9V battery, NOT USB
- [ ] Multimeter confirms Vref = 2.5V ± 0.05V
- [ ] Multimeter confirms INA128 output ≈ 2.5V at rest (inputs shorted)
- [ ] Oscilloscope confirms < 10 mV noise at INA128 output with inputs shorted
- [ ] Signal generator test at 100 Hz / 1 mV → correct gain at final output
- [ ] No USB cable connected to ESP32 during human testing

**Day 22–23: Electrode placement and first recording**

**Skin preparation (do this every time):**
1. Clean the electrode site with alcohol swab
2. Lightly abrade with fine sandpaper (3M Red Dot prep pads or 600-grit)
3. Allow skin to dry (30 seconds)
4. Apply Ag/AgCl electrode firmly — press edges to ensure full contact
5. Wait 2 minutes for electrode-gel interface to stabilize

**Electrode placement for Channel 1 (flexor carpi radialis):**
- Active electrode 1 (IN+): 3 cm distal to the medial epicondyle, on the belly of the flexor carpi radialis muscle
- Active electrode 2 (IN-): 2 cm further distal along the same muscle belly
- Reference electrode: on the bony prominence of the olecranon (elbow tip) — minimal muscle tissue
- Inter-electrode distance: 2 cm (standard for surface EMG)

**Recording procedure:**
1. Power on the circuit (battery)
2. Connect oscilloscope to the final output (after divider, before ADC)
3. Let the signal stabilize for 10 seconds
4. Record 10 seconds of REST — note the noise level
5. Perform deliberate wrist flexion (clench fist) for 3 seconds — note the burst
6. Return to rest for 3 seconds
7. Repeat 5 times

**What you expect to see on the oscilloscope:**
- At rest: baseline centered at ~1.57V (after divider), noise < 50 mV peak-to-peak
- During contraction: burst of activity, 200–800 mV peak-to-peak, frequency content visually faster than 50 Hz
- Clear on/off transitions when you start and stop contracting

**What indicates a problem:**
| Symptom | Likely cause | Fix |
|---|---|---|
| Large 50 Hz sine wave | Ground loop or inadequate CMRR | Verify battery-only power; check electrode reference; improve shielding |
| Signal saturated (stuck at rail) | DC offset × gain exceeds supply | Reduce INA128 gain; verify HPF is working |
| No visible difference rest vs contraction | Electrodes on wrong muscle; gain too low; broken connection | Verify electrode placement; increase variable gain; check continuity |
| Very high noise floor (> 200 mV) | Poor electrode contact; long unshielded leads | Re-prep skin; shorten leads; twist electrode wires |

### Week 8 — Document and Validate

**Day 28–30: Quantitative measurements**

1. **SNR measurement:**
   - Record 10 seconds of rest → compute RMS_rest in Python/MATLAB
   - Record 10 seconds of sustained contraction → compute RMS_contraction
   - SNR = 20 × log10(RMS_contraction / RMS_rest)
   - Target: > 20 dB

2. **Frequency response (bandpass verification):**
   - Disconnect electrodes, connect signal generator
   - Sweep frequency: 5, 10, 15, 20, 30, 50, 100, 200, 300, 400, 500, 600, 700, 800, 1000 Hz
   - At each frequency: input 1 mV, measure output amplitude
   - Plot gain (dB) vs frequency
   - Verify: -3 dB at 20 Hz and 500 Hz, flat ±1 dB between 30–400 Hz

3. **50 Hz notch depth:**
   - Input: 50 Hz, 1 mV
   - Measure output amplitude
   - Compare to passband gain (e.g., at 100 Hz)
   - Rejection = 20 × log10(V_passband / V_50Hz)
   - Target: > 30 dB

**Day 31–32: Save oscilloscope captures**

Use the oscilloscope's USB save function (or photograph the screen) for:
- Rest baseline
- Contraction burst
- Frequency response at 20 Hz, 100 Hz, 500 Hz
- 50 Hz notch depth

These go directly into your thesis as figures.

**Phase 1 Exit Checklist:**
- [ ] Clean, repeatable sEMG visible on oscilloscope
- [ ] SNR > 20 dB (measured)
- [ ] Bandpass 20–500 Hz within ±3 dB (measured)
- [ ] 50 Hz rejection > 30 dB (measured)
- [ ] No saturation during maximum voluntary contraction
- [ ] Rest baseline noise < 50 mV peak-to-peak (at final output)
- [ ] All measurements documented with oscilloscope captures
- [ ] Electrode placement documented with photos

---

## Phase 2 — Analog Front-End Design (Week 6–10, overlaps Phase 1)

### Complete Single-Channel Schematic — Every Component

This is the complete schematic for ONE channel. The 4-channel PCB replicates this four times with shared power and reference.

```
ELECTRODE+                         ELECTRODE-                    REF ELECTRODE
    |                                  |                              |
  [R_in1: 10kΩ]                     [R_in2: 10kΩ]                   |
    |                                  |                              |
  [BAV99 to Vcc/GND]              [BAV99 to Vcc/GND]                |
    |                                  |                              |
    +----[R_G: 1.02kΩ]----+           |                              |
    |    (pins 1-8)        |           |                              |
    v                      v           v                              v
  INA128 IN+ (pin 3)    INA128 (pin 8/1)  INA128 IN- (pin 2)   Vref (2.5V)
    |
  INA128 OUT (pin 6)    (Gain = 50)
    |
  [C_hp1: 470nF film]──┬──[C_hp2: 470nF film]──┬── MCP6002A IN+
                        │                        │
                     [R_hp1: 12kΩ]            [R_hp2: 24kΩ]
                        │                        │
                      Vref                     Vref
                                                 │
                                           MCP6002A OUT ─── (HPF fc = 20 Hz)
                                                 │
                                           MCP6002A IN- ── MCP6002A OUT
    |
  HPF OUT
    |
  [R_lp1: 10kΩ]──┬──[R_lp2: 10kΩ]──┬── MCP6002B IN+
                   │                  │
                [C_lp2: 47nF]      [C_lp1: 22nF]
                   │                  │
                 Vref              Vref
                                     │
                               MCP6002B OUT ─── (LPF fc = 500 Hz)
                                     │
                               MCP6002B IN- ── MCP6002B OUT
    |
  LPF OUT
    |
    ├──[R_n1: 10kΩ]──┬──[R_n2: 10kΩ]──┤
    │                 │                 │
  [C_n1: 330nF]    [R_n3: 5kΩ pot]  [C_n2: 330nF]
    │                 │                 │
  Vref             [C_n3: 680nF]     Vref
                      │
                    Vref                ─── (Notch f = 50 Hz)
    |
  NOTCH OUT
    |
  [R_g1: 10kΩ]──┬── MCP6002C IN-
                  │
               [R_g2: 100kΩ pot]
                  │
            MCP6002C OUT ─── (Variable gain: ×1 to ×10)
                  │
            MCP6002C IN+ ── Vref
    |
  GAIN OUT
    |
  [R_div1: 3.3kΩ]──┬── ESP32-S3 GPIO (ADC1)
                     │
                  [R_div2: 5.6kΩ]
                     │
                    GND
                     │
                  [C_aa: 10nF] (anti-alias)
                     │
                  [BAV99 clamp to 3.3V/GND]
```

**Per-channel component count:**
| Component type | Quantity per channel |
|---|---|
| INA128 | 1 |
| MCP6002 (dual op-amp) | 2 (= 4 op-amp sections: HPF, LPF, gain, spare) |
| Resistors | 11 fixed + 2 trimmers |
| Capacitors | 7 |
| BAV99 diodes | 3 (2 input + 1 output) |

**For 4 channels + shared power/reference:**
- 4 × INA128
- 8 × MCP6002 (or 4 × MCP6004 quad)
- 44 fixed resistors + 8 trimmers (4 notch + 4 gain)
- 28 signal capacitors + ~20 decoupling
- 12 × BAV99

---

## Phase 3 — Simulation (Week 10–12)

### LTspice Setup

**Day 42–43: Install and configure LTspice**

1. Download LTspice from Analog Devices (free): https://www.analog.com/ltspice
2. Download INA128 SPICE model from TI: https://www.ti.com/product/INA128 → Design & Development → SPICE models
3. Download MCP6002 SPICE model from Microchip
4. Place .lib files in your LTspice library directory

**Day 44–46: Simulate each stage independently**

**Simulation 1: INA128 gain and CMRR**
- Schematic: INA128 with R_G = 1.02 kΩ, ±5V or single 5V supply
- AC analysis: 1 Hz to 100 kHz
- Differential input: 1 mV at IN+, 0V at IN-
- Expected: flat gain of ~34 dB (50×) across 1 Hz – 100 kHz
- Common-mode test: same 1V signal on both IN+ and IN-
- Expected: output < -60 dB relative to 1V (CMRR > 60 dB)

**Simulation 2: HPF (20 Hz)**
- Schematic: Sallen-Key HPF with calculated values
- AC analysis: 0.1 Hz to 10 kHz
- Expected: -3 dB at 20 Hz, -40 dB/decade below, flat above
- Record: exact -3 dB frequency, phase at 20 Hz

**Simulation 3: LPF (500 Hz)**
- Same approach
- Expected: -3 dB at ~495 Hz, -40 dB/decade above

**Simulation 4: Twin-T notch (50 Hz)**
- AC analysis: 10 Hz to 200 Hz (narrow range for detail)
- Expected: deep null at 50 Hz, > 30 dB rejection
- Record: rejection depth, -3 dB bandwidth of notch

**Simulation 5: Complete signal chain**
- Connect all stages in sequence
- AC analysis: 1 Hz to 10 kHz
- Expected: bandpass shape with notch at 50 Hz
- Record: Bode plot (magnitude and phase)

**Day 47–48: Monte Carlo / Tolerance Analysis**

In LTspice, add tolerance to each component:
```spice
.param R_hp1 = mc(12000, tolr)   ; 1% tolerance
.param C_hp1 = mc(470e-9, tolc)  ; 5% tolerance
.step param run 1 100 1
```

Run 100 Monte Carlo iterations. Plot the spread of:
- HPF -3 dB frequency: expect 18–22 Hz with 5% capacitors
- LPF -3 dB frequency: expect 470–530 Hz
- Notch center frequency: expect 47–53 Hz (this is why the trimmer is essential)
- Overall gain variation: expect ±2 dB

**Export all Bode plots as PNG files for the thesis.**

**Phase 3 Exit Checklist:**
- [ ] INA128 gain simulation matches calculation (50× ± 1%)
- [ ] HPF -3 dB frequency: 20 Hz ± 10%
- [ ] LPF -3 dB frequency: 500 Hz ± 10%
- [ ] Notch rejection > 30 dB at 50 Hz
- [ ] Combined Bode plot shows clean 20–500 Hz bandpass with 50 Hz null
- [ ] Monte Carlo shows component sensitivity
- [ ] All simulation files saved in `hardware/simulation/`
- [ ] Bode plots exported for thesis

---

*End of Phases 0–3. Continue to 02_DIGITAL_ENGINEERING.md for PCB, ADC, and DSP.*
