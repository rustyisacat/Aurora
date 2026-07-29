package com.rusty.aurora.battery

import kotlin.math.roundToInt

/**
 * Pure estimation logic, decoupled from BatteryManager/System.currentTimeMillis
 * so it's a plain JVM unit test: given a rolling window of (timestampMs, level)
 * samples taken while charging, estimate minutes until 100%.
 *
 * BatteryManager has no reliable cross-version "time remaining" API of its
 * own, so this extrapolates linearly from the phone's own recent charge
 * rate instead - noisy for the first few samples (charging isn't perfectly
 * linear, especially near empty/full), which is why [minElapsedMs] holds
 * back an estimate until there's enough history to smooth that out.
 */
internal object ChargingEtaCalculator {

    private const val MIN_ELAPSED_MS = 3 * 60_000L

    /** [samples] must already be sorted oldest-first and pruned to the
     *  desired window - this makes no assumptions about how much history
     *  is behind it. Null if there isn't enough data yet, the rate is
     *  non-positive (draining or flat despite reportedly charging - e.g.
     *  right after plugging in, or a phone that charges in discrete steps),
     *  or the battery is already full. */
    fun estimateMinutesToFull(samples: List<Pair<Long, Int>>): Int? {
        val oldest = samples.firstOrNull() ?: return null
        val newest = samples.lastOrNull() ?: return null

        val elapsedMs = newest.first - oldest.first
        if (elapsedMs < MIN_ELAPSED_MS) return null

        val remaining = 100 - newest.second
        if (remaining <= 0) return 0

        val levelDelta = newest.second - oldest.second
        if (levelDelta <= 0) return null

        val ratePerMs = levelDelta.toDouble() / elapsedMs
        return (remaining / ratePerMs / 60_000.0).roundToInt()
    }
}
