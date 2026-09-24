# Phases 10–13: Integration, Decision Logic, Validation, Experiment
## Actuator, Confidence Filtering, Electrical Validation, 5-Subject Study

---

## Phase 10 — Actuator and Gripper (Week 36–40)

### Hardware Setup

**Components:**
| Part | Specification | Purpose |
|---|---|---|
| PCA9685 | 16-channel, 12-bit PWM, I2C (addr 0x40) | PWM generation for servos |
| MG996R servo × 2 | Torque: 11 kg·cm, 180° travel, 4.8–7.2V | Grip + wrist rotation |
| 5V 3A BEC or regulator | Dedicated servo supply | Isolate servo current from analog/digital |
| 3D-printed 2-DOF gripper | InMoov finger mechanism or similar | Mechanical demonstrator |

**Wiring:**
```
ESP32 GPIO8 (SDA) ──[4.7kΩ pull-up to 3.3V]── PCA9685 SDA
ESP32 GPIO9 (SCL) ──[4.7kΩ pull-up to 3.3V]── PCA9685 SCL
PCA9685 V+ ──────── Servo 5V rail (NOT analog 5V)
PCA9685 GND ─────── Common GND star point
PCA9685 OE ──────── GND (always enabled)
PCA9685 CH0 ─────── Servo 1 signal (grip open/close)
PCA9685 CH1 ─────── Servo 2 signal (wrist rotation)

Servo 5V rail ──[1000µF electrolytic]── Servo GND
```

**Critical: servo power is completely separate from analog power.** Servo stall current (2.5A per servo) causes voltage sag that corrupts ADC readings. Use a separate battery or BEC regulator.

### Servo Calibration

Each servo maps a pulse width (500–2500 µs) to an angle (0–180°). The PCA9685 generates these pulses at 50 Hz (20 ms period).

```c
// actuator.h
#ifndef ACTUATOR_H
#define ACTUATOR_H

#include <stdint.h>

// Gesture-to-servo mapping
typedef struct {
    uint16_t grip_angle;    // 0=fully open, 180=fully closed
    uint16_t wrist_angle;   // 0=full flex, 90=neutral, 180=full extend
} servo_position_t;

static const servo_position_t gesture_positions[8] = {
    [0] = { .grip_angle =  90, .wrist_angle = 90 },   // Rest: neutral
    [1] = { .grip_angle =  60, .wrist_angle = 90 },   // Index point
    [2] = { .grip_angle = 120, .wrist_angle = 90 },   // Pinch
    [3] = { .grip_angle =  90, .wrist_angle = 60 },   // Thumb up
    [4] = { .grip_angle = 180, .wrist_angle = 90 },   // Fist: full close
    [5] = { .grip_angle =   0, .wrist_angle = 90 },   // Open: full open
    [6] = { .grip_angle =  90, .wrist_angle = 45 },   // Wrist flex
    [7] = { .grip_angle =  90, .wrist_angle = 135},   // Wrist extend
};

void actuator_init(void);
void actuator_set_gesture(uint8_t gesture_class);
void actuator_emergency_stop(void);

#endif
```

