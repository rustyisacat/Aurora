package com.rusty.aurora.battery

/**
 * Reads the phone's current battery state. Backed by [android.os.BatteryManager],
 * which exposes live values directly - no broadcast receiver or cached state
 * to keep in sync, except for the rolling charge-rate samples
 * [getChargingEtaMinutes] needs (BatteryManager has no reliable
 * "time remaining" API of its own to read instead).
 */
interface BatteryRepository {
    fun getBatteryLevelPercent(): Int
    fun isCharging(): Boolean

    /** Minutes until 100%, estimated from recent charge-rate samples - null
     *  while not charging, or until enough history has built up for a
     *  stable estimate (see ChargingEtaCalculator). */
    fun getChargingEtaMinutes(): Int?
}
