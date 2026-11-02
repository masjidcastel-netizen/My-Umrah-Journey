package com.masjidcastel.myumrahjourney.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.masjidcastel.myumrahjourney.app.R
import com.masjidcastel.myumrahjourney.app.ui.components.ResponsiveColumn
import com.masjidcastel.myumrahjourney.core.language.AppLanguage
import com.masjidcastel.myumrahjourney.core.player.GraphicsQuality
import com.masjidcastel.myumrahjourney.core.player.PlayerState

/**
 * Player settings. Edits are kept as a draft until "Save settings" is pressed; saving writes
 * to DataStore, and the stored value flowing back is what the rest of the app (including the
 * UI language) reacts to.
 */
@Composable
fun SettingsScreen(
    player: PlayerState,
    snackbarHostState: SnackbarHostState,
    onSave: (PlayerState) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Draft state. Keys make the draft re-initialise whenever the stored value changes.
    var name by rememberSaveable(player.playerName) { mutableStateOf(player.playerName) }
    var languageCode by rememberSaveable(player.language.code) { mutableStateOf(player.language.code) }
    var soundEnabled by rememberSaveable(player.soundEnabled) { mutableStateOf(player.soundEnabled) }
    var musicEnabled by rememberSaveable(player.musicEnabled) { mutableStateOf(player.musicEnabled) }
    var qualityCode by rememberSaveable(player.graphicsQuality.code) { mutableStateOf(player.graphicsQuality.code) }

    val draftLanguage = AppLanguage.fromCode(languageCode) ?: player.language
    val draftQuality = GraphicsQuality.fromCode(qualityCode) ?: player.graphicsQuality
    val draftName = PlayerState.sanitizeName(name)
    val hasChanges = draftName != player.playerName ||
        draftLanguage != player.language ||
        soundEnabled != player.soundEnabled ||
        musicEnabled != player.musicEnabled ||
        draftQuality != player.graphicsQuality
    val canSave = hasChanges || !player.hasBeenSaved

    // Confirmation message: shown when the *stored* state changes, so it is already in the
    // newly saved language.
    val savedMessage = stringResource(R.string.settings_saved)
    val resetMessage = stringResource(R.string.settings_reset_done)
    var lastSeenSave by remember { mutableStateOf(player.lastSavedEpochMs) }
    LaunchedEffect(player.lastSavedEpochMs) {
        if (player.lastSavedEpochMs != lastSeenSave) {
            lastSeenSave = player.lastSavedEpochMs
            snackbarHostState.showSnackbar(
                if (player.lastSavedEpochMs > 0L) savedMessage else resetMessage,
            )
        }
    }

    val focusManager = LocalFocusManager.current

    ResponsiveColumn(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PlayerState.MAX_NAME_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.settings_name_label)) },
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.settings_name_support,
                                PlayerState.MAX_NAME_LENGTH,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                SectionTitle(text = stringResource(R.string.settings_language_title))
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { option ->
                        val selected = option == draftLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    onClick = { languageCode = option.code },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option.nativeName,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                SectionTitle(text = stringResource(R.string.settings_audio_title))
                SwitchRow(
                    label = stringResource(R.string.settings_sound),
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                )
                SwitchRow(
                    label = stringResource(R.string.settings_music),
                    checked = musicEnabled,
                    onCheckedChange = { musicEnabled = it },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                SectionTitle(text = stringResource(R.string.settings_graphics_title))
                Text(
                    text = stringResource(R.string.settings_graphics_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Column(modifier = Modifier.selectableGroup()) {
                    GraphicsQuality.entries.forEach { option ->
                        val selected = option == draftQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    onClick = { qualityCode = option.code },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(qualityLabel(option)),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }

        if (hasChanges && player.hasBeenSaved) {
            Text(
                text = stringResource(R.string.settings_unsaved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                onSave(
                    PlayerState(
                        playerName = draftName,
                        language = draftLanguage,
                        soundEnabled = soundEnabled,
                        musicEnabled = musicEnabled,
                        graphicsQuality = draftQuality,
                    ),
                )
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(text = stringResource(R.string.settings_save))
        }

        OutlinedButton(
            onClick = {
                focusManager.clearFocus()
                onReset()
            },
            enabled = player.hasBeenSaved,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.settings_reset))
        }
    }
}

@StringRes
private fun qualityLabel(quality: GraphicsQuality): Int = when (quality) {
    GraphicsQuality.LOW -> R.string.graphics_low
    GraphicsQuality.MEDIUM -> R.string.graphics_medium
    GraphicsQuality.HIGH -> R.string.graphics_high
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}
