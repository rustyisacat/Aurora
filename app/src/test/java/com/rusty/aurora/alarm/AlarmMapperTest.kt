package com.rusty.aurora.alarm

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class AlarmMapperTest {

    private lateinit var originalDefaultTimeZone: TimeZone

    @Before
    fun fixTimeZone() {
        originalDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone)
    }

    @Test
    fun `no scheduled alarm maps to null`() {
        assertNull(AlarmMapper.toNextAlarm(null))
    }

    @Test
    fun `trigger time maps to HH-mm with enabled true`() {
        val sevenAm = 7 * 60L * 60L * 1000L

        val result = AlarmMapper.toNextAlarm(sevenAm)

        assertEquals(NextAlarm(time = "07:00", enabled = true), result)
    }
}
