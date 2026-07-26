package com.rusty.aurora.battery

/**
 * Reads the phone's current battery state. Backed by [android.os.BatteryManager],
 * which exposes live values directly - no broadcast receiver or cached state
 * to keep in sync.
 */
interface BatteryRepository {
    fun getBatteryLevelPercent(): Int
    fun isCharging(): Boolean
}
