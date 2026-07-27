package com.rusty.aurora.notifications

/**
 * Pure grouping/sorting logic, decoupled from PackageManager/StatusBarNotification
 * so it's a plain JVM unit test: given the package name of each active
 * notification and a label resolver, produce counts grouped by app, busiest
 * first (ties broken alphabetically - a stable, predictable order for a
 * glance display).
 */
internal object NotificationGrouper {
    fun group(packageNames: List<String>, resolveLabel: (String) -> String): List<NotificationGroup> =
        packageNames
            .groupingBy { it }
            .eachCount()
            .map { (packageName, count) -> NotificationGroup(app = resolveLabel(packageName), count = count) }
            .sortedWith(compareByDescending<NotificationGroup> { it.count }.thenBy { it.app })
}
