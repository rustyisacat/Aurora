package com.rusty.aurora.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCacheTest {

    private val snapshot =
        WeatherSnapshot(temperature = 74, condition = "Clear", high = 86, low = 68, timezone = "America/New_York")

    @Test
    fun `empty cache is stale and has no current value`() {
        val cache = WeatherCache<WeatherSnapshot>(cacheDurationMillis = 1000L) { 0L }

        assertTrue(cache.isStaleOrEmpty)
        assertNull(cache.current)
    }

    @Test
    fun `freshly stored value is not stale and is returned as current`() {
        val now = 0L
        val cache = WeatherCache<WeatherSnapshot>(cacheDurationMillis = 1000L) { now }

        cache.store(snapshot)

        assertEquals(snapshot, cache.current)
        assertFalse(cache.isStaleOrEmpty)
    }

    @Test
    fun `value becomes stale once the cache duration elapses, but is not discarded`() {
        var now = 0L
        val cache = WeatherCache<WeatherSnapshot>(cacheDurationMillis = 1000L) { now }
        cache.store(snapshot)

        now = 1001L

        assertTrue(cache.isStaleOrEmpty)
        // Stale just means "trigger a refresh" - getWeather() still has a
        // last-known-good value to serve while that refresh happens.
        assertEquals(snapshot, cache.current)
    }

    @Test
    fun `a value exactly at the cache boundary is not yet stale`() {
        var now = 0L
        val cache = WeatherCache<WeatherSnapshot>(cacheDurationMillis = 1000L) { now }
        cache.store(snapshot)

        now = 1000L

        assertFalse(cache.isStaleOrEmpty)
    }

    @Test
    fun `not calling store leaves the previous value in place - the failed-refresh path`() {
        var now = 0L
        val cache = WeatherCache<WeatherSnapshot>(cacheDurationMillis = 1000L) { now }
        cache.store(snapshot)

        // Simulate a failed refresh: time passes, but nothing calls store().
        now = 5000L

        assertTrue(cache.isStaleOrEmpty)
        assertEquals(snapshot, cache.current)
    }
}
