# Module 5: Actuator — 5-Finger Tendon-Driven Prosthetic Hand

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#E4|Block E4 -- Actuator Integration]]
**Updated:** 2026-09-25 — upgraded from 2-DOF gripper to 5-finger hand

---

## Design Decision: 5-Finger Hand vs 2-DOF Gripper

**Decision:** Build a 5-finger tendon-driven prosthetic hand.
**Rationale:**
- Same sEMG pipeline — classifier output is a gesture label, not joint angles
- Each gesture maps to a pre-programmed 5-value servo angle table (lookup)
- BOM cost increase: ~KES 600 (~USD 4.50) — stays under USD 20 BOM
- Demo impact: visually mirrors human hand; examiners immediately understand function
- Published term: "5-finger myoelectric prosthetic hand" vs "2-DOF gripper" — stronger thesis framing
- PCA9685 already in BOM (16 channels available); only 6 channels used (5 fingers + 1 wrist)

---

## Components

| Part | Spec | Purpose | Qty | Unit KES | Total KES |
|---|---|---|---|---|---|
| PCA9685 | 16-ch I2C PWM, 12-bit, addr 0x40 | Servo controller | 1 | 250 | 250 |
| SG90 micro servo | 9g, 1.8 kg·cm, 180° | Finger actuation (×5) | 5 | 120 | 600 |
| MG996R servo | 11 kg·cm, 180° | Wrist rotation | 1 | 350 | 350 |
| PETG filament | 1.75mm, ~220g | Hand structure + finger phalanges | — | — | 440 |
| Nylon fishing line 0.4mm | Tendon material | Finger pull tendons | 5m | 50 | 50 |
| Elastic cord 1mm | Tendon return | Dorsal return spring | 2m | 30 | 30 |
| M2 / M3 hardware | Screws, nuts, standoffs | Assembly | 1 kit | 100 | 100 |

**Total actuator BOM: ~KES 1,820 (~USD 14)**

---

## Why SG90 for Fingers (not MG996R)

- MG996R: 55g, 9–11 kg·cm torque — grossly oversized for a finger
- SG90: 9g, 1.8 kg·cm — sufficient to overcome nylon tendon + elastic return force (~0.5 kg·cm max)
- SG90 × 5 = 45g vs MG996R × 2 = 110g — lighter hand, better balance
- SG90 cost: ~KES 120 each; MG996R: KES 350 — significant saving

---

## Mechanical Design: Tendon-Drive Mechanism

```
SG90 drum (on palm)
      │
      ▼ nylon line (0.4mm)
  ┌───────────────────┐
  │  Proximal phalanx │──── pivot (M2 axle)
  └───────┬───────────┘
          │ nylon continues
  ┌───────▼───────────┐
  │  Distal phalanx   │
  └───────────────────┘
          │ elastic cord (dorsal)
         [spring return to open position]
```

- Servo drum rotation ~120° pulls tendon → curls finger from 0° (open) to ~130° (closed)
- Elastic cord on dorsal side returns finger to open when servo releases
- No complex gearbox needed — fishing line is flexible and nearly zero-stretch

---

## Motor Placement in the Hand

```
Palm (dorsal view):
┌────────────────────────────┐
│  [SG90-T] [SG90-I] [SG90-M] [SG90-R] [SG90-P]  │
│  Thumb   Index  Middle  Ring   Pinky              │
│         ← servo bank across dorsal palm →         │
│                                                   │
│  [MG996R] ← wrist servo, base of palm            │
└────────────────────────────┘
```

**Placement rationale:**
- All 5 finger servos sit in the dorsal cavity of the palm — keeps the palm profile slim
- Servo horns face proximally (toward wrist); tendons run through guides in each finger
- MG996R mounts at the proximal end of the palm (near wrist pivot); controls rotation ±30°
- Servo cables routed through a cable channel in the palm base to the PCA9685 board

**Critical clearances:**
- SG90 body: 23mm × 12.5mm × 29mm — palm must be ≥ 30mm deep to house them flat
- Space 5 servos across ~120mm (adult palm width) — 24mm per servo with 1.5mm clearance
- Use 90° servo arm orientation for compact routing (horn toward finger, not sideways)

