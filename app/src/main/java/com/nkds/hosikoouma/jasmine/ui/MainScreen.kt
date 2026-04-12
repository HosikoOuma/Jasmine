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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.nkds.hosikoouma.jasmine.viewmodels.SortType
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
    val focusRequester = remember { FocusRequester() }
    val clipboardManager = LocalClipboardManager.current
    
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isRadioPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    
    val searchQuery by trackViewModel.searchQuery.collectAsStateWithLifecycle()
    val isReversed by trackViewModel.isReversed.collectAsStateWithLifecycle()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showTrackPickerDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showAddRadioDialog by remember { mutableStateOf(false) }

    var selectedTracks by remember { mutableStateOf(setOf<Track>()) }
    var selectedStations by remember { mutableStateOf(setOf<RadioStation>()) }
    
    val isInTrackSelectionMode by remember { derivedStateOf { selectedTracks.isNotEmpty() } }
    val isInRadioSelectionMode by remember { derivedStateOf { selectedStations.isNotEmpty() } }
    val isInSelectionMode by remember { derivedStateOf { isInTrackSelectionMode || isInRadioSelectionMode } }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteTracksDialog by remember { mutableStateOf(false) }
    var showDeleteStationsDialog by remember { mutableStateOf(false) }
    var showRemoveFromPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showTrackInfoForSelection by remember { mutableStateOf<Track?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        selectedTracks = emptySet()
        selectedStations = emptySet()
    }

    val playlistId = remember(navBackStackEntry) { navBackStackEntry?.arguments?.getLong("playlistId") ?: 0L }
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())
    val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
    val currentPlaylistName = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }?.name ?: "Playlist"
    }

    val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        uri?.let { trackViewModel.exportPlaylist(playlistId, it) }
    }

    val deleteLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedTracks = emptySet()
            trackViewModel.loadTracks()
            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        trackViewModel.pendingDeleteIntent.collect { intentSender ->
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    val dynamicTitle = remember(currentRoute, navBackStackEntry, currentPlaylistName, selectedTracks.size, selectedStations.size) {
        if (isInSelectionMode) {
            val count = if (isInTrackSelectionMode) selectedTracks.size else selectedStations.size
            "$count selected"
        } else {
            when {
                currentRoute?.startsWith("album_detail") == true -> {
                    val encoded = navBackStackEntry?.arguments?.getString("albumName") ?: "Album"
                    URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                }
                currentRoute?.startsWith("artist_detail") == true -> {
                    val encoded = navBackStackEntry?.arguments?.getString("artistName") ?: "Artist"
                    URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                }
                currentRoute?.startsWith("folder_detail") == true -> {
                    val encoded = navBackStackEntry?.arguments?.getString("folderPath") ?: "Folder"
                    URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString()).substringAfterLast("/")
                }
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

    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    LaunchedEffect(isPlayerExpanded, isRadioPlayerExpanded) { if (isPlayerExpanded || isRadioPlayerExpanded) keyboardController?.hide() }

    val isRadioMode by playerViewModel.isRadioMode.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val intent = (context as? Activity)?.intent
        if (intent?.getBooleanExtra("OPEN_PLAYER", false) == true) {
            if (playerViewModel.isRadioMode.value) isRadioPlayerExpanded = true else isPlayerExpanded = true
            intent.removeExtra("OPEN_PLAYER")
        }
    }

    val isTracksScreen = currentRoute == Screen.Tracks.route
    val isRadioScreen = currentRoute == Screen.Radio.route
    val isPlaylistDetail = currentRoute?.startsWith("playlist_detail") == true
    val shouldShowSort = remember(currentRoute) {
        currentRoute == Screen.Tracks.route || currentRoute?.contains("library_") == true || isPlaylistDetail
    }

    val isCollapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.8f } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        if (isSearching && isTracksScreen) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { trackViewModel.setSearchQuery(it) },
                                placeholder = { Text("Search tracks...") },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true
                            )
                        } else {
                            Text(dynamicTitle)
                        }
                    },
                    navigationIcon = {
                        if (isInSelectionMode) {
                            IconButton(onClick = { vibrateClick(context); selectedTracks = emptySet(); selectedStations = emptySet() }) {
                                Icon(Icons.Rounded.Close, "Clear")
                            }
                        } else if (canPop && !isSearching) {
                            IconButton(onClick = { vibrateClick(context); navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        MainActions(
                            isInSelectionMode = isInSelectionMode,
                            isInTrackSelectionMode = isInTrackSelectionMode,
                            selectedTracks = selectedTracks,
                            selectedStations = selectedStations,
                            isPlaylistDetail = isPlaylistDetail,
                            isTracksScreen = isTracksScreen,
                            isSearching = isSearching,
                            isCollapsed = isCollapsed,
                            shouldShowSort = shouldShowSort,
                            currentPlaylistName = currentPlaylistName,
                            isReversed = isReversed,
                            onToggleReverse = { trackViewModel.toggleReverse() },
                            onSetSortType = { trackViewModel.setSortType(it) },
                            playerViewModel = playerViewModel,
                            clipboardManager = clipboardManager,
                            exportLauncher = exportLauncher,
                            onToggleSearch = { isSearching = !isSearching },
                            onShowMore = { showMoreMenu = true },
                            onDeletePlaylist = { showDeletePlaylistDialog = true },
                            onDeleteTracks = { showDeleteTracksDialog = true },
                            onDeleteStations = { showDeleteStationsDialog = true },
                            onRemoveFromPlaylist = { showRemoveFromPlaylistDialog = true },
                            onShowTrackInfo = { showTrackInfoForSelection = it }
                        )
                        
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { vibrateClick(context); showMoreMenu = false; ShareHelper.shareTracks(context, selectedTracks.toList()); selectedTracks = emptySet() })
                            DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }, onClick = { vibrateClick(context); showMoreMenu = false; showAddToPlaylistDialog = true })
                            DropdownMenuItem(text = { Text("Delete from device", fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { vibrateClick(context); showMoreMenu = false; showDeleteTracksDialog = true }, colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                MainFAB(
                    isInSelectionMode = isInSelectionMode,
                    isPlaylistDetail = isPlaylistDetail,
                    isRadioScreen = isRadioScreen,
                    playlistTracks = playlistTracks,
                    onAddTracks = { showTrackPickerDialog = true },
                    onAddRadio = { showAddRadioDialog = true }
                )
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                JasmineNavHost(
                    navController = navController,
                    trackViewModel = trackViewModel,
                    playerViewModel = playerViewModel,
                    onNavigateToPlayer = { isPlayerExpanded = true },
                    onNavigateToRadioPlayer = { isRadioPlayerExpanded = true },
                    selectedTracks = selectedTracks,
                    onToggleTrackSelection = { track -> selectedTracks = if (selectedTracks.contains(track)) selectedTracks - track else selectedTracks + track },
                    selectedStations = selectedStations,
                    onToggleStationSelection = { station -> selectedStations = if (selectedStations.contains(station)) selectedStations - station else selectedStations + station },
                    onAddTracksToPlaylist = { showTrackPickerDialog = true },
                    showAddRadioDialog = showAddRadioDialog,
                    onDismissRadioDialog = { showAddRadioDialog = false },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
                
                BackHandler(enabled = isInSelectionMode || isPlayerExpanded || isRadioPlayerExpanded || isSearching || canPop || showAddRadioDialog) {
                    when {
                        showAddRadioDialog -> showAddRadioDialog = false
                        isRadioPlayerExpanded -> isRadioPlayerExpanded = false
                        isInSelectionMode -> { selectedTracks = emptySet(); selectedStations = emptySet() }
                        isPlayerExpanded -> isPlayerExpanded = false
                        isSearching -> { isSearching = false; trackViewModel.setSearchQuery("") }
                        canPop -> navController.popBackStack()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.colorScheme.surface)
                            )
                        )
                )
            }
        }

        BottomNavigationArea(
            playerViewModel = playerViewModel,
            isRadioMode = isRadioMode,
            onExpandPlayer = { if (isRadioMode) isRadioPlayerExpanded = true else isPlayerExpanded = true },
            navController = navController
        )

        PlayerScreensArea(
            isPlayerExpanded = isPlayerExpanded,
            isRadioPlayerExpanded = isRadioPlayerExpanded,
            playerViewModel = playerViewModel,
            trackViewModel = trackViewModel,
            navController = navController,
            onClosePlayer = { isPlayerExpanded = false },
            onCloseRadio = { isRadioPlayerExpanded = false }
        )
    }

    MainDialogs(
        showTrackPickerDialog = showTrackPickerDialog,
        showDeletePlaylistDialog = showDeletePlaylistDialog,
        showDeleteTracksDialog = showDeleteTracksDialog,
        showDeleteStationsDialog = showDeleteStationsDialog,
        showRemoveFromPlaylistDialog = showRemoveFromPlaylistDialog,
        showAddToPlaylistDialog = showAddToPlaylistDialog,
        showTrackInfoForSelection = showTrackInfoForSelection,
        selectedTracks = selectedTracks,
        selectedStations = selectedStations,
        playlistId = playlistId,
        currentPlaylistName = currentPlaylistName,
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
        onClearSelection = { selectedTracks = emptySet(); selectedStations = emptySet() }
    )
}

