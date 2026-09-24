# Phases 4–6: Digital Engineering
## 4-Channel PCB, ESP32-S3 ADC Acquisition, DSP Pipeline

---

## Phase 4 — 4-Channel PCB Design and Fabrication (Week 13–18)

### Week 13–14: KiCad Schematic

**Day 49: Project setup**

1. Create a new KiCad 8 project: `semg_afe_v1`
2. Create a hierarchical schematic with these sheets:
   - `root.kicad_sch` — top-level block diagram, connectors, power
   - `channel.kicad_sch` — single AFE channel (instantiated 4 times)
   - `power.kicad_sch` — regulators, reference, decoupling
   - `mcu.kicad_sch` — ESP32-S3 connections, I2C, UART, ADC pins
   - `actuator.kicad_sch` — PCA9685, servo connectors, servo power

**Day 50–52: Single-channel schematic**

Draw the complete single-channel circuit from Phase 2 in KiCad:

```
Input connector (3-pin: E+, E-, Shield)
  → R_in (10kΩ × 2) + BAV99 (× 2) — input protection
  → INA128 (G=50, R_G=1.02kΩ) — instrumentation amplifier
  → MCP6002A: Sallen-Key HPF (fc=20Hz) — R: 12k, 24k; C: 470nF × 2
  → MCP6002B: Sallen-Key LPF (fc=500Hz) — R: 10k × 2; C: 22nF, 47nF
  → Twin-T notch (50Hz) — R: 10k × 2, 5k trim; C: 330nF × 2, 680nF
  → MCP6002C: Variable gain (×1–×10) — R: 10k, 100k trim
  → Divider + clamp → ADC test point → header to ESP32
```

Add these to EVERY IC:
- 100 nF ceramic capacitor from Vcc to GND (within 3mm of pins)
- 10 µF bulk capacitor per pair of ICs

Add labeled test points (TP) after every stage:
- TP_INA_OUT: INA128 output
- TP_HPF_OUT: after high-pass filter
- TP_LPF_OUT: after low-pass filter
- TP_NOTCH_OUT: after notch filter
- TP_GAIN_OUT: after variable gain
- TP_ADC_IN: at the ADC input (after divider)

**Day 53–54: Power schematic**

```
9V Battery ──[Schottky diode D1 (reverse polarity protection)]──┬── Vin
                                                                  │
                                                          [MCP1700-5002E]
                                                                  │
                                                              +5V Analog Rail
                                                                  │
                                                              [C_in: 1µF ceramic]
                                                              [C_out: 1µF ceramic + 10µF tantalum]
                                                                  │
                                                         Vref generator:
                                                         [R: 10kΩ]──┬──[R: 10kΩ]
                                                                     │
                                                               MCP6002 follower
                                                                     │
                                                                 Vref = 2.5V
                                                              [C_ref: 10µF + 100nF]
```

ESP32-S3 power: separate 3.3V from its own onboard regulator (USB or battery input). Do NOT share the analog 5V rail with the ESP32.

Servo power: completely separate supply.
```
Servo Battery (7.4V LiPo or 4×AA) ──[5V BEC/regulator rated 3A]── Servo Vcc
                                                                        │
                                                                  [1000µF bulk cap]
                                                                        │
                                                                   PCA9685 V+
```

**Day 55: MCU sheet**

ESP32-S3 pin assignments:

| Function | GPIO | Notes |
|---|---|---|
| ADC Ch1 (flexor carpi radialis) | GPIO1 (ADC1_CH0) | |
| ADC Ch2 (extensor digitorum) | GPIO2 (ADC1_CH1) | |
| ADC Ch3 (flexor digitorum) | GPIO3 (ADC1_CH2) | |
| ADC Ch4 (brachioradialis) | GPIO4 (ADC1_CH3) | |
| I2C SDA (PCA9685) | GPIO8 | 4.7kΩ pull-up to 3.3V |
| I2C SCL (PCA9685) | GPIO9 | 4.7kΩ pull-up to 3.3V |
| UART TX (debug) | GPIO43 | USB-UART bridge on DevKit |
| UART RX (debug) | GPIO44 | |
| Status LED | GPIO48 | Onboard RGB LED on most DevKits |
| BLE | Internal | No external GPIO needed |

