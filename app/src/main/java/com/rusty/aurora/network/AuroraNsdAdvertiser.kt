package com.rusty.aurora.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log

/**
 * Advertises the HTTP server on the LAN via mDNS/NSD (`_aurora._tcp`), so
 * anyone with real DNS-SD tooling (e.g. `avahi-browse -r _aurora._tcp`, a
 * network scanner app) can find the phone's current address without
 * hunting through Wi-Fi settings - useful on its own, and directly born
 * from the DHCP-lease-expired incident that made exactly that necessary
 * once this session.
 *
 * Deliberately doesn't try to make the dashboard itself discover Aurora
 * this way: browsers have no API to browse DNS-SD/mDNS services, and
 * verified live against the actual kiosk WebView here, plain `.local`
 * hostname resolution isn't supported either (a `fetch()` to it fails
 * outright) - so a browser-side client genuinely cannot use this, only
 * native tooling can. This class is worth having anyway for that "find it
 * without wifi settings" case.
 *
 * [start]/[stop] are meant to be called alongside [AuroraServerController]'s
 * own start/stop (see AuroraBackgroundService) - the advertisement should
 * only exist while something is actually listening on [port].
 *
 * Holds a [WifiManager.MulticastLock] for as long as it's registered:
 * without it, Wi-Fi power-save can silently drop the inbound multicast
 * packets mDNS registration/response depends on, particularly once the
 * phone goes idle.
 */
class AuroraNsdAdvertiser(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registered = false

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "Registered mDNS service: ${serviceInfo.serviceName}.${serviceInfo.serviceType}")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "mDNS registration failed, error code $errorCode")
            registered = false
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "Unregistered mDNS service")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "mDNS unregistration failed, error code $errorCode")
        }
    }

    fun start(port: Int) {
        if (registered) return
        registered = true

        multicastLock = wifiManager.createMulticastLock(TAG).apply {
            setReferenceCounted(false)
            acquire()
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun stop() {
        if (!registered) return
        registered = false

        try {
            nsdManager.unregisterService(registrationListener)
        } catch (e: IllegalArgumentException) {
            // Registration never actually succeeded (e.g. onRegistrationFailed
            // already fired) - nothing to unregister.
        }
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }

    private companion object {
        const val TAG = "AuroraNsdAdvertiser"
        const val SERVICE_NAME = "Aurora"
        const val SERVICE_TYPE = "_aurora._tcp."
    }
}
