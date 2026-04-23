package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.ui.components.JasmineProgressBar
import com.nkds.hosikoouma.jasmine.ui.components.SettingsClickableItem
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSelectionDialog
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSwitchItem
import com.nkds.hosikoouma.jasmine.ui.theme.getTypography
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsState
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
        onSetProgressBarStyle = viewModel::setProgressBarStyle,
        onUpdateNavigationItems = viewModel::setNavigationItems
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
    onSetProgressBarStyle: (ProgressBarStyle) -> Unit,
    onUpdateNavigationItems: (List<String>) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showNavDialog by remember { mutableStateOf(false) }

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

        // --- ПРЕВЬЮ ПАЛИТРЫ ---
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

        // --- НАСТРОЙКИ НАВИГАЦИИ ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Navigation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            SettingsClickableItem(
                title = "Navigation Bar Items",
                subtitle = "Long press and drag cards to reorder",
                onClick = { showNavDialog = true }
            )
        }

        // --- ТЕМА ---
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

        Spacer(modifier = Modifier.height(180.dp))
    }

    // --- ДИАЛОГИ ---

    if (showNavDialog) {
        NavigationItemsDialog(
            currentItems = settings.navigationItems,
            onDismiss = { showNavDialog = false },
            onSave = onUpdateNavigationItems
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

@Composable
fun NavigationItemsDialog(
    currentItems: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val allPossibleItems = Screen.allMainItems
    var itemsState by remember { 
        mutableStateOf(
            allPossibleItems.map { screen ->
                screen to currentItems.contains(screen.route)
            }.sortedBy { (screen, _) -> 
                val index = currentItems.indexOf(screen.route)
                if (index != -1) index else 100 
            }
        )
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        itemsState = itemsState.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Navigation Bar Items") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Long press and drag cards to reorder. Settings cannot be hidden. Min 2 items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(itemsState, key = { _, (screen, _) -> screen.route }) { index, (screen, isVisible) ->
                        ReorderableItem(reorderableState, key = screen.route) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                            val isSettings = screen.route == "settings"
                            val visibleCount = itemsState.count { it.second }
                            
                            val cardColor by animateColorAsState(
                                targetValue = if (isVisible) MaterialTheme.colorScheme.surfaceVariant 
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                label = "cardColor"
                            )

                            // Обертка для устранения эффекта "тени под тенью" и корректного перетаскивания
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .draggableHandle() // Теперь вся карточка - область для захвата
                                    .graphicsLayer { 
                                        shadowElevation = elevation.toPx()
                                        shape = RoundedCornerShape(20.dp)
                                        clip = true
                                    }
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = screen.icon, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isVisible) MaterialTheme.colorScheme.onSurface 
                                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        
                                        Spacer(Modifier.width(16.dp))
                                        
                                        Text(
                                            text = screen.title, 
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isVisible) MaterialTheme.colorScheme.onSurface 
                                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )

                                        Switch(
                                            checked = isVisible,
                                            enabled = !isSettings && (!isVisible || visibleCount > 2),
                                            onCheckedChange = { checked ->
                                                itemsState = itemsState.toMutableList().apply {
                                                    this[index] = screen to checked
                                                }
                                            },
                                            thumbContent = if (isVisible) {
                                                { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRoutes = itemsState
                        .filter { it.second }
                        .map { it.first.route }
                    onSave(finalRoutes)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
