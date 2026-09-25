package com.ian.myocontrol.presentation.train

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ian.myocontrol.core.theme.McColors

@Composable
fun TrainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TRAIN",
            style = MaterialTheme.typography.headlineLarge,
            color = McColors.Accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Calibration + gesture practice coming in Phase 4",
            style = MaterialTheme.typography.bodyMedium,
            color = McColors.TextSecondary
        )
    }
}
