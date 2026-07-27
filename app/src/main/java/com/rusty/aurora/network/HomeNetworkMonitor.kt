package com.rusty.aurora.network

import kotlinx.coroutines.flow.StateFlow

interface HomeNetworkMonitor {
    /** Whether the phone is currently on the home Wi-Fi network - the same
     *  one the Echo Show is on. False until [start] has run at least once. */
    val isOnHomeNetwork: StateFlow<Boolean>

    fun start()
    fun stop()
}