---

## Gesture-to-Servo Mapping (5-finger, degrees)

| Gesture | Thumb | Index | Middle | Ring | Pinky | Wrist |
|---|---|---|---|---|---|---|
| REST | 45 | 30 | 30 | 30 | 30 | 90 |
| OPEN HAND | 0 | 0 | 0 | 0 | 0 | 90 |
| POWER GRASP | 120 | 130 | 130 | 130 | 130 | 90 |
| PINCH | 110 | 100 | 130 | 130 | 130 | 90 |
| POINT | 120 | 0 | 130 | 130 | 130 | 90 |
| WRIST FLEX | 30 | 20 | 20 | 20 | 20 | 55 |
| WRIST EXT | 30 | 20 | 20 | 20 | 20 | 125 |
| THUMBS UP | 0 | 130 | 130 | 130 | 130 | 90 |

All 8 gesture classes produce a **visually distinct hand posture** — exactly the goal.

---

## Open-Source Designs (pick one, modify for servo-tendon)

| Design | Status | Motor placement | Notes |
|---|---|---|---|
| **e-NABLE Raptor Reloaded** | Proven, widely printed | Wrist-powered (needs servo mod) | Best documentation; large community |
| **Open Bionics Ada Hand** | Professional, open license | 4 servos (ring+pinky coupled) | STLs at github.com/Open-Bionics |
| **Dextrus v2** | 5 independent servos | Forearm-mounted servos | Tendons run up forearm — bulkier |
| **InMoov hand** | Full arm, extract hand only | Forearm servos | Best servo placement docs online |

**Recommended:** Start with Open Bionics Ada Hand STLs (GitHub) — it already has servo pockets designed into the palm and is sized for adults. Extract just the hand assembly. Modify to fit SG90 instead of their servos if dimensions differ.

---

## 3D Printing Specs

| Setting | Value | Reason |
|---|---|---|
| Material | PETG | Better layer adhesion for thin hinges vs PLA; less brittle |
| Layer height | 0.2mm | Sufficient for hinge pivots and tendon channels |
| Infill | 30% (palm/knuckles), 20% (phalanges) | Weight reduction; phalanges don't need strength |
| Supports | Yes for palm cavity | Servo pockets are overhanging |
| Estimated print time | 10–14 hours total | 4–5 print sessions |
| Estimated filament | 180–220g PETG | ~KES 400–450 |

---

## Assembly Order

1. Print palm body (longest print — start first)
2. Print 3 phalanges × 5 fingers = 15 pieces (print in batches)
3. Install M2 axle pins in knuckle pivots
4. Insert SG90 servos into palm dorsal pockets — test fit before gluing
5. Route nylon tendons from servo drums through finger channels
6. Tie elastic return cord on dorsal side of each finger
7. Connect SG90 PWM cables to PCA9685 channels 0–4
8. Connect MG996R to PCA9685 channel 5
9. Calibrate servo travel limits in firmware (zero-position + full-close)
10. Test each finger individually before running full gesture table

---

## PCA9685 Channel Assignment

| Channel | Device | Direction |
|---|---|---|
| 0 | Thumb SG90 | Pull = close |
| 1 | Index SG90 | Pull = close |
| 2 | Middle SG90 | Pull = close |
| 3 | Ring SG90 | Pull = close |
| 4 | Pinky SG90 | Pull = close |
| 5 | Wrist MG996R | CW = flex |
| 6–15 | Reserved | Future use |

---

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| Servo response time (gesture to motion) | < 200 ms |
| All 8 gestures produce distinct postures | Visual verification |
| Index extension (POINT) clears all others | Mechanical clearance test |
| Grip holds 300g object (POWER GRASP) | Load test |
| Wrist rotates ±30° without binding | Range-of-motion test |
| Endurance | > 200 gesture cycles without tendon slip |
| No ADC noise from servos during acquisition | SNR measurement |

---

## Log

_Entries added as work progresses._
