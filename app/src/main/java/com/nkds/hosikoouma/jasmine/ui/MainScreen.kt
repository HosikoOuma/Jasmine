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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.navigation.JasmineNavHost
import com.nkds.hosikoouma.jasmine.ui.components.AddToPlaylistDialog
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.JasmineBottomBar
import com.nkds.hosikoouma.jasmine.ui.components.MiniPlayer
import com.nkds.hosikoouma.jasmine.ui.components.TrackInfoBottomSheet
import com.nkds.hosikoouma.jasmine.ui.screens.PlayerScreen
import com.nkds.hosikoouma.jasmine.ui.screens.RadioPlayerScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
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
    
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isRadioPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showTrackPickerDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showAddRadioDialog by remember { mutableStateOf(false) }

    // Общее состояние выделения треков
    var selectedTracks by remember { mutableStateOf(setOf<Track>()) }
    val isInSelectionMode by remember { derivedStateOf { selectedTracks.isNotEmpty() } }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteTracksDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showTrackInfoForSelection by remember { mutableStateOf<Track?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Снимаем выделение при смене экрана
    LaunchedEffect(currentRoute) {
        selectedTracks = emptySet()
    }

    val playlistId = navBackStackEntry?.arguments?.getLong("playlistId") ?: 0L
    val playlistTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())
    val playlists by trackViewModel.playlists.collectAsState()
    val currentPlaylistName = remember(playlists, playlistId) {
        playlists.find { it.id == playlistId }?.name ?: "Playlist"
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
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

    val dynamicTitle = remember(currentRoute, navBackStackEntry, currentPlaylistName, selectedTracks.size) {
        if (isInSelectionMode) {
            "${selectedTracks.size} selected"
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
                    val path = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    path.substringAfterLast("/")
                }
                currentRoute?.startsWith("playlist_detail") == true -> currentPlaylistName
                currentRoute == Screen.LibraryAlbums.route -> "Albums"
                currentRoute == Screen.LibraryArtists.route -> "Artists"
                currentRoute == Screen.LibraryFolders.route -> "Folders"
                currentRoute == Screen.LibraryPlaylists.route -> "Playlists"
                else -> {
                    val currentScreen = Screen.items.find { it.route == currentRoute }
                    currentScreen?.title ?: "Jasmine"
                }
            }
        }
    }

    val isMainDestination = remember(currentRoute) {
        Screen.items.any { it.route == currentRoute }
    }

    val canPop = remember(navBackStackEntry, isMainDestination) {
        navController.previousBackStackEntry != null && !isMainDestination
    }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    LaunchedEffect(isPlayerExpanded, isRadioPlayerExpanded) {
        if (isPlayerExpanded || isRadioPlayerExpanded) keyboardController?.hide()
    }

    val isRadioMode by playerViewModel.isRadioMode.collectAsState()

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val intent = activity?.intent
        if (intent?.getBooleanExtra("OPEN_PLAYER", false) == true) {
            // Проверяем режим радио при открытии из уведомления
            if (playerViewModel.isRadioMode.value) {
                isRadioPlayerExpanded = true
            } else {
                isPlayerExpanded = true
            }
            intent.removeExtra("OPEN_PLAYER")
        }
    }

    val isTracksScreen = currentRoute == Screen.Tracks.route
    val isRadioScreen = currentRoute == Screen.Radio.route
    val shouldShowSort = remember(currentRoute) {
        currentRoute == Screen.Tracks.route ||
        currentRoute == Screen.LibraryAlbums.route ||
        currentRoute == Screen.LibraryArtists.route ||
        currentRoute == Screen.LibraryFolders.route ||
        currentRoute == Screen.LibraryPlaylists.route ||
        currentRoute?.startsWith("playlist_detail") == true
    }

    val isCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.8f }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        if (isSearching && isTracksScreen) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { trackViewModel.setSearchQuery(it) },
                                placeholder = { Text("Search tracks...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        } else {
                            Text(dynamicTitle)
                        }
                    },
                    navigationIcon = {
                        if (isInSelectionMode) {
                            IconButton(onClick = { selectedTracks = emptySet() }) {
                                Icon(Icons.Rounded.Close, "Clear selection")
                            }
                        } else if (canPop && !isSearching) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (isInSelectionMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedTracks.size == 1) {
                                    IconButton(onClick = { showTrackInfoForSelection = selectedTracks.first() }) {
                                        Icon(Icons.Rounded.Info, null)
                                    }
                                }
                                IconButton(onClick = {
                                    playerViewModel.addTracksToQueue(selectedTracks.toList())
                                    selectedTracks = emptySet()
                                }) {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                                }
                                Box {
                                    IconButton(onClick = { showMoreMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, null)
                                    }
                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false },
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.width(220.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                ShareHelper.shareTracks(context, selectedTracks.toList())
                                                selectedTracks = emptySet()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add to playlist") },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                                            onClick = {
                                                showMoreMenu = false
                                                showAddToPlaylistDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete from device", fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showMoreMenu = false
                                                showDeleteTracksDialog = true
                                            },
                                            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }
                            }
                        } else {
                            AnimatedVisibility(
                                visible = (isCollapsed || isSearching || shouldShowSort) && (isTracksScreen || shouldShowSort),
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentRoute?.startsWith("playlist_detail") == true && !isSearching) {
                                        IconButton(onClick = { showDeletePlaylistDialog = true }) {
                                            Icon(Icons.Rounded.Delete, "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    if (isTracksScreen) {
                                        IconButton(onClick = { 
                                            if (isSearching) trackViewModel.setSearchQuery("")
                                            isSearching = !isSearching 
                                        }) {
                                            Icon(
                                                imageVector = if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search,
                                                contentDescription = "Search"
                                            )
                                        }
                                    }
                                    
                                    if (!isSearching && shouldShowSort) {
                                        Box {
                                            IconButton(onClick = { showSortMenu = true }) {
                                                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                                            }
                                            DropdownMenu(
                                                expanded = showSortMenu,
                                                onDismissRequest = { showSortMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("By Name") },
                                                    onClick = { trackViewModel.setSortType(SortType.BY_NAME); showSortMenu = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("By Artist") },
                                                    onClick = { trackViewModel.setSortType(SortType.BY_ARTIST); showSortMenu = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("By Date Added") },
                                                    onClick = { trackViewModel.setSortType(SortType.BY_DATE); showSortMenu = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("By Duration") },
                                                    onClick = { trackViewModel.setSortType(SortType.BY_DURATION); showSortMenu = false }
                                                )
                                                HorizontalDivider()
                                                val isReversed by trackViewModel.isReversed.collectAsState()
                                                DropdownMenuItem(
                                                    text = { Text(if (isReversed) "Normal Order" else "Reverse Order") },
                                                    leadingIcon = { Icon(Icons.Rounded.FilterList, null) },
                                                    onClick = { trackViewModel.toggleReverse(); showSortMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                if (!isInSelectionMode) {
                    if (currentRoute == Screen.LibraryPlaylists.route) {
                        FloatingActionButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier.padding(bottom = 140.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Rounded.Add, "Create Playlist")
                        }
                    } else if (currentRoute?.startsWith("playlist_detail") == true && playlistTracks.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { showTrackPickerDialog = true },
                            modifier = Modifier.padding(bottom = 140.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Rounded.Add, "Add tracks")
                        }
                    } else if (isRadioScreen) {
                        FloatingActionButton(
                            onClick = { showAddRadioDialog = true },
                            modifier = Modifier.padding(bottom = 140.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Rounded.Add, "Add station")
                        }
                    }
                }
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
                    onToggleTrackSelection = { track ->
                        selectedTracks = if (selectedTracks.contains(track)) selectedTracks - track else selectedTracks + track
                    },
                    onAddTracksToPlaylist = { showTrackPickerDialog = true },
                    showAddRadioDialog = showAddRadioDialog,
                    onDismissRadioDialog = { showAddRadioDialog = false },
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
                
                BackHandler(enabled = isInSelectionMode || isPlayerExpanded || isRadioPlayerExpanded || isSearching || canPop || showAddRadioDialog) {
                    when {
                        showAddRadioDialog -> showAddRadioDialog = false
                        isRadioPlayerExpanded -> isRadioPlayerExpanded = false
                        isInSelectionMode -> selectedTracks = emptySet()
                        isPlayerExpanded -> isPlayerExpanded = false
                        isSearching -> {
                            isSearching = false
                            trackViewModel.setSearchQuery("")
                        }
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
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MiniPlayer(
                    viewModel = playerViewModel,
                    onClick = { 
                        if (isRadioMode) isRadioPlayerExpanded = true else isPlayerExpanded = true 
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                JasmineBottomBar(navController = navController)
            }
        }

        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = ExitTransition.None
        ) {
            PlayerScreen(
                viewModel = playerViewModel,
                trackViewModel = trackViewModel,
                navController = navController,
                onClose = { isPlayerExpanded = false }
            )
        }

        AnimatedVisibility(
            visible = isRadioPlayerExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = ExitTransition.None
        ) {
            val currentStation by playerViewModel.currentRadioStation.collectAsState()
            
            currentStation?.let { station ->
                RadioPlayerScreen(
                    station = station,
                    playerViewModel = playerViewModel,
                    onClose = { isRadioPlayerExpanded = false }
                )
            }
        }
    }

    // Диалоги
    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            trackViewModel.createPlaylist(playlistName)
                            showCreatePlaylistDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTrackPickerDialog) {
        val allTracks by trackViewModel.allTracks.collectAsState()
        val pTracks by trackViewModel.getTracksForPlaylist(playlistId).collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { showTrackPickerDialog = false },
            title = { Text("Select Tracks") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    items(allTracks.size) { index ->
                        val track = allTracks[index]
                        val isAlreadyInPlaylist = pTracks.any { it.id == track.id }
                        ListItem(
                            leadingContent = {
                                AlbumArt(
                                    albumArtUri = track.albumArtUri,
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            },
                            headlineContent = { 
                                Text(
                                    track.title, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlreadyInPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            supportingContent = { 
                                Text(
                                    track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            trailingContent = {
                                if (isAlreadyInPlaylist) {
                                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                trackViewModel.addTrackToPlaylist(playlistId, track.id)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPickerDialog = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeletePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePlaylistDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete \"$currentPlaylistName\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        trackViewModel.deletePlaylist(playlistId)
                        showDeletePlaylistDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeleteTracksDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTracksDialog = false },
            title = { Text("Delete Tracks") },
            text = { Text("Are you sure you want to delete ${selectedTracks.size} tracks from your device? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteTracksDialog = false
                        playerViewModel.prepareForDeletion(selectedTracks.toList())
                        trackViewModel.deleteTracks(selectedTracks.toList())
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTracksDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAddToPlaylistDialog) {
        val currentTrack by playerViewModel.currentTrack.collectAsState()
        AddToPlaylistDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { pid ->
                currentTrack?.let { track ->
                    trackViewModel.addTrackToPlaylist(pid, track.id)
                    showAddToPlaylistDialog = false
                    Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                }
            },
            trackViewModel = trackViewModel
        )
    }

    if (showTrackInfoForSelection != null) {
        TrackInfoBottomSheet(
            track = showTrackInfoForSelection!!,
            onDismissRequest = { showTrackInfoForSelection = null }
        )
    }
}