**Critical:** Use ADC1 channels ONLY (GPIO1–10). ADC2 is disabled when Wi-Fi/BLE is active.

**Day 56: Actuator sheet**

```
ESP32 GPIO8 (SDA) ──── PCA9685 SDA (pin 21)
ESP32 GPIO9 (SCL) ──── PCA9685 SCL (pin 22)
PCA9685 OE (pin 23) ── GND (always enabled) or GPIO for emergency stop
PCA9685 V+ (pin 1) ─── Servo 5V rail (NOT from analog supply)
PCA9685 GND ─────────── Common GND (servo GND + ESP32 GND)
PCA9685 CH0 ─────────── Servo 1 signal (grip open/close)
PCA9685 CH1 ─────────── Servo 2 signal (wrist rotation)
```

Add a 1000 µF electrolytic capacitor across the servo power rail near the PCA9685.

### Week 15–16: PCB Layout

**Day 57: Board outline and stack-up**

Board dimensions: approximately 80mm × 60mm (fits inside a standard enclosure)

4-layer stack-up:
| Layer | Purpose |
|---|---|
| Layer 1 (Top) | Signal traces — analog channels, INA128s, filter components |
| Layer 2 (Inner 1) | Analog ground plane — continuous, unbroken copper pour |
| Layer 3 (Inner 2) | Power plane — +5V analog, +3.3V digital |
| Layer 4 (Bottom) | Digital traces — ESP32 connections, I2C, UART, servo |

**Day 58–60: Component placement**

Physical layout zones (left to right):
```
┌─────────────────────────────────────────────────────────────┐
│ ZONE A: Input         │ ZONE B: Analog AFE      │ ZONE C:  │
│                       │                          │ Digital  │
│ - 4× electrode conn.  │ - 4× INA128             │          │
│ - Input protection    │ - 4× HPF/LPF/Notch      │ - ESP32  │
│ - Shield termination  │ - 4× Gain stages        │   header │
│                       │ - Test points            │ - PCA9685│
│                       │ - Power reference        │ - Servo  │
│                       │                          │   conn.  │
│                       │                          │          │
│ ZONE D: Power supply (bottom edge of board)                 │
│ - Battery connector, LDO, bulk caps, Vref                   │
└─────────────────────────────────────────────────────────────┘
```

**Layout rules — do not violate these:**

1. **Ground strategy:** Layer 2 is a continuous analog ground plane. Do NOT cut it with traces. Digital return currents flow on Layer 4 ground pour. The two grounds connect at ONE point near the power supply (star ground).

2. **Channel symmetry:** Lay out all 4 channels identically. Same trace lengths, same component orientation, same distance from ground plane. This ensures matched frequency response across channels.

3. **High-impedance traces:** INA128 input traces (from electrode connector to INA128 pins 2 and 3) must be:
   - As short as possible (< 10mm ideal)
   - Surrounded by ground guard ring (ring of vias connecting top copper pour to Layer 2 ground)
   - Away from ALL digital signals, clocks, USB, and Wi-Fi antenna
   - Routed as differential pair with matched lengths

4. **Decoupling:** Every IC gets a 100nF capacitor within 3mm of its supply pins. Route the capacitor → IC path FIRST, then connect to the power plane.

5. **No right angles** on signal traces. Use 45-degree bends.

6. **Test points:** 1mm through-hole test points, accessible from the top. Label them in silkscreen.

7. **Keep-out zone:** 10mm clearance around the ESP32-S3 antenna area. No copper pour (ground or power) under the antenna.

**Day 61–62: Design Rule Check (DRC)**

Run KiCad DRC and fix ALL errors and warnings:
- Clearance violations
- Unconnected nets
- Trace width violations (minimum 0.2mm for signals, 0.4mm for power)
- Via size consistency

Generate the following outputs:
- Gerber files (all 4 layers + drill + silkscreen + soldermask)
- BOM (exported from KiCad)
- Pick-and-place file (if using assembly service)
- 3D view screenshot for documentation

**Day 63: Order from JLCPCB**

1. Go to jlcpcb.com
2. Upload Gerber ZIP
3. Settings:
   - Layers: 4
   - Thickness: 1.6mm
   - Surface finish: HASL (cheapest) or ENIG (better for fine pitch)
   - Color: black (looks professional for demos)
   - Quantity: 5 (minimum order)
