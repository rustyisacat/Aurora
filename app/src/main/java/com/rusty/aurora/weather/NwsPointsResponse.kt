package com.rusty.aurora.weather

import kotlinx.serialization.Serializable

/** Shape of the subset of the NWS `/points/{lat},{lon}` response Aurora
 *  actually reads - just enough to resolve which radar station covers a
 *  given coordinate. */
@Serializable
internal data class NwsPointsResponse(
    val properties: Properties
) {
    @Serializable
    data class Properties(
        // e.g. "KJAX" - the 4-letter station id radar.weather.gov's ridge
        // image endpoints expect.
        val radarStation: String? = null
    )
}
