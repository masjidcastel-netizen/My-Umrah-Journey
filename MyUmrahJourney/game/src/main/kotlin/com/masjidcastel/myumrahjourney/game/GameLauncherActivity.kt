package com.masjidcastel.myumrahjourney.game

import android.os.Bundle
import android.widget.Toast
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.masjidcastel.myumrahjourney.core.data.DataStoreRepository
import com.masjidcastel.myumrahjourney.core.language.LanguageManager
import com.masjidcastel.myumrahjourney.core.launch.GameLaunchContract
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.game.render.QualityProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Entry point of the libGDX module. The Compose app starts it with the player's settings in the
 * Intent (see [GameLaunchContract]); it hosts [DrivingGame] on a real OpenGL ES surface.
 * Finishing this Activity (Back -> pause -> Exit, or the pause menu's exit button) returns to the app.
 */
class GameLauncherActivity : AndroidApplication(), GameHost {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val player = GameLaunchContract.readFrom(intent)
        val profile = QualityProfile.of(player.graphicsQuality)

        val configuration = AndroidApplicationConfiguration().apply {
            r = 8
            g = 8
            b = 8
            a = 8
            depth = 24 // 16-bit depth causes road markings to flicker at distance
            stencil = 0
            numSamples = profile.msaaSamples
            useAccelerometer = false
            useCompass = false
            useImmersiveMode = true
            useWakelock = true
        }
        initialize(DrivingGame(player, this), configuration)

        if (savedInstanceState == null) {
            val localized = LanguageManager.localizedContext(this, player.language)
            Toast.makeText(this, localized.getString(R.string.game_controls_hint), Toast.LENGTH_LONG).show()
        }
    }

    /** Called from the render thread when the pause menu changes the preset. */
    override fun onQualityChanged(quality: GraphicsQuality) {
        val repository = DataStoreRepository(applicationContext)
        persistScope.launch { repository.setGraphicsQuality(quality) }
    }

    private companion object {
        /** Process-wide so a change made just before exiting is still written after this Activity is gone. */
        val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
