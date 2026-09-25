package com.ian.myocontrol.core.ble

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/**
 * Returns a lambda that, when invoked, either calls [onGranted] immediately
 * (if permissions are already granted or not needed) or launches the system
 * permission dialog and calls [onGranted] once all permissions are accepted.
 *
 * Usage in a Composable:
 *   val requestBle = rememberBlePermissionRequest { bleManager.startScan() }
 *   Button(onClick = requestBle) { ... }
 */
@Composable
fun rememberBlePermissionRequest(onGranted: () -> Unit): () -> Unit {
    // On API < 31 only ACCESS_FINE_LOCATION is required for BLE scanning.
    val permissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) onGranted()
    }

    return { launcher.launch(permissions) }
}
