package com.rusty.aurora.api

import com.rusty.aurora.network.HomeNetworkRepository
import com.rusty.aurora.network.normalizeSubnetPrefix
import com.rusty.aurora.profile.UserProfileRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The two remaining phone-app-only settings that are pure data (no secret,
 * no OS permission dialog, no filesystem picker involved) - display name
 * and the home-network subnet prefix used to gate the foreground service.
 * Same query-string-params convention as SoundRoutes/WakeAlarmRoutes.
 */

private val settingsJson = Json { encodeDefaults = true }

private fun okResponse(): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")

private fun NanoHTTPD.IHTTPSession.singleParam(name: String): String? =
    parameters[name]?.firstOrNull()

class SetUserNameRoute(private val userProfileRepository: UserProfileRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/settings/name"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val value = session.singleParam("value")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'value'")
        userProfileRepository.setUserName(value)
        return okResponse()
    }
}

@Serializable
private data class HomeNetworkResponse(val prefix: String?)

class GetHomeNetworkRoute(private val homeNetworkRepository: HomeNetworkRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/settings/home-network"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response =
        NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            settingsJson.encodeToString(HomeNetworkResponse(homeNetworkRepository.getHomeSubnetPrefix()))
        )
}

class SetHomeNetworkRoute(private val homeNetworkRepository: HomeNetworkRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/settings/home-network"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val prefix = session.singleParam("prefix")?.let(::normalizeSubnetPrefix)
            ?: return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'prefix' (expected three 0-255 octets, e.g. 192.168.1)"
            )
        homeNetworkRepository.setHomeSubnetPrefix(prefix)
        return okResponse()
    }
}