```c
// actuator.c
#include "actuator.h"
#include "driver/i2c.h"
#include "esp_log.h"

#define PCA9685_ADDR   0x40
#define PCA9685_MODE1  0x00
#define PCA9685_PRESCALE 0xFE
#define PCA9685_LED0_ON_L 0x06

static const char *TAG = "ACT";

// Convert angle (0-180) to PCA9685 pulse value (for 50 Hz)
// At 50 Hz: period=20ms, 12-bit resolution=4096 ticks
// Servo expects 500-2500µs → 102-512 ticks at 50 Hz
static uint16_t angle_to_pwm(uint16_t angle) {
    return 102 + (uint32_t)angle * 410 / 180;
}

static void pca9685_write(uint8_t reg, uint8_t val) {
    uint8_t buf[2] = {reg, val};
    i2c_master_write_to_device(I2C_NUM_0, PCA9685_ADDR, buf, 2, pdMS_TO_TICKS(100));
}

static void pca9685_set_pwm(uint8_t channel, uint16_t on, uint16_t off) {
    uint8_t buf[5] = {
        PCA9685_LED0_ON_L + 4 * channel,
        on & 0xFF, (on >> 8) & 0x0F,
        off & 0xFF, (off >> 8) & 0x0F
    };
    i2c_master_write_to_device(I2C_NUM_0, PCA9685_ADDR, buf, 5, pdMS_TO_TICKS(100));
}

void actuator_init(void) {
    // I2C init
    i2c_config_t conf = {
        .mode = I2C_MODE_MASTER,
        .sda_io_num = 8,
        .scl_io_num = 9,
        .sda_pullup_en = GPIO_PULLUP_ENABLE,
        .scl_pullup_en = GPIO_PULLUP_ENABLE,
        .master.clk_speed = 400000,
    };
    i2c_param_config(I2C_NUM_0, &conf);
    i2c_driver_install(I2C_NUM_0, I2C_MODE_MASTER, 0, 0, 0);

    // PCA9685 init: sleep → set prescaler → wake
    pca9685_write(PCA9685_MODE1, 0x10);     // sleep
    // Prescaler for 50 Hz: round(25MHz / (4096 × 50)) - 1 = 121
    pca9685_write(PCA9685_PRESCALE, 121);
    pca9685_write(PCA9685_MODE1, 0x20);     // wake, auto-increment
    vTaskDelay(pdMS_TO_TICKS(5));

    // Move to neutral
    actuator_set_gesture(0);
    ESP_LOGI(TAG, "Actuator initialized — servos at neutral");
}

void actuator_set_gesture(uint8_t gesture_class) {
    if (gesture_class >= 8) return;
    const servo_position_t *pos = &gesture_positions[gesture_class];
    pca9685_set_pwm(0, 0, angle_to_pwm(pos->grip_angle));
    pca9685_set_pwm(1, 0, angle_to_pwm(pos->wrist_angle));
}

void actuator_emergency_stop(void) {
    // Set all PWM to 0 (servos go limp)
    for (int ch = 0; ch < 16; ch++) {
        pca9685_set_pwm(ch, 0, 0);
    }
    ESP_LOGW(TAG, "EMERGENCY STOP — all servos disabled");
}
```

### 3D-Printed Gripper