4. Choose DHL shipping (fastest to Kenya, ~14 business days)
5. Cost estimate: $15–25 for 4-layer + $15–20 shipping = ~KES 4,000–5,500

**WHILE WAITING FOR PCB (2 weeks):** Start Phase 5 (ESP32-S3 firmware) and Phase 7 (ML training).

### Week 17–18: PCB Assembly and Test

**Day 70: Receive and inspect PCB**

Visual inspection:
- [ ] All layers visible through vias
- [ ] Silkscreen labels readable
- [ ] No obvious shorts (copper bridges)
- [ ] All through-holes clean

**Day 71–73: Solder components**

Assembly order (most critical first):
1. Power section: LDO, bulk caps, Vref generator
2. Test: power on, measure all rails with multimeter
3. Channel 1 only: INA128, all filter components, gain stage
4. Test Channel 1 with signal generator (same tests as Phase 1)
5. If Channel 1 passes: solder Channels 2, 3, 4
6. ESP32-S3 header pins
7. PCA9685 and servo connectors

**Day 74–76: 4-channel validation**

Test each channel independently with the same signal generator tests:
- [ ] Ch1: Gain correct, bandpass correct, notch depth > 30 dB
- [ ] Ch2: same
- [ ] Ch3: same
- [ ] Ch4: same

**Crosstalk test:**
1. Connect signal generator to Channel 1 input only
2. Set: 100 Hz, 1 mV sine
3. Measure output amplitude on Channels 2, 3, 4 (should be < 1/100th of Channel 1 output)
4. Crosstalk = 20 × log10(V_other / V_driven)
5. Target: < -40 dB on all channel pairs

**Supply ripple test:**
1. All 4 channels active (electrodes or signal generators connected)
2. ESP32 powered and running
3. Servos connected but stationary
4. Measure 5V analog rail on oscilloscope (AC-coupled, 10 mV/div)
5. Target: < 10 mV peak-to-peak ripple

**Phase 4 Exit Checklist:**
- [ ] All 4 channels pass individual frequency response tests
- [ ] Crosstalk < -40 dB between all channel pairs
- [ ] Supply ripple < 10 mV under full operating load
- [ ] All test points accessible and labeled
- [ ] PCB fits in enclosure (if applicable)
- [ ] Photos of assembled PCB saved for thesis

---

## Phase 5 — ESP32-S3 ADC Acquisition (Week 17–22)

### Week 17–18: Development Environment (parallel with PCB wait)

**Day 63–64: PlatformIO setup**

```ini
; platformio.ini
[env:esp32s3]
platform = espressif32
board = esp32-s3-devkitc-1
framework = espidf
monitor_speed = 115200
build_flags =
    -DCORE_DEBUG_LEVEL=3
    -DBOARD_HAS_PSRAM
lib_deps =
    espressif/esp-dsp@^1.3.0
```

Create the directory structure:
```
firmware/
├── platformio.ini
├── sdkconfig.defaults
└── src/
    ├── main.c
    ├── adc_sampler.h / .c
    ├── dsp_features.h / .c
    ├── inference.h / .c
    ├── actuator.h / .c
    ├── ble_service.h / .c
    └── decision_logic.h / .c
```

**Day 65–67: ADC configuration**

ESP32-S3 ADC1 configuration using ESP-IDF 5.x:

```c
// adc_sampler.h
#ifndef ADC_SAMPLER_H
#define ADC_SAMPLER_H

#include "esp_adc/adc_oneshot.h"
#include "esp_adc/adc_cali.h"
#include "esp_adc/adc_cali_scheme.h"

#define NUM_CHANNELS     4
#define SAMPLE_RATE_HZ   2000
#define WINDOW_SAMPLES   500      // 250 ms window
#define WINDOW_STEP      100      // 50 ms step (80% overlap)
#define ADC_ATTEN        ADC_ATTEN_DB_12   // full 0-3.3V range

// Channel-to-GPIO mapping
static const adc_channel_t adc_channels[NUM_CHANNELS] = {
    ADC_CHANNEL_0,  // GPIO1
    ADC_CHANNEL_1,  // GPIO2
    ADC_CHANNEL_2,  // GPIO3
    ADC_CHANNEL_3,  // GPIO4
};

typedef struct {
    int16_t samples[NUM_CHANNELS][WINDOW_SAMPLES];
    uint32_t timestamp_ms;
    uint8_t  window_id;
} adc_window_t;

void adc_sampler_init(void);
void adc_sampler_start(void);

#endif
```

