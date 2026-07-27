package com.rusty.aurora.util

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /**
     * Unlike [getLocalIpAddress], this looks specifically at the active
     * Wi-Fi network's own link addresses rather than "first interface
     * found" - needed because phones commonly hold an active mobile data
     * connection at the same time as Wi-Fi, and interface enumeration
     * order isn't guaranteed to prefer Wi-Fi. Used for home-network
     * detection, where picking up the wrong transport would misdetect.
     */
    fun getWifiIpAddress(connectivityManager: ConnectivityManager): String? =
        connectivityManager.allNetworks
            .firstOrNull { network ->
                connectivityManager.getNetworkCapabilities(network)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            ?.let { network -> connectivityManager.getLinkProperties(network) }
            ?.linkAddresses
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull()
            ?.hostAddress

    /** "192.168.1.130" -> "192.168.1." - assumes a /24, same as the rest of
     *  HomeNetworkMonitor's subnet-prefix matching. */
    fun subnetPrefixOf(ipv4Address: String): String = ipv4Address.substringBeforeLast(".") + "."
}
