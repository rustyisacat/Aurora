package com.rusty.aurora.network

import android.content.Context

/** SharedPreferences-backed, same pattern as UserProfileRepositoryImpl. */
class HomeNetworkRepositoryImpl(context: Context) : HomeNetworkRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getHomeSubnetPrefix(): String? =
        prefs.getString(KEY_SUBNET_PREFIX, null)?.takeIf { it.isNotBlank() }

    override fun setHomeSubnetPrefix(prefix: String) {
        val trimmed = prefix.trim()
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_SUBNET_PREFIX, trimmed).apply()
    }

    private companion object {
        const val PREFS_NAME = "aurora_home_network"
        const val KEY_SUBNET_PREFIX = "subnet_prefix"
    }
}