**Source:** InMoov finger mechanism (https://inmoov.fr/) or Thingiverse "2DOF gripper"

Print settings:
- Material: PETG (stronger than PLA, heat resistant)
- Layer height: 0.2 mm
- Infill: 30%
- Supports: yes (for overhangs)
- Print time: approximately 4–6 hours

Test the gripper mechanically BEFORE connecting to electronics:
1. Manually rotate the servo horn — gripper should open and close smoothly
2. Place a 500g object (water bottle) in the gripper — should hold without slipping
3. Cycle open/close 200 times — no cracking, no loose joints

**Phase 10 Exit Checklist:**
- [ ] PCA9685 responds on I2C (addr 0x40 detected)
- [ ] Both servos move to all 8 gesture positions correctly
- [ ] Servo response time < 200 ms (measured from command to position)
- [ ] Gripper holds 500g object in closed position
- [ ] Gripper survives 200 open/close cycles without failure
- [ ] No ADC noise increase when servos are active (separate power verified)
- [ ] Emergency stop function tested

---

## Phase 11 — Decision Logic and Confidence Filtering (Week 40–42)

### Why You Need This

Raw classifier output changes every 250 ms (every window). Without filtering:
- Brief noise spikes cause false gesture detections
- Transition between gestures produces 1–2 frames of wrong class
- Low-confidence predictions cause jittery servo movement

The decision logic layer sits between the classifier and the actuator, providing stability.

### Confidence Threshold

The softmax output gives a probability distribution over 8 classes. The confidence is the maximum probability.

```c
// decision_logic.h
#ifndef DECISION_LOGIC_H
#define DECISION_LOGIC_H

#include <stdint.h>

#define CONFIDENCE_THRESHOLD  0.60f   // 60% — reject below this
#define DEBOUNCE_COUNT        3       // require 3 consecutive same class
#define HOLD_TIMEOUT_MS       2000    // revert to rest after 2s of low confidence

typedef struct {
    uint8_t  current_gesture;     // gesture being sent to actuator
    uint8_t  candidate_gesture;   // pending candidate
    uint8_t  candidate_count;     // consecutive frames of candidate
    float    current_confidence;
    uint32_t last_high_conf_ms;   // timestamp of last confident prediction
} decision_state_t;

void decision_init(decision_state_t *state);

// Returns the gesture to actuate (may differ from raw prediction)
uint8_t decision_update(decision_state_t *state,
                         uint8_t raw_class, float confidence,
                         uint32_t now_ms);

#endif
```

```c
// decision_logic.c
#include "decision_logic.h"

void decision_init(decision_state_t *s) {
    s->current_gesture    = 0;   // rest
    s->candidate_gesture  = 0;
    s->candidate_count    = 0;
    s->current_confidence = 0;
    s->last_high_conf_ms  = 0;
}

uint8_t decision_update(decision_state_t *s,
                          uint8_t raw_class, float confidence,
                          uint32_t now_ms) {
    // Rule 1: reject low-confidence predictions
    if (confidence < CONFIDENCE_THRESHOLD) {
        // If no high-confidence prediction for HOLD_TIMEOUT_MS, revert to rest
        if (now_ms - s->last_high_conf_ms > HOLD_TIMEOUT_MS) {
            s->current_gesture   = 0;   // rest
            s->candidate_count   = 0;
            s->current_confidence = 0;
        }
        return s->current_gesture;  // hold previous gesture
    }

    s->last_high_conf_ms = now_ms;

    // Rule 2: debounce — require DEBOUNCE_COUNT consecutive same class
    if (raw_class == s->candidate_gesture) {
        s->candidate_count++;
    } else {
        s->candidate_gesture = raw_class;
        s->candidate_count   = 1;
    }

    if (s->candidate_count >= DEBOUNCE_COUNT) {
        s->current_gesture    = s->candidate_gesture;
        s->current_confidence = confidence;
    }

    return s->current_gesture;
}
```

### Main Loop Integration

```c
// main.c — core task running on Core 1
void inference_actuator_task(void *arg) {
    decision_state_t decision;
    decision_init(&decision);
    
    feature_vector_t features;
    
    while (1) {
        // Wait for DSP to produce a feature vector
        if (xQueueReceive(feature_queue, &features, portMAX_DELAY)) {
            // Run inference
            uint8_t raw_class;
            float confidence;
            inference_run(features.features, &raw_class, &confidence);
            
            // Decision filtering
            uint32_t now = esp_timer_get_time() / 1000;
            uint8_t gesture = decision_update(&decision, raw_class, confidence, now);
            
            // Actuate
            actuator_set_gesture(gesture);
            
            // Notify BLE
            ble_notify_gesture(gesture, (uint8_t)(confidence * 100));
        }
    }
}
```

**Phase 11 Exit Checklist:**
- [ ] Confidence threshold prevents random gesture firing during rest
- [ ] Debounce eliminates single-frame false positives
- [ ] Hold timeout reverts to rest after 2 seconds of silence
- [ ] System never actuates on < 60% confidence
- [ ] Transition between gestures takes < 0.75 seconds (3 frames × 250 ms)
- [ ] Emergency stop accessible via BLE command or physical button

---

## Phase 12 — Electrical Validation (Week 42–46)

### Complete Measurement Campaign

Record all 18 engineering metrics from the master plan. Each measurement requires a specific test setup, documented procedure, and recorded result.

**E1: Single-channel SNR**
```
Setup: Channel 1 connected to forearm electrodes (skin prepped)
Procedure:
  1. Record 10s rest → export raw ADC values → compute RMS_noise
  2. Record 10s maximum voluntary contraction (MVC) → compute RMS_signal
  3. SNR_dB = 20 × log10(RMS_signal / RMS_noise)
Record: SNR value, oscilloscope screenshot of rest and MVC
Target: > 20 dB
```

**E2: Bandpass frequency response**
```
Setup: Signal generator → Channel 1 input (through 10kΩ to simulate electrode impedance)
Procedure:
  1. Set amplitude: 1 mV peak-to-peak
  2. Sweep: 5, 10, 15, 20, 25, 30, 50, 100, 200, 300, 400, 500, 600, 700, 800, 1000 Hz
  3. At each frequency: measure output amplitude on oscilloscope
  4. Compute gain_dB = 20 × log10(V_out / V_in)
  5. Plot Bode magnitude response
Record: table of (frequency, V_in, V_out, gain_dB), Bode plot
Target: ±3 dB flatness from 20–500 Hz
```

**E3: 50 Hz notch rejection**
```
Setup: Signal generator → Channel 1 input
Procedure:
  1. Set: 50 Hz, 1 mV
  2. Measure output
  3. Compare to passband output (e.g., at 100 Hz, same 1 mV input)
  4. Rejection_dB = 20 × log10(V_passband / V_50Hz)
Record: both measurements, rejection value
Target: > 30 dB
```

**E4: Inter-channel crosstalk**
```
Setup: Signal generator → Channel 1 only. Channels 2–4 inputs grounded through 10kΩ.
Procedure:
  1. Set: 100 Hz, 1 mV on Channel 1
  2. Measure output amplitude on Channels 2, 3, 4
  3. Crosstalk_dB = 20 × log10(V_other / V_driven)
Record: crosstalk for all 6 channel pairs (1→2, 1→3, 1→4, 2→3, 2→4, 3→4)
Target: < -40 dB for all pairs
```

**E5–E18:** Follow the same structured format. Each metric documented with setup, procedure, recorded values, and pass/fail.

### Data Recording

Create a validation spreadsheet:

| Metric ID | Description | Target | Measured Value | Pass/Fail | Date | Notes |
|---|---|---|---|---|---|---|
| E1 | Single-channel SNR | > 20 dB | | | | |
| E2 | Bandpass response | 20–500 Hz ±3 dB | | | | |
| ... | ... | ... | | | | |
| E18 | Total component cost | < KES 8,500 | | | | |

**Phase 12 Exit Checklist:**
- [ ] All 18 engineering metrics measured and recorded
- [ ] All measurements include: setup photo, procedure, raw data, computed result
- [ ] Bode plot generated (for thesis Figure X)
- [ ] Crosstalk matrix table generated (for thesis Table Y)
- [ ] Validation spreadsheet complete with pass/fail for each metric
- [ ] Any failed metrics documented with root cause and corrective action

---

## Phase 13 — Five-Subject Experiment (Week 46–50)

### Ethics and Consent

Before recruiting subjects:
1. Write a one-page informed consent form explaining:
   - What the device does (records muscle signals from the skin surface)
   - What the subject will do (wear electrodes, perform hand gestures)
   - Duration (~45 minutes per session)
   - Risks (minimal — surface electrodes only, battery powered, no mains connection)
   - Right to withdraw at any time without reason
2. Get approval from your supervisor (and university ethics committee if required)
3. Each subject signs the consent form before the session

### Subject Selection

| Criterion | Value |
|---|---|
| Number | 5 minimum (concept paper target) |
| Source | JKUAT engineering students (convenient access) |
| Age range | 18–30 (typical student population) |
| Exclusion | No neuromuscular disorders, no skin conditions at electrode site |
| Compensation | Voluntary (offer tea/snack as courtesy) |

### Experiment Protocol — Per Subject

**Total session time: approximately 45 minutes**

**Phase A: Setup (10 minutes)**
1. Explain the experiment and obtain signed consent
2. Clean the forearm with alcohol swab
3. Lightly abrade 4 electrode sites + 1 reference site
4. Apply Ag/AgCl electrodes
5. Connect electrodes to the sEMG device
6. Wait 2 minutes for electrode stabilisation
7. Check signal quality on BLE app — all 4 channels green

**Phase B: Calibration (3 minutes)**
1. Run the BLE app calibration wizard
2. Subject performs 3 reps × 8 gestures × 5 sec each
3. App fine-tunes the model
4. App confirms calibration complete

**Phase C: Recording Session (20 minutes)**
1. Guided protocol — the app displays gesture instructions:
   ```
   Trial 1:  REST (5s) → FIST (5s) → REST (5s) → OPEN (5s) → REST (5s) → ...
   ```
2. Each gesture: 10 repetitions × 5 second holds × 3 second rest between
3. Order: randomised per subject (to avoid ordering bias)
4. Total: 8 gestures × 10 reps × 8 seconds = 640 seconds ≈ 10.7 minutes
5. Include 2 minutes of continuous free movement (subject does whatever gestures they want — tests real-world naturalistic use)

**Phase D: Live Demo (5 minutes)**
1. Switch to live inference mode
2. Subject controls the gripper freely
3. Record a 3-minute video for thesis/demo

**Phase E: Teardown (5 minutes)**
1. Remove electrodes
2. Clean skin
3. Subject fills a brief questionnaire: comfort, intuitiveness, any issues

### Data Collected Per Subject

| Data | Format | Storage |
|---|---|---|
| Raw ADC samples (all 4 channels) | CSV, 2 kHz × 4 ch | `tests/subject_data/S{N}/raw/` |
| Feature vectors (24-dim) | CSV | `tests/subject_data/S{N}/features/` |
| Predictions + ground truth labels | CSV | `tests/subject_data/S{N}/predictions/` |
| Confusion matrix | PNG + CSV | `tests/subject_data/S{N}/` |
| Per-class F1 scores | CSV | `tests/subject_data/S{N}/` |
| Latency measurements | CSV | `tests/subject_data/S{N}/` |
| Video of live demo | MP4 | `tests/subject_data/S{N}/` |
| Consent form (scanned) | PDF | `tests/subject_data/S{N}/` |

### Analysis

```python
# analyse_experiment.py
import numpy as np, pandas as pd
from sklearn.metrics import f1_score, confusion_matrix, classification_report
import matplotlib.pyplot as plt

subjects = [1, 2, 3, 4, 5]
gesture_names = ['Rest','Index','Pinch','ThumbUp','Fist','Open','WristFlex','WristExt']

results = []
all_y_true, all_y_pred = [], []

for s in subjects:
    df = pd.read_csv(f'tests/subject_data/S{s}/predictions/results.csv')
    y_true = df['true_label'].values
    y_pred = df['predicted_label'].values
    latency = df['latency_ms'].values

    f1_macro = f1_score(y_true, y_pred, average='macro')
    f1_per   = f1_score(y_true, y_pred, average=None)
    acc      = np.mean(y_true == y_pred)
    lat_mean = np.mean(latency)
    lat_p95  = np.percentile(latency, 95)

    results.append({
        'subject': s,
        'accuracy': acc,
        'f1_macro': f1_macro,
        'latency_mean_ms': lat_mean,
        'latency_p95_ms': lat_p95,
    })
    all_y_true.extend(y_true)
    all_y_pred.extend(y_pred)

    # Per-subject confusion matrix
    cm = confusion_matrix(y_true, y_pred)
    fig, ax = plt.subplots(figsize=(7, 7))
    ax.imshow(cm, cmap='Blues')
    for i in range(8):
        for j in range(8):
            ax.text(j, i, str(cm[i,j]), ha='center', va='center', fontsize=9)
    ax.set_xticks(range(8)); ax.set_xticklabels(gesture_names, rotation=45)
    ax.set_yticks(range(8)); ax.set_yticklabels(gesture_names)
    ax.set_title(f'Subject {s} — F1={f1_macro:.3f}, Acc={acc:.1%}')
    plt.tight_layout()
    plt.savefig(f'tests/subject_data/S{s}/confusion_matrix.png', dpi=150)
    plt.close()

    print(f'S{s}: Acc={acc:.1%}, F1={f1_macro:.3f}, Lat={lat_mean:.1f}ms (p95={lat_p95:.1f}ms)')

# Aggregate results
df_results = pd.DataFrame(results)
print('\n=== AGGREGATE RESULTS ===')
print(f'Mean accuracy:    {df_results["accuracy"].mean():.1%} +/- {df_results["accuracy"].std():.1%}')
print(f'Mean F1 (macro):  {df_results["f1_macro"].mean():.3f} +/- {df_results["f1_macro"].std():.3f}')
print(f'Mean latency:     {df_results["latency_mean_ms"].mean():.1f} ms')
print(f'P95 latency:      {df_results["latency_p95_ms"].mean():.1f} ms')

# Overall confusion matrix
cm_all = confusion_matrix(all_y_true, all_y_pred)
fig, ax = plt.subplots(figsize=(8, 8))
ax.imshow(cm_all, cmap='Blues')
for i in range(8):
    for j in range(8):
        ax.text(j, i, str(cm_all[i,j]), ha='center', va='center', fontsize=10)
ax.set_xticks(range(8)); ax.set_xticklabels(gesture_names, rotation=45)
ax.set_yticks(range(8)); ax.set_yticklabels(gesture_names)
ax.set_xlabel('Predicted'); ax.set_ylabel('True')
ax.set_title(f'Aggregate Confusion Matrix (5 Subjects, F1={f1_score(all_y_true, all_y_pred, average="macro"):.3f})')
plt.tight_layout()
plt.savefig('tests/subject_data/aggregate_confusion_matrix.png', dpi=150)
df_results.to_csv('tests/subject_data/experiment_summary.csv', index=False)
```

### Thesis Tables from This Phase

**Table: Per-Subject Results**
| Subject | Accuracy | Macro F1 | Mean Latency | P95 Latency |
|---|---|---|---|---|
| S1 | | | | |
| S2 | | | | |
| S3 | | | | |
| S4 | | | | |
| S5 | | | | |
| **Mean** | | | | |
| **Std** | | | | |

**Table: Per-Gesture F1 Across All Subjects**
| Gesture | S1 | S2 | S3 | S4 | S5 | Mean |
|---|---|---|---|---|---|---|
| Rest | | | | | | |
| Index | | | | | | |
| ... | | | | | | |

**Phase 13 Exit Checklist:**
- [ ] 5 subjects recruited, consented, and tested
- [ ] Raw data collected and stored for all 5 subjects
- [ ] Per-subject confusion matrices generated
- [ ] Aggregate confusion matrix generated
- [ ] Mean accuracy > 80% (project target)
- [ ] Mean F1 > 0.80 (project target)
- [ ] Mean latency < 50 ms (project target)
- [ ] Calibration improves each subject's accuracy by > 10%
- [ ] Video demonstration recorded for at least 2 subjects
- [ ] All data committed to `tests/subject_data/`

---

*End of Phases 10–13. Continue to 05_BOM_AND_PROCUREMENT.md for the complete bill of materials.*
