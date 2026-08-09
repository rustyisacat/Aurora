package com.rusty.aurora.weather

/**
 * A staleness/store cache generic over its stored value, so both
 * [WeatherRepositoryImpl] and [WeatherAlertRepositoryImpl] can share this
 * same policy with different cache durations instead of duplicating it.
 * Isolated from network and Android dependencies so it's a plain JVM unit
 * test: inject a fake clock, verify staleness transitions and that a failed
 * refresh (which simply never calls [store]) leaves the previous value in
 * place.
 */
internal class WeatherCache<T>(
    private val cacheDurationMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Entry<T>(val value: T, val fetchedAtMillis: Long)

    @Volatile
    private var entry: Entry<T>? = null

    val current: T?
        get() = entry?.value

    val isStaleOrEmpty: Boolean
        get() {
            val current = entry ?: return true
            return clock() - current.fetchedAtMillis > cacheDurationMillis
        }

    fun store(value: T) {
        entry = Entry(value, clock())
    }
}
