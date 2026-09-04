package com.example.socialbaby.di

import com.example.socialbaby.data.local.datastore.ParentAuthStore
import com.example.socialbaby.data.local.datastore.SettingsStore
import com.example.socialbaby.data.repository.ParentAuthRepositoryImpl
import com.example.socialbaby.data.repository.SettingsRepositoryImpl
import com.example.socialbaby.domain.repository.ParentAuthRepository
import com.example.socialbaby.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindParentAuthRepository(impl: ParentAuthRepositoryImpl): ParentAuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
