package com.rusty.aurora.weather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Lets [WeatherRepositoryImpl] skip a fetch attempt entirely while offline,
 * instead of eating a guaranteed-to-fail request/timeout on every stale
 * cache check.
 */
internal class NetworkStatusProvider(private val context: Context) {

    fun isConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
