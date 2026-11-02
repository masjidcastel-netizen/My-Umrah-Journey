package com.masjidcastel.myumrahjourney.app

import android.app.Application
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masjidcastel.myumrahjourney.app.navigation.MyUmrahNavigation
import com.masjidcastel.myumrahjourney.app.ui.theme.MyUmrahTheme
import com.masjidcastel.myumrahjourney.app.viewmodel.JourneyUiState
import com.masjidcastel.myumrahjourney.app.viewmodel.JourneyViewModel
import com.masjidcastel.myumrahjourney.core.data.DataStoreRepository
import com.masjidcastel.myumrahjourney.core.language.AppLanguage
import com.masjidcastel.myumrahjourney.core.language.LanguageManager

/** Process-wide singletons. The repository wraps the one DataStore instance of this process. */
class MyUmrahJourneyApplication : Application() {
    val repository: DataStoreRepository by lazy { DataStoreRepository(this) }
}

/** Root composable: theme, then language/layout direction, then the navigation graph. */
@Composable
fun MyUmrahApp(
    viewModel: JourneyViewModel = viewModel(factory = JourneyViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyUmrahTheme {
        when (val state = uiState) {
            JourneyUiState.Loading -> LoadingScreen()
            is JourneyUiState.Ready -> LocalizedContent(language = state.player.language) {
                MyUmrahNavigation(
                    player = state.player,
                    onSaveSettings = { viewModel.saveSettings(it) },
                    onResetSettings = { viewModel.resetSettings() },
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Makes everything below resolve strings in [language] and lay out left-to-right or
 * right-to-left (Arabic). Changing the language recomposes the UI in place, without
 * restarting the Activity, so the user stays on the current screen.
 */
@Composable
private fun LocalizedContent(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, baseConfiguration, language) {
        LanguageManager.localizedContext(baseContext, language)
    }
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Keep the window's own View direction in sync too (popups, dialogs, IME).
    val rootView = LocalView.current
    SideEffect {
        val wanted = if (language.isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        if (rootView.layoutDirection != wanted) rootView.layoutDirection = wanted
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides layoutDirection,
        content = content,
    )
}
