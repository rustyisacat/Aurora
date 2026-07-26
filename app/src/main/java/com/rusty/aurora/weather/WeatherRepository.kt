package com.rusty.aurora.weather

/**
 * Live + cached weather for Aurora's fixed home location (see [WeatherConfig]).
 *
 * [getWeather] never blocks on network: it returns whatever is cached right
 * now - possibly null on first run, possibly stale - and triggers an
 * asynchronous refresh if the cache is missing or older than the cache
 * window. A failed refresh (offline, timeout, bad response) never clears an
 * existing cache; only a location that has never fetched successfully
 * reports null.
 */
interface WeatherRepository {
    fun getWeather(): WeatherSnapshot?
}
