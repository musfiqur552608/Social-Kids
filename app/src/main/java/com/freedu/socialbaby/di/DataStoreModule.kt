package com.freedu.socialbaby.di

import com.freedu.socialbaby.data.local.datastore.ParentAuthStore
import com.freedu.socialbaby.data.local.datastore.SettingsStore
import com.freedu.socialbaby.data.repository.ParentAuthRepositoryImpl
import com.freedu.socialbaby.data.repository.SettingsRepositoryImpl
import com.freedu.socialbaby.domain.repository.ParentAuthRepository
import com.freedu.socialbaby.domain.repository.SettingsRepository
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
