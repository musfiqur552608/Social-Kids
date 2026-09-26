package com.freedu.socialbaby.di

import com.freedu.socialbaby.data.repository.MediaRepositoryImpl
import com.freedu.socialbaby.data.repository.ShelfRepositoryImpl
import com.freedu.socialbaby.domain.repository.MediaRepository
import com.freedu.socialbaby.domain.repository.ShelfRepository
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
