# Module 5: Actuator and Gripper

**Status:** Not started
**Phase:** [[docs/COMPREHENSIVE_PLAN#E4|Block E4 -- Actuator Integration]]

---

## Components

| Part | Spec |
|---|---|
| PCA9685 | 16-ch I2C PWM, 12-bit, addr 0x40 |
| MG996R x2 | 11 kg-cm torque, 180 deg, 4.8-7.2V |
| 2-DOF gripper | 3D printed PETG, tendon-driven |

## Gesture-to-Servo Mapping

| Gesture | Servo 1 (Grip) | Servo 2 (Wrist) |
|---|---|---|
| Rest | 90 deg | 90 deg |
| Open | 0 deg | hold |
| Power grasp | 180 deg | hold |
| Pinch | 120 deg | hold |
| Wrist flex | hold | 45 deg |
| Wrist ext | hold | 135 deg |

**Critical:** Servo power is separate from analog/digital supply. See [[docs/decisions/DECISIONS]].

## Pass/Fail Criteria

| Metric | Target |
|---|---|
| Servo response time | < 200 ms |
| Grip (500g object) | Holds reliably |
| Endurance | > 200 open/close cycles |
| No ADC noise from servos | Verified |

## Log

_Entries added as work progresses._
