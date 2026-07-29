package com.rusty.aurora.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** SharedPreferences-backed, same single-JSON-blob-per-key pattern as
 *  PhotoRepositoryImpl - both fields always change as a whole, never
 *  field-by-field, so there's no reason to split them across prefs keys. */
class NotificationBlocklistRepositoryImpl(context: Context) : NotificationBlocklistRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _blockedPackages = MutableStateFlow(loadBlocked())
    override val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _knownApps = MutableStateFlow(loadKnownApps())
    override val knownApps: StateFlow<List<KnownApp>> = _knownApps.asStateFlow()

    override fun recordSeen(packageName: String, label: String) {
        if (_knownApps.value.any { it.packageName == packageName }) return
        val updated = _knownApps.value + KnownApp(packageName, label)
        _knownApps.value = updated
        prefs.edit().putString(KEY_KNOWN_APPS, json.encodeToString(updated.map { it.toEntry() })).apply()
    }

    override fun setBlocked(packageName: String, blocked: Boolean) {
        val updated = if (blocked) _blockedPackages.value + packageName else _blockedPackages.value - packageName
        _blockedPackages.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED, updated).apply()
    }

    private fun loadBlocked(): Set<String> = prefs.getStringSet(KEY_BLOCKED, null)?.toSet() ?: emptySet()

    private fun loadKnownApps(): List<KnownApp> {
        val raw = prefs.getString(KEY_KNOWN_APPS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<KnownAppEntry>>(raw) }
            .getOrDefault(emptyList())
            .map { it.toKnownApp() }
    }

    @Serializable
    private data class KnownAppEntry(val packageName: String, val label: String) {
        fun toKnownApp() = KnownApp(packageName, label)
    }

    private fun KnownApp.toEntry() = KnownAppEntry(packageName, label)

    private companion object {
        const val PREFS_NAME = "aurora_notification_blocklist"
        const val KEY_BLOCKED = "blocked_packages"
        const val KEY_KNOWN_APPS = "known_apps"
        val json = Json { ignoreUnknownKeys = true }
    }
}
