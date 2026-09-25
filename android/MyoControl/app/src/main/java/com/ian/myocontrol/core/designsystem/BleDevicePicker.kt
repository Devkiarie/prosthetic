package com.ian.myocontrol.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.BleDeviceInfo

/**
 * Bottom sheet that shows found BLE devices during scanning.
 * Appears automatically when scan starts; user taps a device to connect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDevicePicker(
    devices:      List<BleDeviceInfo>,
    isScanning:   Boolean,
    onDismiss:    () -> Unit,
    onDevicePick: (BleDeviceInfo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,  // swipe-down and X both call onDismiss
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {

            // ---- Header row ------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = "Select Device",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            McStatusDot(connected = true) // green pulse = active scan
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text  = "Scanning for devices...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text  = "${devices.size} device${if (devices.size == 1) "" else "s"} found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Cancel scan",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Device list -----------------------------------------------
            if (devices.isEmpty() && isScanning) {
                // Scanning placeholder
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color     = McColors.Coral,
                        modifier  = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text  = "Looking for ESP32-S3...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = "Make sure Bluetooth is on and the device is powered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else if (devices.isEmpty()) {
                // Scan finished — nothing found
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "No devices found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.address }) { device ->
                        DeviceRow(
                            device  = device,
                            onClick = { onDevicePick(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device:  BleDeviceInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bluetooth icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(McColors.CoralContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint               = McColors.Coral,
                modifier           = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = device.name,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // RSSI signal strength
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = Icons.Filled.SignalCellularAlt,
                contentDescription = "Signal strength",
                tint               = rssiColor(device.rssi),
                modifier           = Modifier.size(18.dp)
            )
            Text(
                text  = "${device.rssi} dBm",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun rssiColor(rssi: Int) = when {
    rssi >= -60 -> McColors.Success
    rssi >= -75 -> McColors.Warning
    else        -> McColors.Error
}
