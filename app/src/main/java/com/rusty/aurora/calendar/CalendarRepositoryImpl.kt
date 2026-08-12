package com.rusty.aurora.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarRepositoryImpl(private val context: Context) : CalendarRepository {

    override fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    override fun isShowingTomorrow(): Boolean =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= NOON_HOUR

    override fun getEvents(): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val dayOffset = if (isShowingTomorrow()) 1 else 0
        val (startOfDay, endOfDay) = dayRangeMillis(dayOffset)
        return CalendarEventMapper.toSortedEvents(queryInstances(startOfDay, endOfDay))
    }

    override fun getWeekEvents(): List<WeekDay> = (0..6).map { offset ->
        val (startOfDay, endOfDay) = dayRangeMillis(offset)
        val events = if (hasCalendarPermission()) {
            CalendarEventMapper.toSortedEvents(queryInstances(startOfDay, endOfDay))
        } else {
            emptyList()
        }
        WeekDay(date = isoDate(startOfDay), events = events)
    }

    private fun isoDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

    private fun queryInstances(startOfDay: Long, endOfDay: Long): List<RawCalendarInstance> {
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS
        )

        val instances = mutableListOf<RawCalendarInstance>()
        CalendarContract.Instances.query(context.contentResolver, projection, startOfDay, endOfDay)
            ?.use { cursor ->
                val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val statusIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.STATUS)

                while (cursor.moveToNext()) {
                    instances += RawCalendarInstance(
                        title = cursor.getString(titleIndex),
                        beginMillis = cursor.getLong(beginIndex),
                        endMillis = cursor.getLong(endIndex),
                        allDay = cursor.getInt(allDayIndex) != 0,
                        isCancelled = cursor.getInt(statusIndex) == CalendarContract.Events.STATUS_CANCELED
                    )
                }
            }
        return instances
    }

    private fun dayRangeMillis(dayOffset: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return start.timeInMillis to end.timeInMillis
    }

    private companion object {
        const val NOON_HOUR = 12
    }
}
