package com.nkds.hosikoouma.jasmine.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nkds.hosikoouma.jasmine.core.utils.VibrationUtils
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.AlbumArt
import com.nkds.hosikoouma.jasmine.ui.components.SwipeableTrackCard
import com.nkds.hosikoouma.jasmine.ui.components.simpleVerticalScrollbar
import com.nkds.hosikoouma.jasmine.ui.components.vibrateClick
import com.nkds.hosikoouma.jasmine.viewmodels.PlayerViewModel
import com.nkds.hosikoouma.jasmine.viewmodels.TelegramCloudViewModel
import java.io.File

@Composable
fun TelegramChannelDetailScreen(
    chatId: Long,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    viewModel: TelegramCloudViewModel = hiltViewModel()
) {
    // ОПТИМИЗАЦИЯ: используем remember, чтобы Flow не пересоздавался на каждой рекомпозиции
    // Это предотвращает бесконечный цикл обновлений и 100% загрузку CPU
    val tracksFlow = remember(chatId) { viewModel.getTracksForChannel(chatId) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val channel = remember(channels, chatId) { channels.find { it.chatId == chatId } }
    val channelTitle = channel?.title ?: "Telegram Channel"
    
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (tracks.isEmpty() && channel == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .simpleVerticalScrollbar(listState),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ChannelHeader(
                        title = channelTitle,
                        tracksCount = tracks.size,
                        photoPath = channel?.photoPath,
                        onShuffleClick = {
                            vibrateClick(context)
                            playerViewModel.shuffleAndPlay(tracks, sourceName = "Cloud: $channelTitle")
                            onNavigateToPlayer()
                        }
                    )
                }

                itemsIndexed(tracks, key = { _, track -> track.uid }) { index, track ->
                    SwipeableTrackCard(
                        track = track,
                        isCurrent = currentTrack?.uid == track.uid,
                        isPlaying = isPlaying,
                        onSwipeAction = { playerViewModel.addToQueue(track, showToast = true) },
                        onClick = {
                            playerViewModel.playTracks(tracks, index, sourceName = "Cloud: $channelTitle")
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
private fun ChannelHeader(
    title: String,
    tracksCount: Int,
    photoPath: String?,
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
            val displayArt = remember(photoPath) {
                photoPath?.let { Uri.fromFile(File(it)) }
            }

            AlbumArt(
                albumArtUri = displayArt,
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = !isExpanded }
                    ),
                isLowRes = false
            )

            if (expandProgress < 0.5f) {
                Spacer(modifier = Modifier.width((20.dp * (1f - expandProgress * 2)).coerceAtLeast(0.dp)))
                
                Column(modifier = Modifier.weight(1f).graphicsLayer { alpha = 1f - expandProgress * 2 }) {
                    Text(
                        text = "$tracksCount tracks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Telegram Cloud",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ShuffleButton(
                    modifier = Modifier.padding(start = 8.dp).graphicsLayer { alpha = 1f - expandProgress * 2 },
                    onShuffle = onShuffleClick
                )
            }
        }

        if (expandProgress >= 0.5f) {
            Spacer(modifier = Modifier.height((16.dp * ((expandProgress - 0.5f) * 2)).coerceAtLeast(0.dp)))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer { alpha = (expandProgress - 0.5f) * 2 },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$tracksCount tracks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Telegram Cloud",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ShuffleButton(
                    modifier = Modifier,
                    onShuffle = onShuffleClick
                )
            }
        }
    }
}
