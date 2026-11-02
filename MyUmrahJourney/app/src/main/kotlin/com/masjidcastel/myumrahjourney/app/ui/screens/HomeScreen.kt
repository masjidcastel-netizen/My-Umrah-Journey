package com.masjidcastel.myumrahjourney.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.masjidcastel.myumrahjourney.app.R
import com.masjidcastel.myumrahjourney.app.ui.components.ResponsiveColumn
import com.masjidcastel.myumrahjourney.app.ui.theme.MyUmrahColors
import com.masjidcastel.myumrahjourney.core.language.LanguageManager
import com.masjidcastel.myumrahjourney.core.player.PlayerState

@Composable
fun HomeScreen(
    player: PlayerState,
    onStartJourney: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResponsiveColumn(modifier = modifier) {
        HeroCard(player = player)

        Button(
            onClick = onStartJourney,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_start_journey),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = stringResource(R.string.home_start_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SavedSettingsCard(player = player)

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.home_open_settings))
        }
    }
}

@Composable
private fun HeroCard(player: PlayerState) {
    val greeting = if (player.playerName.isBlank()) {
        stringResource(R.string.home_greeting_anonymous)
    } else {
        stringResource(R.string.home_greeting_named, player.playerName)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(MyUmrahColors.HeroGradient))
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun SavedSettingsCard(player: PlayerState) {
    val notSet = stringResource(R.string.common_not_set)
    val on = stringResource(R.string.common_on)
    val off = stringResource(R.string.common_off)
    val never = stringResource(R.string.home_saved_never)

    val formattedLastSaved = remember(player.lastSavedEpochMs, player.language) {
        if (player.hasBeenSaved) {
            LanguageManager.formatTimestamp(player.lastSavedEpochMs, player.language)
        } else {
            ""
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_saved_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(
                label = stringResource(R.string.home_saved_name),
                value = player.playerName.ifBlank { notSet },
            )
            InfoRow(
                label = stringResource(R.string.home_saved_language),
                value = player.language.nativeName,
            )
            InfoRow(
                label = stringResource(R.string.home_saved_sound),
                value = if (player.soundEnabled) on else off,
            )
            InfoRow(
                label = stringResource(R.string.home_saved_music),
                value = if (player.musicEnabled) on else off,
            )
            InfoRow(
                label = stringResource(R.string.home_saved_last),
                value = if (player.hasBeenSaved) formattedLastSaved else never,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
