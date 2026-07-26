package com.rusty.aurora.calendar

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class CalendarEventMapperTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    @Before
    fun fixTimeZone() {
        // Timed events format in the device default timezone - pin it to UTC
        // so expected "HH:mm" strings don't depend on where this test runs.
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    @Test
    fun `cancelled events are filtered out`() {
        val instances = listOf(
            instance(title = "Kept", beginMillis = 1_000L, isCancelled = false),
            instance(title = "Cancelled", beginMillis = 2_000L, isCancelled = true)
        )

        val result = CalendarEventMapper.toSortedEvents(instances)

        assertEquals(listOf("Kept"), result.map { it.title })
    }

    @Test
    fun `events are sorted chronologically by start time regardless of input order`() {
        val instances = listOf(
            instance(title = "Afternoon", beginMillis = hoursToMillis(15)),
            instance(title = "Morning", beginMillis = hoursToMillis(8)),
            instance(title = "Noon", beginMillis = hoursToMillis(12))
        )

        val result = CalendarEventMapper.toSortedEvents(instances)

        assertEquals(listOf("Morning", "Noon", "Afternoon"), result.map { it.title })
    }

    @Test
    fun `timed event formats start and end as HH-mm`() {
        val instances = listOf(
            instance(
                title = "School",
                beginMillis = hoursToMillis(8),
                endMillis = hoursToMillis(15),
                allDay = false
            )
        )

        val result = CalendarEventMapper.toSortedEvents(instances).single()

        assertEquals("08:00", result.start)
        assertEquals("15:00", result.end)
        assertEquals(false, result.allDay)
    }

    @Test
    fun `all-day event formats using UTC day boundaries`() {
        val instances = listOf(
            instance(title = "Holiday", beginMillis = 0L, endMillis = hoursToMillis(24), allDay = true)
        )

        val result = CalendarEventMapper.toSortedEvents(instances).single()

        assertEquals("00:00", result.start)
        assertEquals("00:00", result.end)
        assertEquals(true, result.allDay)
    }

    @Test
    fun `missing title becomes an empty string, not a crash`() {
        val instances = listOf(instance(title = null, beginMillis = 0L))

        val result = CalendarEventMapper.toSortedEvents(instances).single()

        assertEquals("", result.title)
    }

    private fun instance(
        title: String?,
        beginMillis: Long,
        endMillis: Long = beginMillis,
        allDay: Boolean = false,
        isCancelled: Boolean = false
    ) = RawCalendarInstance(
        title = title,
        beginMillis = beginMillis,
        endMillis = endMillis,
        allDay = allDay,
        isCancelled = isCancelled
    )

    private fun hoursToMillis(hour: Int): Long = hour * 60L * 60L * 1000L
}
