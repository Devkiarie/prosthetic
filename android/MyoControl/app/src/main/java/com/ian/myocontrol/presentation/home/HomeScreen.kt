package com.ian.myocontrol.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.myocontrol.core.ble.rememberBlePermissionRequest
import com.ian.myocontrol.core.designsystem.*
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.BleConnectionState

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val isConnected = uiState.connectionState is BleConnectionState.Connected
    val isScanning  = uiState.connectionState is BleConnectionState.Scanning ||
                      uiState.connectionState is BleConnectionState.Connecting

    // Runtime BLE permissions — request on Connect press, call ViewModel only when granted
    val requestBlePermissions = rememberBlePermissionRequest { viewModel.onConnectClick() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Decorative background blobs (bottom of screen) ────────────────────
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 40.dp)
                .clip(CircleShape)
                .background(McColors.DecorPeach.copy(alpha = 0.45f))
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = 30.dp)
                .clip(CircleShape)
                .background(McColors.Coral.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 20.dp)
                .clip(CircleShape)
                .background(McColors.DecorBeige.copy(alpha = 0.40f))
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = 50.dp)
                .clip(CircleShape)
                .background(McColors.DecorSage.copy(alpha = 0.30f))
        )

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ---- Top bar -----------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text          = "MYOCONTROL",
                        fontSize      = 20.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 2.sp
                    )
                    if (uiState.isFakeMode) {
                        Text(
                            text  = "demo mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector        = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---- Device status card ------------------------------------
            McCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        McStatusDot(connected = isConnected)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text  = when (val s = uiState.connectionState) {
                                    is BleConnectionState.Connected -> s.deviceName
                                    else -> "ESP32-S3"
                                },
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text  = when (uiState.connectionState) {
                                    is BleConnectionState.Connected    -> "Connected"
                                    is BleConnectionState.Scanning     -> "Scanning..."
                                    is BleConnectionState.Connecting   -> "Connecting..."
                                    is BleConnectionState.Disconnected -> "Not connected"
                                    is BleConnectionState.Error        -> "Error"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isConnected) McColors.Success
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connect / Disconnect button
                    OutlinedButton(
                        onClick = {
                            if (isConnected) viewModel.onConnectClick()
                            else requestBlePermissions()
                        },
                        shape  = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isConnected) MaterialTheme.colorScheme.onSurfaceVariant
                                           else McColors.Coral
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector    = if (isConnected) Icons.Filled.BluetoothDisabled
                                             else Icons.Filled.Bluetooth,
                            contentDescription = null,
                            modifier       = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = when {
                                isConnected -> "Disconnect"
                                isScanning  -> "Scanning..."
                                else        -> "Connect"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ---- Hero gesture card ------------------------------------
            GestureDisplayCard(
                gestureResult = uiState.currentGesture,
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ---- Today's stats row ------------------------------------
            Text(
                text          = "TODAY",
                style         = MaterialTheme.typography.labelMedium,
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                McStatCard(
                    value      = "${"%.1f".format(uiState.todayAccuracy)}%",
                    label      = "Accuracy",
                    modifier   = Modifier.weight(1f),
                    valueColor = when {
                        uiState.todayAccuracy >= 90f -> McColors.Success
                        uiState.todayAccuracy >= 75f -> McColors.Coral
                        else -> McColors.Warning
                    }
                )
                McStatCard(
                    value      = "${uiState.todayGestureCount}",
                    label      = "Gestures",
                    modifier   = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                McStatCard(
                    value      = "${uiState.currentGesture?.latencyMs ?: 0} ms",
                    label      = "Latency",
                    modifier   = Modifier.weight(1f),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Signal quality (when metrics available) ---------------
            AnimatedVisibility(
                visible = uiState.signalMetrics != null,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                uiState.signalMetrics?.let { metrics ->
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text          = "SIGNAL QUALITY",
                            style         = MaterialTheme.typography.labelMedium,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        McCard(modifier = Modifier.fillMaxWidth()) {
                            McChannelBar("CH1", metrics.ch1Rms, McColors.Ch1)
                            Spacer(modifier = Modifier.height(10.dp))
                            McChannelBar("CH2", metrics.ch2Rms, McColors.Ch2)
                            Spacer(modifier = Modifier.height(10.dp))
                            McChannelBar("CH3", metrics.ch3Rms, McColors.Ch3)
                            Spacer(modifier = Modifier.height(10.dp))
                            McChannelBar("CH4", metrics.ch4Rms, McColors.Ch4)
                        }
                    }
                }
            }

            // ---- Emergency stop (only when connected) ------------------
            AnimatedVisibility(
                visible = isConnected,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.onEmergencyStop() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = McColors.Error,
                            contentColor   = McColors.DarkOnSurface
                        ),
                        shape = RoundedCornerShape(50)   // pill
                    ) {
                        Icon(
                            imageVector    = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier       = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text       = "EMERGENCY STOP",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
