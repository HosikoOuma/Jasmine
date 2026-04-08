package com.nkds.hosikoouma.jasmine.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksScreen(
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    var isFavoritesMode by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    val tracks by if (isFavoritesMode) {
        trackViewModel.favoriteTracks.collectAsState()
    } else {
        trackViewModel.filteredTracks.collectAsState()
    }
    
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isRefreshing by trackViewModel.isRefreshing.collectAsState()

    LaunchedEffect(isFavoritesMode) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        playerViewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { trackViewModel.loadTracks() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isEmpty()) {
                        if (isFavoritesMode) Text("No favorites yet") else CircularProgressIndicator()
                    } else {
                        Text("Nothing found")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 70.dp, bottom = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                            SwipeableTrackCard(
                                track = track,
                                isCurrent = currentTrack?.id == track.id,
                                isPlaying = isPlaying,
                                isManualMarkingEnabled = false,
                                onSwipeToAdd = { playerViewModel.addToQueue(track, showToast = true) },
                                onClick = {
                                    playerViewModel.playTracks(tracks, index)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    FastScrollbar(
                        listState = listState,
                        itemCount = tracks.size,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 80.dp, bottom = 180.dp, end = 4.dp)
                            .width(20.dp)
                    )
                }
            }

            // Кнопка перемешивания слева (идентична по размеру правой панели)
            Surface(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .align(Alignment.TopStart),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .height(40.dp)
                        .width(80.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            if (tracks.isNotEmpty()) {
                                // Запускаем перемешанный список и активируем режим перемешивания
                                playerViewModel.shuffleAndPlay(tracks)
                                onNavigateToPlayer()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle all",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Панель переключения режимов справа
            Surface(
                modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .align(Alignment.TopEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeToggleButton(
                        selected = !isFavoritesMode,
                        icon = Icons.Default.MusicNote,
                        onClick = { isFavoritesMode = false }
                    )
                    
                    ModeToggleButton(
                        selected = isFavoritesMode,
                        icon = Icons.Default.Favorite,
                        onClick = { isFavoritesMode = true }
                    )
                }
            }
        }
    }
}

@Composable
fun FastScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val maxHeight = constraints.maxHeight.toFloat()
        val firstVisibleIndex = listState.firstVisibleItemIndex
        val layoutInfo = listState.layoutInfo
        val visibleItemsCount = layoutInfo.visibleItemsInfo.size
        if (itemCount > visibleItemsCount && visibleItemsCount > 0) {
            val thumbHeight = (visibleItemsCount.toFloat() / itemCount) * maxHeight
            val scrollPercent = firstVisibleIndex.toFloat() / (itemCount - visibleItemsCount).coerceAtLeast(1)
            val thumbOffset = scrollPercent * (maxHeight - thumbHeight)
            Box(
                modifier = Modifier
                    .offset(y = (thumbOffset / LocalContext.current.resources.displayMetrics.density).dp)
                    .width(4.dp)
                    .height((thumbHeight / LocalContext.current.resources.displayMetrics.density).dp)
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}

@Composable
fun ModeToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "icon"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(backgroundColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
