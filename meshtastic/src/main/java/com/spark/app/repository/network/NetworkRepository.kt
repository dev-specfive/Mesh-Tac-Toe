package com.spark.app.repository.network

import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import com.spark.app.android.Logging
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    private val nsdManagerLazy: dagger.Lazy<NsdManager?>,
    private val connectivityManager: dagger.Lazy<ConnectivityManager>,
) : Logging {

    val networkAvailable get() = connectivityManager.get().networkAvailable()

    val resolvedList
        get() = nsdManagerLazy.get()?.serviceList(SERVICE_TYPE, SERVICE_NAME) ?: flowOf(emptyList())

    companion object {
        // To find all available services use SERVICE_TYPE = "_services._dns-sd._udp"
        internal const val SERVICE_NAME = "Meshtastic"
        internal const val SERVICE_TYPE = "_https._tcp."
    }
}
