package com.nkds.hosikoouma.jasmine.ui.screens

import android.os.Vibrator
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.simpleVerticalScrollbar
import com.nkds.hosikoouma.jasmine.ui.components.vibrateClick
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TrackViewModel

@Composable
fun OnRepeatScreen(
    navController: NavController,
    trackViewModel: TrackViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val tracks by trackViewModel.onRepeatTracks.collectAsStateWithLifecycle()
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val intervalDays by trackViewModel.onRepeatIntervalDays.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks on repeat yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .simpleVerticalScrollbar(listState),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OnRepeatHeader(
                        tracksCount = tracks.size,
                        intervalDays = intervalDays,
                        onShuffleClick = {
                            vibrateClick(context)
                            playerViewModel.shuffleAndPlay(tracks, sourceName = "On Repeat")
                            onNavigateToPlayer()
                        }
                    )
                }

                itemsIndexed(
                    items = tracks,
                    key = { _, track -> track.id }
                ) { index, track ->
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying,
                        onSwipeAction = {
                            if (track.isManual) {
                                playerViewModel.removeFromQueue(track)
                            } else {
                                playerViewModel.addToQueue(track, showToast = true)
                            }
                        },
                        onClick = {
                            playerViewModel.playTracks(tracks, index, sourceName = "On Repeat")
                            onNavigateToPlayer()
                        },
                        onLongClick = { VibrationUtils.performLongPressHaptic(haptic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnRepeatHeader(
    tracksCount: Int,
    intervalDays: Int,
    onShuffleClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "expandProgress"
    )

    val artSize = (150.dp + (screenWidth - 32.dp - 150.dp) * expandProgress).coerceAtLeast(0.dp)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnRepeatPlaceholderArt(
                size = artSize,
                onClick = { isExpanded = !isExpanded }
            )

            if (expandProgress < 0.5f) {
                Spacer(modifier = Modifier.width((20.dp * (1f - expandProgress * 2)).coerceAtLeast(0.dp)))
                
                OnRepeatInfo(
                    tracksCount = tracksCount,
                    intervalDays = intervalDays,
                    modifier = Modifier.weight(1f)
                )

                ShuffleButton(
                    modifier = Modifier.padding(start = 8.dp),
                    onShuffle = onShuffleClick
                )
            }
        }

        if (expandProgress >= 0.5f) {
            Spacer(modifier = Modifier.height((16.dp * ((expandProgress - 0.5f) * 2)).coerceAtLeast(0.dp)))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OnRepeatInfo(
                    tracksCount = tracksCount,
                    intervalDays = intervalDays,
                    modifier = Modifier.weight(1f)
                )
                
                ShuffleButton(
                    modifier = Modifier,
                    onShuffle = onShuffleClick
                )
            }
        }
    }
}

@Composable
private fun OnRepeatPlaceholderArt(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Repeat,
            contentDescription = null,
            modifier = Modifier.size(size * 0.4f),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun OnRepeatInfo(
    tracksCount: Int,
    intervalDays: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "$tracksCount tracks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "On Repeat • Last $intervalDays days",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