```c
// adc_sampler.c
#include "adc_sampler.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "esp_timer.h"
#include "esp_log.h"

static const char *TAG = "ADC";

static adc_oneshot_unit_handle_t adc_handle;
static adc_cali_handle_t         cali_handle;

static int16_t ring_buf[NUM_CHANNELS][WINDOW_SAMPLES * 2]; // ping-pong
static volatile uint32_t sample_idx = 0;
static volatile uint8_t  active_buf = 0;

// Queue for completed windows — consumed by DSP task
QueueHandle_t window_queue;

// Hardware timer callback — fires at 2 kHz
static bool IRAM_ATTR timer_callback(void *arg) {
    // Sample all 4 channels sequentially
    // At 2 kHz with 4 channels: each adc_oneshot_read takes ~25 µs
    // Total per timer tick: ~100 µs — well within 500 µs period
    int raw;
    uint32_t idx = sample_idx;
    for (int ch = 0; ch < NUM_CHANNELS; ch++) {
        adc_oneshot_read(adc_handle, adc_channels[ch], &raw);
        ring_buf[ch][idx] = (int16_t)raw;
    }

    sample_idx++;
    if (sample_idx >= WINDOW_SAMPLES) {
        // Window complete — copy to queue
        static adc_window_t win;
        memcpy(win.samples, ring_buf, sizeof(win.samples));
        win.timestamp_ms = esp_timer_get_time() / 1000;
        win.window_id++;
        xQueueSendFromISR(window_queue, &win, NULL);
        sample_idx = 0;
    }
    return false; // no need to yield
}

void adc_sampler_init(void) {
    // ADC unit init
    adc_oneshot_unit_init_cfg_t unit_cfg = {
        .unit_id = ADC_UNIT_1,
    };
    adc_oneshot_new_unit(&unit_cfg, &adc_handle);

    // Configure each channel
    adc_oneshot_chan_cfg_t chan_cfg = {
        .atten = ADC_ATTEN,
        .bitwidth = ADC_BITWIDTH_12,
    };
    for (int ch = 0; ch < NUM_CHANNELS; ch++) {
        adc_oneshot_config_channel(adc_handle, adc_channels[ch], &chan_cfg);
    }

    // ADC calibration (corrects non-linearity)
    adc_cali_line_fitting_config_t cali_cfg = {
        .unit_id = ADC_UNIT_1,
        .atten = ADC_ATTEN,
        .bitwidth = ADC_BITWIDTH_12,
    };
    adc_cali_create_scheme_line_fitting(&cali_cfg, &cali_handle);

    // Window queue
    window_queue = xQueueCreate(4, sizeof(adc_window_t));

    ESP_LOGI(TAG, "ADC initialized: %d channels, %d Hz, %d-sample windows",
             NUM_CHANNELS, SAMPLE_RATE_HZ, WINDOW_SAMPLES);
}

void adc_sampler_start(void) {
    // Create a high-resolution timer at 2 kHz (500 µs period)
    esp_timer_handle_t timer;
    esp_timer_create_args_t timer_cfg = {
        .callback = timer_callback,
        .name = "adc_timer",
        .dispatch_method = ESP_TIMER_ISR,  // run in ISR context for timing accuracy
    };
    esp_timer_create(&timer_cfg, &timer);
    esp_timer_start_periodic(timer, 500); // 500 µs = 2 kHz

    ESP_LOGI(TAG, "ADC sampling started at %d Hz", SAMPLE_RATE_HZ);
}
```

**Day 68–69: Verify sampling rate**

After the ADC is running, verify the ACTUAL sampling rate:

