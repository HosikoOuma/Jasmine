package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nkds.hosikoouma.jasmine.PlaybackService
import com.nkds.hosikoouma.jasmine.data.FavoritesRepository
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) controllerFuture?.get() else null

    private val favoritesRepository = FavoritesRepository(application)

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist = _playlist.asStateFlow()

    private var originalPlaylist: List<Track> = emptyList()

    val isCurrentFavorite: StateFlow<Boolean> = combine(
        _currentTrack,
        favoritesRepository.favoriteTrackIds
    ) { track, favoriteIds ->
        track?.let { favoriteIds.contains(it.id.toString()) } ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var progressJob: Job? = null

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            setupController()
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        val controller = controller ?: return
        
        controller.currentMediaItem?.let { updateCurrentTrack(it) }
        updatePlaylist()
        
        _isPlaying.value = controller.isPlaying
        _duration.value = if (controller.duration > 0) controller.duration else 0L
        _progress.value = if (controller.currentPosition > 0) controller.currentPosition else 0L
        _shuffleModeEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        
        if (controller.isPlaying) startProgressUpdate()

        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { updateCurrentTrack(it) }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updatePlaylist()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = controller.duration
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
                updatePlaylist()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun updateCurrentTrack(mediaItem: MediaItem) {
        _currentTrack.value = mediaToTrack(mediaItem)
    }

    private fun updatePlaylist() {
        val controller = controller ?: return
        val items = mutableListOf<Track>()
        for (i in 0 until controller.mediaItemCount) {
            val mediaItem = controller.getMediaItemAt(i)
            items.add(mediaToTrack(mediaItem))
        }
        _playlist.value = items
    }

    private fun mediaToTrack(mediaItem: MediaItem): Track {
        return Track(
            id = mediaItem.mediaId.toLongOrNull() ?: 0L,
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
            duration = 0,
            contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = mediaItem.mediaMetadata.artworkUri
        )
    }

    fun toggleFavoriteCurrent() {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track.id.toString())
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val controller = controller ?: return
        originalPlaylist = tracks 
        
        val mediaItems = tracks.map { track -> createMediaItem(track) }
        
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    private fun createMediaItem(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.albumArtUri)
                    .build()
            )
            .build()
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val controller = controller ?: return
        controller.moveMediaItem(fromIndex, toIndex)
    }

    fun skipToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _progress.value = position
    }

    fun skipToNext() {
        controller?.seekToNext()
    }

    fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        val controller = controller ?: return
        val currentlyEnabled = _shuffleModeEnabled.value
        
        if (!currentlyEnabled) {
            // Activate shuffle by reordering the timeline items
            val currentItems = _playlist.value.toMutableList()
            if (currentItems.isEmpty()) return
            
            val currentTrack = _currentTrack.value
            val currentIndex = currentItems.indexOfFirst { it.id == currentTrack?.id }
            
            val currentPosition = controller.currentPosition
            
            // Remove current track, shuffle others, put current back at 0
            val removedTrack = if (currentIndex != -1) currentItems.removeAt(currentIndex) else null
            currentItems.shuffle()
            if (removedTrack != null) currentItems.add(0, removedTrack)
            
            val mediaItems = currentItems.map { createMediaItem(it) }
            controller.setMediaItems(mediaItems, 0, currentPosition)
            _shuffleModeEnabled.value = true
        } else {
            // Restore original order
            if (originalPlaylist.isNotEmpty()) {
                val currentTrack = _currentTrack.value
                val indexInOriginal = originalPlaylist.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
                val currentPosition = controller.currentPosition
                
                val mediaItems = originalPlaylist.map { createMediaItem(it) }
                controller.setMediaItems(mediaItems, indexInOriginal, currentPosition)
            }
            _shuffleModeEnabled.value = false
        }
    }

    fun toggleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                controller?.let {
                    _progress.value = it.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        stopProgressUpdate()
    }
}
