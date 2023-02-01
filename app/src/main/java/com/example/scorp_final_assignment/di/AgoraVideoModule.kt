package com.example.scorp_final_assignment.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AgoraVideoModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Application {
        return application
    }

}