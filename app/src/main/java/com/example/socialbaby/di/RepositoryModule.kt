package com.example.socialbaby.di

import com.example.socialbaby.data.repository.MediaRepositoryImpl
import com.example.socialbaby.data.repository.ShelfRepositoryImpl
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.repository.ShelfRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindShelfRepository(impl: ShelfRepositoryImpl): ShelfRepository
}
