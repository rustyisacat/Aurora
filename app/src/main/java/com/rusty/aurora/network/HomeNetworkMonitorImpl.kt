package com.rusty.aurora.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.rusty.aurora.util.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches for Wi-Fi connectivity changes and reports whether the phone is
 * currently on the home subnet - user-configured via HomeNetworkRepository
 * (first-launch prompt, or "Change Home Network" later), not hardcoded,
 * since this app is public on GitHub and a fixed subnet would only ever
 * match one person's router. Uses NetworkUtil.getWifiIpAddress() - reading
 * the active Wi-Fi network's link address directly - rather than the Wi-Fi
 * SSID, which needs a location permission this check has no other reason
 * to require.
 */
class HomeNetworkMonitorImpl(
    context: Context,
    private val homeNetworkRepository: HomeNetworkRepository
) : HomeNetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnHomeNetwork = MutableStateFlow(false)
    override val isOnHomeNetwork: StateFlow<Boolean> = _isOnHomeNetwork.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onLost(network: Network) = refresh()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = refresh()
    }

    private fun refresh() {
        val homePrefix = homeNetworkRepository.getHomeSubnetPrefix()
        _isOnHomeNetwork.value = homePrefix != null &&
            NetworkUtil.getWifiIpAddress(connectivityManager)?.startsWith(homePrefix) == true
    }

    override fun recheck() = refresh()

    override fun currentWifiSubnetPrefix(): String? =
        NetworkUtil.getWifiIpAddress(connectivityManager)?.let(NetworkUtil::subnetPrefixOf)

    override fun start() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        refresh() // covers "already connected before start() was called"
    }

    override fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}
