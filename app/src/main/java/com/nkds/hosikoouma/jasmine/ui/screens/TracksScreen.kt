package com.nkds.hosikoouma.jasmine.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current
    
    val tracks by if (isFavoritesMode) {
        trackViewModel.favoriteTracks.collectAsState()
    } else {
        trackViewModel.filteredTracks.collectAsState()
    }
    
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isRefreshing by trackViewModel.isRefreshing.collectAsState()

    LaunchedEffect(isFavoritesMode) { listState.scrollToItem(0) }
    LaunchedEffect(searchQuery) { if (searchQuery.isNotEmpty()) listState.scrollToItem(0) }

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
                }
            }

            // Shuffle Button
            Surface(
                modifier = Modifier.padding(top = 16.dp, start = 16.dp).align(Alignment.TopStart),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                tonalElevation = 8.dp,
                shadowElevation = 6.dp
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .padding(4.dp).height(40.dp).width(80.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (tracks.isNotEmpty()) {
                                playerViewModel.shuffleAndPlay(tracks)
                                onNavigateToPlayer()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shuffle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }

            // Mode Selector (Bubble)
            Surface(
                modifier = Modifier.padding(top = 16.dp, end = 16.dp).align(Alignment.TopEnd),
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
fun ModeToggleButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "icon"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
    }
}
