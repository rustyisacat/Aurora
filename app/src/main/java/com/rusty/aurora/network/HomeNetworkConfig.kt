package com.rusty.aurora.network

/**
 * Same "bedside dashboard bolted to one place" reasoning as WeatherConfig's
 * fixed coordinate: Aurora's server only matters to devices on this one
 * home network, so a fixed subnet prefix is simpler and more reliable than
 * matching by Wi-Fi SSID (which needs ACCESS_FINE_LOCATION, not just
 * coarse, on modern Android). Update this if the home network ever changes.
 */
object HomeNetworkConfig {
    const val HOME_SUBNET_PREFIX = "192.168.1."
}
