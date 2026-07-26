package com.rusty.aurora.weather

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A singleton in [com.rusty.aurora.di.AppContainer] with no natural
 * ViewModel-scoped lifecycle to borrow, so it owns its own background
 * scope for refreshes - same reasoning as why AuroraServerController owns
 * its own lifecycle rather than depending on one.
 */
class WeatherRepositoryImpl(
    context: Context,
    latitude: Double = WeatherConfig.LATITUDE,
    longitude: Double = WeatherConfig.LONGITUDE
) : WeatherRepository {

    private val networkStatusProvider = NetworkStatusProvider(context)
    private val client = OpenMeteoClient(latitude, longitude)
    private val cache = WeatherCache(WeatherConfig.CACHE_DURATION_MILLIS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Guards against a burst of /dashboard polls each kicking off their own
    // redundant network call while one refresh is already in flight.
    private val isRefreshing = AtomicBoolean(false)

    override fun getWeather(): WeatherSnapshot? {
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
                    cache.store(client.fetchCurrentWeather())
                }
                // Offline: leave the existing cache (possibly still null) untouched.
            } catch (e: IOException) {
                Log.w(TAG, "Weather refresh failed, keeping cached data", e)
            } catch (e: SerializationException) {
                Log.w(TAG, "Weather response could not be parsed, keeping cached data", e)
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    private companion object {
        const val TAG = "WeatherRepository"
    }
}
