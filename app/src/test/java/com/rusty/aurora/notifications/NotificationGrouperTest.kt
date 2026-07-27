package com.rusty.aurora.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationGrouperTest {

    @Test
    fun `counts notifications per package and resolves labels`() {
        val packages = listOf("com.discord", "com.discord", "com.discord", "com.google.gmail")
        val labels = mapOf("com.discord" to "Discord", "com.google.gmail" to "Gmail")

        val result = NotificationGrouper.group(packages) { labels.getValue(it) }

        assertEquals(
            listOf(NotificationGroup("Discord", 3), NotificationGroup("Gmail", 1)),
            result
        )
    }

    @Test
    fun `sorts busiest app first, ties broken alphabetically by resolved name`() {
        val packages = listOf("com.a", "com.a", "com.b", "com.b", "com.c")
        val labels = mapOf("com.a" to "Zebra", "com.b" to "Apple", "com.c" to "Middle")

        val result = NotificationGrouper.group(packages) { labels.getValue(it) }

        assertEquals(
            listOf(
                NotificationGroup("Apple", 2),
                NotificationGroup("Zebra", 2),
                NotificationGroup("Middle", 1)
            ),
            result
        )
    }

    @Test
    fun `falls back to whatever the resolver returns, including the raw package name`() {
        val result = NotificationGrouper.group(listOf("com.unknown.app")) { it }

        assertEquals(listOf(NotificationGroup("com.unknown.app", 1)), result)
    }

    @Test
    fun `empty input produces an empty result`() {
        assertEquals(emptyList<NotificationGroup>(), NotificationGrouper.group(emptyList()) { it })
    }
}
