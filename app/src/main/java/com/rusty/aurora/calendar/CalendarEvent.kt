package com.rusty.aurora.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    val title: String,
    val start: String,
    val end: String,
    val allDay: Boolean
)
