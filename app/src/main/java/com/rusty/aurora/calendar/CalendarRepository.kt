package com.rusty.aurora.calendar

/**
 * Reads the "currently relevant" day's events from the platform Calendar
 * Provider - today's before noon, tomorrow's from noon onward - so the
 * bedside dashboard shows what's ahead when checked in the morning, and
 * what's ahead tomorrow when checked again at night, resetting at midnight.
 *
 * Permission denial and "no events" are deliberately indistinguishable
 * here - [getEvents] always returns a list and never throws, since the
 * /dashboard JSON schema expects `"calendar": []` either way. Callers that
 * need to know *why* the list is empty (e.g. to show a "grant access"
 * prompt) use [hasCalendarPermission] separately.
 */
interface CalendarRepository {
    fun hasCalendarPermission(): Boolean
    fun getEvents(): List<CalendarEvent>

    /** Whether [getEvents] is currently returning tomorrow's events rather
     *  than today's - a plain clock check (local hour >= noon), independent
     *  of calendar permission or data. Lets callers label the list
     *  correctly (e.g. "Tomorrow's Events" instead of "Today's Events"). */
    fun isShowingTomorrow(): Boolean
}
