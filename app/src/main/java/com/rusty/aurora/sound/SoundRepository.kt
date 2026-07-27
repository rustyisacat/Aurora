package com.rusty.aurora.sound

import android.net.Uri

/**
 * Tracks the sound machine's DESIRED state. Aurora does not play any audio
 * itself - the Echo Show's kiosk browser does, via the Web Audio API,
 * fetching bytes from GET /sound/stream - so this is state-tracking, not
 * playback control. The dashboard's /dashboard poll loop reads [getState]
 * to know what it should be doing (including reconciling after its own
 * page reload), and calls play/pause/stop/setVolume/setSleepTimer to
 * record what it's actually doing.
 */
interface SoundRepository {
    fun getState(): SoundMachineState
    fun getLibrary(): List<SoundInfo>

    /** Null [soundId] resumes the current/last sound; falls back to the first library entry. */
    fun play(soundId: String?)
    fun pause()
    fun stop()
    fun setVolume(percent: Int)

    /** [rawPreset]: "off", "15", "30", "45", "60", "120", or "untilAlarm". */
    fun setSleepTimer(rawPreset: String)

    /** [uri] must already have a persisted (takePersistableUriPermission'd) read grant. */
    fun importCustomSound(uri: Uri, displayName: String): SoundInfo

    /** Opens the raw bytes for [soundId] plus its MIME type, or null if unknown/unreadable. */
    fun openSoundStream(soundId: String): SoundStream?
}

/** [mimeType] e.g. "audio/mpeg"; [bytes] the whole file - these are small personal audio clips. */
data class SoundStream(val bytes: ByteArray, val mimeType: String)
