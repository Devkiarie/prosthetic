package com.ian.myocontrol.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.myocontrol.core.ble.BleManager
import com.ian.myocontrol.domain.model.BleConnectionState
import com.ian.myocontrol.domain.model.GestureLabel
import com.ian.myocontrol.domain.model.GestureResult
import com.ian.myocontrol.domain.model.SignalMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin
import kotlin.random.Random

data class MonitorUiState(
    val isConnected: Boolean = false,
    val isFakeMode:  Boolean = true,
    val channelHistory: List<List<Float>> = List(4) { emptyList() },   // 4 ch × N samples
    val signalMetrics:  SignalMetrics? = null,
    val currentGesture: GestureResult? = null,
    val historyMaxSize: Int = 200
)

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var fakeJob: Job? = null
    private var fakeT = 0f

    init {
        observeConnection()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                val connected = state is BleConnectionState.Connected
                _uiState.update { it.copy(isConnected = connected) }
                if (connected) {
                    fakeJob?.cancel()
                    _uiState.update { it.copy(isFakeMode = false) }
                    collectRealSignal()
                } else {
                    startFakeSignal()
                    _uiState.update { it.copy(isFakeMode = true) }
                }
            }
        }
    }

    /** Collect real SignalMetrics from BLE and append to rolling history */
    private fun collectRealSignal() {
        viewModelScope.launch {
            bleManager.signalMetrics.filterNotNull().collect { metrics ->
                val newSample = listOf(metrics.ch1Rms, metrics.ch2Rms,
                                       metrics.ch3Rms, metrics.ch4Rms)
                _uiState.update { state ->
                    val updated = state.channelHistory.mapIndexed { ch, hist ->
                        (hist + newSample[ch]).takeLast(state.historyMaxSize)
                    }
                    state.copy(channelHistory = updated, signalMetrics = metrics)
                }
            }
        }
        viewModelScope.launch {
            bleManager.gestureResult.filterNotNull().collect { result ->
                _uiState.update { it.copy(currentGesture = result) }
            }
        }
    }

    /** Synthetic waveform when no hardware — shows what the real signal will look like */
    private fun startFakeSignal() {
        if (fakeJob?.isActive == true) return
        fakeJob = viewModelScope.launch {
            val rng = Random(0)
            // Each channel gets a different dominant frequency (models forearm muscles)
            val freqs = floatArrayOf(80f, 120f, 95f, 110f)
            val amps  = floatArrayOf(0.6f, 0.5f, 0.7f, 0.55f)
            val gestures = GestureLabel.entries

            while (true) {
                delay(25L)  // ~40 Hz update rate for smooth scroll
                fakeT += 0.025f

                val newSamples = List(4) { ch ->
                    val burst = 0.3f + 0.4f * sin(2 * Math.PI.toFloat() * 0.8f * fakeT).coerceIn(-1f, 1f)
                    val sig = amps[ch] * burst * sin(2 * Math.PI.toFloat() * freqs[ch] * fakeT)
                    sig + rng.nextFloat() * 0.08f - 0.04f
                }
                val fakeMetrics = SignalMetrics(
                    ch1Rms = (newSamples[0] * newSamples[0]).coerceIn(0.1f, 1f),
                    ch2Rms = (newSamples[1] * newSamples[1]).coerceIn(0.1f, 1f),
                    ch3Rms = (newSamples[2] * newSamples[2]).coerceIn(0.1f, 1f),
                    ch4Rms = (newSamples[3] * newSamples[3]).coerceIn(0.1f, 1f),
                )
                _uiState.update { state ->
                    val updated = state.channelHistory.mapIndexed { ch, hist ->
                        (hist + newSamples[ch]).takeLast(state.historyMaxSize)
                    }
                    state.copy(channelHistory = updated, signalMetrics = fakeMetrics)
                }

                // Periodically emit a fake gesture (every ~2s)
                if ((fakeT % 2f) < 0.025f) {
                    val g = gestures.random(rng)
                    _uiState.update {
                        it.copy(currentGesture = GestureResult(
                            gestureClass = g.ordinal,
                            confidence   = rng.nextFloat() * 0.25f + 0.72f,
                            latencyMs    = rng.nextInt(15, 45)
                        ))
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fakeJob?.cancel()
    }
}
