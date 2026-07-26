package com.rusty.aurora.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of everything the Echo Show dashboard needs to render in one
 * request. New data sources (weather, calendar, alarm, ...) get added as
 * new fields here - clients simply see new keys appear in the JSON.
 */
@Serializable
data class DashboardResponse(
    val battery: Int,
    val charging: Boolean,
    val notifications: Int
)
