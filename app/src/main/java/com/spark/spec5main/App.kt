package com.spark.spec5main

import com.geeksville.mesh.MeshUtilApplication
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class App : MeshUtilApplication() {

    override fun onCreate() {
        super.onCreate()
        setupData()
    }

    private fun setupData() {
        com.geeksville.mesh.util.Constants.apply {
            VERSION_CODE = 30221
            VERSION_NAME = "2.2.21"
            ApplicationID = "com.geeksville.mesh"
        }
    }
}