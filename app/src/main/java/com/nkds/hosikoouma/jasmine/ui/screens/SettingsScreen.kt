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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.viewmodels.AppFontFamily
import com.nkds.hosikoouma.jasmine.viewmodels.DarkMode
import com.nkds.hosikoouma.jasmine.viewmodels.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsState()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsState()
    val minTrackDuration by viewModel.minTrackDuration.collectAsState()
    val defaultSortType by viewModel.defaultSortType.collectAsState()
    val isDefaultSortReversed by viewModel.isDefaultSortReversed.collectAsState()
    val progressBarStyle by viewModel.progressBarStyle.collectAsState()
    val appFontFamily by viewModel.appFontFamily.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    
    val paletteStyle by viewModel.paletteStyle.collectAsState()
    val amoledDarkMode by viewModel.amoledDarkMode.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val useAlbumArtColor by viewModel.useAlbumArtColor.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- PLAYBACK ---
        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Crossfade") },
            supportingContent = { Text("Smoothly transition between tracks") },
            trailingContent = {
                Switch(
                    checked = isCrossfadeEnabled,
                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                )
            }
        )

        if (isCrossfadeEnabled) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration", style = MaterialTheme.typography.bodyMedium)
                    Text("${(crossfadeDuration / 1000f)}s", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = crossfadeDuration.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.roundToLong()) },
                    valueRange = 1000f..10000f,
                    steps = 8
                )
            }
        }

        ListItem(
            headlineContent = { Text("Player Progress Style") },
            supportingContent = { 
                val styleLabel = when(progressBarStyle) {
                    "WAVE" -> "Wave Visualizer"
                    "NEON" -> "Neon Glow"
                    "DOTTED" -> "Dotted Line"
                    "SOLID" -> "Solid Thick"
                    else -> "Standard Slider"
                }
                Text(styleLabel)
            },
            modifier = Modifier.clickable { showStyleDialog = true }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- APPEARANCE ---
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Theme Mode") },
            supportingContent = { 
                val themeLabel = when(darkMode) {
                    "DARK" -> "Dark"
                    "LIGHT" -> "Light"
                    else -> "Follow System"
                }
                Text(themeLabel)
            },
            modifier = Modifier.clickable { showThemeDialog = true }
        )

        ListItem(
            headlineContent = { Text("AMOLED Dark Mode") },
            supportingContent = { Text("Pure black background in dark theme") },
            trailingContent = {
                Switch(
                    checked = amoledDarkMode,
                    onCheckedChange = { viewModel.setAmoledDarkMode(it) }
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text("Dynamic Colors (Material You)") },
                supportingContent = { Text("Use system accent colors") },
                trailingContent = {
                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = { viewModel.setUseDynamicColor(it) }
                    )
                }
            )
        }

        ListItem(
            headlineContent = { Text("Use Album Art Color") },
            supportingContent = { Text("Generate theme from current track cover") },
            trailingContent = {
                Switch(
                    checked = useAlbumArtColor,
                    onCheckedChange = { viewModel.setUseAlbumArtColor(it) }
                )
            }
        )

        if (!useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text("Palette Style") },
                supportingContent = { Text(paletteStyle) },
                modifier = Modifier.clickable { showPaletteDialog = true }
            )
        }

        ListItem(
            headlineContent = { Text("App Font") },
            supportingContent = { 
                val fontLabel = when(appFontFamily) {
                    "GOOGLE_SANS" -> "Google Sans"
                    "JETBRAINS_MONO" -> "JetBrains Mono Nerd"
                    else -> "System Default"
                }
                Text(fontLabel)
            },
            modifier = Modifier.clickable { showFontDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- LIBRARY ---
        Text(
            text = "Library",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Filter short tracks") },
            supportingContent = { Text("Hide tracks shorter than ${minTrackDuration}s") }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = minTrackDuration.toFloat(),
                onValueChange = { viewModel.setMinTrackDuration(it.roundToInt()) },
                valueRange = 0f..30f,
                steps = 29
            )
        }

        ListItem(
            headlineContent = { Text("Default Sorting") },
            supportingContent = { 
                val sortLabel = when(defaultSortType) {
                    "BY_NAME" -> "Name"
                    "BY_ARTIST" -> "Artist"
                    "BY_DURATION" -> "Duration"
                    else -> "Date Added"
                }
                Text("Currently sorted by $sortLabel ${if(isDefaultSortReversed) "(Reversed)" else ""}")
            },
            modifier = Modifier.clickable { showSortDialog = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0.0 (Jasmine)") }
        )

        Spacer(modifier = Modifier.height(160.dp))
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default Sorting") },
            text = {
                Column {
                    SortOption("By Name", "BY_NAME", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Artist", "BY_ARTIST", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Date Added", "BY_DATE", defaultSortType) { viewModel.setDefaultSortType(it) }
                    SortOption("By Duration", "BY_DURATION", defaultSortType) { viewModel.setDefaultSortType(it) }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDefaultSortReversed(!isDefaultSortReversed) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = isDefaultSortReversed, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reverse order by default")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Done") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme Mode") },
            text = {
                Column {
                    ThemeOption("Follow System", DarkMode.FOLLOW_SYSTEM, darkMode) { 
                        viewModel.setDarkMode(it) 
                    }
                    ThemeOption("Light", DarkMode.LIGHT, darkMode) { 
                        viewModel.setDarkMode(it) 
                    }
                    ThemeOption("Dark", DarkMode.DARK, darkMode) { 
                        viewModel.setDarkMode(it) 
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Done") }
            }
        )
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = { Text("Progress Bar Style") },
            text = {
                Column {
                    StyleOption("Standard Slider", ProgressBarStyle.STANDARD, progressBarStyle) { 
                        viewModel.setProgressBarStyle(it) 
                    }
                    StyleOption("Solid Thick", ProgressBarStyle.SOLID, progressBarStyle) { 
                        viewModel.setProgressBarStyle(it) 
                    }
                    StyleOption("Dotted Line", ProgressBarStyle.DOTTED, progressBarStyle) { 
                        viewModel.setProgressBarStyle(it) 
                    }
                    StyleOption("Wave Visualizer", ProgressBarStyle.WAVE, progressBarStyle) { 
                        viewModel.setProgressBarStyle(it) 
                    }
                    StyleOption("Neon Glow", ProgressBarStyle.NEON, progressBarStyle) { 
                        viewModel.setProgressBarStyle(it) 
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleDialog = false }) { Text("Done") }
            }
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("App Font") },
            text = {
                Column {
                    FontOption("System Default", AppFontFamily.DEFAULT, appFontFamily) { 
                        viewModel.setAppFontFamily(it) 
                    }
                    FontOption("Google Sans", AppFontFamily.GOOGLE_SANS, appFontFamily) { 
                        viewModel.setAppFontFamily(it) 
                    }
                    FontOption("JetBrains Mono Nerd", AppFontFamily.JETBRAINS_MONO, appFontFamily) { 
                        viewModel.setAppFontFamily(it) 
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Done") }
            }
        )
    }

    if (showPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = { Text("Palette Style") },
            text = {
                val styles = listOf(
                    "TonalSpot", "Neutral", "Vibrant", "Expressive", 
                    "Rainbow", "FruitSalad", "Monochrome", "Fidelity", "Content"
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    styles.forEach { style ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPaletteStyle(style) }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = style == paletteStyle, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaletteDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun SortOption(label: String, value: String, currentValue: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 8.dp)) {
        RadioButton(selected = value == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(8.dp)); Text(label)
    }
}

@Composable
fun ThemeOption(label: String, value: DarkMode, currentValue: String, onSelect: (DarkMode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 8.dp)) {
        RadioButton(selected = value.name == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(8.dp)); Text(label)
    }
}

@Composable
fun StyleOption(label: String, value: ProgressBarStyle, currentValue: String, onSelect: (ProgressBarStyle) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 8.dp)) {
        RadioButton(selected = value.name == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(8.dp)); Text(label)
    }
}

@Composable
fun FontOption(label: String, value: AppFontFamily, currentValue: String, onSelect: (AppFontFamily) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 8.dp)) {
        RadioButton(selected = value.name == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(8.dp)); Text(label)
    }
}
