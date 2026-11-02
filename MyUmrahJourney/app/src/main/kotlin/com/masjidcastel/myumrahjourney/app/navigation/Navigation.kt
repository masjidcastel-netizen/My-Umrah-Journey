package com.masjidcastel.myumrahjourney.app.navigation

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.masjidcastel.myumrahjourney.app.R
import com.masjidcastel.myumrahjourney.app.ui.screens.AboutScreen
import com.masjidcastel.myumrahjourney.app.ui.screens.HomeScreen
import com.masjidcastel.myumrahjourney.app.ui.screens.SettingsScreen
import com.masjidcastel.myumrahjourney.core.launch.GameLaunchContract
import com.masjidcastel.myumrahjourney.core.player.PlayerState
import com.masjidcastel.myumrahjourney.game.GameLauncherActivity

/** The three top-level destinations shown in the bottom navigation bar. */
sealed class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object Home : Destination("home", R.string.nav_home, Icons.Filled.Home)
    data object Settings : Destination("settings", R.string.nav_settings, Icons.Filled.Settings)
    data object About : Destination("about", R.string.nav_about, Icons.Filled.Info)

    companion object {
        // A getter, not a stored val: a val here is built while Destination itself is still
        // initialising, which can capture null for a subclass object that is mid-initialisation.
        val topLevel: List<Destination>
            get() = listOf(Home, Settings, About)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUmrahNavigation(
    player: PlayerState,
    onSaveSettings: (PlayerState) -> Unit,
    onResetSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Read through State so the NavHost graph builder below never captures a changing value;
    // otherwise every settings change would rebuild the navigation graph.
    val currentPlayer by rememberUpdatedState(player)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val titleRes = when (currentDestination?.route) {
        Destination.Settings.route -> R.string.nav_settings
        Destination.About.route -> R.string.nav_about
        else -> R.string.app_name
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(titleRes)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.topLevel.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(destination) },
                        icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) {
                val context = LocalContext.current
                HomeScreen(
                    player = currentPlayer,
                    onStartJourney = { launchGame(context, currentPlayer) },
                    onOpenSettings = { navController.navigateTopLevel(Destination.Settings) },
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    player = currentPlayer,
                    snackbarHostState = snackbarHostState,
                    onSave = onSaveSettings,
                    onReset = onResetSettings,
                )
            }
            composable(Destination.About.route) {
                AboutScreen()
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun launchGame(context: Context, player: PlayerState) {
    val intent = Intent(context, GameLauncherActivity::class.java)
    GameLaunchContract.writeTo(intent, player)
    context.startActivity(intent)
}
