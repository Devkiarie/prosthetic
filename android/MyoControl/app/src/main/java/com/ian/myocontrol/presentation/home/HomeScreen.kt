package com.ian.myocontrol.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ian.myocontrol.core.designsystem.*
import com.ian.myocontrol.core.theme.McColors
import com.ian.myocontrol.domain.model.BleConnectionState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val isConnected = uiState.connectionState is BleConnectionState.Connected
    val isScanning  = uiState.connectionState is BleConnectionState.Scanning ||
                      uiState.connectionState is BleConnectionState.Connecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Top bar -------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MYOCONTROL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = McColors.Accent,
                    letterSpacing = 2.sp
                )
                if (uiState.isFakeMode) {
                    Text(
                        text = "demo mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = McColors.TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = McColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Device card ---------------------------------------------------
        McCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    McStatusDot(connected = isConnected)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (val s = uiState.connectionState) {
                                is BleConnectionState.Connected -> s.deviceName
                                else -> "ESP32-S3"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = McColors.TextPrimary
                        )
                        Text(
                            text = when (uiState.connectionState) {
                                is BleConnectionState.Connected    -> "Connected"
                                is BleConnectionState.Scanning     -> "Scanning..."
                                is BleConnectionState.Connecting   -> "Connecting..."
                                is BleConnectionState.Disconnected -> "Not connected"
                                is BleConnectionState.Error        -> "Error"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) McColors.Success else McColors.TextSecondary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.onConnectClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) McColors.SurfaceVariant
                                         else McColors.AccentContainer,
                        contentColor   = if (isConnected) McColors.TextSecondary
                                         else McColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.BluetoothDisabled
                                      else Icons.Filled.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isConnected -> "Disconnect"
                            isScanning  -> "Scanning..."
                            else        -> "Connect"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Gesture display card ------------------------------------------
        GestureDisplayCard(
            gestureResult = uiState.currentGesture,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Today's stats row ---------------------------------------------
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = McColors.TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            McStatCard(
                value = "${"%.1f".format(uiState.todayAccuracy)}%",
                label = "Accuracy",
                modifier = Modifier.weight(1f),
                valueColor = when {
                    uiState.todayAccuracy >= 90f -> McColors.Success
                    uiState.todayAccuracy >= 75f -> McColors.Accent
                    else -> McColors.Warning
                }
            )
            McStatCard(
                value = "${uiState.todayGestureCount}",
                label = "Gestures",
                modifier = Modifier.weight(1f),
                valueColor = McColors.TextPrimary
            )
            McStatCard(
                value = "${uiState.currentGesture?.latencyMs ?: 0} ms",
                label = "Latency",
                modifier = Modifier.weight(1f),
                valueColor = McColors.TextSecondary
            )
        }

        // ---- Signal quality (only when metrics available) ------------------
        AnimatedVisibility(
            visible = uiState.signalMetrics != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.signalMetrics?.let { metrics ->
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SIGNAL QUALITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = McColors.TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    McCard(modifier = Modifier.fillMaxWidth()) {
                        McChannelBar(
                            label = "CH1",
                            rms = metrics.ch1Rms,
                            channelColor = McColors.Ch1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        McChannelBar(
                            label = "CH2",
                            rms = metrics.ch2Rms,
                            channelColor = McColors.Ch2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        McChannelBar(
                            label = "CH3",
                            rms = metrics.ch3Rms,
                            channelColor = McColors.Ch3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        McChannelBar(
                            label = "CH4",
                            rms = metrics.ch4Rms,
                            channelColor = McColors.Ch4
                        )
                    }
                }
            }
        }

        // ---- Emergency stop (only when connected) --------------------------
        AnimatedVisibility(
            visible = isConnected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.onEmergencyStop() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = McColors.Error,
                        contentColor   = McColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EMERGENCY STOP",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
