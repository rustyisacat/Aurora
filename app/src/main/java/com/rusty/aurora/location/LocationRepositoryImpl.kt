package com.rusty.aurora.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Weather used to be pinned to one hardcoded coordinate (a bedside dashboard
 * doesn't move, so a fixed constant was simpler and more reliable than
 * asking for location). Now that Aurora resolves it automatically, the same
 * reliability goal shapes this class: no async callback on the read path,
 * no request that can time out and block a /dashboard poll.
 *
 * `getLastKnownLocation()` returns synchronously and immediately - a cached
 * fix from this or any other app's prior location requests (Play Services
 * keeps this reasonably fresh from ordinary phone usage: Maps, weather
 * apps, etc.), or the most recent fix this repository itself obtained. It
 * also fires off a best-effort single fresh-location request in the
 * background on every call, which - if it lands before the *next* call -
 * makes the coordinate self-correcting over time without ever blocking.
 */
class LocationRepositoryImpl(private val context: Context) : LocationRepository {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Volatile
    private var freshLocation: LocationSnapshot? = null
    private val fetchInFlight = AtomicBoolean(false)

    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun getLastKnownLocation(): LocationSnapshot? {
        if (!hasLocationPermission()) return null

        requestFreshLocationInBackground()
        return freshLocation ?: bestCachedLocation()
    }

    @SuppressLint("MissingPermission") // hasLocationPermission() already checked above
    private fun requestFreshLocationInBackground() {
        if (!fetchInFlight.compareAndSet(false, true)) return

        val provider = locationManager.getProviders(true).firstOrNull()
        if (provider == null) {
            fetchInFlight.set(false)
            return
        }

        runCatching {
            locationManager.requestSingleUpdate(
                provider,
                LocationListener { location ->
                    freshLocation = LocationSnapshot(location.latitude, location.longitude)
                    fetchInFlight.set(false)
                },
                Looper.getMainLooper()
            )
        }.onFailure { fetchInFlight.set(false) }
    }

    @SuppressLint("MissingPermission") // hasLocationPermission() already checked above
    private fun bestCachedLocation(): LocationSnapshot? =
        locationManager.allProviders
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { LocationSnapshot(it.latitude, it.longitude) }
}
