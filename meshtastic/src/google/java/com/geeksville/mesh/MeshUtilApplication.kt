package com.specfive.app

import android.os.Debug
import com.specfive.app.android.AppPrefs
import com.specfive.app.android.BuildUtils.isEmulator
import com.specfive.app.android.GeeksvilleApplication
import com.specfive.app.android.Logging
import com.specfive.app.util.Exceptions
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeshUtilApplication : GeeksvilleApplication() {

    override fun onCreate() {
        super.onCreate()

        Logging.showLogs = BuildConfig.DEBUG

        // We default to off in the manifest - we turn on here if the user approves
        // leave off when running in the debugger
        if (!isEmulator && (!BuildConfig.DEBUG || !Debug.isDebuggerConnected())) {
            val crashlytics = Firebase.crashlytics
            crashlytics.setCrashlyticsCollectionEnabled(isAnalyticsAllowed)
            crashlytics.setCustomKey("debug_build", BuildConfig.DEBUG)

            val pref = AppPrefs(this)
            crashlytics.setUserId(pref.getInstallId()) // be able to group all bugs per anonymous user

            // We always send our log messages to the crashlytics lib, but they only get sent to the server if we report an exception
            // This makes log messages work properly if someone turns on analytics just before they click report bug.
            // send all log messages through crashyltics, so if we do crash we'll have those in the report
            val standardLogger = Logging.printlog
            Logging.printlog = { level, tag, message ->
                crashlytics.log("$tag: $message")
                standardLogger(level, tag, message)
            }

            fun sendCrashReports() {
                if (isAnalyticsAllowed)
                    crashlytics.sendUnsentReports()
            }

            // Send any old reports if user approves
            sendCrashReports()

            // Attach to our exception wrapper
            Exceptions.reporter = { exception, _, _ ->
                crashlytics.recordException(exception)
                sendCrashReports() // Send the new report
            }
        }
    }
}
