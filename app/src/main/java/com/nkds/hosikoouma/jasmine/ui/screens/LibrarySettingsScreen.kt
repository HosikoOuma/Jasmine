package com.nkds.hosikoouma.jasmine.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.datamodels.Folder
import com.nkds.hosikoouma.jasmine.ui.components.SettingsClickableItem
import com.nkds.hosikoouma.jasmine.ui.components.SettingsSliderItem
import com.nkds.hosikoouma.jasmine.viewmodels.SettingsViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlin.math.roundToInt

@Composable
fun LibrarySettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    trackViewModel: TrackViewModel
) {
    val settings by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val folders by trackViewModel.allFolders.collectAsStateWithLifecycle()
    val blacklistedFolders by trackViewModel.blacklistedFolders.collectAsStateWithLifecycle()

    LibrarySettingsContent(
        minTrackDuration = settings.minTrackDuration,
        defaultSortType = settings.defaultSortType,
        isDefaultSortReversed = settings.isDefaultSortReversed,
        folders = folders,
        blacklistedFolders = blacklistedFolders,
        onSetMinTrackDuration = settingsViewModel::setMinTrackDuration,
        onSetDefaultSortType = settingsViewModel::setDefaultSortType,
        onSetDefaultSortReversed = settingsViewModel::setDefaultSortReversed,
        onToggleFolderBlacklist = { path ->
            if (blacklistedFolders.contains(path)) trackViewModel.removeFolderFromBlacklist(path)
            else trackViewModel.addFolderToBlacklist(path)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsContent(
    minTrackDuration: Int,
    defaultSortType: SortType,
    isDefaultSortReversed: Boolean,
    folders: List<Folder>,
    blacklistedFolders: Set<String>,
    onSetMinTrackDuration: (Int) -> Unit,
    onSetDefaultSortType: (SortType) -> Unit,
    onSetDefaultSortReversed: (Boolean) -> Unit,
    onToggleFolderBlacklist: (String) -> Unit
) {
    var showSortDialog by remember { mutableStateOf(false) }
    var showBlacklistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSliderItem(
            label = "Filter short tracks",
            value = minTrackDuration.toFloat(),
            valueRange = 0f..30f,
            steps = 29,
            displayValue = "${minTrackDuration}s",
            onValueChange = { onSetMinTrackDuration(it.roundToInt()) }
        )

        SettingsClickableItem(
            title = "Default Sorting",
            subtitle = "By ${defaultSortType.name.substringAfter("BY_").lowercase()} ${if(isDefaultSortReversed) "(Reversed)" else ""}",
            onClick = { showSortDialog = true }
        )

        SettingsClickableItem(
            title = "Blacklisted Folders",
            subtitle = "Manage music folders to exclude",
            onClick = { showBlacklistDialog = true }
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Default Sorting") },
            text = {
                Column {
                    SortType.entries.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetDefaultSortType(type) }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = defaultSortType == type, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name.substringAfter("BY_").lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDefaultSortReversed(!isDefaultSortReversed) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(checked = isDefaultSortReversed, onCheckedChange = null)
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
                    if (folders.isEmpty()) {
                        Text("No folders found", modifier = Modifier.padding(16.dp))
                    }
                    folders.forEach { folder ->
                        val isBlacklisted = blacklistedFolders.contains(folder.path)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFolderBlacklist(folder.path) }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isBlacklisted) Icons.Rounded.Block else Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isBlacklisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isBlacklisted) FontWeight.Normal else FontWeight.Bold,
                                    color = if (isBlacklisted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = folder.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Checkbox(
                                checked = isBlacklisted,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBlacklistDialog = false }) { Text("Done") } }
        )
    }
}
