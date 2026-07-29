package com.rusty.aurora.api

import com.rusty.aurora.wakealarm.WakeAlarm
import com.rusty.aurora.wakealarm.WakeAlarmRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Aurora's own alarm clock's HTTP surface - grouped in one file for the
 * same reason as SoundRoutes: a tightly-coupled resource, not unrelated
 * concerns. Same query-string-params convention as SoundRoutes, including
 * for /wakealarms/set despite its handful of fields - a comma-separated
 * "days" value is still simpler than introducing this app's first JSON
 * request body just for this one endpoint.
 */

private val wakeAlarmJson = Json { encodeDefaults = true }

private fun okResponse(): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")

private fun jsonResponse(body: String): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", body)

private fun NanoHTTPD.IHTTPSession.singleParam(name: String): String? =
    parameters[name]?.firstOrNull()

class GetWakeAlarmsRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/wakealarms"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response =
        jsonResponse(wakeAlarmJson.encodeToString(wakeAlarmRepository.getAlarms()))
}

class SetWakeAlarmRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/wakealarms/set"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val hour = session.singleParam("hour")?.toIntOrNull()?.takeIf { it in 0..23 }
        val minute = session.singleParam("minute")?.toIntOrNull()?.takeIf { it in 0..59 }
        if (hour == null || minute == null) {
            return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'hour' (0-23) / 'minute' (0-59)"
            )
        }

        val alarm = WakeAlarm(
            id = session.singleParam("id")?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            hour = hour,
            minute = minute,
            daysOfWeek = session.singleParam("days")
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
            enabled = session.singleParam("enabled")?.toBooleanStrictOrNull() ?: true,
            label = session.singleParam("label") ?: "",
            soundId = session.singleParam("soundId")?.takeIf { it.isNotBlank() }
        )
        wakeAlarmRepository.setAlarm(alarm)
        return jsonResponse(wakeAlarmJson.encodeToString(alarm))
    }
}

class DeleteWakeAlarmRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/wakealarms/delete"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val id = session.singleParam("id")
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'id'")
        wakeAlarmRepository.deleteAlarm(id)
        return okResponse()
    }
}

class DismissWakeAlarmRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/wakealarms/dismiss"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        wakeAlarmRepository.dismiss()
        return okResponse()
    }
}

class SnoozeWakeAlarmRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/wakealarms/snooze"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val minutes = session.singleParam("minutes")?.toIntOrNull() ?: DEFAULT_SNOOZE_MINUTES
        wakeAlarmRepository.snooze(minutes)
        return okResponse()
    }

    private companion object {
        const val DEFAULT_SNOOZE_MINUTES = 9
    }
}

class SetDefaultAlarmSoundRoute(private val wakeAlarmRepository: WakeAlarmRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/wakealarms/default-sound"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val id = session.singleParam("id")?.takeIf { it.isNotBlank() }
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'id'")
        wakeAlarmRepository.setDefaultAlarmSoundId(id)
        return okResponse()
    }
}
