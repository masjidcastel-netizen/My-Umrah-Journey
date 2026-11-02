package com.masjidcastel.myumrahjourney.core.player

import com.masjidcastel.myumrahjourney.core.language.AppLanguage

/**
 * Everything the app remembers about the player between launches.
 * Stage 1 stores settings only; journey progress is added in later stages.
 */
data class PlayerState(
    val playerName: String = "",
    val language: AppLanguage = AppLanguage.DEFAULT,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.DEFAULT,
    /** Epoch millis of the last explicit save. `0` means nothing has been saved yet. */
    val lastSavedEpochMs: Long = 0L,
) {
    val hasBeenSaved: Boolean
        get() = lastSavedEpochMs > 0L

    companion object {
        const val MAX_NAME_LENGTH = 24

        fun sanitizeName(raw: String): String = raw.trim().take(MAX_NAME_LENGTH).trimEnd()
    }
}
