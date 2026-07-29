package com.rusty.aurora.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingEtaCalculatorTest {

    @Test
    fun `estimates minutes to full from a steady charge rate`() {
        // 50% -> 60% over 10 minutes = 1%/min, 40% remaining -> 40 minutes.
        val samples = listOf(0L to 50, 10 * 60_000L to 60)

        assertEquals(40, ChargingEtaCalculator.estimateMinutesToFull(samples))
    }

    @Test
    fun `returns null before enough time has elapsed to trust the rate`() {
        // Only 1 minute of history - too noisy to extrapolate from.
        val samples = listOf(0L to 50, 60_000L to 51)

        assertNull(ChargingEtaCalculator.estimateMinutesToFull(samples))
    }

    @Test
    fun `returns zero when already at 100`() {
        val samples = listOf(0L to 100, 5 * 60_000L to 100)

        assertEquals(0, ChargingEtaCalculator.estimateMinutesToFull(samples))
    }

    @Test
    fun `returns null when the level hasn't actually risen despite the window`() {
        // Reported as charging but flat/draining - don't extrapolate garbage.
        val samples = listOf(0L to 50, 10 * 60_000L to 50)

        assertNull(ChargingEtaCalculator.estimateMinutesToFull(samples))
    }

    @Test
    fun `returns null with fewer than two samples`() {
        assertNull(ChargingEtaCalculator.estimateMinutesToFull(emptyList()))
        assertNull(ChargingEtaCalculator.estimateMinutesToFull(listOf(0L to 50)))
    }
}
