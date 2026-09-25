package com.ian.myocontrol.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ian.myocontrol.data.local.entity.GestureSessionEntity

@Database(
    entities = [GestureSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MyoDatabase : RoomDatabase() {
    // DAOs will be added in Phase 4 (calibration) and Phase 5 (analytics)
}
