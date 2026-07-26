package com.rusty.aurora.weather

/**
 * The 15-minute cache/staleness policy, isolated from network and Android
 * dependencies so it's a plain JVM unit test: inject a fake clock, verify
 * staleness transitions and that a failed refresh (which simply never calls
 * [store]) leaves the previous snapshot in place.
 */
internal class WeatherCache(
    private val cacheDurationMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Entry(val snapshot: WeatherSnapshot, val fetchedAtMillis: Long)

    @Volatile
    private var entry: Entry? = null

    val current: WeatherSnapshot?
        get() = entry?.snapshot

    val isStaleOrEmpty: Boolean
        get() {
            val current = entry ?: return true
            return clock() - current.fetchedAtMillis > cacheDurationMillis
        }

    fun store(snapshot: WeatherSnapshot) {
        entry = Entry(snapshot, clock())
    }
}
