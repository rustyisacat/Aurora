package com.rusty.aurora.sound

/** One entry in the sound library - either bundled with the app or user-imported. */
data class SoundInfo(
    val id: String,
    val displayName: String,
    val source: SoundSource
)

sealed interface SoundSource {
    /** A file bundled under app/src/main/assets/, relative to the assets root. */
    data class Asset(val assetPath: String) : SoundSource

    /** A user-imported file, referenced by a persisted SAF document URI. */
    data class CustomUri(val uriString: String) : SoundSource
}
