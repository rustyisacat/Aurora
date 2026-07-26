package com.rusty.aurora.util

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Best-effort local IPv4 address, for showing the URL the Echo Show should
 * hit. Reads active network interfaces directly rather than WifiManager,
 * which is deprecated for this purpose on modern API levels and only
 * covers Wi-Fi specifically.
 */
object NetworkUtil {
    fun getLocalIpAddress(): String? =
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.firstOrNull { address -> !address.isLoopbackAddress && address is Inet4Address }
            ?.hostAddress
}
