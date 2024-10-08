package com.specfive.app.repository.location

import android.annotation.SuppressLint
import android.app.Application
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.specfive.app.android.GeeksvilleApplication
import com.specfive.app.android.Logging
import com.specfive.app.android.hasBackgroundPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val context: Application,
    private val locationManager: dagger.Lazy<LocationManager>,
) : Logging {

    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    private val _receivingLocationUpdates: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val receivingLocationUpdates: StateFlow<Boolean> get() = _receivingLocationUpdates

    @SuppressLint("MissingPermission")
    private fun LocationManager.requestLocationUpdates() = callbackFlow {
        if (!context.hasBackgroundPermission()) close()

        val intervalMs = 30 * 1000L // 30 seconds
        val minDistanceM = 0f

        val locationRequest = LocationRequestCompat.Builder(intervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()

        val locationListener = LocationListenerCompat { location ->
            // info("New location: $location")
            trySend(location)
        }

        val providerList = buildList {
            val providers = allProviders
            if (android.os.Build.VERSION.SDK_INT >= 31 && LocationManager.FUSED_PROVIDER in providers) {
                add(LocationManager.FUSED_PROVIDER)
            } else {
                if (LocationManager.GPS_PROVIDER in providers) add(LocationManager.GPS_PROVIDER)
                if (LocationManager.NETWORK_PROVIDER in providers) add(LocationManager.NETWORK_PROVIDER)
            }
        }

        info("Starting location updates with $providerList intervalMs=${intervalMs}ms and minDistanceM=${minDistanceM}m")
        _receivingLocationUpdates.value = true
        GeeksvilleApplication.analytics.track("location_start") // Figure out how many users needed to use the phone GPS

        try {
            providerList.forEach { provider ->
                LocationManagerCompat.requestLocationUpdates(
                    this@requestLocationUpdates,
                    provider,
                    locationRequest,
                    Dispatchers.IO.asExecutor(),
                    locationListener,
                )
            }
        } catch (e: Exception) {
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            info("Stopping location requests")
            _receivingLocationUpdates.value = false
            GeeksvilleApplication.analytics.track("location_stop")

            LocationManagerCompat.removeUpdates(this@requestLocationUpdates, locationListener)
        }
    }

    /**
     * Observable flow for location updates
     */
    fun getLocations() = locationManager.get().requestLocationUpdates()
}
