package com.nkds.hosikoouma.jasmine.ui

import android.app.Activity
import android.net.Uri
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.nkds.hosikoouma.jasmine.ui.screens.PlaylistCoverEditor
import com.nkds.hosikoouma.jasmine.ui.screens.RadioPlayerScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.RadioViewModel
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramCloudViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    trackViewModel: TrackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    radioViewModel: RadioViewModel = viewModel(),
    telegramCloudViewModel: TelegramCloudViewModel = viewModel()
) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // States
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isRadioPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var selectedTracks by remember { mutableStateOf(setOf<Track>()) }
    var selectedStations by remember { mutableStateOf(setOf<RadioStation>()) }
    var showAddRadioDialog by remember { mutableStateOf(false) }

    val searchQuery by trackViewModel.searchQuery.collectAsStateWithLifecycle()
    val isReversed by trackViewModel.isReversed.collectAsStateWithLifecycle()
    val isRadioMode by playerViewModel.isRadioMode.collectAsStateWithLifecycle()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMainDestination = remember(currentRoute) { Screen.items.any { it.route == currentRoute } }
    val canPop = remember(navBackStackEntry, isMainDestination) { navController.previousBackStackEntry != null && !isMainDestination }

    // Данные Telegram каналов для заголовка
    val telegramChannels by telegramCloudViewModel.channels.collectAsStateWithLifecycle()

    // 1. Оптимизация заголовка через derivedStateOf
    val isInSelectionMode = selectedTracks.isNotEmpty() || selectedStations.isNotEmpty()
    val dynamicTitle by remember(currentRoute, navBackStackEntry, isInSelectionMode, selectedTracks.size, selectedStations.size, telegramChannels) {
        derivedStateOf {
            if (isInSelectionMode) {
                "${if (selectedTracks.isNotEmpty()) selectedTracks.size else selectedStations.size} selected"
            } else {
                when {
                    currentRoute?.startsWith("album_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("albumName") ?: "Album", StandardCharsets.UTF_8.toString())
                    currentRoute?.startsWith("artist_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("artistName") ?: "Artist", StandardCharsets.UTF_8.toString())
                    currentRoute?.startsWith("folder_detail") == true -> URLDecoder.decode(navBackStackEntry?.arguments?.getString("folderPath") ?: "Folder", StandardCharsets.UTF_8.toString()).substringAfterLast("/")
                    currentRoute?.startsWith("playlist_detail") == true -> {
                        val pId = navBackStackEntry?.arguments?.getLong("playlistId") ?: 0L
                        trackViewModel.getPlaylistNameSync(pId) ?: "Playlist"
                    }
                    currentRoute?.startsWith("telegram_channel_detail") == true -> {
                        val chatId = navBackStackEntry?.arguments?.getLong("chatId") ?: 0L
                        telegramChannels.find { it.chatId == chatId }?.title ?: "Telegram Channel"
                    }
                    currentRoute == Screen.LibraryAlbums.route -> "Albums"
                    currentRoute == Screen.LibraryArtists.route -> "Artists"
                    currentRoute == Screen.LibraryFolders.route -> "Folders"
                    currentRoute == Screen.LibraryPlaylists.route -> "Playlists"
                    currentRoute == Screen.SettingsPlayback.route -> "Playback"
                    currentRoute == Screen.SettingsAppearance.route -> "Appearance"
                    currentRoute == Screen.SettingsLibrary.route -> "Library Settings"
                    currentRoute == Screen.SettingsMaintenance.route -> "Maintenance"
                    currentRoute == Screen.SettingsShapes.route -> "Shapes Gallery"
                    currentRoute == Screen.About.route -> "About Jasmine"
                    currentRoute == Screen.TelegramCloud.route -> "Telegram Cloud"
                    currentRoute == Screen.SettingsTelegram.route -> "Telegram Cloud"
                    currentRoute == Screen.TelegramChatPicker.route -> "Chat Picker"
                    else -> Screen.items.find { it.route == currentRoute }?.title ?: "Jasmine"
                }
            }
        }
    }

    // Side Effects
    LaunchedEffect(currentRoute) { 
        selectedTracks = emptySet()
        selectedStations = emptySet() 
        if (isSearching) {
            isSearching = false
            trackViewModel.setSearchQuery("")
        }
    }
    
    LaunchedEffect(isPlayerExpanded, isRadioPlayerExpanded) { 
        if (isPlayerExpanded || isRadioPlayerExpanded) keyboardController?.hide() 
    }
    
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { 
            selectedTracks = emptySet()
            trackViewModel.loadTracks()
            playerViewModel.showToast(null, ToastType.DELETE_SUCCESS)
        } else {
            playerViewModel.showToast(null, ToastType.DELETE_FAILED)
        }
    }
    
    LaunchedEffect(Unit) { 
        trackViewModel.pendingDeleteIntent.collect { intentSender -> 
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build()) 
        } 
    }

    // UI Structure
    MainContent(
        navController = navController,
        trackViewModel = trackViewModel,
        playerViewModel = playerViewModel,
        radioViewModel = radioViewModel,
        scrollBehavior = scrollBehavior,
        currentRoute = currentRoute,
        dynamicTitle = dynamicTitle,
        searchQuery = searchQuery,
        isSearching = isSearching,
        isReversed = isReversed,
        isRadioMode = isRadioMode,
        canPop = canPop,
        selectedTracks = selectedTracks,
        selectedStations = selectedStations,
        onSearchQueryChange = trackViewModel::setSearchQuery,
        onToggleSearch = { 
            if (isSearching) trackViewModel.setSearchQuery("")
            isSearching = !isSearching 
        },
        onClearSelection = { selectedTracks = emptySet(); selectedStations = emptySet() },
        onSelectTracks = { tracks -> selectedTracks = tracks.toSet() },
        onToggleReverse = trackViewModel::toggleReverse,
        onSetSortType = trackViewModel::setSortType,
        onTogglePlayer = { isPlayerExpanded = it },
        onToggleRadioPlayer = { isRadioPlayerExpanded = it },
        onToggleAddRadioDialog = { showAddRadioDialog = it },
        onToggleTrackSelection = { track -> selectedTracks = if (selectedTracks.contains(track)) selectedTracks - track else selectedTracks + track },
        onToggleStationSelection = { station -> selectedStations = if (selectedStations.contains(station)) selectedStations - station else selectedStations + station },
        isPlayerExpanded = isPlayerExpanded,
        isRadioPlayerExpanded = isRadioPlayerExpanded,
        showAddRadioDialog = showAddRadioDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    navController: androidx.navigation.NavHostController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    radioViewModel: RadioViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    currentRoute: String?,
    dynamicTitle: String,
    searchQuery: String,
    isSearching: Boolean,
    isReversed: Boolean,
    isRadioMode: Boolean,
    canPop: Boolean,
    selectedTracks: Set<Track>,
    selectedStations: Set<RadioStation>,
    isPlayerExpanded: Boolean,
    isRadioPlayerExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectTracks: (List<Track>) -> Unit,
    onToggleReverse: () -> Unit,
    onSetSortType: (SortType) -> Unit,
    onTogglePlayer: (Boolean) -> Unit,
    onToggleRadioPlayer: (Boolean) -> Unit,
    onToggleAddRadioDialog: (Boolean) -> Unit,
    onToggleTrackSelection: (Track) -> Unit,
    onToggleStationSelection: (RadioStation) -> Unit,
    showAddRadioDialog: Boolean,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val playlistId = remember(navBackStackEntry) { navBackStackEntry?.arguments?.getLong("playlistId") ?: 0L }
    val folderPath = remember(navBackStackEntry) { navBackStackEntry?.arguments?.getString("folderPath") ?: "" }
    val focusRequester = remember { FocusRequester() }
    
    val isPlaylistDetail = remember(currentRoute) { currentRoute?.startsWith("playlist_detail") == true }
    val isFolderDetail = remember(currentRoute) { currentRoute?.startsWith("folder_detail") == true }
    val isTracksScreen = remember(currentRoute) { currentRoute == Screen.Tracks.route }
    val isRadioScreen = remember(currentRoute) { currentRoute == Screen.Radio.route }
    
    val canSearchHere = remember(isTracksScreen, isPlaylistDetail, isFolderDetail) {
        isTracksScreen || isPlaylistDetail || isFolderDetail
    }

    val shouldShowSort = remember(currentRoute) { 
        currentRoute == Screen.Tracks.route || currentRoute?.contains("library_") == true || currentRoute?.startsWith("playlist_detail") == true 
    }

    val isCollapsed by remember { derivedStateOf { scrollBehavior.state.collapsedFraction > 0.8f } }

    var showTrackPickerDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showRenamePlaylistDialog by remember { mutableStateOf(false) }
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
                    if (isSearching && canSearchHere) {
                        val placeholder = when {
                            isTracksScreen -> "Search tracks..."
                            isPlaylistDetail -> "Search in playlist..."
                            isFolderDetail -> "Search in folder..."
                            else -> "Search..."
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(placeholder) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            singleLine = true
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    } else {
                        Text(dynamicTitle)
                    }
                },
                navigationIcon = {
                    if (selectedTracks.isNotEmpty() || selectedStations.isNotEmpty()) {
                        IconButton(onClick = { onClearSelection() }) { Icon(Icons.Rounded.Close, "Clear") }
                    } else if (canPop && !isSearching) {
                        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                    }
                },
                actions = {
                    MainActionsSection(
                        currentRoute = currentRoute,
                        selectedTracks = selectedTracks,
                        selectedStations = selectedStations,
                        isSearching = isSearching,
                        isTracksScreen = isTracksScreen,
                        isPlaylistDetail = isPlaylistDetail,
                        isFolderDetail = isFolderDetail,
                        shouldShowSort = shouldShowSort,
                        isReversed = isReversed,
                        isCollapsed = isCollapsed,
                        canSearchHere = canSearchHere,
                        onToggleSearch = onToggleSearch,
                        onToggleReverse = onToggleReverse,
                        onSetSortType = onSetSortType,
                        onExportPlaylist = { exportLauncher.launch("Playlist.m3u") },
                        onDeletePlaylist = { showDeletePlaylistDialog = true },
                        onRenamePlaylist = { showRenamePlaylistDialog = true },
                        onDeleteTracks = { showDeleteTracksDialog = true },
                        onDeleteStations = { showDeleteStationsDialog = true },
                        onRemoveFromPlaylist = { showRemoveFromPlaylistDialog = true },
                        onAddToPlaylist = { showAddToPlaylistDialog = true },
                        onShowTrackInfo = { showTrackInfoForSelection = it },
                        onSelectAll = {
                            if (isPlaylistDetail) {
                                trackViewModel.getPlaylistTracksSync(playlistId).let { onSelectTracks(it) }
                            } else if (isFolderDetail) {
                                trackViewModel.getFolderTracksSync(folderPath).let { onSelectTracks(it) }
                            }
                        },
                        playerViewModel = playerViewModel,
                        radioViewModel = radioViewModel
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (selectedTracks.isEmpty() && selectedStations.isEmpty()) {
                if (isPlaylistDetail) {
                    FloatingActionButton(onClick = { showTrackPickerDialog = true }, modifier = Modifier.padding(bottom = 140.dp), containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
                } else if (isRadioScreen) {
                    FloatingActionButton(onClick = { onToggleAddRadioDialog(true) }, modifier = Modifier.padding(bottom = 140.dp), containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Add, null) }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            JasmineNavHost(
                navController = navController,
                trackViewModel = trackViewModel,
                playerViewModel = playerViewModel,
                radioViewModel = radioViewModel,
                onNavigateToPlayer = { /* Пусто */ },
                onNavigateToRadioPlayer = { onToggleRadioPlayer(true) },
                selectedTracks = selectedTracks,
                onToggleTrackSelection = onToggleTrackSelection,
                selectedStations = selectedStations,
                onToggleStationSelection = onToggleStationSelection,
                onAddTracksToPlaylist = { showTrackPickerDialog = true },
                showAddRadioDialog = showAddRadioDialog,
                onDismissRadioDialog = { onToggleAddRadioDialog(false) },
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            )
            
            // ВАЖНО: Мы убираем canPop из BackHandler, чтобы системный жест Predictive Back 
            // мог обработать переходы между экранами самостоятельно через NavHost.
            BackHandler(enabled = selectedTracks.isNotEmpty() || selectedStations.isNotEmpty() || isPlayerExpanded || isRadioPlayerExpanded || isSearching || showAddRadioDialog) {
                when {
                    showAddRadioDialog -> onToggleAddRadioDialog(false)
                    isRadioPlayerExpanded -> onToggleRadioPlayer(false)
                    selectedTracks.isNotEmpty() || selectedStations.isNotEmpty() -> onClearSelection()
                    isPlayerExpanded -> onTogglePlayer(false)
                    isSearching -> { onToggleSearch(); onSearchQueryChange("") }
                }
            }

            // Bottom Gradient Overlay
            Box(modifier = Modifier.fillMaxWidth().height(240.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.colorScheme.surface))))
        }
    }

    // Bottom Layers
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars), contentAlignment = Alignment.BottomCenter) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPlayer(viewModel = playerViewModel, onClick = { if (isRadioMode) onToggleRadioPlayer(true) else onTogglePlayer(true) }, modifier = Modifier.padding(bottom = 8.dp))
            JasmineBottomBar(navController = navController)
        }
    }

    if (isPlayerExpanded) {
        PlayerScreen(viewModel = playerViewModel, trackViewModel = trackViewModel, navController = navController, onClose = { onTogglePlayer(false) })
    }

    if (isRadioPlayerExpanded) {
        val currentStation by playerViewModel.currentRadioStation.collectAsStateWithLifecycle()
        currentStation?.let { RadioPlayerScreen(station = it, playerViewModel = playerViewModel, onClose = { onToggleRadioPlayer(false) }) }
    }

    MainDialogs(
        showTrackPickerDialog = showTrackPickerDialog,
        showDeletePlaylistDialog = showDeletePlaylistDialog,
        showRenamePlaylistDialog = showRenamePlaylistDialog,
        showDeleteTracksDialog = showDeleteTracksDialog,
        showDeleteStationsDialog = showDeleteStationsDialog,
        showRemoveFromPlaylistDialog = showRemoveFromPlaylistDialog,
        showAddToPlaylistDialog = showAddToPlaylistDialog,
        showTrackInfoForSelection = showTrackInfoForSelection,
        selectedTracks = selectedTracks,
        selectedStations = selectedStations,
        playlistId = playlistId,
        trackViewModel = trackViewModel,
        playerViewModel = playerViewModel,
        radioViewModel = radioViewModel,
        navController = navController,
        onDismissTrackPicker = { showTrackPickerDialog = false },
        onDismissDeletePlaylist = { showDeletePlaylistDialog = false },
        onDismissRenamePlaylist = { showRenamePlaylistDialog = false },
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
    currentRoute: String?,
    selectedTracks: Set<Track>,
    selectedStations: Set<RadioStation>,
    isSearching: Boolean,
    isTracksScreen: Boolean,
    isPlaylistDetail: Boolean,
    isFolderDetail: Boolean,
    shouldShowSort: Boolean,
    isReversed: Boolean,
    isCollapsed: Boolean,
    canSearchHere: Boolean,
    onToggleSearch: () -> Unit,
    onToggleReverse: () -> Unit,
    onSetSortType: (SortType) -> Unit,
    onExportPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeleteTracks: () -> Unit,
    onDeleteStations: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowTrackInfo: (Track) -> Unit,
    onSelectAll: () -> Unit,
    playerViewModel: PlayerViewModel,
    radioViewModel: RadioViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMoreMenu by remember { mutableStateOf(false) }

    if (selectedTracks.isNotEmpty() || selectedStations.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedTracks.isNotEmpty() && (isPlaylistDetail || isFolderDetail)) {
                IconButton(onClick = { onSelectAll() }) { Icon(Icons.Rounded.SelectAll, "Select All") }
            }

            if (selectedTracks.isNotEmpty()) {
                if (selectedTracks.size == 1) IconButton(onClick = { onShowTrackInfo(selectedTracks.first()) }) { Icon(Icons.Rounded.Info, null) }
                if (isPlaylistDetail) IconButton(onClick = { onRemoveFromPlaylist() }) { Icon(Icons.Rounded.PlaylistRemove, null, tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = { playerViewModel.addTracksToQueue(selectedTracks.toList()) }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }
                IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }, shape = RoundedCornerShape(24.dp), modifier = Modifier.width(220.dp)) {
                    DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = { showMoreMenu = false; ShareHelper.shareTracks(context, selectedTracks.toList()) })
                    DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }, onClick = { showMoreMenu = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text("Delete from device", fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showMoreMenu = false; onDeleteTracks() }, colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error))
                }
            } else {
                if (selectedStations.size == 1) IconButton(onClick = { clipboardManager.setText(AnnotatedString(selectedStations.first().url)); android.widget.Toast.makeText(context, "URL copied", android.widget.Toast.LENGTH_SHORT).show() }) { Icon(Icons.Rounded.ContentCopy, null) }
                IconButton(onClick = { onDeleteStations() }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    } else {
        AnimatedVisibility(visible = isCollapsed || isSearching || shouldShowSort || canSearchHere) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaylistDetail && !isSearching) {
                    IconButton(onClick = { onExportPlaylist() }) { Icon(Icons.Rounded.FileUpload, null) }
                    
                    var showPlaylistMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showPlaylistMenu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                    DropdownMenu(expanded = showPlaylistMenu, onDismissRequest = { showPlaylistMenu = false }, shape = RoundedCornerShape(24.dp)) {
                        DropdownMenuItem(text = { Text("Edit Details") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { showPlaylistMenu = false; onRenamePlaylist() })
                        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { showPlaylistMenu = false; onDeletePlaylist() })
                    }
                }
                if (canSearchHere) IconButton(onClick = { onToggleSearch() }) { Icon(if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search, null) }
                if (!isSearching && shouldShowSort) {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, null) }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, shape = RoundedCornerShape(24.dp)) {
                        DropdownMenuItem(text = { Text("By Name") }, onClick = { onSetSortType(SortType.BY_TITLE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Artist") }, onClick = { onSetSortType(SortType.BY_ARTIST); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Date Added") }, onClick = { onSetSortType(SortType.BY_DATE); showSortMenu = false })
                        DropdownMenuItem(text = { Text("By Duration") }, onClick = { onSetSortType(SortType.BY_DURATION); showSortMenu = false })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(if (isReversed) "Normal Order" else "Reverse Order") }, leadingIcon = { Icon(Icons.Rounded.FilterList, null) }, onClick = { onToggleReverse(); showSortMenu = false })
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
    showRenamePlaylistDialog: Boolean,
    showDeleteTracksDialog: Boolean,
    showDeleteStationsDialog: Boolean,
    showRemoveFromPlaylistDialog: Boolean,
    showAddToPlaylistDialog: Boolean,
    showTrackInfoForSelection: Track?,
    selectedTracks: Set<Track>,
    selectedStations: Set<RadioStation>,
    playlistId: Long,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    radioViewModel: RadioViewModel,
    navController: androidx.navigation.NavController,
    onDismissTrackPicker: () -> Unit,
    onDismissDeletePlaylist: () -> Unit,
    onDismissRenamePlaylist: () -> Unit,
    onDismissDeleteTracks: () -> Unit,
    onDismissDeleteStations: () -> Unit,
    onDismissRemoveFromPlaylist: () -> Unit,
    onDismissAddToPlaylist: () -> Unit,
    onDismissTrackInfo: () -> Unit,
    onClearSelection: () -> Unit
) {
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

    if (showRenamePlaylistDialog) {
        val playlists by trackViewModel.playlists.collectAsStateWithLifecycle()
        val currentPlaylist = remember(playlists, playlistId) { playlists.find { it.id == playlistId } }
        val currentPlaylistName = currentPlaylist?.name ?: "Playlist"
        val currentPlaylistCover = currentPlaylist?.coverUri

        var newName by remember { mutableStateOf(currentPlaylistName) }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var showEditor by remember { mutableStateOf(false) }

        val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { 
                selectedImageUri = it
                showEditor = true
            }
        }

        if (showEditor && selectedImageUri != null) {
            PlaylistCoverEditor(
                imageUri = selectedImageUri!!,
                onDismiss = { showEditor = false },
                onConfirm = { bitmap ->
                    trackViewModel.updatePlaylistCover(playlistId, bitmap)
                    showEditor = false
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismissRenamePlaylist,
                title = { Text("Edit Playlist Details") },
                text = {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        ) {
                            AlbumArt(albumArtUri = currentPlaylistCover, modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White)
                            }
                        }

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank() && newName != currentPlaylistName) {
                            trackViewModel.renamePlaylist(playlistId, newName)
                        }
                        onDismissRenamePlaylist()
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismissRenamePlaylist) { Text("Cancel") } },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }

    if (showDeletePlaylistDialog) {
        AlertDialog(onDismissRequest = onDismissDeletePlaylist, title = { Text("Delete Playlist") }, text = { Text("Delete this playlist?") }, confirmButton = { TextButton(onClick = { trackViewModel.deletePlaylist(playlistId); onDismissDeletePlaylist(); navController.popBackStack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissDeletePlaylist() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteTracksDialog) {
        AlertDialog(onDismissRequest = onDismissDeleteTracks, title = { Text("Delete Tracks") }, text = { Text("Delete ${selectedTracks.size} tracks from device?") }, confirmButton = { TextButton(onClick = { onDismissDeleteTracks(); playerViewModel.prepareForDeletion(selectedTracks.toList()); trackViewModel.deleteTracks(selectedTracks.toList()); onClearSelection() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { onDismissDeleteTracks() }) { Text("Cancel") } }, shape = RoundedCornerShape(28.dp))
    }

    if (showDeleteStationsDialog) {
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
