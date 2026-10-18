package com.ian.myocontrol.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.myocontrol.data.local.dao.GestureSessionDao
import com.ian.myocontrol.domain.model.GestureLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val totalPredictions: Int = 0,
    val averageConfidence: Float = 0f,
    val averageLatencyMs: Float = 0f,
    /** per-gesture prediction count: index = gesture class */
    val gestureCounts: List<Int> = List(8) { 0 },
    val isLoading: Boolean = true,
    val hasData: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val dao: GestureSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        collectStats()
    }

    private fun collectStats() {
        viewModelScope.launch {
            combine(
                dao.getCountPerGesture(),
                dao.getAverageConfidence(),
                dao.getAverageLatency(),
                dao.getRecentSessions(500)
            ) { counts, avgConf, avgLat, sessions ->
                val countMap = counts.associate { it.gestureClass to it.count }
                val perGesture = List(8) { i -> countMap[i] ?: 0 }
                val total = perGesture.sum()
                AnalyticsUiState(
                    totalPredictions  = total,
                    averageConfidence = avgConf ?: 0f,
                    averageLatencyMs  = avgLat ?: 0f,
                    gestureCounts     = perGesture,
                    isLoading         = false,
                    hasData           = total > 0
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun clearData() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}
