package com.masjidcastel.myumrahjourney.core.launch

import android.content.Intent
import com.masjidcastel.myumrahjourney.core.language.AppLanguage
import com.masjidcastel.myumrahjourney.core.language.LanguageManager
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.core.player.PlayerState

/**
 * The only data the `app` module hands to the `game` module: a snapshot of the player's
 * settings, carried as Intent extras. Both sides use this object so the keys cannot drift.
 */
object GameLaunchContract {
    const val EXTRA_PLAYER_NAME = "com.masjidcastel.myumrahjourney.extra.PLAYER_NAME"
    const val EXTRA_LANGUAGE_CODE = "com.masjidcastel.myumrahjourney.extra.LANGUAGE_CODE"
    const val EXTRA_SOUND_ENABLED = "com.masjidcastel.myumrahjourney.extra.SOUND_ENABLED"
    const val EXTRA_MUSIC_ENABLED = "com.masjidcastel.myumrahjourney.extra.MUSIC_ENABLED"
    const val EXTRA_GRAPHICS_QUALITY = "com.masjidcastel.myumrahjourney.extra.GRAPHICS_QUALITY"

    fun writeTo(intent: Intent, state: PlayerState): Intent = intent.apply {
        putExtra(EXTRA_PLAYER_NAME, state.playerName)
        putExtra(EXTRA_LANGUAGE_CODE, state.language.code)
        putExtra(EXTRA_SOUND_ENABLED, state.soundEnabled)
        putExtra(EXTRA_MUSIC_ENABLED, state.musicEnabled)
        putExtra(EXTRA_GRAPHICS_QUALITY, state.graphicsQuality.code)
    }

    fun readFrom(intent: Intent?): PlayerState = PlayerState(
        playerName = intent?.getStringExtra(EXTRA_PLAYER_NAME).orEmpty(),
        language = AppLanguage.fromCode(intent?.getStringExtra(EXTRA_LANGUAGE_CODE))
            ?: LanguageManager.resolveSystemDefault(),
        soundEnabled = intent?.getBooleanExtra(EXTRA_SOUND_ENABLED, true) ?: true,
        musicEnabled = intent?.getBooleanExtra(EXTRA_MUSIC_ENABLED, true) ?: true,
        graphicsQuality = GraphicsQuality.fromCode(intent?.getStringExtra(EXTRA_GRAPHICS_QUALITY))
            ?: GraphicsQuality.DEFAULT,
    )
}
