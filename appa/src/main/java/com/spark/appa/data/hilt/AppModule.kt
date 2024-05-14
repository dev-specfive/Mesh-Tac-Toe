package com.spark.appa.data.hilt

import android.app.Application
import android.content.Context
import com.spark.appa.data.prefs.PreferencesHelper
import com.spark.appa.data.prefs.PreferencesHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Singleton
    @Provides
    fun providePreferenceHelper(preferencesHelperImpl: PreferencesHelperImpl): PreferencesHelper {
        return preferencesHelperImpl
    }


    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application
    }

    @Provides
    fun providePreferenceName(): String {
        return "Spec5_TickTacToe"
    }

}