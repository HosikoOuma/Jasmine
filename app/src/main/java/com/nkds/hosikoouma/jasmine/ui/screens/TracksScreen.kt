package com.nkds.hosikoouma.jasmine.ui.screens

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nkds.hosikoouma.jasmine.data.ShareHelper
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.AddToPlaylistDialog
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.TrackInfoBottomSheet
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksScreen(
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    selectedTracks: Set<Track>,
    onToggleTrackSelection: (Track) -> Unit
) {
    var isFavoritesMode by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    
    val tracks by if (isFavoritesMode) {
        trackViewModel.favoriteTracks.collectAsState()
    } else {
        trackViewModel.filteredTracks.collectAsState()
    }
    
    val searchQuery by trackViewModel.searchQuery.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isRefreshing by trackViewModel.isRefreshing.collectAsState()
    val isLoaded by trackViewModel.isLoaded.collectAsState()

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
            if (!isLoaded && tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (searchQuery.isEmpty()) {
                        Text(if (isFavoritesMode) "No favorites yet" else "No tracks found")
                    } else {
                        Text("Nothing found")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .simpleVerticalScrollbar(listState),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 70.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                        val isSelected = selectedTracks.contains(track)
                        SwipeableTrackCard(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            isSelected = isSelected,
                            isManualMarkingEnabled = true,
                            enabled = selectedTracks.isEmpty(), 
                            onSwipeAction = { 
                                if (track.isManual) {
                                    playerViewModel.removeFromQueue(track)
                                } else {
                                    playerViewModel.addToQueue(track, showToast = true) 
                                }
                            },
                            onClick = {
                                if (selectedTracks.isNotEmpty()) {
                                    selectionVibrate(vibrator)
                                    onToggleTrackSelection(track)
                                } else {
                                    playerViewModel.playTracks(tracks, index)
                                    onNavigateToPlayer()
                                }
                            },
                            onLongClick = {
                                selectionVibrate(vibrator)
                                onToggleTrackSelection(track)
                            }
                        )
                    }
                }
            }

            if (selectedTracks.isEmpty()) {
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
                        Icon(Icons.Rounded.Shuffle, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                // Mode Selector
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
                            icon = Icons.Rounded.MusicNote,
                            onClick = { isFavoritesMode = false }
                        )
                        ModeToggleButton(
                            selected = isFavoritesMode,
                            icon = Icons.Rounded.Favorite,
                            onClick = { isFavoritesMode = true }
                        )
                    }
                }
            }
        }
    }
}

private fun selectionVibrate(vibrator: Vibrator?) {
    if (vibrator == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(15, 120))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(15)
    }
}

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    width: Dp = 6.dp
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbarAlpha"
    )

    val color = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()

    return this
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown()
                if (down.position.x > size.width - 40.dp.toPx()) {
                    verticalDrag(down.id) { change ->
                        change.consume()
                        val totalItems = state.layoutInfo.totalItemsCount
                        if (totalItems > 0) {
                            val ratio = (change.position.y / size.height).coerceIn(0f, 1f)
                            val targetIndex = (ratio * totalItems).toInt().coerceIn(0, totalItems - 1)
                            scope.launch {
                                state.scrollToItem(targetIndex)
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()

            val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

            if (needDrawScrollbar && firstVisibleElementIndex != null) {
                val elementCount = state.layoutInfo.totalItemsCount
                val scrollbarFullHeight = size.height
                
                if (elementCount <= state.layoutInfo.visibleItemsInfo.size) return@drawWithContent

                val scrollbarHeight = (scrollbarFullHeight / elementCount) * state.layoutInfo.visibleItemsInfo.size
                val scrollbarOffsetY = (scrollbarFullHeight / elementCount) * firstVisibleElementIndex

                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
                    size = Size(width.toPx(), scrollbarHeight),
                    alpha = alpha,
                    cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
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
