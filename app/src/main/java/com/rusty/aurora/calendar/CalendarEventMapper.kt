package com.rusty.aurora.calendar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * One row out of CalendarContract.Instances, decoupled from the actual
 * cursor/URI/permission machinery so the filtering, sorting, and time
 * formatting below can be a plain JVM unit test - no Robolectric or
 * instrumentation needed. [CalendarRepositoryImpl] is the only thing that
 * builds these.
 */
internal data class RawCalendarInstance(
    val title: String?,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val isCancelled: Boolean
)

internal object CalendarEventMapper {

    fun toSortedEvents(instances: List<RawCalendarInstance>): List<CalendarEvent> =
        instances
            .filterNot { it.isCancelled }
            .sortedBy { it.beginMillis }
            .map { it.toCalendarEvent() }

    private fun RawCalendarInstance.toCalendarEvent() = CalendarEvent(
        title = title.orEmpty(),
        start = formatTime(beginMillis, allDay),
        end = formatTime(endMillis, allDay),
        allDay = allDay
    )

    // All-day event begin/end are stored as UTC-midnight day boundaries, not
    // local wall-clock instants - formatting in the device's local timezone
    // could shift the displayed time by a day depending on offset.
    private fun formatTime(epochMillis: Long, allDay: Boolean): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        if (allDay) {
            formatter.timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(epochMillis))
    }
}
