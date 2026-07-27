package com.rusty.aurora.network

interface HomeNetworkRepository {
    /** Null until the user has answered the first-launch home-network
     *  prompt (or later, "Change Home Network"). */
    fun getHomeSubnetPrefix(): String?

    fun setHomeSubnetPrefix(prefix: String)
}
