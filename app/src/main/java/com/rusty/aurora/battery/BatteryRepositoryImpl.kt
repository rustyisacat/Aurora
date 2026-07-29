package com.rusty.aurora.battery

import android.content.Context
import android.os.BatteryManager

/** [samples] is touched from both the HTTP server's request thread
 *  (DashboardRoute) and AuroraViewModel's own poll loop - @Synchronized on
 *  every access keeps that safe without pulling in a whole
 *  java.util.concurrent collection for what's a handful of entries. */
class BatteryRepositoryImpl(context: Context) : BatteryRepository {

    private val batteryManager =
        context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val samples = mutableListOf<Pair<Long, Int>>()

    override fun getBatteryLevelPercent(): Int {
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        recordSample(level)
        return level
    }

    override fun isCharging(): Boolean = batteryManager.isCharging

    override fun getChargingEtaMinutes(): Int? {
        if (!isCharging()) return null
        return ChargingEtaCalculator.estimateMinutesToFull(snapshotSamples())
    }

    @Synchronized
    private fun recordSample(level: Int) {
        if (!isCharging()) {
            samples.clear()
            return
        }
        val now = System.currentTimeMillis()
        samples.add(now to level)
        val cutoff = now - SAMPLE_WINDOW_MS
        samples.removeAll { it.first < cutoff }
    }

    @Synchronized
    private fun snapshotSamples(): List<Pair<Long, Int>> = samples.toList()

    private companion object {
        const val SAMPLE_WINDOW_MS = 30 * 60_000L
    }
}
