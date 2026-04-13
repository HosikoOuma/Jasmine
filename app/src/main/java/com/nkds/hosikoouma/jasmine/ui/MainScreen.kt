package com.nkds.hosikoouma.jasmine.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.data.RadioStation
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.navigation.JasmineNavHost
import com.nkds.hosikoouma.jasmine.ui.components.*
import com.nkds.hosikoouma.jasmine.ui.screens.PlayerScreen
import com.nkds.hosikoouma.jasmine.ui.screens.RadioPlayerScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// --- UI State ---
data class MainUiState(
    val currentRoute: String? = null,
    val dynamicTitle: String = "Jasmine",
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isPlayerExpanded: Boolean = false,
    val isRadioPlayerExpanded: Boolean = false,
    val isReversed: Boolean = false,
    val selectedTracks: Set<Track> = emptySet(),
    val selectedStations: Set<RadioStation> = emptySet(),
    val isRadioMode: Boolean = false,
    val currentPlaylistName: String = "Playlist",
    val canPop: Boolean = false,
    val isPlaylistDetail: Boolean = false,
    val isTracksScreen: Boolean = false,
    val isRadioScreen: Boolean = false,
    val shouldShowSort: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    trackViewModel: TrackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // States
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isRadioPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTracks by remember { mutableStateOf(setOf<Track>()) }
    var selectedStations by remember { mutableStateOf(setOf<RadioStation>()) }
    var showAddRadioDialog by remember { mutableStateOf(false) }

    val searchQuery by trackViewModel.searchQuery.collectAsStateWithLifecycle()
    val isReversed by trackViewModel.isReversed.collectAsStateWithLifecycle()
    val isRadioMode by playerViewModel.isRadioMode.collectAsStateWithLifecycle()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Compute Derived UI values
    val playlistId = remember(navBackStackEntry) { navBackStackEntry?.arguments?.getLong("playlistId") ?: 0L }
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
    val currentPlaylistName = remember(playlists, playlistId) { playlists.find { it.id == playlistId }?.name ?: "Playlist" }
    
    val isInSelectionMode = selectedTracks.isNotEmpty() || selectedStations.isNotEmpty()
    
    val dynamicTitle = remember(currentRoute, navBackStackEntry, currentPlaylistName, selectedTracks.size, selectedStations.size) {
        if (isInSelectionMode) {
            "${if (selectedTracks.isNotEmpty()) selectedTracks.size else selectedStations.size} selected"
        } else {
            when {
                currentRoute?.startsWith("album_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("albumName") ?: "Album", StandardCharsets.UTF_8.toString())
                currentRoute?.startsWith("artist_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("artistName") ?: "Artist", StandardCharsets.UTF_8.toString())
                currentRoute?.startsWith("folder_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("folderPath") ?: "Folder", StandardCharsets.UTF_8.toString()).substringAfterLast("/")
                currentRoute?.startsWith("playlist_detail") == true -> currentPlaylistName
                currentRoute == Screen.LibraryAlbums.route -> "Albums"
                currentRoute == Screen.LibraryArtists.route -> "Artists"
                currentRoute == Screen.LibraryFolders.route -> "Folders"
                currentRoute == Screen.LibraryPlaylists.route -> "Playlists"
                else -> Screen.items.find { it.route == currentRoute }?.title ?: "Jasmine"
            }
        }
    }

    val isMainDestination = remember(currentRoute) { Screen.items.any { it.route == currentRoute } }
    val canPop = remember(navBackStackEntry, isMainDestination) { navController.previousBackStackEntry != null && !isMainDestination }

    val uiState = MainUiState(
        currentRoute = currentRoute,
        dynamicTitle = dynamicTitle,
        searchQuery = searchQuery,
        isSearching = isSearching,
        isPlayerExpanded = isPlayerExpanded,
        isRadioPlayerExpanded = isRadioPlayerExpanded,
        isReversed = isReversed,
        selectedTracks = selectedTracks,
        selectedStations = selectedStations,
        isRadioMode = isRadioMode,
        currentPlaylistName = currentPlaylistName,
        canPop = canPop,
        isPlaylistDetail = currentRoute?.startsWith("playlist_detail") == true,
        isTracksScreen = currentRoute == Screen.Tracks.route,
        isRadioScreen = currentRoute == Screen.Radio.route,
        shouldShowSort = currentRoute == Screen.Tracks.route || currentRoute?.contains("library_") == true || currentRoute?.startsWith("playlist_detail") == true
    )

    // Side Effects
    LaunchedEffect(currentRoute) { selectedTracks = emptySet(); selectedStations = emptySet() }
    LaunchedEffect(isPlayerExpanded, isRadioPlayerExpanded) { if (isPlayerExpanded || isRadioPlayerExpanded) keyboardController?.hide() }
    
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { selectedTracks = emptySet(); trackViewModel.loadTracks(); Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) { trackViewModel.pendingDeleteIntent.collect { intentSender -> deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build()) } }

    // UI Structure
    MainContent(
        uiState = uiState,
        navController = navController,
        trackViewModel = trackViewModel,
        playerViewModel = playerViewModel,
        scrollBehavior = scrollBehavior,
        onSearchQueryChange = trackViewModel::setSearchQuery,
        onToggleSearch = { isSearching = !isSearching },
        onClearSelection = { selectedTracks = emptySet(); selectedStations = emptySet() },
        onToggleReverse = trackViewModel::toggleReverse,
        onSetSortType = trackViewModel::setSortType,
        onTogglePlayer = { isPlayerExpanded = it },
        onToggleRadioPlayer = { isRadioPlayerExpanded = it },
        onToggleAddRadioDialog = { showAddRadioDialog = it },
        onToggleTrackSelection = { track -> selectedTracks = if (selectedTracks.contains(track)) selectedTracks - track else selectedTracks + track },
        onToggleStationSelection = { station -> selectedStations = if (selectedStations.contains(station)) selectedStations - station else selectedStations + station },
        showAddRadioDialog = showAddRadioDialog,
        playlistId = playlistId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    uiState: MainUiState,
    navController: androidx.navigation.NavHostController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleReverse: () -> Unit,
    onSetSortType: (SortType) -> Unit,
    onTogglePlayer: (Boolean) -> Unit,
    onToggleRadioPlayer: (Boolean) -> Unit,
    onToggleAddRadioDialog: (Boolean) -> Unit,
    onToggleTrackSelection: (Track) -> Unit,
    onToggleStationSelection: (RadioStation) -> Unit,
    showAddRadioDialog: Boolean,
    playlistId: Long
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
    val isCollapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.8f } }

    var showTrackPickerDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteTracksDialog by remember { mutableStateOf(false) }
    var showDeleteStationsDialog by remember { mutableStateOf(false) }
    var showRemoveFromPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showTrackInfoForSelection by remember { mutableStateOf<Track?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        uri?.let { trackViewModel.exportPlaylist(playlistId, it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (uiState.isSearching && uiState.isTracksScreen) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search tracks...") },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            singleLine = true
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    } else {
                        Text(uiState.dynamicTitle)
                    }
                },
                navigationIcon = {
                    if (uiState.selectedTracks.isNotEmpty() || uiState.selectedStations.isNotEmpty()) {
                        IconButton(onClick = { onClearSelection() }) { Icon(Icons.Rounded.Close, "Clear") }
                    } else if (uiState.canPop && !uiState.isSearching) {
                        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                    }
                },
                actions = {
                    MainActionsSection(
                        uiState = uiState,
                        isCollapsed = isCollapsed,
                        onToggleSearch = onToggleSearch,
                        onToggleReverse = onToggleReverse,
                        onSetSortType = onSetSortType,
                        onExportPlaylist = { exportLauncher.launch("${uiState.currentPlaylistName}.m3u") },
                        onDeletePlaylist = { showDeletePlaylistDialog = true },
                        onDeleteTracks = { showDeleteTracksDialog = true },
                        onDeleteStations = { showDeleteStationsDialog = true },
                        onRemoveFromPlaylist = { showRemoveFromPlaylistDialog = true },
                        onAddToPlaylist = { showAddToPlaylistDialog = true },
                        onShowTrackInfo = { showTrackInfoForSelection = it },
                        playerViewModel = playerViewModel
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (uiState.selectedTracks.isEmpty() && uiState.selectedStations.isEmpty()) {
                if (uiState.isPlaylistDetail && playlistTracks.isNotEmpty()) {
                    FloatingActionButton(onClick = { showTrackPickerDialog = true }, modifier = Modifier.padding(bottom = 140.dp).bouncingClickable { showTrackPickerDialog = true }, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
                } else if (uiState.isRadioScreen) {
                    FloatingActionButton(onClick = { onToggleAddRadioDialog(true) }, modifier = Modifier.padding(bottom = 140.dp).bouncingClickable { onToggleAddRadioDialog(true) }, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            JasmineNavHost(
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = { /* ТЕПЕРЬ ПУСТО: НЕ ОТКРЫВАЕМ ПРИ КЛИКЕ */ },
                onNavigateToRadioPlayer = { onToggleRadioPlayer(true) },
                selectedTracks = uiState.selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection,
                selectedStations = uiState.selectedStations,
                onToggleStationSelection = onToggleStationSelection,
                onAddTracksToPlaylist = { showTrackPickerDialog = true },
                showAddRadioDialog = showAddRadioDialog,
                onDismissRadioDialog = { onToggleAddRadioDialog(false) },
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            )
            
            BackHandler(enabled = uiState.selectedTracks.isNotEmpty() || uiState.selectedStations.isNotEmpty() || uiState.isPlayerExpanded || uiState.isRadioPlayerExpanded || uiState.isSearching || uiState.canPop || showAddRadioDialog) {
                when {
                    showAddRadioDialog -> onToggleAddRadioDialog(false)
                    uiState.isRadioPlayerExpanded -> onToggleRadioPlayer(false)
                    uiState.selectedTracks.isNotEmpty() || uiState.selectedStations.isNotEmpty() -> onClearSelection()
                    uiState.isPlayerExpanded -> onTogglePlayer(false)
                    uiState.isSearching -> { onToggleSearch(); onSearchQueryChange("") }
                    uiState.canPop -> navController.popBackStack()
                }
            }

            // Bottom Gradient Overlay
            Box(modifier = Modifier.fillMaxWidth().height(240.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.colorScheme.surface))))
        }
    }

    // Bottom Layers
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars), contentAlignment = Alignment.BottomCenter) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPlayer(viewModel = playerViewModel, onClick = { if (uiState.isRadioMode) onToggleRadioPlayer(true) else onTogglePlayer(true) }, modifier = Modifier.padding(bottom = 8.dp))
            JasmineBottomBar(navController = navController)
        }
    }

    AnimatedVisibility(visible = uiState.isPlayerExpanded, enter = fadeIn(tween(300)), exit = ExitTransition.None) {
        PlayerScreen(viewModel = playerViewModel, trackViewModel = trackViewModel, navController = navController, onClose = { onTogglePlayer(false) })
    }

    AnimatedVisibility(visible = uiState.isRadioPlayerExpanded, enter = fadeIn(tween(300)), exit = ExitTransition.None) {
        val currentStation by playerViewModel.currentRadioStation.collectAsStateWithLifecycle()
        currentStation?.let { RadioPlayerScreen(station = it, playerViewModel = playerViewModel, onClose = { onToggleRadioPlayer(false) }) }
    }

    MainDialogs(
        showTrackPickerDialog = showTrackPickerDialog,
        showDeletePlaylistDialog = showDeletePlaylistDialog,
        showDeleteTracksDialog = showDeleteTracksDialog,
        showDeleteStationsDialog = showDeleteStationsDialog,
        showRemoveFromPlaylistDialog = showRemoveFromPlaylistDialog,
        showAddToPlaylistDialog = showAddToPlaylistDialog,
        showTrackInfoForSelection = showTrackInfoForSelection,
        selectedTracks = uiState.selectedTracks,
        selectedStations = uiState.selectedStations,
        playlistId = playlistId,
        currentPlaylistName = uiState.currentPlaylistName,
        trackViewModel = trackViewModel,
        playerViewModel = playerViewModel,
        navController = navController,
        onDismissTrackPicker = { showTrackPickerDialog = false },
        onDismissDeletePlaylist = { showDeletePlaylistDialog = false },
        onDismissDeleteTracks = { showDeleteTracksDialog = false },
        onDismissDeleteStations = { showDeleteStationsDialog = false },
        onDismissRemoveFromPlaylist = { showRemoveFromPlaylistDialog = false },
        onDismissAddToPlaylist = { showAddToPlaylistDialog = false },
        onDismissTrackInfo = { showTrackInfoForSelection = null },
        onClearSelection = onClearSelection
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainActionsSection(
    uiState: MainUiState,
    isCollapsed: Boolean,
    onToggleSearch: () -> Unit,
    onToggleReverse: () -> Unit,
    onSetSortType: (SortType) -> Unit,
    onExportPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onDeleteTracks: () -> Unit,
    onDeleteStations: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowTrackInfo: (Track) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMoreMenu by remember { mutableStateOf(false) }

    if (uiState.selectedTracks.isNotEmpty() || uiState.selectedStations.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.selectedTracks.isNotEmpty()) {
                if (uiState.selectedTracks.size == 1) IconButton(onClick = { onShowTrackInfo(uiState.selectedTracks.first()) }) { Icon(Icons.Rounded.Info, null) }
                if (uiState.isPlaylistDetail) IconButton(onClick = { onRemoveFromPlaylist() }) { Icon(Icons.Rounded.PlaylistRemove, null, tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = { playerViewModel.addTracksToQueue(uiState.selectedTracks.toList()) }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }
                IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }, shape = RoundedCornerShape(24.dp), modifier = Modifier.width(220.dp)) {
                    DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { showMoreMenu = false; ShareHelper.shareTracks(context, uiState.selectedTracks.toList()) })
                    DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }, onClick = { showMoreMenu = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text("Delete from device", fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showMoreMenu = false; onDeleteTracks() }, colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error))
                }
            } else {
                if (uiState.selectedStations.size == 1) IconButton(onClick = { clipboardManager.setText(AnnotatedString(uiState.selectedStations.first().url)); Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Rounded.ContentCopy, null) }
                IconButton(onClick = { onDeleteStations() }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    } else {
        AnimatedVisibility(visible = (isCollapsed || uiState.isSearching || uiState.shouldShowSort) && (uiState.isTracksScreen || uiState.shouldShowSort)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isPlaylistDetail && !uiState.isSearching) {
                    IconButton(onClick = { onExportPlaylist() }) { Icon(Icons.Rounded.FileUpload, null) }
                    IconButton(onClick = { onDeletePlaylist() }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
                if (uiState.isTracksScreen) IconButton(onClick = { onToggleSearch() }) { Icon(if (uiState.isSearching) Icons.Rounded.Close else Icons.Rounded.Search, null) }
                if (!uiState.isSearching && uiState.shouldShowSort) {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null) }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, shape = RoundedCornerShape(24.dp)) {
                        DropdownMenuItem(text = { Text("By Name") }, onClick = { onSetSortType(SortType.BY_TITLE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Artist") }, onClick = { onSetSortType(SortType.BY_ARTIST); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Date Added") }, onClick = { onSetSortType(SortType.BY_DATE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Duration") }, onClick = { onSetSortType(SortType.BY_DURATION); showSortMenu = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(if (uiState.isReversed) "Normal Order" else "Reverse Order") }, leadingIcon = { Icon(Icons.Rounded.FilterList, null) }, onClick = { onToggleReverse(); showSortMenu = false })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainDialogs(
    showTrackPickerDialog: Boolean,
    showDeletePlaylistDialog: Boolean,
    showDeleteTracksDialog: Boolean,
    showDeleteStationsDialog: Boolean,
    showRemoveFromPlaylistDialog: Boolean,
    showAddToPlaylistDialog: Boolean,
    showTrackInfoForSelection: Track?,
    selectedTracks: Set<Track>,
    selectedStations: Set<RadioStation>,
    playlistId: Long,
    currentPlaylistName: String,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    navController: NavController,
    onDismissTrackPicker: () -> Unit,
    onDismissDeletePlaylist: () -> Unit,
    onDismissDeleteTracks: () -> Unit,
    onDismissDeleteStations: () -> Unit,
    onDismissRemoveFromPlaylist: () -> Unit,
    onDismissAddToPlaylist: () -> Unit,
    onDismissTrackInfo: () -> Unit,
    onClearSelection: () -> Unit
) {
    val context = LocalContext.current

    if (showTrackPickerDialog) {
        val allTracks by trackViewModel.allTracks.collectAsStateWithLifecycle()
        val pTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
        AlertDialog(
            onDismissRequest = onDismissTrackPicker,
            title = { Text("Select Tracks") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(allTracks.size) { index ->
                        val track = allTracks[index]
                        val isAlreadyInPlaylist = pTracks.any { it.id == track.id }
                        ListItem(
                            leadingContent = { AlbumArt(track.albumArtUri, Modifier.size(48.dp), RoundedCornerShape(10.dp), isLowRes = true) },
                            headlineContent = { Text(track.title, fontWeight = FontWeight.Bold, color = if (isAlreadyInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = { Icon(if (isAlreadyInPlaylist) Icons.Rounded.Check else Icons.Rounded.Add, null, tint = if (isAlreadyInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { trackViewModel.addTrackToPlaylist(playlistId, track.id) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissTrackPicker) { Text("Done") } },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeletePlaylistDialog) {
        AlertDialog(onDismissRequest = onDismissDeletePlaylist, title = { Text("Delete Playlist") }, text = { Text("Delete \"$currentPlaylistName\"?") }, confirmButton = { TextButton(onClick = { trackViewModel.deletePlaylist(playlistId); onDismissDeletePlaylist(); navController.popBackStack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissDeletePlaylist() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteTracksDialog) {
        AlertDialog(onDismissRequest = onDismissDeleteTracks, title = { Text("Delete Tracks") }, text = { Text("Delete ${selectedTracks.size} tracks?") }, confirmButton = { TextButton(onClick = { onDismissDeleteTracks(); playerViewModel.prepareForDeletion(selectedTracks.toList()); trackViewModel.deleteTracks(selectedTracks.toList()); onClearSelection() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissDeleteTracks() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteStationsDialog) {
        val radioViewModel: RadioViewModel = viewModel()
        AlertDialog(onDismissRequest = onDismissDeleteStations, title = { Text("Delete Stations") }, text = { Text("Delete ${selectedStations.size} stations?") }, confirmButton = { TextButton(onClick = { selectedStations.forEach { radioViewModel.deleteStation(it) }; onClearSelection(); onDismissDeleteStations() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissDeleteStations() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showRemoveFromPlaylistDialog) {
        AlertDialog(onDismissRequest = onDismissRemoveFromPlaylist, title = { Text("Remove Tracks") }, text = { Text("Remove ${selectedTracks.size} tracks from playlist?") }, confirmButton = { TextButton(onClick = { trackViewModel.removeTracksFromPlaylist(playlistId, selectedTracks.toList()); onClearSelection(); onDismissRemoveFromPlaylist() }) { Text("Remove", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissRemoveFromPlaylist() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(onDismissRequest = onDismissAddToPlaylist, onPlaylistSelected = { pid -> trackViewModel.addTracksToPlaylist(pid, selectedTracks.toList()); onClearSelection(); onDismissAddToPlaylist() }, trackViewModel = trackViewModel)
    }

    if (showTrackInfoForSelection != null) {
        TrackInfoBottomSheet(track = showTrackInfoForSelection, onDismissRequest = { onDismissTrackInfo() })
    }
}
