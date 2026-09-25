package com.ian.myocontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.ian.myocontrol.core.theme.AppTheme
import com.ian.myocontrol.core.theme.MyoControlTheme
import com.ian.myocontrol.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var appTheme by remember { mutableStateOf(AppTheme.LIGHT) }
            MyoControlTheme(appTheme = appTheme) {
                AppNavHost(
                    appTheme   = appTheme,
                    onSetTheme = { appTheme = it }
                )
            }
        }
    }
}
