package com.spark.spec5main

import com.geeksville.mesh.MeshUtilApplication
import com.geeksville.mesh.analytics.FirebaseAnalytics
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class App : MeshUtilApplication() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
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