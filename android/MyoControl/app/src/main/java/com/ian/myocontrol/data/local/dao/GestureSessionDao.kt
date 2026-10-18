package com.ian.myocontrol.data.local.dao

import androidx.room.*
import com.ian.myocontrol.data.local.entity.GestureSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureSessionDao {

    @Insert
    suspend fun insert(session: GestureSessionEntity)

    /** All sessions ordered newest-first */
    @Query("SELECT * FROM gesture_sessions ORDER BY timestampMs DESC")
    fun getAllSessions(): Flow<List<GestureSessionEntity>>

    /** Last N sessions */
    @Query("SELECT * FROM gesture_sessions ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 100): Flow<List<GestureSessionEntity>>

    /** Count total predictions per gesture class */
    @Query("SELECT gestureClass, COUNT(*) as count FROM gesture_sessions GROUP BY gestureClass")
    fun getCountPerGesture(): Flow<List<GestureCount>>

    /** Average confidence across all sessions */
    @Query("SELECT AVG(confidence) FROM gesture_sessions")
    fun getAverageConfidence(): Flow<Float?>

    /** Average latency */
    @Query("SELECT AVG(latencyMs) FROM gesture_sessions")
    fun getAverageLatency(): Flow<Float?>

    /** Delete all — for reset */
    @Query("DELETE FROM gesture_sessions")
    suspend fun deleteAll()
}

data class GestureCount(
    val gestureClass: Int,
    val count: Int
)
