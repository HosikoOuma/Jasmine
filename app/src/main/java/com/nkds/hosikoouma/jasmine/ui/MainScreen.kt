package com.nkds.hosikoouma.jasmine.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nkds.hosikoouma.jasmine.datamodels.Screen
import com.nkds.hosikoouma.jasmine.navigation.JasmineNavHost
import com.nkds.hosikoouma.jasmine.ui.components.JasmineBottomBar
import com.nkds.hosikoouma.jasmine.ui.components.MiniPlayer
import com.nkds.hosikoouma.jasmine.ui.screens.PlayerScreen
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.SortType
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
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
    var isSearching by remember { mutableStateOf(false) }
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val dynamicTitle = remember(currentRoute, navBackStackEntry) {
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

    val isMainDestination = remember(currentRoute) {
        Screen.items.any { it.route == currentRoute }
    }

    val canPop = remember(navBackStackEntry, isMainDestination) {
        navController.previousBackStackEntry != null && !isMainDestination
    }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    LaunchedEffect(isPlayerExpanded) {
        if (isPlayerExpanded) keyboardController?.hide()
    }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val intent = activity?.intent
        if (intent?.getBooleanExtra("OPEN_PLAYER", false) == true) {
            isPlayerExpanded = true
            intent.removeExtra("OPEN_PLAYER")
        }
    }

    val isTracksScreen = currentRoute == Screen.Tracks.route
    
    // Определяем, нужно ли показывать кнопку сортировки на текущем экране
    val shouldShowSort = remember(currentRoute) {
        currentRoute == Screen.Tracks.route || 
        currentRoute == Screen.LibraryAlbums.route ||
        currentRoute == Screen.LibraryArtists.route ||
        currentRoute == Screen.LibraryFolders.route ||
        currentRoute == Screen.LibraryPlaylists.route
    }

    val isCollapsed by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0.8f }
    }

    BackHandler(enabled = isPlayerExpanded || isSearching || canPop) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (isSearching) {
            isSearching = false
            trackViewModel.setSearchQuery("")
        } else if (canPop) {
            navController.popBackStack()
        }
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
                        if (canPop && !isSearching) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = (isCollapsed || isSearching || shouldShowSort) && (isTracksScreen || shouldShowSort),
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                if (currentRoute == Screen.LibraryPlaylists.route) {
                    LargeFloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.padding(bottom = 140.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Rounded.Add, "Create Playlist")
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
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                )
                
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
                    onClick = { isPlayerExpanded = true },
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
                onClose = { isPlayerExpanded = false }
            )
        }
    }

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
}
