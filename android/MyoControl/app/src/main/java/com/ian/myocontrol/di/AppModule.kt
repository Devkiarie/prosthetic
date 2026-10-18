package com.ian.myocontrol.di

import android.content.Context
import androidx.room.Room
import com.ian.myocontrol.data.local.dao.GestureSessionDao
import com.ian.myocontrol.data.local.database.MyoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMyoDatabase(@ApplicationContext context: Context): MyoDatabase =
        Room.databaseBuilder(
            context,
            MyoDatabase::class.java,
            "myocontrol.db"
        ).build()

    @Provides
    @Singleton
    fun provideGestureSessionDao(db: MyoDatabase): GestureSessionDao =
        db.gestureSessionDao()
}
