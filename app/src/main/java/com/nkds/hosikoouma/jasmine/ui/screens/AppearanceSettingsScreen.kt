package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.ui.components.SettingsClickableItem
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSelectionDialog
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSwitchItem
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsState
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel

@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    AppearanceSettingsContent(
        settings = settings,
        onSetDarkMode = viewModel::setDarkMode,
        onSetAmoledMode = viewModel::setAmoledDarkMode,
        onSetUseDynamicColor = viewModel::setUseDynamicColor,
        onSetUseAlbumArtColor = viewModel::setUseAlbumArtColor,
        onSetPaletteStyle = viewModel::setPaletteStyle,
        onSetAppFontFamily = viewModel::setAppFontFamily,
        onSetProgressBarStyle = viewModel::setProgressBarStyle
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(
    settings: SettingsState,
    onSetDarkMode: (DarkMode) -> Unit,
    onSetAmoledMode: (Boolean) -> Unit,
    onSetUseDynamicColor: (Boolean) -> Unit,
    onSetUseAlbumArtColor: (Boolean) -> Unit,
    onSetPaletteStyle: (String) -> Unit,
    onSetAppFontFamily: (AppFontFamily) -> Unit,
    onSetProgressBarStyle: (ProgressBarStyle) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsClickableItem(
            title = "Theme Mode",
            subtitle = settings.darkMode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showThemeDialog = true }
        )

        SettingsSwitchItem(
            title = "AMOLED Dark Mode",
            subtitle = "Pure black background in dark theme",
            checked = settings.amoledDarkMode,
            onCheckedChange = onSetAmoledMode
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsSwitchItem(
                title = "Dynamic Colors (Material You)",
                subtitle = "Use system accent colors",
                checked = settings.useDynamicColor,
                onCheckedChange = onSetUseDynamicColor
            )
        }

        SettingsSwitchItem(
            title = "Use Album Art Color",
            subtitle = "Generate theme from current track cover",
            checked = settings.useAlbumArtColor,
            onCheckedChange = onSetUseAlbumArtColor
        )

        if (!settings.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            SettingsClickableItem(
                title = "Palette Style",
                subtitle = settings.paletteStyle,
                onClick = { showPaletteDialog = true }
            )
        }

        SettingsClickableItem(
            title = "App Font",
            subtitle = settings.appFontFamily.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showFontDialog = true }
        )

        SettingsClickableItem(
            title = "Player Progress Style",
            subtitle = settings.progressBarStyle.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showStyleDialog = true }
        )
    }

    if (showThemeDialog) {
        SettingsSelectionDialog(
            title = "Theme Mode",
            options = DarkMode.entries.toTypedArray(),
            selectedOption = settings.darkMode,
            onOptionSelected = onSetDarkMode,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFontDialog) {
        SettingsSelectionDialog(
            title = "App Font",
            options = AppFontFamily.entries.toTypedArray(),
            selectedOption = settings.appFontFamily,
            onOptionSelected = onSetAppFontFamily,
            onDismiss = { showFontDialog = false }
        )
    }

    if (showStyleDialog) {
        SettingsSelectionDialog(
            title = "Progress Bar Style",
            options = ProgressBarStyle.entries.toTypedArray(),
            selectedOption = settings.progressBarStyle,
            onOptionSelected = onSetProgressBarStyle,
            onDismiss = { showStyleDialog = false }
        )
    }

    if (showPaletteDialog) {
        val styles = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive", "Rainbow", "FruitSalad", "Monochrome", "Fidelity", "Content")
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = { Text("Palette Style") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    styles.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetPaletteStyle(style) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = style == settings.paletteStyle, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPaletteDialog = false }) { Text("Done") } }
        )
    }
}