@Composable
private fun BottomNavigationArea(
    playerViewModel: PlayerViewModel,
    isRadioMode: Boolean,
    onExpandPlayer: () -> Unit,
    navController: androidx.navigation.NavHostController
) {
    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPlayer(
                viewModel = playerViewModel,
                onClick = onExpandPlayer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            JasmineBottomBar(navController = navController)
        }
    }
}

@Composable
private fun PlayerScreensArea(
    isPlayerExpanded: Boolean,
    isRadioPlayerExpanded: Boolean,
    playerViewModel: PlayerViewModel,
    trackViewModel: TrackViewModel,
    navController: androidx.navigation.NavHostController,
    onClosePlayer: () -> Unit,
    onCloseRadio: () -> Unit
) {
    AnimatedVisibility(visible = isPlayerExpanded, enter = fadeIn(tween(300)), exit = ExitTransition.None) {
        PlayerScreen(viewModel = playerViewModel, trackViewModel = trackViewModel, navController = navController, onClose = onClosePlayer)
    }

    AnimatedVisibility(visible = isRadioPlayerExpanded, enter = fadeIn(tween(300)), exit = ExitTransition.None) {
        val currentStation by playerViewModel.currentRadioStation.collectAsStateWithLifecycle()
        currentStation?.let { RadioPlayerScreen(station = it, playerViewModel = playerViewModel, onClose = onCloseRadio) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainActions(
    isInSelectionMode: Boolean,
    isInTrackSelectionMode: Boolean,
    selectedTracks: Set<Track>,
    selectedStations: Set<RadioStation>,
    isPlaylistDetail: Boolean,
    isTracksScreen: Boolean,
    isSearching: Boolean,
    isCollapsed: Boolean,
    shouldShowSort: Boolean,
    currentPlaylistName: String,
    isReversed: Boolean,
    onToggleReverse: () -> Unit,
    onSetSortType: (SortType) -> Unit,
    playerViewModel: PlayerViewModel,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onToggleSearch: () -> Unit,
    onShowMore: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onDeleteTracks: () -> Unit,
    onDeleteStations: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onShowTrackInfo: (Track) -> Unit
) {
    val context = LocalContext.current
    if (isInSelectionMode) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isInTrackSelectionMode) {
                if (selectedTracks.size == 1) IconButton(onClick = { vibrateClick(context); onShowTrackInfo(selectedTracks.first()) }) { Icon(Icons.Rounded.Info, null) }
                if (isPlaylistDetail) IconButton(onClick = { vibrateClick(context); onRemoveFromPlaylist() }) { Icon(Icons.Rounded.PlaylistRemove, null, tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = { vibrateClick(context); playerViewModel.addTracksToQueue(selectedTracks.toList()) }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }
                IconButton(onClick = { vibrateClick(context); onShowMore() }) { Icon(Icons.Rounded.MoreVert, null) }
            } else {
                if (selectedStations.size == 1) IconButton(onClick = { vibrateClick(context); clipboardManager.setText(AnnotatedString(selectedStations.first().url)); Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Rounded.ContentCopy, null) }
                IconButton(onClick = { vibrateClick(context); onDeleteStations() }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    } else {
        AnimatedVisibility(visible = (isCollapsed || isSearching || shouldShowSort) && (isTracksScreen || shouldShowSort)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaylistDetail && !isSearching) {
                    IconButton(onClick = { vibrateClick(context); exportLauncher.launch("$currentPlaylistName.m3u") }) { Icon(Icons.Rounded.FileUpload, null) }
                    IconButton(onClick = { vibrateClick(context); onDeletePlaylist() }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
                if (isTracksScreen) IconButton(onClick = { vibrateClick(context); onToggleSearch() }) { Icon(if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search, null) }
                if (!isSearching && shouldShowSort) {
                    var showSort by remember { mutableStateOf(false) }
                    IconButton(onClick = { vibrateClick(context); showSort = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null) }
                    
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }, shape = RoundedCornerShape(24.dp)) {
                        DropdownMenuItem(text = { Text("By Name") }, onClick = { vibrateClick(context); onSetSortType(SortType.BY_NAME); showSort = false })
                        DropdownMenuItem(text = { Text("By Artist") }, onClick = { vibrateClick(context); onSetSortType(SortType.BY_ARTIST); showSort = false })
                        DropdownMenuItem(text = { Text("By Date Added") }, onClick = { vibrateClick(context); onSetSortType(SortType.BY_DATE); showSort = false })
                        DropdownMenuItem(text = { Text("By Duration") }, onClick = { vibrateClick(context); onSetSortType(SortType.BY_DURATION); showSort = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(if (isReversed) "Normal Order" else "Reverse Order") }, leadingIcon = { Icon(Icons.Rounded.FilterList, null) }, onClick = { vibrateClick(context); onToggleReverse(); showSort = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun MainFAB(
    isInSelectionMode: Boolean,
    isPlaylistDetail: Boolean,
    isRadioScreen: Boolean,
    playlistTracks: List<Track>,
    onAddTracks: () -> Unit,
    onAddRadio: () -> Unit
) {
    if (!isInSelectionMode) {
        val context = LocalContext.current
        if (isPlaylistDetail && playlistTracks.isNotEmpty()) {
            FloatingActionButton(onClick = onAddTracks, modifier = Modifier.padding(bottom = 140.dp).bouncingClickable { vibrateClick(context); onAddTracks() }, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
        } else if (isRadioScreen) {
            FloatingActionButton(onClick = onAddRadio, modifier = Modifier.padding(bottom = 140.dp).bouncingClickable { vibrateClick(context); onAddRadio() }, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
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
    navController: androidx.navigation.NavController,
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
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vibrateClick(context); trackViewModel.addTrackToPlaylist(playlistId, track.id) },
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
        AlertDialog(onDismissRequest = onDismissDeletePlaylist, title = { Text("Delete Playlist") }, text = { Text("Delete \"$currentPlaylistName\"?") }, confirmButton = { TextButton(onClick = { vibrateClick(context); trackViewModel.deletePlaylist(playlistId); onDismissDeletePlaylist(); navController.popBackStack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { vibrateClick(context); onDismissDeletePlaylist() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteTracksDialog) {
        AlertDialog(onDismissRequest = onDismissDeleteTracks, title = { Text("Delete Tracks") }, text = { Text("Delete ${selectedTracks.size} tracks?") }, confirmButton = { TextButton(onClick = { vibrateClick(context); onDismissDeleteTracks(); playerViewModel.prepareForDeletion(selectedTracks.toList()); trackViewModel.deleteTracks(selectedTracks.toList()); onClearSelection() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { vibrateClick(context); onDismissDeleteTracks() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteStationsDialog) {
        val radioViewModel: RadioViewModel = viewModel()
        AlertDialog(onDismissRequest = onDismissDeleteStations, title = { Text("Delete Stations") }, text = { Text("Delete ${selectedStations.size} stations?") }, confirmButton = { TextButton(onClick = { vibrateClick(context); selectedStations.forEach { radioViewModel.deleteStation(it) }; onClearSelection(); onDismissDeleteStations() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { vibrateClick(context); onDismissDeleteStations() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showRemoveFromPlaylistDialog) {
        AlertDialog(onDismissRequest = onDismissRemoveFromPlaylist, title = { Text("Remove Tracks") }, text = { Text("Remove ${selectedTracks.size} tracks from playlist?") }, confirmButton = { TextButton(onClick = { vibrateClick(context); trackViewModel.removeTracksFromPlaylist(playlistId, selectedTracks.toList()); onClearSelection(); onDismissRemoveFromPlaylist() }) { Text("Remove", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { vibrateClick(context); onDismissRemoveFromPlaylist() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(onDismissRequest = onDismissAddToPlaylist, onPlaylistSelected = { pid -> vibrateClick(context); trackViewModel.addTracksToPlaylist(pid, selectedTracks.toList()); onClearSelection(); onDismissAddToPlaylist() }, trackViewModel = trackViewModel)
    }

    if (showTrackInfoForSelection != null) {
        TrackInfoBottomSheet(track = showTrackInfoForSelection, onDismissRequest = onDismissTrackInfo)
    }
}
