package com.rusty.aurora.network

/** "192.168.1" or "192.168.1." -> "192.168.1."; null if not three valid
 *  0-255 octets. Shared by HomeNetworkEntryScreen (phone) and
 *  SetHomeNetworkRoute (dashboard) so both entry points enforce the same
 *  validation instead of drifting apart. */
fun normalizeSubnetPrefix(input: String): String? {
    val octets = input.trim().removeSuffix(".").split(".")
    if (octets.size != 3) return null
    if (octets.any { octet -> octet.toIntOrNull()?.let { it in 0..255 } != true }) return null
    return octets.joinToString(".", postfix = ".")
}
