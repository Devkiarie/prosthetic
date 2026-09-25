package com.ian.myocontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a completed gesture recognition session summary.
 * Populated after Phase 5 (real classifier) is integrated.
 */
@Entity(tableName = "gesture_sessions")
data class GestureSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val gestureClass: Int,
    val confidence: Float,
    val latencyMs: Int
)
