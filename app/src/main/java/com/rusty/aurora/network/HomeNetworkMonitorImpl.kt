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
 * currently on the home subnet (see HomeNetworkConfig). Reuses
 * NetworkUtil.getLocalIpAddress() - the same "what's my own IP" read
 * already used to show the dashboard URL - rather than reading the Wi-Fi
 * SSID, which needs a location permission this check has no other reason
 * to require.
 */
class HomeNetworkMonitorImpl(context: Context) : HomeNetworkMonitor {

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
        val ip = NetworkUtil.getLocalIpAddress()
        _isOnHomeNetwork.value = ip?.startsWith(HomeNetworkConfig.HOME_SUBNET_PREFIX) == true
    }

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
