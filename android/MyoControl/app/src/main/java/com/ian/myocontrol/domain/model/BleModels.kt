package com.ian.myocontrol.domain.model

/**
 * The state of the BLE connection to the ESP32-S3.
 */
sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Scanning : BleConnectionState()
    data class Connecting(val deviceAddress: String) : BleConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

/**
 * A decoded GESTURE_RESULT characteristic payload (0xAA01).
 */
data class GestureResult(
    val gestureClass: Int,       // 0-7, maps to GestureLabel
    val confidence: Float,       // 0.0 - 1.0
    val latencyMs: Int,          // end-to-end inference latency
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * A decoded SIGNAL_METRICS characteristic payload (0xAA02).
 * RMS value per channel (0.0 - 1.0 normalised).
 */
data class SignalMetrics(
    val ch1Rms: Float,
    val ch2Rms: Float,
    val ch3Rms: Float,
    val ch4Rms: Float,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Human-readable label for each gesture class.
 * Must match the mapping in explore_ninapro.py and CNN training notebook.
 */
enum class GestureLabel(val displayName: String) {
    REST("Rest"),
    OPEN_HAND("Open Hand"),
    POWER_GRASP("Power Grasp"),
    PINCH("Pinch"),
    POINT("Point"),
    WRIST_FLEX("Wrist Flex"),
    WRIST_EXT("Wrist Extend"),
    THUMBS_UP("Thumbs Up");

    companion object {
        fun fromClass(classIndex: Int): GestureLabel =
            entries.getOrElse(classIndex) { REST }
    }
}
