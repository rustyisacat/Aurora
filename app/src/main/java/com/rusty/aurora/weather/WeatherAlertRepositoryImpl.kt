package com.rusty.aurora.weather

import android.content.Context
import android.util.Log
import com.rusty.aurora.location.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mirrors [WeatherRepositoryImpl]'s own shape - own background scope, own
 * refresh-in-flight guard, cache that survives a failed refresh untouched -
 * just against a shorter cache window (see [WeatherConfig]) and NWS's
 * alerts endpoint instead of Open-Meteo's forecast one.
 */
class WeatherAlertRepositoryImpl(
    context: Context,
    private val locationRepository: LocationRepository
) : WeatherAlertRepository {

    private val networkStatusProvider = NetworkStatusProvider(context)
    private val cache = WeatherCache<WeatherAlert?>(WeatherConfig.ALERT_CACHE_DURATION_MILLIS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val isRefreshing = AtomicBoolean(false)

    override fun getAlert(): WeatherAlert? {
        if (cache.isStaleOrEmpty) {
            refreshIfNotAlreadyRunning()
        }
        return cache.current
    }

    private fun refreshIfNotAlreadyRunning() {
        if (!isRefreshing.compareAndSet(false, true)) return

        scope.launch {
            try {
                if (networkStatusProvider.isConnected()) {
                    val location = locationRepository.getLastKnownLocation()
                    val latitude = location?.latitude ?: WeatherConfig.LATITUDE
                    val longitude = location?.longitude ?: WeatherConfig.LONGITUDE
                    cache.store(NwsAlertClient(latitude, longitude).fetchActiveAlert())
                }
                // Offline: leave the existing cache (possibly still empty) untouched.
            } catch (e: IOException) {
                Log.w(TAG, "Weather alert refresh failed, keeping cached data", e)
            } catch (e: SerializationException) {
                Log.w(TAG, "Weather alert response could not be parsed, keeping cached data", e)
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    private companion object {
        const val TAG = "WeatherAlertRepository"
    }
}
