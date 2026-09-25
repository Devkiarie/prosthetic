package com.ian.myocontrol.core.ble

import java.util.UUID

/**
 * MyoControl GATT UUIDs.
 * These must match exactly what the ESP32-S3 firmware advertises.
 *
 * Base service: 0000AA00-0000-1000-8000-00805F9B34FB
 */
object MyoGatt {
    val SERVICE_UUID: UUID = UUID.fromString("0000AA00-0000-1000-8000-00805F9B34FB")

    /** ESP32 -> Phone (notify): {gesture:u8, confidence:f32, latency_ms:u16} */
    val CHAR_GESTURE_RESULT: UUID = UUID.fromString("0000AA01-0000-1000-8000-00805F9B34FB")

    /** ESP32 -> Phone (notify): {ch1:f32, ch2:f32, ch3:f32, ch4:f32} RMS per window */
    val CHAR_SIGNAL_METRICS: UUID = UUID.fromString("0000AA02-0000-1000-8000-00805F9B34FB")

    /** Phone -> ESP32 (write): {cmd:u8, gesture:u8} */
    val CHAR_CALIBRATION_CMD: UUID = UUID.fromString("0000AA03-0000-1000-8000-00805F9B34FB")

    /** Phone -> ESP32 (write): {confidence_threshold:f32, debounce_ms:u16} */
    val CHAR_CONFIG: UUID = UUID.fromString("0000AA04-0000-1000-8000-00805F9B34FB")

    /** ESP32 -> Phone (notify, optional debug): raw 4-ch samples at 200 Hz */
    val CHAR_RAW_STREAM: UUID = UUID.fromString("0000AA05-0000-1000-8000-00805F9B34FB")

    /** Standard CCCD UUID (Client Characteristic Configuration Descriptor) */
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Calibration command bytes (CHAR_CALIBRATION_CMD cmd field)
    const val CMD_IDLE: Byte    = 0x00
    const val CMD_START: Byte   = 0x01
    const val CMD_RECORD: Byte  = 0x02
    const val CMD_DONE: Byte    = 0x03
    const val CMD_STOP: Byte    = 0xFF.toByte()  // emergency stop

    // BLE device name the ESP32 advertises
    const val DEVICE_NAME = "MyoControl"
}
