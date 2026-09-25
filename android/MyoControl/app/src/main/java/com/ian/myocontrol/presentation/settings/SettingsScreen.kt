package com.ian.myocontrol.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.myocontrol.core.theme.AppTheme
import com.ian.myocontrol.core.theme.McColors

/**
 * Settings screen — Light / Dark / System theme selector.
 * Accessed via gear icon on HomeScreen.
 */
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onSetTheme:   (AppTheme) -> Unit,
    onBack:       () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ---- Top bar --------------------------------------------------------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text       = "Settings",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ---- Section label --------------------------------------------------
        Text(
            text          = "APPEARANCE",
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Theme options --------------------------------------------------
        ThemeOptionCard(
            title       = "Light",
            subtitle    = "Warm cream — default",
            selected    = currentTheme == AppTheme.LIGHT,
            previewBg   = McColors.LightBg,
            previewCard = McColors.LightSurface,
            onClick     = { onSetTheme(AppTheme.LIGHT) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ThemeOptionCard(
            title       = "Dark",
            subtitle    = "ForceMultiplier absolute black",
            selected    = currentTheme == AppTheme.DARK,
            previewBg   = McColors.DarkBg,
            previewCard = McColors.DarkSurface,
            onClick     = { onSetTheme(AppTheme.DARK) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ThemeOptionCard(
            title       = "System default",
            subtitle    = "Follows your device setting",
            selected    = currentTheme == AppTheme.SYSTEM,
            previewBg   = McColors.LightSurface2,
            previewCard = McColors.DarkSurface2,
            onClick     = { onSetTheme(AppTheme.SYSTEM) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- About section --------------------------------------------------
        Text(
            text          = "ABOUT",
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("App",       "MyoControl")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Purpose",   "sEMG Prosthetic Control")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Device",    "ESP32-S3 via BLE")
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 8.dp))
                InfoRow("Project",   "FYP — JKUAT ENE212-0069/2022")
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title:       String,
    subtitle:    String,
    selected:    Boolean,
    previewBg:   Color,
    previewCard: Color,
    onClick:     () -> Unit
) {
    val borderColor = if (selected) McColors.Coral else MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini theme preview
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewBg)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(previewCard)
            )
            // Mini coral dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.BottomEnd)
                    .offset((-4).dp, (-4).dp)
                    .clip(CircleShape)
                    .background(McColors.Coral)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selected) {
            Icon(
                imageVector        = Icons.Filled.Check,
                contentDescription = "Selected",
                tint               = McColors.Coral,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface
        )
    }
}
