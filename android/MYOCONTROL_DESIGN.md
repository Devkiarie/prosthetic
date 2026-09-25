# MyoControl — Android BLE App Design

**Status:** Design phase -- 2026-09-25
**Stack:** Kotlin + Jetpack Compose + Material 3
**Related module:** [[modules/ble-app]]
**Plan step:** Phase 9 (Month 9)

---

## App Architecture

```
Phone (Android)                       ESP32-S3
─────────────────────────────         ─────────────────────
[Home Screen]                         [DSP + CNN loop]
[Calibration Screen]   <── BLE ──>    [BLE GATT server]
[Live Monitor Screen]                 [Decision logic]
[Prosthetic Screen]                   [PCA9685 + servos]
[Analytics Screen]
[Settings Screen]
```

**Rule:** Phone = observe, configure, calibrate, analyse.
ESP32 = sense, process, classify, control.
Phone NEVER controls the gripper directly.

---

## BLE GATT Structure

**Service UUID:** `0000AA00-0000-1000-8000-00805F9B34FB`

| Characteristic | Short UUID | Direction | Format | Rate |
|---|---|---|---|---|
| GESTURE_RESULT | `0xAA01` | ESP32 → Phone (notify) | `{gesture:u8, confidence:f32, latency_ms:u16}` | Every inference (~10 Hz) |
| SIGNAL_METRICS | `0xAA02` | ESP32 → Phone (notify) | `{ch1:f32, ch2:f32, ch3:f32, ch4:f32}` (RMS per window) | Every window (10 Hz) |
| CALIBRATION_CMD | `0xAA03` | Phone → ESP32 (write) | `{cmd:u8, gesture:u8}` cmd: 0=idle,1=start,2=record,3=done | On user action |
| CONFIG | `0xAA04` | Phone → ESP32 (write) | `{confidence_threshold:f32, debounce_ms:u16}` | On settings change |
| RAW_STREAM | `0xAA05` | ESP32 → Phone (notify, optional) | 4×f32 per sample | 200 Hz when active |

**MTU:** Request 247 bytes (max for Android BLE). RAW_STREAM fits: 4 × 4 bytes = 16 bytes per packet.

---

## Screen Inventory

| Screen | Tab | Phase | Status |
|---|---|---|---|
| Home (device status, current gesture, daily stats) | Home | 2 (fake) | Not started |
| Calibration Studio (signal check, guided recording) | Train | 4 | Not started |
| Gesture Practice | Train | 4 | Not started |
| Live Monitor (4-ch waveform, features, classifier) | Monitor | 3 | Not started |
| Prosthetic Control (animated hand, servo positions) | Monitor | 6 | Not started |
| Analytics (accuracy, F1, confusion matrix, history) | Analytics | 5 | Not started |
| Settings (BLE, thresholds, data export) | (gear icon) | 2 | Not started |

---

## Phased Build Plan

| Phase | Depends on | What gets built |
|---|---|---|
| **Phase 1** (now) | Nothing | BLE scan, connect, disconnect, reconnect. Basic device discovery. |
| **Phase 2** (now) | Phase 1 | Fake classifier (ESP32 sends random gestures). Full UI built against fake data. |
| **Phase 3** (Month 2) | AFE hardware | Live signal streaming. 4-channel waveform charts. |
| **Phase 4** (Month 8) | Trained CNN | Calibration flow -- signal check, guided recording, fine-tune last layer on phone. |
| **Phase 5** (Month 9) | Full firmware | Replace fake classifier with real ESP32 CNN output. |
| **Phase 6** (Month 9) | Servo firmware | Prosthetic Control screen -- animated hand, servo positions, emergency stop. |

---

## Calibration Data Flow (Option A -- on-device fine-tuning)

```
User performs gesture × 3 reps
       ↓
ESP32 streams raw 4-ch signal (RAW_STREAM)
       ↓
Phone extracts features (MAV/RMS/WL/ZC/SSC/Var)
       ↓
Phone fine-tunes last Dense layer (TFLite Model Maker API)
       ↓ ~ 20s inference on Android CPU
Phone sends updated weight bytes → ESP32 (CALIBRATION_CMD char)
       ↓
ESP32 loads updated weights into TFLite Micro tensor arena
```

---

## Emergency Stop (Safety Critical)

- Always-visible **FloatingActionButton** in red, bottom-right, all screens
- Single tap -- no confirmation dialog -- immediate
- Sends `{cmd:0xFF}` to CONFIG characteristic
- ESP32 firmware handles it in its interrupt handler, < 100 ms response
- Servos move to safe rest position

---

## Design Language

- Dark clinical/engineering aesthetic: `#0A0F1E` background
- Accent: `#00D2FF` (cyan-blue) for active states, waveforms, confidence bars
- Success: `#00E676` (green) for gesture confirmed
- Alert: `#FF1744` (red) for emergency stop, poor signal quality
- Font: Inter or Roboto Mono for values, Inter for labels
- No emojis in Kotlin source

---

## Related Files

- Plan: `03_ML_AND_BLE.md` (Phases 8-9)
- Module spec: `modules/ble-app.md`
- BLE firmware: `firmware/src/ble_service.cpp` (to be created)

## Last Updated
2026-09-25 -- initial design, architecture decided
