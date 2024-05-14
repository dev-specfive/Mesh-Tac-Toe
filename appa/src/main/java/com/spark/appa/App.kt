package com.spark.appa

import android.util.Log
import com.spark.app.MeshUtilApplication
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : MeshUtilApplication() {
    override fun onCreate() {
        super.onCreate()
        initTimber()
    }


    private fun initTimber() {
        //make sure you are using com.packagename.BuildConfig e.g: "com.io.abc.BuildConfig"
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Log.e("TIMBER", "Timber Start")
        } else Log.e("TIMBER", "Timber Stop")

    }
}