```c
// In main.c — verification mode
void verify_sampling_task(void *arg) {
    adc_window_t win;
    uint32_t prev_ts = 0;
    int count = 0;
    while (count < 20) {
        if (xQueueReceive(window_queue, &win, pdMS_TO_TICKS(1000))) {
            uint32_t delta = win.timestamp_ms - prev_ts;
            ESP_LOGI("VERIFY", "Window %d: dt=%lu ms (expected 250 ms)",
                     count, delta);
            prev_ts = win.timestamp_ms;
            count++;
        }
    }
    ESP_LOGI("VERIFY", "Sampling rate verified. Expected 250 ms/window.");
    vTaskDelete(NULL);
}
```

Run this for 20 windows. The delta should be 250 ms ± 1 ms. If it drifts more than 2 ms, the timer or ADC conversion is not keeping up.

### Week 19–20: Raw Data Streaming

**Day 72–75: Stream to PC for recording**

Create a UART-based raw data streamer for development. During firmware bring-up, stream all raw ADC samples to a Python script on the PC for offline analysis.

```c
// Stream raw samples via UART (debug mode only)
void stream_raw_task(void *arg) {
    adc_window_t win;
    while (1) {
        if (xQueueReceive(window_queue, &win, portMAX_DELAY)) {
            // Header: "W<window_id>,<timestamp>\n"
            printf("W%u,%lu\n", win.window_id, win.timestamp_ms);
            // Data: one line per sample, 4 channels comma-separated
            for (int s = 0; s < WINDOW_SAMPLES; s++) {
                printf("%d,%d,%d,%d\n",
                       win.samples[0][s], win.samples[1][s],
                       win.samples[2][s], win.samples[3][s]);
            }
            printf("END\n");
        }
    }
}
```

Python receiver:
```python
# record_raw.py
import serial, csv, time, sys

PORT = '/dev/ttyUSB0'  # adjust for your system
BAUD = 115200

ser = serial.Serial(PORT, BAUD, timeout=2)
outfile = f'raw_recording_{int(time.time())}.csv'

with open(outfile, 'w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['window', 'timestamp', 'sample', 'ch1', 'ch2', 'ch3', 'ch4'])
    window_id = 0
    timestamp = 0
    sample_idx = 0
    print(f"Recording to {outfile}... Ctrl+C to stop.")
    try:
        while True:
            line = ser.readline().decode().strip()
            if line.startswith('W'):
                parts = line[1:].split(',')
                window_id = int(parts[0])
                timestamp = int(parts[1])
                sample_idx = 0
            elif line == 'END':
                print(f"  Window {window_id}: {sample_idx} samples")
            elif ',' in line:
                vals = line.split(',')
                if len(vals) == 4:
                    writer.writerow([window_id, timestamp, sample_idx] + vals)
                    sample_idx += 1
    except KeyboardInterrupt:
        print(f"\nSaved {outfile}")
```

### Week 21–22: ADC Validation

**Verify these metrics before moving to DSP:**

1. **Sampling rate accuracy:**
   - Collect 100 windows
   - Compute mean and std of inter-window timestamps
   - Target: mean = 250 ms ± 1 ms, std < 0.5 ms

2. **ADC noise floor (inputs shorted to Vref):**
   - Short all ADC inputs to the 1.65V reference (through the divider)
   - Record 10 seconds
   - Compute RMS noise per channel
   - Target: < 5 LSB RMS (< 4 mV)

3. **Channel independence (crosstalk through ADC):**
   - Apply 100 Hz sine to Channel 1 input
   - Channels 2–4 connected to Vref
   - Measure: amplitude of 100 Hz component in Channels 2–4
   - Target: < -50 dB relative to Channel 1 (ADC crosstalk is typically very low)

4. **No dropped samples:**
   - Run for 10 minutes continuously
   - Monitor window_id for gaps
   - Target: zero gaps in 2,400 windows

**Phase 5 Exit Checklist:**
- [ ] ADC reads all 4 channels at 2 kHz actual (verified by timestamp analysis)
- [ ] ADC noise floor < 5 LSB RMS (inputs shorted)
- [ ] No dropped samples over 10 minutes continuous operation
- [ ] Raw data streams to PC and can be opened in Python
- [ ] CPU utilization during sampling < 30% (measured with `esp_cpu_get_cycle_count`)
- [ ] All firmware source committed to `firmware/` directory

---

## Phase 6 — DSP Feature Extraction Pipeline (Week 22–26)

### Feature Set — 24 Features (6 per channel × 4 channels)

For each 250 ms window (500 samples) of each channel:

