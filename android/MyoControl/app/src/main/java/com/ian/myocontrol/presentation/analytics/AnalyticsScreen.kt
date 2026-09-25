package com.ian.myocontrol.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.myocontrol.core.theme.McColors

@Composable
fun AnalyticsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ANALYTICS",
            style = MaterialTheme.typography.headlineLarge,
            color = McColors.Accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Accuracy, F1, confusion matrix coming in Phase 5",
            style = MaterialTheme.typography.bodyMedium,
            color = McColors.TextSecondary
        )
    }
}
