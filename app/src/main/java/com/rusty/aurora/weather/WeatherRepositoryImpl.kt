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
 * A singleton in [com.rusty.aurora.di.AppContainer] with no natural
 * ViewModel-scoped lifecycle to borrow, so it owns its own background
 * scope for refreshes - same reasoning as why AuroraServerController owns
 * its own lifecycle rather than depending on one.
 *
 * Resolves the phone's current location on every refresh rather than once
 * at construction - a new [OpenMeteoClient] is built per refresh with
 * whatever coordinate [LocationRepository] currently reports, falling back
 * to [WeatherConfig]'s fixed coordinate if location isn't available (no
 * permission, or no fix ever obtained).
 */
class WeatherRepositoryImpl(
    context: Context,
    private val locationRepository: LocationRepository
) : WeatherRepository {

    private val networkStatusProvider = NetworkStatusProvider(context)
    private val cache = WeatherCache<WeatherSnapshot>(WeatherConfig.CACHE_DURATION_MILLIS)
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
                    val location = locationRepository.getLastKnownLocation()
                    val latitude = location?.latitude ?: WeatherConfig.LATITUDE
                    val longitude = location?.longitude ?: WeatherConfig.LONGITUDE
                    val snapshot = OpenMeteoClient(latitude, longitude).fetchCurrentWeather()
                    val radarStation = fetchRadarStation(latitude, longitude)
                    val airQualityIndex = fetchAirQualityIndex(latitude, longitude)
                    cache.store(snapshot.copy(radarStation = radarStation, airQualityIndex = airQualityIndex))
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

    /** Own try/catch, separate from the outer one: a failed radar station
     *  lookup shouldn't discard an otherwise-successful weather fetch. Falls
     *  back to whatever radar station was already cached (it essentially
     *  never changes for a fixed-ish location) rather than blanking out a
     *  working radar image over one transient NWS hiccup. */
    private fun fetchRadarStation(latitude: Double, longitude: Double): String? =
        try {
            NwsPointsClient(latitude, longitude).fetchRadarStation()
        } catch (e: IOException) {
            Log.w(TAG, "Radar station lookup failed, keeping previous value", e)
            cache.current?.radarStation
        } catch (e: SerializationException) {
            Log.w(TAG, "Radar station response could not be parsed, keeping previous value", e)
            cache.current?.radarStation
        }

    /** Same own-try/catch, fall-back-to-cached reasoning as
     *  [fetchRadarStation] - a failed air quality lookup shouldn't discard
     *  an otherwise-successful weather fetch or blank out a working AQI
     *  reading over one transient hiccup. */
    private fun fetchAirQualityIndex(latitude: Double, longitude: Double): Int? =
        try {
            OpenMeteoAirQualityClient(latitude, longitude).fetchUsAqi()
        } catch (e: IOException) {
            Log.w(TAG, "Air quality refresh failed, keeping previous value", e)
            cache.current?.airQualityIndex
        } catch (e: SerializationException) {
            Log.w(TAG, "Air quality response could not be parsed, keeping previous value", e)
            cache.current?.airQualityIndex
        }

    private companion object {
        const val TAG = "WeatherRepository"
    }
}
