package com.example.socialbaby.di

import android.content.Context
import androidx.room.Room
import com.example.socialbaby.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "kidtube.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMediaDao(db: AppDatabase) = db.mediaItemDao()

    @Provides
    fun provideShelfDao(db: AppDatabase) = db.shelfDao()

    @Provides
    fun provideWatchLogDao(db: AppDatabase) = db.watchLogDao()
}
