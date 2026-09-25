package com.ian.myocontrol.presentation.home

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
import kotlin.random.Random

data class HomeUiState(
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val currentGesture: GestureResult? = null,
    val signalMetrics: SignalMetrics? = null,
    val todayGestureCount: Int = 0,
    val todayAccuracy: Float = 0f,
    val isFakeMode: Boolean = true  // Phase 2: fake until real ESP32 connected
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fakeEmitterJob: Job? = null
    private var gestureCount = 0
    private var correctCount = 0

    init {
        observeBleState()
        startFakeEmitter()
    }

    // ---- BLE state ----------------------------------------------------------

    private fun observeBleState() {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }

                when (state) {
                    is BleConnectionState.Connected -> {
                        // Real device connected -- stop fake, use real data
                        stopFakeEmitter()
                        _uiState.update { it.copy(isFakeMode = false) }
                        observeRealData()
                    }
                    is BleConnectionState.Disconnected -> {
                        // Fall back to fake mode so UI is never blank
                        startFakeEmitter()
                        _uiState.update { it.copy(isFakeMode = true) }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun observeRealData() {
        viewModelScope.launch {
            bleManager.gestureResult.filterNotNull().collect { result ->
                gestureCount++
                if (result.confidence >= 0.80f) correctCount++
                _uiState.update {
                    it.copy(
                        currentGesture = result,
                        todayGestureCount = gestureCount,
                        todayAccuracy = if (gestureCount > 0)
                            correctCount.toFloat() / gestureCount * 100f else 0f
                    )
                }
            }
        }
        viewModelScope.launch {
            bleManager.signalMetrics.filterNotNull().collect { metrics ->
                _uiState.update { it.copy(signalMetrics = metrics) }
            }
        }
    }

    // ---- Fake classifier (Phase 2) ------------------------------------------

    private fun startFakeEmitter() {
        if (fakeEmitterJob?.isActive == true) return
        fakeEmitterJob = viewModelScope.launch {
            val gestures = GestureLabel.entries
            var localCount = 0
            var localCorrect = 0
            while (true) {
                delay(1_800L)
                val gesture = gestures.random()
                val confidence = Random.nextFloat() * 0.25f + 0.72f  // 72-97%
                val latency = Random.nextInt(28, 55)
                val result = GestureResult(
                    gestureClass = gesture.ordinal,
                    confidence   = confidence,
                    latencyMs    = latency
                )
                localCount++
                if (confidence >= 0.80f) localCorrect++
                val metrics = SignalMetrics(
                    ch1Rms = Random.nextFloat() * 0.4f + 0.5f,
                    ch2Rms = Random.nextFloat() * 0.4f + 0.5f,
                    ch3Rms = Random.nextFloat() * 0.4f + 0.5f,
                    ch4Rms = Random.nextFloat() * 0.4f + 0.5f,
                )
                _uiState.update {
                    it.copy(
                        currentGesture    = result,
                        signalMetrics     = metrics,
                        todayGestureCount = localCount,
                        todayAccuracy     = if (localCount > 0)
                            localCorrect.toFloat() / localCount * 100f else 0f
                    )
                }
            }
        }
    }

    private fun stopFakeEmitter() {
        fakeEmitterJob?.cancel()
        fakeEmitterJob = null
    }

    // ---- Actions ------------------------------------------------------------

    fun onConnectClick() {
        when (_uiState.value.connectionState) {
            is BleConnectionState.Disconnected -> bleManager.startScan()
            is BleConnectionState.Connected    -> bleManager.disconnect()
            else -> Unit
        }
    }

    fun onEmergencyStop() {
        bleManager.sendEmergencyStop()
    }
}
