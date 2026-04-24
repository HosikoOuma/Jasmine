package com.nkds.hosikoouma.jasmine.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSliderItem
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsState
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlin.math.roundToLong

@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    PlaybackSettingsContent(
        settings = settings,
        onSetCrossfadeEnabled = viewModel::setCrossfadeEnabled,
        onSetCrossfadeDuration = viewModel::setCrossfadeDuration,
        onSetManageAudioFocus = viewModel::setManageAudioFocus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsContent(
    settings: SettingsState,
    onSetCrossfadeEnabled: (Boolean) -> Unit,
    onSetCrossfadeDuration: (Long) -> Unit,
    onSetManageAudioFocus: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ListItem(
            headlineContent = { Text("Crossfade") },
            supportingContent = { Text("Smoothly transition between tracks") },
            trailingContent = {
                Switch(
                    checked = settings.isCrossfadeEnabled,
                    onCheckedChange = onSetCrossfadeEnabled
                )
            }
        )

        if (settings.isCrossfadeEnabled) {
            SettingsSliderItem(
                label = "Duration",
                value = settings.crossfadeDuration.toFloat(),
                valueRange = 1000f..10000f,
                steps = 8,
                displayValue = "${(settings.crossfadeDuration / 1000f)}s",
                onValueChange = { onSetCrossfadeDuration(it.roundToLong()) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ListItem(
            headlineContent = { Text("Manage Audio Focus") },
            supportingContent = { Text("Pause playback when another app plays sound (calls, notifications, etc.)") },
            trailingContent = {
                Switch(
                    checked = settings.manageAudioFocus,
                    onCheckedChange = onSetManageAudioFocus
                )
            }
        )
    }
}
