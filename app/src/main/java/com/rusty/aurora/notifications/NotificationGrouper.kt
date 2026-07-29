package com.rusty.aurora.notifications

/**
 * Pure grouping/sorting logic, decoupled from PackageManager/StatusBarNotification
 * so it's a plain JVM unit test: given each active notification's package,
 * title, text, and post time, plus a label resolver, produce one
 * [NotificationGroup] per app - busiest first (ties broken alphabetically -
 * a stable, predictable order for a glance display), each carrying a
 * preview of its most recently posted notification.
 */
internal object NotificationGrouper {

    data class Entry(
        val packageName: String,
        val title: String,
        val text: String,
        val postTimeMs: Long
    )

    fun group(entries: List<Entry>, resolveLabel: (String) -> String): List<NotificationGroup> =
        entries
            .groupBy { it.packageName }
            .map { (packageName, items) ->
                val latest = items.maxBy { it.postTimeMs }
                NotificationGroup(
                    app = resolveLabel(packageName),
                    packageName = packageName,
                    count = items.size,
                    latestTitle = latest.title,
                    latestText = latest.text
                )
            }
            .sortedWith(compareByDescending<NotificationGroup> { it.count }.thenBy { it.app })
}
