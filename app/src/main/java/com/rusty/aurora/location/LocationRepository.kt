package com.rusty.aurora.location

interface LocationRepository {
    fun hasLocationPermission(): Boolean

    /**
     * Best available location right now, or null if permission hasn't been
     * granted or no fix has ever been obtained (by this app or any other -
     * see LocationRepositoryImpl). Callers should fall back to a fixed
     * default when this is null; it is never a suspending/blocking call.
     */
    fun getLastKnownLocation(): LocationSnapshot?
}
