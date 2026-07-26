package com.rusty.aurora.alarm

import android.app.AlarmManager
import android.content.Context

class AlarmRepositoryImpl(context: Context) : AlarmRepository {

    private val alarmManager =
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun getNextAlarm(): NextAlarm? =
        AlarmMapper.toNextAlarm(alarmManager.nextAlarmClock?.triggerTime)
}
