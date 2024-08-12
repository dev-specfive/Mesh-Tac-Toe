package com.specfive.app

import com.specfive.app.android.GeeksvilleApplication
import com.specfive.app.android.Logging

open class MeshUtilApplication : GeeksvilleApplication() {

    override fun onCreate() {
        super.onCreate()

        Logging.showLogs = BuildConfig.DEBUG

    }
}