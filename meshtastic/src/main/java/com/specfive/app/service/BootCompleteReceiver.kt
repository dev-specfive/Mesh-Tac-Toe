package com.specfive.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.specfive.app.android.Logging


class BootCompleteReceiver : BroadcastReceiver(), Logging {
    override fun onReceive(mContext: Context, intent: Intent) {
        // start listening for bluetooth messages from our device
        MeshService.startServiceLater(mContext)
    }
}