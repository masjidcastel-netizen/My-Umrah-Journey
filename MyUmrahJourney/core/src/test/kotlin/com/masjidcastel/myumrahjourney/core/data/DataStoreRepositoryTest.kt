package com.masjidcastel.myumrahjourney.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.masjidcastel.myumrahjourney.core.language.AppLanguage
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.core.player.PlayerState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "player_test.preferences_pb")
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun repository(
        now: Long = 1_700_000_000_000L,
        systemLanguage: AppLanguage = AppLanguage.ENGLISH,
    ) = DataStoreRepository(dataStore, systemLanguage = { systemLanguage }, clock = { now })

    @Test
    fun firstLaunchReturnsDefaultsWithSystemLanguage() = runBlocking<Unit> {
        val state = repository(systemLanguage = AppLanguage.FRENCH).playerState.first()

        assertEquals("", state.playerName)
        assertEquals(AppLanguage.FRENCH, state.language)
        assertTrue(state.soundEnabled)
        assertTrue(state.musicEnabled)
        assertEquals(GraphicsQuality.MEDIUM, state.graphicsQuality)
        assertFalse(state.hasBeenSaved)
    }

    @Test
    fun savedSettingsAreRestoredByAFreshRepository() = runBlocking<Unit> {
        val returned = repository().save(
            PlayerState(
                playerName = "  Ahmed  ",
                language = AppLanguage.ARABIC,
                soundEnabled = false,
                musicEnabled = true,
                graphicsQuality = GraphicsQuality.HIGH,
            ),
        )

        val restored = repository(systemLanguage = AppLanguage.ENGLISH).playerState.first()

        assertEquals("Ahmed", restored.playerName)
        assertEquals(AppLanguage.ARABIC, restored.language)
        assertFalse(restored.soundEnabled)
        assertTrue(restored.musicEnabled)
        assertEquals(GraphicsQuality.HIGH, restored.graphicsQuality)
        assertEquals(1_700_000_000_000L, restored.lastSavedEpochMs)
        assertEquals(returned, restored)
    }

    @Test
    fun savedLanguageWinsOverSystemLanguage() = runBlocking<Unit> {
        repository().save(PlayerState(language = AppLanguage.MAURITIAN_CREOLE))

        val restored = repository(systemLanguage = AppLanguage.FRENCH).playerState.first()

        assertEquals(AppLanguage.MAURITIAN_CREOLE, restored.language)
    }

    @Test
    fun longNamesAreTruncated() = runBlocking<Unit> {
        repository().save(PlayerState(playerName = "x".repeat(60)))

        val restored = repository().playerState.first()

        assertEquals(PlayerState.MAX_NAME_LENGTH, restored.playerName.length)
    }

    @Test
    fun clearReturnsToDefaults() = runBlocking<Unit> {
        val repo = repository(systemLanguage = AppLanguage.ENGLISH)
        repo.save(PlayerState(playerName = "Aisha", language = AppLanguage.FRENCH))

        repo.clear()
        val state = repo.playerState.first()

        assertEquals("", state.playerName)
        assertEquals(AppLanguage.ENGLISH, state.language)
        assertFalse(state.hasBeenSaved)
    }
}
