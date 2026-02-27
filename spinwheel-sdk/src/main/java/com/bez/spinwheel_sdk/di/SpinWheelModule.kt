package com.bez.spinwheel_sdk.di

import com.bez.spinwheel_sdk.data.mock.MockConfigRepository
import com.bez.spinwheel_sdk.domain.repository.ConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpinWheelModule {

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: MockConfigRepository): ConfigRepository
}
