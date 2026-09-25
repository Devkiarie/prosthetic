package com.ian.myocontrol.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.myocontrol.core.theme.McColors

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MYOCONTROL",
            style = MaterialTheme.typography.headlineLarge,
            color = McColors.Accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Device dashboard coming in Phase 2",
            style = MaterialTheme.typography.bodyMedium,
            color = McColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Placeholder device status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = McColors.CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ESP32-S3",
                    style = MaterialTheme.typography.titleMedium,
                    color = McColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Not connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = McColors.TextSecondary
                )
            }
        }
    }
}
