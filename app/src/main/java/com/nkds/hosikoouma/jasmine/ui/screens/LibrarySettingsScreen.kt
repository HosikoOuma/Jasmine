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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nkds.hosikoouma.jasmine.R
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

    val reversedLabel = stringResource(R.string.reversed_label)
    
    val currentSortLabel = remember(defaultSortType, isDefaultSortReversed, reversedLabel) {
        val typeLabel = when(defaultSortType) {
            SortType.BY_TITLE -> "Title"
            SortType.BY_ARTIST -> "Artist"
            SortType.BY_DATE -> "Date"
            SortType.BY_DURATION -> "Duration"
        }
        "By $typeLabel ${if(isDefaultSortReversed) reversedLabel else ""}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSliderItem(
            label = stringResource(R.string.filter_short_tracks),
            value = minTrackDuration.toFloat(),
            valueRange = 0f..30f,
            steps = 29,
            displayValue = "${minTrackDuration}s",
            onValueChange = { onSetMinTrackDuration(it.roundToInt()) }
        )

        SettingsClickableItem(
            title = stringResource(R.string.default_sorting),
            subtitle = stringResource(R.string.default_sorting_desc, 
                when(defaultSortType) {
                    SortType.BY_TITLE -> stringResource(R.string.sort_by_name)
                    SortType.BY_ARTIST -> stringResource(R.string.sort_by_artist)
                    SortType.BY_DATE -> stringResource(R.string.sort_by_date)
                    SortType.BY_DURATION -> stringResource(R.string.sort_by_duration)
                },
                if(isDefaultSortReversed) stringResource(R.string.reversed_label) else ""
            ),
            onClick = { showSortDialog = true }
        )

        SettingsClickableItem(
            title = stringResource(R.string.blacklisted_folders),
            subtitle = stringResource(R.string.blacklisted_folders_desc),
            onClick = { showBlacklistDialog = true }
        )
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.default_sorting)) },
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
                            Text(when(type) {
                                SortType.BY_TITLE -> stringResource(R.string.sort_by_name)
                                SortType.BY_ARTIST -> stringResource(R.string.sort_by_artist)
                                SortType.BY_DATE -> stringResource(R.string.sort_by_date)
                                SortType.BY_DURATION -> stringResource(R.string.sort_by_duration)
                            })
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
                        Text(stringResource(R.string.reverse_order_default))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSortDialog = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showBlacklistDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(stringResource(R.string.blacklisted_folders)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    if (folders.isEmpty()) {
                        Text(stringResource(R.string.no_folders), modifier = Modifier.padding(16.dp))
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
            confirmButton = { TextButton(onClick = { showBlacklistDialog = false }) { Text(stringResource(R.string.done)) } }
        )
    }
}