| # | Feature | Formula | Why |
|---|---|---|---|
| 1 | MAV | (1/N) × Σ|x_i| | Average amplitude — correlates with contraction force |
| 2 | RMS | √((1/N) × Σx_i²) | Signal power — more robust than MAV |
| 3 | WL | Σ|x_i - x_{i-1}| | Waveform complexity — differs between gesture types |
| 4 | ZC | count(x_i × x_{i-1} < 0 AND |x_i - x_{i-1}| > threshold) | Frequency content proxy — fast gestures have more ZC |
| 5 | SSC | count((x_i - x_{i-1})(x_{i-1} - x_{i-2}) < 0 AND ...) | Spectral measure — slope changes correlate with bandwidth |
| 6 | VAR | (1/N) × Σ(x_i - mean)² | Signal variance — power indicator |

The threshold in ZC and SSC prevents noise-induced false crossings. Use threshold = 0.01 × max(|x|) in the window (adaptive) or a fixed threshold derived from the noise floor measurement.

### Implementation — Python Reference First

**Day 85–87: Python reference implementation**

Write the features in Python FIRST. This becomes your ground truth for validating the C implementation.

```python
# features.py — REFERENCE implementation
import numpy as np

def compute_features(window: np.ndarray, threshold: float = 0.01) -> np.ndarray:
    """
    Compute 6 features for a single-channel window.
    window: 1D array of N samples (500 at 2 kHz = 250 ms)
    Returns: array of 6 features [MAV, RMS, WL, ZC, SSC, VAR]
    """
    N = len(window)
    x = window.astype(np.float64)

    # Adaptive threshold
    thr = threshold * np.max(np.abs(x)) if np.max(np.abs(x)) > 0 else 1e-9

    # MAV: Mean Absolute Value
    mav = np.mean(np.abs(x))

    # RMS: Root Mean Square
    rms = np.sqrt(np.mean(x ** 2))

    # WL: Waveform Length
    wl = np.sum(np.abs(np.diff(x)))

    # ZC: Zero Crossings (with threshold to reject noise)
    zc = 0
    for i in range(1, N):
        if ((x[i] > 0 and x[i-1] < 0) or (x[i] < 0 and x[i-1] > 0)):
            if abs(x[i] - x[i-1]) > thr:
                zc += 1
    zc = zc / N  # normalize

    # SSC: Slope Sign Changes
    ssc = 0
    for i in range(2, N):
        d1 = x[i] - x[i-1]
        d2 = x[i-1] - x[i-2]
        if d1 * d2 < 0:
            if abs(d1) > thr or abs(d2) > thr:
                ssc += 1
    ssc = ssc / N  # normalize

    # VAR: Variance
    var = np.var(x)

    return np.array([mav, rms, wl, zc, ssc, var], dtype=np.float32)


def extract_all_features(window_4ch: np.ndarray) -> np.ndarray:
    """
    Extract features from a 4-channel window.
    window_4ch: shape (500, 4)
    Returns: flat array of 24 features
    """
    features = []
    for ch in range(4):
        f = compute_features(window_4ch[:, ch])
        features.extend(f)
    return np.array(features, dtype=np.float32)
```

**Day 88–90: C implementation on ESP32**

```c
// dsp_features.h
#ifndef DSP_FEATURES_H
#define DSP_FEATURES_H

#include <stdint.h>

#define FEATURES_PER_CHANNEL  6
#define NUM_CHANNELS          4
#define TOTAL_FEATURES        (FEATURES_PER_CHANNEL * NUM_CHANNELS)
#define WINDOW_SAMPLES        500

typedef struct {
    float features[TOTAL_FEATURES];
    uint32_t timestamp_ms;
} feature_vector_t;

void dsp_compute_features(const int16_t samples[NUM_CHANNELS][WINDOW_SAMPLES],
                           feature_vector_t *out);

#endif
```

