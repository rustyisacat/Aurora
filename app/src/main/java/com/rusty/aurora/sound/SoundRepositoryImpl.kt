package com.rusty.aurora.sound

import android.content.Context
import android.net.Uri
import com.rusty.aurora.alarm.AlarmRepository

class SoundRepositoryImpl(
    private val context: Context,
    private val soundLibrary: SoundLibrary,
    alarmRepository: AlarmRepository
) : SoundRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sleepTimer = SleepTimer(alarmRepository, onExpired = ::handleSleepTimerExpired)

    // Restored from prefs, not hardcoded to false: if Aurora's own process
    // restarts (phone reboot, app killed and reopened), the Echo Show's
    // browser is a completely separate device and keeps playing (or not)
    // exactly as it was - Aurora needs to resume believing whatever it
    // last recorded, not reset to "nothing playing".
    @Volatile
    private var isPlaying: Boolean = prefs.getBoolean(KEY_WAS_PLAYING, false)

    @Volatile
    private var currentSoundId: String? = prefs.getString(KEY_SOUND_ID, null)

    @Volatile
    private var volumePercent: Int = prefs.getInt(KEY_VOLUME, DEFAULT_VOLUME_PERCENT)

    override fun getState(): SoundMachineState = SoundMachineState(
        playing = isPlaying,
        sound = currentSoundId?.let { soundLibrary.findById(it)?.displayName },
        volume = volumePercent,
        sleepTimerMinutes = sleepTimer.minutesRemaining
    )

    override fun getLibrary(): List<SoundInfo> = soundLibrary.getAll()

    override fun play(soundId: String?) {
        val targetId = soundId ?: currentSoundId ?: soundLibrary.getAll().firstOrNull()?.id ?: return
        if (soundLibrary.findById(targetId) == null) return

        currentSoundId = targetId
        isPlaying = true
        persistState()
    }

    override fun pause() {
        isPlaying = false
        persistState()
    }

    override fun stop() {
        sleepTimer.cancel()
        isPlaying = false
        persistState()
    }

    override fun setVolume(percent: Int) {
        volumePercent = percent.coerceIn(0, 100)
        prefs.edit().putInt(KEY_VOLUME, volumePercent).apply()
    }

    override fun setSleepTimer(rawPreset: String) {
        sleepTimer.start(rawPreset)
    }

    override fun importCustomSound(uri: Uri, displayName: String): SoundInfo =
        soundLibrary.addCustomSound(uri, displayName)

    override fun openSoundStream(soundId: String): SoundStream? {
        val sound = soundLibrary.findById(soundId) ?: return null
        return when (val source = sound.source) {
            is SoundSource.Asset -> runCatching {
                val bytes = context.assets.open(source.assetPath).use { it.readBytes() }
                SoundStream(bytes, mimeTypeForPath(source.assetPath))
            }.getOrNull()

            is SoundSource.CustomUri -> runCatching {
                val uri = Uri.parse(source.uriString)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
                val mimeType = context.contentResolver.getType(uri) ?: mimeTypeForPath(source.uriString)
                SoundStream(bytes, mimeType)
            }.getOrNull()
        }
    }

    private fun handleSleepTimerExpired() {
        isPlaying = false
        persistState()
    }

    private fun persistState() {
        prefs.edit()
            .putBoolean(KEY_WAS_PLAYING, isPlaying)
            .putString(KEY_SOUND_ID, currentSoundId)
            .apply()
    }

    private fun mimeTypeForPath(path: String): String = when {
        path.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
        path.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
        path.endsWith(".flac", ignoreCase = true) -> "audio/flac"
        path.endsWith(".wav", ignoreCase = true) -> "audio/wav"
        else -> "application/octet-stream"
    }

    private companion object {
        const val PREFS_NAME = "aurora_sound_state"
        const val KEY_WAS_PLAYING = "was_playing"
        const val KEY_SOUND_ID = "sound_id"
        const val KEY_VOLUME = "volume"
        const val DEFAULT_VOLUME_PERCENT = 50
    }
}
