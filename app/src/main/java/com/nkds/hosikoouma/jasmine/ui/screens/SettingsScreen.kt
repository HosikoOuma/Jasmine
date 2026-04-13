package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.core.models.*
import com.nkds.hosikoouma.jasmine.datamodels.Folder
import com.nkds.hosikoouma.jasmine.ui.theme.JasmineTheme
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsState
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    trackViewModel: TrackViewModel
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val folders by trackViewModel.folders.collectAsStateWithLifecycle()
    // Исправлено: используем .value напрямую, чтобы избежать проблем с выводом типов
    val blacklistedFoldersState = trackViewModel.blacklistedFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val blacklistedFolders = blacklistedFoldersState.value

    SettingsContent(
        settings = settings,
        folders = folders,
        blacklistedFolders = blacklistedFolders,
        onSetCrossfadeEnabled = viewModel::setCrossfadeEnabled,
        onSetCrossfadeDuration = viewModel::setCrossfadeDuration,
        onSetProgressBarStyle = viewModel::setProgressBarStyle,
        onSetDarkMode = viewModel::setDarkMode,
        onSetAmoledMode = viewModel::setAmoledDarkMode,
        onSetUseDynamicColor = viewModel::setUseDynamicColor,
        onSetUseAlbumArtColor = viewModel::setUseAlbumArtColor,
        onSetPaletteStyle = viewModel::setPaletteStyle,
        onSetAppFontFamily = viewModel::setAppFontFamily,
        onSetMinTrackDuration = viewModel::setMinTrackDuration,
        onSetDefaultSortType = viewModel::setDefaultSortType,
        onSetDefaultSortReversed = viewModel::setDefaultSortReversed,
        onToggleFolderBlacklist = { path ->
            if (blacklistedFolders.contains(path)) trackViewModel.removeFolderFromBlacklist(path)
            else trackViewModel.addFolderToBlacklist(path)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: SettingsState,
    folders: List<Folder> = emptyList(),
    blacklistedFolders: Set<String> = emptySet(),
    onSetCrossfadeEnabled: (Boolean) -> Unit,
    onSetCrossfadeDuration: (Long) -> Unit,
    onSetProgressBarStyle: (ProgressBarStyle) -> Unit,
    onSetDarkMode: (DarkMode) -> Unit,
    onSetAmoledMode: (Boolean) -> Unit,
    onSetUseDynamicColor: (Boolean) -> Unit,
    onSetUseAlbumArtColor: (Boolean) -> Unit,
    onSetPaletteStyle: (String) -> Unit,
    onSetAppFontFamily: (AppFontFamily) -> Unit,
    onSetMinTrackDuration: (Int) -> Unit,
    onSetDefaultSortType: (SortType) -> Unit,
    onSetDefaultSortReversed: (Boolean) -> Unit,
    onToggleFolderBlacklist: (String) -> Unit
) {
    var showStyleDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSectionTitle("Playback")
        ListItem(
            headlineContent = { Text("Crossfade") },
            supportingContent = { Text("Smoothly transition between tracks") },
            trailingContent = { Switch(checked = settings.isCrossfadeEnabled, onCheckedChange = onSetCrossfadeEnabled) }
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
        SettingsClickableItem(
            title = "Player Progress Style",
            subtitle = settings.progressBarStyle.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showStyleDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        SettingsSectionTitle("Appearance")
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
            SettingsClickableItem(title = "Palette Style", subtitle = settings.paletteStyle, onClick = { showPaletteDialog = true })
        }
        SettingsClickableItem(
            title = "App Font",
            subtitle = settings.appFontFamily.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showFontDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        SettingsSectionTitle("Library")
        SettingsSliderItem(
            label = "Filter short tracks",
            value = settings.minTrackDuration.toFloat(),
            valueRange = 0f..30f,
            steps = 29,
            displayValue = "${settings.minTrackDuration}s",
            onValueChange = { onSetMinTrackDuration(it.roundToInt()) }
        )
        SettingsClickableItem(
            title = "Default Sorting",
            subtitle = "By ${settings.defaultSortType.name.substringAfter("BY_").lowercase()} ${if(settings.isDefaultSortReversed) "(Reversed)" else ""}",
            onClick = { showSortDialog = true }
        )
        SettingsClickableItem(
            title = "Blacklisted Folders",
            subtitle = "Manage excluded music folders",
            onClick = { showBlacklistDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        SettingsSectionTitle("About")
        ListItem(headlineContent = { Text("Version") }, supportingContent = { Text("1.0.0 (Jasmine)") })
        Spacer(modifier = Modifier.height(160.dp))
    }

    // --- Dialogs ---
    if (showStyleDialog) {
        SettingsSelectionDialog(
            title = "Progress Bar Style",
            options = ProgressBarStyle.entries.toTypedArray(),
            selectedOption = settings.progressBarStyle,
            onOptionSelected = onSetProgressBarStyle,
            onDismiss = { showStyleDialog = false }
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
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default Sorting") },
            text = {
                Column {
                    SortType.entries.forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSetDefaultSortType(type) }.padding(vertical = 8.dp)) {
                            RadioButton(selected = settings.defaultSortType == type, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name.substringAfter("BY_").lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSetDefaultSortReversed(!settings.isDefaultSortReversed) }.padding(vertical = 8.dp)) {
                        Checkbox(checked = settings.isDefaultSortReversed, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reverse order by default")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSortDialog = false }) { Text("Done") } }
        )
    }
    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text("Blacklisted Folders") },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    if (folders.isEmpty()) Text("No folders found")
                    folders.forEach { folder ->
                        val isBlacklisted = blacklistedFolders.contains(folder.path)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggleFolderBlacklist(folder.path) }.padding(vertical = 12.dp)) {
                            Icon(imageVector = if (isBlacklisted) Icons.Rounded.Block else Icons.Rounded.Folder, contentDescription = null, tint = if (isBlacklisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isBlacklisted) FontWeight.Normal else FontWeight.Bold, color = if (isBlacklisted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                Text(text = folder.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(checked = isBlacklisted, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlacklistDialog = false }) { Text("Done") } }
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSetPaletteStyle(style) }.padding(vertical = 8.dp)) {
                            RadioButton(selected = style == settings.paletteStyle, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp)); Text(style)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPaletteDialog = false }) { Text("Done") } }
        )
    }
}

// --- Internal Helpers ---

@Composable
fun SettingsSectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun SettingsClickableItem(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, modifier = Modifier.clickable(onClick = onClick))
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) })
}

@Composable
fun SettingsSliderItem(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, steps: Int, displayValue: String, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(displayValue, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
fun <T : Enum<T>> SettingsSelectionDialog(title: String, options: Array<T>, selectedOption: T, onOptionSelected: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onOptionSelected(option) }.padding(vertical = 8.dp)) {
                        RadioButton(selected = option == selectedOption, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    JasmineTheme {
        SettingsContent(
            settings = SettingsState(),
            onSetCrossfadeEnabled = {},
            onSetCrossfadeDuration = {},
            onSetProgressBarStyle = {},
            onSetDarkMode = {},
            onSetAmoledMode = {},
            onSetUseDynamicColor = {},
            onSetUseAlbumArtColor = {},
            onSetPaletteStyle = {},
            onSetAppFontFamily = {},
            onSetMinTrackDuration = {},
            onSetDefaultSortType = {},
            onSetDefaultSortReversed = {},
            onToggleFolderBlacklist = {}
        )
    }
}
