package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.SettingsClickableItem
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSelectionDialog
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSwitchItem
import com.nkds.hosikoouma.jasmine.ui.theme.getTypography
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- ПРЕВЬЮ ШРИФТА ---
        PreviewCard(
            title = "App Font",
            icon = Icons.Rounded.TextFields,
            currentValue = settings.appFontFamily.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showFontDialog = true }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Jasmine Music Player",
                    style = getTypography(settings.appFontFamily).headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    style = getTypography(settings.appFontFamily).bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- ПРЕВЬЮ ПРОГРЕСС БАРА ---
        PreviewCard(
            title = "Progress Bar Style",
            icon = Icons.Rounded.Timeline,
            currentValue = settings.progressBarStyle.name.lowercase().replaceFirstChar { it.uppercase() },
            onClick = { showStyleDialog = true }
        ) {
            if (settings.progressBarStyle == ProgressBarStyle.STANDARD) {
                // В плеере используется обычный M3 Slider для стиля STANDARD
                Slider(
                    value = 0.6f,
                    onValueChange = {},
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                JasmineProgressBar(
                    value = 0.6f,
                    onValueChange = {},
                    onValueChangeFinished = {},
                    valueRange = 0f..1f,
                    style = settings.progressBarStyle,
                    isPlaying = true,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // --- ПРЕВЬЮ ПАЛИТРЫ (Цветовая схема) ---
        PreviewCard(
            title = "Palette Style",
            icon = Icons.Rounded.Palette,
            currentValue = settings.paletteStyle,
            onClick = { showPaletteDialog = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorCircle(MaterialTheme.colorScheme.primary, "Primary")
                ColorCircle(MaterialTheme.colorScheme.secondary, "Secondary")
                ColorCircle(MaterialTheme.colorScheme.tertiary, "Tertiary")
                ColorCircle(MaterialTheme.colorScheme.error, "Error")
            }
        }

        // --- ОСТАЛЬНЫЕ НАСТРОЙКИ ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "System Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
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
        }

        // Отступ снизу для доступа к пунктам под миниплеером
        Spacer(modifier = Modifier.height(180.dp))
    }

    // --- ДИАЛОГИ ВЫБОРА ---

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

@Composable
fun PreviewCard(
    title: String,
    icon: ImageVector,
    currentValue: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentValue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
fun ColorCircle(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
