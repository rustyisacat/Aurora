package com.rusty.aurora.battery

import android.content.Context
import android.os.BatteryManager

class BatteryRepositoryImpl(context: Context) : BatteryRepository {

    private val batteryManager =
        context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    override fun getBatteryLevelPercent(): Int =
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    override fun isCharging(): Boolean =
        batteryManager.isCharging
}
