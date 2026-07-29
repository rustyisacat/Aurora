package com.rusty.aurora.sound

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Pure catalog merge/lookup, decoupled from SharedPreferences/Context so
 * it's a plain JVM unit test. Built-in entries always win an id collision
 * (defensive; in practice custom ids are hash-derived and won't collide).
 */
internal object SoundCatalog {
    fun merge(builtIn: List<SoundInfo>, custom: List<SoundInfo>): List<SoundInfo> {
        val builtInIds = builtIn.map { it.id }.toSet()
        return builtIn + custom.filterNot { it.id in builtInIds }
    }

    fun findById(catalog: List<SoundInfo>, id: String): SoundInfo? =
        catalog.firstOrNull { it.id == id }
}

/**
 * The full sound catalog: [BUILT_IN] assets plus whatever the user has
 * imported via Storage Access Framework (persisted as a small JSON list in
 * SharedPreferences - a database is overkill for what's realistically a
 * handful of entries). Adding a new built-in sound is dropping a file into
 * assets/sounds/ and adding one line to [BUILT_IN] - see the README there.
 */
class SoundLibrary(private val context: Context) {

    fun getAll(): List<SoundInfo> = SoundCatalog.merge(BUILT_IN, loadCustomSounds())

    fun findById(id: String): SoundInfo? = SoundCatalog.findById(getAll(), id)

    /** [uri] must already have a persisted (takePersistableUriPermission'd) read grant. */
    fun addCustomSound(uri: Uri, displayName: String): SoundInfo {
        val entry = CustomSoundEntry(
            id = "custom_${uri.toString().hashCode()}",
            displayName = displayName,
            uriString = uri.toString()
        )
        saveCustomEntries(loadCustomEntries().filterNot { it.id == entry.id } + entry)
        return entry.toSoundInfo()
    }

    fun removeCustomSound(id: String) {
        saveCustomEntries(loadCustomEntries().filterNot { it.id == id })
    }

    private fun loadCustomSounds(): List<SoundInfo> = loadCustomEntries().map { it.toSoundInfo() }

    private fun loadCustomEntries(): List<CustomSoundEntry> {
        val raw = prefs.getString(CUSTOM_SOUNDS_KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<CustomSoundEntry>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveCustomEntries(entries: List<CustomSoundEntry>) {
        prefs.edit().putString(CUSTOM_SOUNDS_KEY, json.encodeToString(entries)).apply()
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Serializable
    private data class CustomSoundEntry(
        val id: String,
        val displayName: String,
        val uriString: String
    ) {
        fun toSoundInfo() = SoundInfo(id, displayName, SoundSource.CustomUri(uriString))
    }

    companion object {
        private const val PREFS_NAME = "aurora_sound_library"
        private const val CUSTOM_SOUNDS_KEY = "custom_sounds"
        private val json = Json { ignoreUnknownKeys = true }

        val BUILT_IN = listOf(
            SoundInfo("rain", "Rain", SoundSource.Asset("sounds/rain.mp3")),
            SoundInfo("heavy_rain", "Heavy Rain", SoundSource.Asset("sounds/heavy_rain.mp3")),
            SoundInfo("thunderstorm", "Thunderstorm", SoundSource.Asset("sounds/thunderstorm.mp3")),
            SoundInfo("forest", "Forest", SoundSource.Asset("sounds/forest.mp3")),
            SoundInfo("wind", "Wind", SoundSource.Asset("sounds/wind.mp3")),
            SoundInfo("ocean_waves", "Ocean Waves", SoundSource.Asset("sounds/ocean_waves.mp3")),
            SoundInfo("rain_on_tent", "Rain on Tent", SoundSource.Asset("sounds/rain_on_tent.mp3")),
            SoundInfo("fireplace", "Fireplace", SoundSource.Asset("sounds/fireplace.mp3")),
            // Kept last, not first: SoundRepository.play(null) and this
            // list's other "first entry" fallback uses are for the
            // *ambient* sound machine, where a loud alarm clip defaulting
            // in would be exactly wrong. Wake alarms reference this one by
            // id specifically - see WakeAlarmRepositoryImpl.BUNDLED_ALARM_SOUND_ID.
            SoundInfo("alarm_reveille", "Reveille (Alarm)", SoundSource.Asset("sounds/alarm_reveille.mp3"))
        )
    }
}