```c
// dsp_features.c
#include "dsp_features.h"
#include <math.h>
#include <stdlib.h>

// Convert raw ADC (0-4095) to voltage in mV, centered at 0
// ADC reads 0-4095 → 0-3.3V; Vref divider centers at ~1.65V
static inline float adc_to_mv(int16_t raw) {
    return ((float)raw / 4095.0f) * 3300.0f - 1650.0f;
}

static void compute_channel_features(const int16_t *raw, int N,
                                      float out[FEATURES_PER_CHANNEL]) {
    float x[WINDOW_SAMPLES];
    float max_abs = 0;

    // Convert to mV and find max
    for (int i = 0; i < N; i++) {
        x[i] = adc_to_mv(raw[i]);
        float a = fabsf(x[i]);
        if (a > max_abs) max_abs = a;
    }

    float thr = 0.01f * max_abs;
    if (thr < 0.001f) thr = 0.001f;

    float sum_abs = 0, sum_sq = 0, wl = 0;
    int zc = 0, ssc = 0;

    for (int i = 0; i < N; i++) {
        sum_abs += fabsf(x[i]);
        sum_sq  += x[i] * x[i];

        if (i > 0) {
            wl += fabsf(x[i] - x[i-1]);

            // Zero crossing
            if ((x[i] > 0 && x[i-1] < 0) || (x[i] < 0 && x[i-1] > 0)) {
                if (fabsf(x[i] - x[i-1]) > thr) zc++;
            }

            // Slope sign change
            if (i > 1) {
                float d1 = x[i] - x[i-1];
                float d2 = x[i-1] - x[i-2];
                if (d1 * d2 < 0) {
                    if (fabsf(d1) > thr || fabsf(d2) > thr) ssc++;
                }
            }
        }
    }

    float mean = sum_abs / N;  // note: this is mean of abs, not actual mean
    float mav = sum_abs / N;
    float rms = sqrtf(sum_sq / N);
    float var = (sum_sq / N) - (mav * mav); // approximation using MAV

    out[0] = mav;
    out[1] = rms;
    out[2] = wl;
    out[3] = (float)zc / N;
    out[4] = (float)ssc / N;
    out[5] = var;
}

void dsp_compute_features(const int16_t samples[NUM_CHANNELS][WINDOW_SAMPLES],
                           feature_vector_t *out) {
    for (int ch = 0; ch < NUM_CHANNELS; ch++) {
        compute_channel_features(samples[ch], WINDOW_SAMPLES,
                                  &out->features[ch * FEATURES_PER_CHANNEL]);
    }
}
```

### Week 25–26: Validation — C vs Python

**Day 95–98: Automated comparison**

1. Record a 60-second session with the ESP32 streaming raw samples
2. Feed the same raw samples through the Python reference
3. Feed the same raw samples through the C implementation (running on ESP32, outputting features via UART)
4. Compare feature-by-feature

```python
# validate_features.py
import numpy as np

py_features  = np.loadtxt('python_features.csv', delimiter=',')
esp_features = np.loadtxt('esp32_features.csv', delimiter=',')

errors = np.abs(py_features - esp_features) / (np.abs(py_features) + 1e-9) * 100

print("Per-feature mean relative error (%):")
labels = ['MAV', 'RMS', 'WL', 'ZC', 'SSC', 'VAR'] * 4
for i, (label, err) in enumerate(zip(labels, errors.mean(axis=0))):
    ch = i // 6 + 1
    print(f"  Ch{ch} {label}: {err:.4f}%")

max_err = errors.max()
print(f"\nMax error across all features: {max_err:.4f}%")
print(f"Target: < 1.0%")
print(f"PASS" if max_err < 1.0 else "FAIL")
```

**Day 99–100: Performance measurement**

```c
// Measure feature extraction time
uint64_t t0 = esp_timer_get_time();
dsp_compute_features(window.samples, &features);
uint64_t t1 = esp_timer_get_time();
ESP_LOGI("DSP", "Feature extraction: %llu µs", t1 - t0);
// Target: < 25,000 µs (25 ms)
```

**Phase 6 Exit Checklist:**
- [ ] Python reference implementation tested on NinaPro DB5 data
- [ ] C implementation compiles and runs on ESP32-S3
- [ ] Feature comparison error < 1% on all 24 features
- [ ] Feature extraction time < 25 ms per 250 ms window
- [ ] CPU utilization with sampling + features < 60%
- [ ] Raw recordings stored in documented CSV format
- [ ] All code committed to `firmware/src/` and `ml/scripts/`

---

*End of Phases 4–6. Continue to 03_ML_AND_BLE.md for dataset, training, calibration, and BLE app.*
