package com.masjidcastel.myumrahjourney.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masjidcastel.myumrahjourney.core.language.AppLanguage
import com.masjidcastel.myumrahjourney.core.language.LanguageManager
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.core.player.PlayerState
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "player_state"

// One DataStore instance per process, as DataStore requires.
private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

/**
 * Saves and restores [PlayerState] with Preferences DataStore.
 *
 * The primary constructor takes the [DataStore] directly so it can be unit tested on the JVM;
 * the `Context` constructor is what the app uses.
 */
class DataStoreRepository(
    private val dataStore: DataStore<Preferences>,
    private val systemLanguage: () -> AppLanguage = { LanguageManager.resolveSystemDefault() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    constructor(context: Context) : this(dataStore = context.applicationContext.playerDataStore)

    /** Emits the stored state, and again after every change. Defaults when nothing is stored. */
    val playerState: Flow<PlayerState> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences.toPlayerState() }

    /** Persists [state] and returns what was actually stored (sanitized name, save time). */
    suspend fun save(state: PlayerState): PlayerState {
        val stored = state.copy(
            playerName = PlayerState.sanitizeName(state.playerName),
            lastSavedEpochMs = clock(),
        )
        dataStore.edit { preferences ->
            preferences[Keys.PLAYER_NAME] = stored.playerName
            preferences[Keys.LANGUAGE] = stored.language.code
            preferences[Keys.SOUND_ENABLED] = stored.soundEnabled
            preferences[Keys.MUSIC_ENABLED] = stored.musicEnabled
            preferences[Keys.GRAPHICS_QUALITY] = stored.graphicsQuality.code
            preferences[Keys.LAST_SAVED] = stored.lastSavedEpochMs
        }
        return stored
    }

    /** Changes only the graphics preset (used from inside the game) without touching the saved time. */
    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        dataStore.edit { preferences -> preferences[Keys.GRAPHICS_QUALITY] = quality.code }
    }

    /** Forgets everything; [playerState] returns to its defaults. */
    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private fun Preferences.toPlayerState(): PlayerState = PlayerState(
        playerName = this[Keys.PLAYER_NAME].orEmpty(),
        language = AppLanguage.fromCode(this[Keys.LANGUAGE]) ?: systemLanguage(),
        soundEnabled = this[Keys.SOUND_ENABLED] ?: true,
        musicEnabled = this[Keys.MUSIC_ENABLED] ?: true,
        graphicsQuality = GraphicsQuality.fromCode(this[Keys.GRAPHICS_QUALITY]) ?: GraphicsQuality.DEFAULT,
        lastSavedEpochMs = this[Keys.LAST_SAVED] ?: 0L,
    )

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val LAST_SAVED = longPreferencesKey("last_saved_epoch_ms")
    }
}
