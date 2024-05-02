package com.spark.app

import com.spark.app.android.GeeksvilleApplication
import com.spark.app.android.Logging
import dagger.hilt.android.HiltAndroidApp
open class MeshUtilApplication : GeeksvilleApplication() {

    override fun onCreate() {
        super.onCreate()

        Logging.showLogs = BuildConfig.DEBUG

    }
}