/**
 * config.h — System-wide constants
 * sEMG Prosthetic Hand | FYP ENE212-0069/2022 | Ian Kiarie
 */
#pragma once
#include <cstdint>

// ── ADC ──────────────────────────────────────────────────────────────────────
// Only ADC1 can be used (ADC2 is disabled when Wi-Fi/BLE active — ESP32-S3 errata)
// Round-robin: sample CH0,CH1,CH2,CH3 at 8kHz total → 2kHz per channel
static constexpr int   ADC_PINS[4]     = {1, 2, 3, 4};     // GPIO1-4 (ADC1_CH0-CH3)
static constexpr int   SAMPLE_RATE     = 2000;              // Hz per channel
static constexpr int   TOTAL_ADC_RATE  = SAMPLE_RATE * 4;  // 8000 Hz round-robin
static constexpr int   ADC_ATTEN       = ADC_11db;          // range: 0-2.5V
static constexpr float ADC_VREF        = 3.3f;
static constexpr int   ADC_BITS        = 12;
static constexpr float ADC_VMIN        = 0.15f;             // usable range start
static constexpr float ADC_VMAX        = 2.45f;             // usable range end
static constexpr float ADC_VMID        = 1.65f;             // REF bias voltage

// ── Feature Extraction ───────────────────────────────────────────────────────
static constexpr int   WIN_SAMPLES     = 500;               // 250ms at 2kHz
static constexpr int   STEP_SAMPLES    = 100;               // 50ms step (80% overlap)
static constexpr int   N_CH            = 4;
static constexpr int   N_FEAT_PER_CH   = 6;                 // MAV, RMS, WL, ZC, SSC, VAR
static constexpr int   FEAT_SIZE       = N_CH * N_FEAT_PER_CH;  // 24 — avoid clash with -DN_FEATURES

// ── ML Model ─────────────────────────────────────────────────────────────────
static constexpr int   GESTURE_CLASSES = 8;                 // avoid clash with -DN_CLASSES
static constexpr int   TENSOR_ARENA    = 98304;             // 96KB — allocate in PSRAM
static constexpr float CONF_THRESHOLD  = 0.65f;            // min confidence to act

// Gesture labels (index = class ID)
static const char* GESTURE_NAMES[8] = {
    "Rest", "Open", "Power", "Pinch", "Point",
    "WristFlex", "WristExt", "ThumbsUp"
};

// ── PCA9685 Servo Driver ─────────────────────────────────────────────────────
static constexpr uint8_t  PCA9685_ADDR = 0x40;
static constexpr int      I2C_SDA      = 8;
static constexpr int      I2C_SCL      = 9;
static constexpr int      PWM_FREQ     = 50;                // Hz (standard servo)
// Pulse widths in microseconds (0° = 500, 90° = 1500, 180° = 2500)
static constexpr int      SERVO_MIN_US = 500;
static constexpr int      SERVO_MAX_US = 2500;

// PCA9685 channel assignments
static constexpr uint8_t  SERVO_THUMB   = 0;  // CH0 — SG90
static constexpr uint8_t  SERVO_INDEX   = 1;  // CH1 — SG90
static constexpr uint8_t  SERVO_MIDDLE  = 2;  // CH2 — SG90
static constexpr uint8_t  SERVO_RING    = 3;  // CH3 — SG90
static constexpr uint8_t  SERVO_PINKY   = 4;  // CH4 — SG90
static constexpr uint8_t  SERVO_WRIST   = 5;  // CH5 — MG996R

// Gesture → [thumb, index, middle, ring, pinky, wrist] angles in degrees
// 0 = fully extended, 90 = half-curl, 180 = fully closed
static constexpr uint8_t GESTURE_POSTURES[8][6] = {
    //  T    I    M    R    P   Wrist
    {   0,   0,   0,   0,   0,  90 },  // 0 Rest        — all open, wrist neutral
    {   0,   0,   0,   0,   0,  90 },  // 1 Open hand   — all extended
    { 180, 180, 180, 180, 180,  90 },  // 2 Power grasp — all closed
    {   0, 180, 180, 180, 180,  90 },  // 3 Pinch       — index+thumb closed
    { 180,   0, 180, 180, 180,  90 },  // 4 Point       — index extended
    {   0,   0,   0,   0,   0,  45 },  // 5 Wrist flex  — all open, wrist down
    {   0,   0,   0,   0,   0, 135 },  // 6 Wrist ext   — all open, wrist up
    {   0, 180, 180, 180, 180,  90 },  // 7 Thumbs up   — thumb extended only
};

// ── BLE ──────────────────────────────────────────────────────────────────────
#define BLE_DEVICE_NAME        "MyoControl"
#define BLE_SERVICE_UUID       "00001234-0000-1000-8000-00805f9b34fb"
#define BLE_CHAR_EMG_UUID      "00001235-0000-1000-8000-00805f9b34fb"  // notify: 4xint16 raw
#define BLE_CHAR_GESTURE_UUID  "00001236-0000-1000-8000-00805f9b34fb"  // notify: uint8 gesture
#define BLE_CHAR_CONF_UUID     "00001237-0000-1000-8000-00805f9b34fb"  // notify: float32 confidence
#define BLE_CHAR_CALIB_UUID    "00001238-0000-1000-8000-00805f9b34fb"  // write: calibration cmds
#define BLE_CHAR_STATUS_UUID   "00001239-0000-1000-8000-00805f9b34fb"  // read/notify: device state

// ── Timing ───────────────────────────────────────────────────────────────────
static constexpr uint32_t  SERVO_MOVE_MS   = 200;  // max servo move time
static constexpr uint32_t  BLE_NOTIFY_MS   = 50;   // notify at 20Hz max
static constexpr uint32_t  WATCHDOG_MS     = 5000; // reset if no sample for 5s
