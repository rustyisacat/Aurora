package com.rusty.aurora.calendar

import kotlinx.serialization.Serializable

/** One day's worth of events for the dashboard's weekly view (see
 *  CalendarRepository.getWeekEvents) - [date] is a plain ISO
 *  "yyyy-MM-dd", not a weekday name or anything locale-specific, since
 *  the dashboard already has its own timezone-aware date formatting and
 *  shouldn't need to parse one out of a human-readable string. */
@Serializable
data class WeekDay(
    val date: String,
    val events: List<CalendarEvent>
)
