package com.example.scorp_final_assignment.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(ViewModelScoped::class)
object AgoraVideoModule {

    @Provides
    @ViewModelScoped
    fun provideContext(application: Application): Application {
        return application
    }

}