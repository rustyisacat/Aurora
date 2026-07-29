package com.rusty.aurora.notifications

import com.rusty.aurora.notifications.NotificationGrouper.Entry
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationGrouperTest {

    @Test
    fun `counts notifications per package and resolves labels`() {
        val entries = listOf(
            Entry("com.discord", "Alice", "hi", postTimeMs = 1),
            Entry("com.discord", "Bob", "yo", postTimeMs = 2),
            Entry("com.discord", "Cara", "hey", postTimeMs = 3),
            Entry("com.google.gmail", "New mail", "from work", postTimeMs = 1)
        )
        val labels = mapOf("com.discord" to "Discord", "com.google.gmail" to "Gmail")

        val result = NotificationGrouper.group(entries) { labels.getValue(it) }

        assertEquals(
            listOf(
                NotificationGroup("Discord", "com.discord", 3, "Cara", "hey"),
                NotificationGroup("Gmail", "com.google.gmail", 1, "New mail", "from work")
            ),
            result
        )
    }

    @Test
    fun `picks the most recently posted notification as the preview, not the last in the list`() {
        val entries = listOf(
            Entry("com.discord", "Old", "old text", postTimeMs = 100),
            Entry("com.discord", "Newest", "newest text", postTimeMs = 300),
            Entry("com.discord", "Middle", "middle text", postTimeMs = 200)
        )

        val result = NotificationGrouper.group(entries) { it }

        assertEquals("Newest", result.single().latestTitle)
        assertEquals("newest text", result.single().latestText)
    }

    @Test
    fun `sorts busiest app first, ties broken alphabetically by resolved name`() {
        val entries = listOf(
            Entry("com.a", "t", "x", 1), Entry("com.a", "t", "x", 2),
            Entry("com.b", "t", "x", 1), Entry("com.b", "t", "x", 2),
            Entry("com.c", "t", "x", 1)
        )
        val labels = mapOf("com.a" to "Zebra", "com.b" to "Apple", "com.c" to "Middle")

        val result = NotificationGrouper.group(entries) { labels.getValue(it) }

        assertEquals(
            listOf("Apple", "Zebra", "Middle"),
            result.map { it.app }
        )
    }

    @Test
    fun `falls back to whatever the resolver returns, including the raw package name`() {
        val result = NotificationGrouper.group(listOf(Entry("com.unknown.app", "t", "x", 1))) { it }

        assertEquals("com.unknown.app", result.single().app)
    }

    @Test
    fun `empty input produces an empty result`() {
        assertEquals(emptyList<NotificationGroup>(), NotificationGrouper.group(emptyList()) { it })
    }
}
