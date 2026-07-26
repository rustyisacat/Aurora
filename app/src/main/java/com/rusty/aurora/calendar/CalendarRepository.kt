package com.rusty.aurora.calendar

/**
 * Reads today's events from the platform Calendar Provider.
 *
 * Permission denial and "no events today" are deliberately indistinguishable
 * here - [getTodayEvents] always returns a list and never throws, since the
 * /dashboard JSON schema expects `"calendar": []` either way. Callers that
 * need to know *why* the list is empty (e.g. to show a "grant access"
 * prompt) use [hasCalendarPermission] separately.
 */
interface CalendarRepository {
    fun hasCalendarPermission(): Boolean
    fun getTodayEvents(): List<CalendarEvent>
}
