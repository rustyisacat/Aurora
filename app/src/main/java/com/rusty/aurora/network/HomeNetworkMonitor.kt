package com.rusty.aurora.network

import kotlinx.coroutines.flow.StateFlow

interface HomeNetworkMonitor {
    /** Whether the phone is currently on the home Wi-Fi network - the same
     *  one the Echo Show is on. False until [start] has run at least once,
     *  and stays false if no home network has been configured yet. */
    val isOnHomeNetwork: StateFlow<Boolean>

    fun start()
    fun stop()

    /** Re-evaluates [isOnHomeNetwork] immediately against whatever subnet
     *  is currently configured, without waiting for the next Wi-Fi
     *  connectivity event - needed right after the user sets or changes
     *  the home network while already connected to it, since no
     *  connectivity event fires in that case. */
    fun recheck()

    /** Best-effort subnet prefix (e.g. "192.168.1.") for whatever Wi-Fi
     *  network the phone is on right now, regardless of whether a home
     *  network has been configured - used to suggest a value on the
     *  first-launch home-network prompt. Null if not on Wi-Fi. */
    fun currentWifiSubnetPrefix(): String?
}
