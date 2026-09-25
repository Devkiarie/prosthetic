package com.ian.myocontrol.core.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.ian.myocontrol.domain.model.BleConnectionState
import com.ian.myocontrol.domain.model.BleDeviceInfo
import com.ian.myocontrol.domain.model.GestureResult
import com.ian.myocontrol.domain.model.SignalMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BleManager — owns the single BLE connection lifecycle.
 *
 * Scanning now accumulates found devices into [scannedDevices] instead of
 * auto-connecting. The UI shows a picker; the user taps a device to call
 * [connectToDevice]. This allows choosing between multiple ESP32 units.
 */
@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    /** Devices found during current scan session — cleared on each new scan start. */
    private val _scannedDevices = MutableStateFlow<List<BleDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _gestureResult = MutableStateFlow<GestureResult?>(null)
    val gestureResult: StateFlow<GestureResult?> = _gestureResult.asStateFlow()

    private val _signalMetrics = MutableStateFlow<SignalMetrics?>(null)
    val signalMetrics: StateFlow<SignalMetrics?> = _signalMetrics.asStateFlow()

    private var gatt: BluetoothGatt? = null

    // ── Scan ─────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        _scannedDevices.value = emptyList()          // clear previous results
        _connectionState.value = BleConnectionState.Scanning

        // Broad scan — no service UUID filter so we show ALL nearby BLE devices,
        // not just those already advertising our custom service UUID.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _scannedDevices.value = emptyList()   // clear so picker hides
        // If still in Scanning state (user cancelled), revert to Disconnected
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val info = BleDeviceInfo(
                name    = device.name ?: "Unknown device",
                address = device.address,
                rssi    = result.rssi,
                device  = device
            )
            _scannedDevices.update { current ->
                val existing = current.indexOfFirst { it.address == info.address }
                if (existing >= 0) {
                    current.toMutableList().also { it[existing] = info }
                } else {
                    current + info
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.Error("Scan failed: code $errorCode")
        }
    }

    // ── Connect ───────────────────────────────────────────────────────────────

    /** Called from the UI when the user selects a device from the picker. */
    @SuppressLint("MissingPermission")
    fun connectToDevice(info: BleDeviceInfo) {
        stopScan()
        _connectionState.value = BleConnectionState.Connecting(info.address)
        gatt = info.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = BleConnectionState.Disconnected
        _scannedDevices.value = emptyList()
    }

    // ── GATT callbacks ────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName    = gatt.device.name ?: "ESP32-S3",
                        deviceAddress = gatt.device.address
                    )
                    gatt.requestMtu(247)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                    gatt.close()
                    this@BleManager.gatt = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            enableNotification(gatt, MyoGatt.CHAR_GESTURE_RESULT)
            enableNotification(gatt, MyoGatt.CHAR_SIGNAL_METRICS)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                MyoGatt.CHAR_GESTURE_RESULT -> decodeGestureResult(value)
                MyoGatt.CHAR_SIGNAL_METRICS -> decodeSignalMetrics(value)
            }
        }
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, charUuid: java.util.UUID) {
        val service = gatt.getService(MyoGatt.SERVICE_UUID) ?: return
        val char    = service.getCharacteristic(charUuid) ?: return
        gatt.setCharacteristicNotification(char, true)
        val descriptor = char.getDescriptor(MyoGatt.CCCD_UUID) ?: return
        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }

    // ── Payload decoders ──────────────────────────────────────────────────────

    private fun decodeGestureResult(bytes: ByteArray) {
        if (bytes.size < 7) return
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val gesture    = buf.get().toInt() and 0xFF
        val confidence = buf.float
        val latency    = buf.short.toInt() and 0xFFFF
        _gestureResult.value = GestureResult(
            gestureClass = gesture,
            confidence   = confidence,
            latencyMs    = latency
        )
    }

    private fun decodeSignalMetrics(bytes: ByteArray) {
        if (bytes.size < 16) return
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        _signalMetrics.value = SignalMetrics(
            ch1Rms = buf.float,
            ch2Rms = buf.float,
            ch3Rms = buf.float,
            ch4Rms = buf.float
        )
    }

    // ── Config write ──────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun sendEmergencyStop() {
        val service = gatt?.getService(MyoGatt.SERVICE_UUID) ?: return
        val char    = service.getCharacteristic(MyoGatt.CHAR_CONFIG) ?: return
        gatt?.writeCharacteristic(
            char,
            byteArrayOf(MyoGatt.CMD_STOP),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }
}
