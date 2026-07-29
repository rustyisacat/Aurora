package com.rusty.aurora.alarm

import android.app.AlarmManager
import android.content.Context
import com.rusty.aurora.wakealarm.WakeAlarmRepository

class AlarmRepositoryImpl(
    context: Context,
    private val wakeAlarmRepository: WakeAlarmRepository
) : AlarmRepository {

    private val alarmManager =
        context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun getNextAlarm(): NextAlarm? = AlarmMapper.toNextAlarm(getNextAlarmTriggerMillis())

    override fun getNextAlarmTriggerMillis(): Long? =
        listOfNotNull(
            alarmManager.nextAlarmClock?.triggerTime,
            wakeAlarmRepository.getEarliestEnabledTriggerMillis()
        ).minOrNull()
}
