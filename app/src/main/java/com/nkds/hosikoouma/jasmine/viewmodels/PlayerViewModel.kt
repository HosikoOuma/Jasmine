package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nkds.hosikoouma.jasmine.PlaybackService
import com.nkds.hosikoouma.jasmine.data.FavoritesRepository
import com.nkds.hosikoouma.jasmine.data.LyricsHelper
import com.nkds.hosikoouma.jasmine.data.LyricsRepository
import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import com.nkds.hosikoouma.jasmine.datamodels.LyricsLine
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
    private val lyricsRepository = LyricsRepository(application)

    private val _currentTrackBase = MutableStateFlow<Track?>(null)
    private val _playlistBase = MutableStateFlow<List<Track>>(emptyList())
    private val _manualQueueUids = MutableStateFlow<Set<String>>(emptySet())

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

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private var originalPlaylist: List<Track> = emptyList()

    // Lyrics States
    private val _localLyrics = MutableStateFlow<String?>(null)
    val localLyrics = _localLyrics.asStateFlow()

    private val _remoteLyrics = MutableStateFlow<Lyrics?>(null)
    val remoteLyrics = _remoteLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    // ЧЕТКОЕ РАЗДЕЛЕНИЕ СИНХРОНИЗИРОВАННЫХ ТЕКСТОВ
    val syncedLocalLyrics: StateFlow<List<LyricsLine>?> = _localLyrics
        .map { LyricsHelper.parseLrc(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val syncedRemoteLyrics: StateFlow<List<LyricsLine>?> = _remoteLyrics
        .map { LyricsHelper.parseLrc(it?.syncedLyrics) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Reactive current track with manual marking
    val currentTrack: StateFlow<Track?> = combine(_currentTrackBase, _manualQueueUids) { track, manualUids ->
        track?.copy(isManual = manualUids.contains(track.uid))
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val playlist: StateFlow<List<Track>> = combine(_playlistBase, _manualQueueUids) { list, manualUids ->
        list.map { it.copy(isManual = manualUids.contains(it.uid)) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isCurrentFavorite: StateFlow<Boolean> = combine(
        currentTrack,
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

        viewModelScope.launch {
            currentTrack.collect { track ->
                if (track != null) {
                    delay(500)
                    loadLyrics(track, _duration.value)
                } else {
                    _localLyrics.value = null
                    _remoteLyrics.value = null
                }
            }
        }
    }

    private fun loadLyrics(track: Track, actualDuration: Long) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _localLyrics.value = lyricsRepository.getLocalLyrics(track)
            _remoteLyrics.value = lyricsRepository.getRemoteLyrics(track, actualDuration)
            _isLoadingLyrics.value = false
        }
    }

    private fun setupController() {
        val controller = controller ?: return
        
        updateCurrentTrack(controller.currentMediaItem)
        updatePlaylist()
        
        _isPlaying.value = controller.isPlaying
        _duration.value = if (controller.duration > 0) controller.duration else 0L
        _progress.value = if (controller.currentPosition > 0) controller.currentPosition else 0L
        _shuffleModeEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        
        if (controller.isPlaying) startProgressUpdate()

        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrack(mediaItem)
                updatePlaylist()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updatePlaylist()
                updateCurrentTrack(controller.currentMediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
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

    private fun updateCurrentTrack(mediaItem: MediaItem?) {
        val controller = controller ?: return
        if (mediaItem == null) {
            _currentTrackBase.value = null
            return
        }
        
        val timeline = controller.currentTimeline
        val window = Timeline.Window()
        val currentIndex = controller.currentMediaItemIndex
        
        val uid = if (!timeline.isEmpty && currentIndex != -1 && currentIndex < timeline.windowCount) {
            timeline.getWindow(currentIndex, window).uid.toString()
        } else {
            "fallback_${mediaItem.mediaId}_$currentIndex"
        }
        
        _currentTrackBase.value = mediaToTrack(mediaItem).copy(uid = uid)
    }

    private fun updatePlaylist() {
        val controller = controller ?: return
        val timeline = controller.currentTimeline
        val items = mutableListOf<Track>()
        val window = Timeline.Window()
        
        if (timeline.isEmpty) {
            for (i in 0 until controller.mediaItemCount) {
                val mediaItem = controller.getMediaItemAt(i)
                items.add(mediaToTrack(mediaItem).copy(uid = "fallback_${mediaItem.mediaId}_$i"))
            }
        } else {
            for (i in 0 until timeline.windowCount) {
                timeline.getWindow(i, window)
                val mediaItem = window.mediaItem ?: continue
                items.add(mediaToTrack(mediaItem).copy(uid = window.uid.toString()))
            }
        }
        _playlistBase.value = items
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
        val track = currentTrack.value ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track.id.toString())
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val controller = controller ?: return
        originalPlaylist = tracks 
        _manualQueueUids.value = emptySet()
        
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

    fun addToQueue(track: Track, showToast: Boolean = false) {
        val controller = controller ?: return
        val currentIdx = controller.currentMediaItemIndex
        if (currentIdx == -1) return

        var insertPos = currentIdx + 1
        val currentList = _playlistBase.value
        val manualUids = _manualQueueUids.value
        
        while (insertPos < currentList.size && manualUids.contains(currentList[insertPos].uid)) {
            insertPos++
        }

        controller.addMediaItem(insertPos, createMediaItem(track))
        
        // Sync original playlist
        val newList = originalPlaylist.toMutableList()
        val curTrack = _currentTrackBase.value
        val currentInOriginal = originalPlaylist.indexOfFirst { it.id == curTrack?.id }
        if (currentInOriginal != -1) {
            var manualEndInOriginal = currentInOriginal + 1
            while (manualEndInOriginal < originalPlaylist.size && originalPlaylist[manualEndInOriginal].isManual) {
                manualEndInOriginal++
            }
            newList.add(manualEndInOriginal, track.copy(isManual = true))
        } else {
            newList.add(track.copy(isManual = true))
        }
        originalPlaylist = newList

        viewModelScope.launch {
            delay(400)
            controller.currentTimeline.let { timeline ->
                if (insertPos < timeline.windowCount) {
                    val window = Timeline.Window()
                    val uid = timeline.getWindow(insertPos, window).uid.toString()
                    _manualQueueUids.value = _manualQueueUids.value + uid
                }
            }
        }

        if (showToast) {
            viewModelScope.launch {
                _toastEvent.emit("Added to queue: ${track.title}")
            }
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        controller?.moveMediaItem(fromIndex, toIndex)
    }

    fun skipToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _progress.value = position
    }

    fun skipToNext() { controller?.seekToNext() }
    fun skipToPrevious() { controller?.seekToPrevious() }

    fun toggleShuffle() {
        val controller = controller ?: return
        val currentlyEnabled = _shuffleModeEnabled.value
        
        if (!currentlyEnabled) {
            val currentItems = _playlistBase.value.toMutableList()
            if (currentItems.isEmpty()) return
            
            val curTrack = _currentTrackBase.value
            val currentIndex = currentItems.indexOfFirst { it.uid == curTrack?.uid }
            
            val currentPosition = controller.currentPosition
            val removedTrack = if (currentIndex != -1) currentItems.removeAt(currentIndex) else null
            currentItems.shuffle()
            if (removedTrack != null) currentItems.add(0, removedTrack)
            
            val mediaItems = currentItems.map { createMediaItem(it) }
            controller.setMediaItems(mediaItems, 0, currentPosition)
            _shuffleModeEnabled.value = true
        } else {
            if (originalPlaylist.isNotEmpty()) {
                val curTrack = _currentTrackBase.value
                val indexInOriginal = originalPlaylist.indexOfFirst { it.id == curTrack?.id }.coerceAtLeast(0)
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
                controller?.let { _progress.value = it.currentPosition }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() { progressJob?.cancel() }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        stopProgressUpdate()
    }
}
