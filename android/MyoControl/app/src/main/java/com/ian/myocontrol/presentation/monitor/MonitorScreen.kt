package com.ian.myocontrol.presentation.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.myocontrol.core.theme.McColors

@Composable
fun MonitorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MONITOR",
            style = MaterialTheme.typography.headlineLarge,
            color = McColors.Accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Live sEMG waveform (4 channels) coming in Phase 3",
            style = MaterialTheme.typography.bodyMedium,
            color = McColors.TextSecondary
        )
    }
}
