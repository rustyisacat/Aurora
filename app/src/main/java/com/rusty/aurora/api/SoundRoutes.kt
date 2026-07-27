package com.rusty.aurora.api

import com.rusty.aurora.sound.SoundRepository
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

/**
 * The sound machine's HTTP surface: control routes plus the byte stream
 * itself, grouped in a single file rather than one-class-per-file - unlike
 * HealthRoute/DashboardRoute (unrelated concerns), these are one
 * resource's tightly-coupled surface.
 *
 * POST endpoints take query-string parameters, not JSON bodies - NanoHTTPD
 * parses query params identically for GET and POST with zero body-parsing
 * code, which is all these 0-1-argument control calls need.
 *
 * Aurora never plays audio itself - SoundStreamRoute exists so the Echo
 * Show's kiosk browser can fetch the raw bytes and play them locally
 * (Web Audio API) through its own speakers. Everything else here just
 * records what the browser is doing, for /dashboard consistency.
 */

private val soundJson = Json { encodeDefaults = true }

private fun okResponse(): NanoHTTPD.Response =
    NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", "OK")

/** session.parms is deprecated in favor of the multi-value getParameters() API. */
private fun NanoHTTPD.IHTTPSession.singleParam(name: String): String? =
    parameters[name]?.firstOrNull()

class PlaySoundRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/sound/play"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        soundRepository.play(session.singleParam("id"))
        return okResponse()
    }
}

class PauseSoundRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/sound/pause"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        soundRepository.pause()
        return okResponse()
    }
}

class StopSoundRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/sound/stop"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        soundRepository.stop()
        return okResponse()
    }
}

class SetVolumeRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/sound/volume"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val value = session.singleParam("value")?.toIntOrNull()
            ?: return NanoHTTPD.newFixedLengthResponse(
                Status.BAD_REQUEST, "text/plain", "Missing or invalid 'value' (expected 0-100)"
            )
        soundRepository.setVolume(value)
        return okResponse()
    }
}

class SetSleepTimerRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.POST && session.uri == "/sound/timer"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        // "off", "15", "30", "45", "60", "120", or "untilAlarm" - see SleepTimerCalculator.
        soundRepository.setSleepTimer(session.singleParam("preset") ?: "off")
        return okResponse()
    }
}

@Serializable
private data class SoundLibraryEntry(val id: String, val displayName: String)

class SoundLibraryRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/sound/library"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val entries = soundRepository.getLibrary().map { SoundLibraryEntry(it.id, it.displayName) }
        return NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            "application/json",
            soundJson.encodeToString(entries)
        )
    }
}

class SoundStreamRoute(private val soundRepository: SoundRepository) : Route {
    override fun matches(session: NanoHTTPD.IHTTPSession): Boolean =
        session.method == NanoHTTPD.Method.GET && session.uri == "/sound/stream"

    override fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val id = session.singleParam("id")
            ?: return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", "Missing 'id'")

        val stream = soundRepository.openSoundStream(id)
            ?: return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Unknown sound: $id")

        val response = NanoHTTPD.newFixedLengthResponse(
            Status.OK,
            stream.mimeType,
            ByteArrayInputStream(stream.bytes),
            stream.bytes.size.toLong()
        )
        // Sounds are static per id - safe for the browser to cache, avoiding a
        // re-fetch every time the same sound is selected again.
        response.addHeader("Cache-Control", "public, max-age=86400")
        return response
    }
}
