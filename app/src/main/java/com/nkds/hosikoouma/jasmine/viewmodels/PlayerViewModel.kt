package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
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
import com.nkds.hosikoouma.jasmine.data.RadioStation
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
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _currentRadioStation = MutableStateFlow<RadioStation?>(null)
    val currentRadioStation = _currentRadioStation.asStateFlow()

    private val _isRadioMode = MutableStateFlow(false)
    val isRadioMode = _isRadioMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist = _playlist.asStateFlow()

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

    private val _systemVolume = MutableStateFlow(0f)
    val systemVolume = _systemVolume.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private var originalPlaylist: List<Track> = emptyList()

    private val _localLyrics = MutableStateFlow<String?>(null)
    val localLyrics = _localLyrics.asStateFlow()

    private val _remoteLyrics = MutableStateFlow<Lyrics?>(null)
    val remoteLyrics = _remoteLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    val syncedLocalLyrics: StateFlow<List<LyricsLine>?> = _localLyrics
        .map { LyricsHelper.parseLrc(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val syncedRemoteLyrics: StateFlow<List<LyricsLine>?> = _remoteLyrics
        .map { LyricsHelper.parseLrc(it?.syncedLyrics) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isCurrentFavorite: StateFlow<Boolean> = combine(
        currentTrack,
        favoritesRepository.favoriteTrackIds
    ) { track, favoriteIds ->
        track?.let { favoriteIds.contains(it.id.toString()) } ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var progressJob: Job? = null

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolumeState()
            }
        }
    }

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

        updateVolumeState()
        application.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    private fun updateVolumeState() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        _systemVolume.value = current.toFloat() / max.toFloat()
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
            _currentTrack.value = null
            _isRadioMode.value = false
            _currentRadioStation.value = null
            return
        }
        
        val extras = mediaItem.mediaMetadata.extras
        val isRadio = extras?.getBoolean("isRadio") ?: false
        _isRadioMode.value = isRadio

        if (isRadio) {
            _currentRadioStation.value = RadioStation(
                id = mediaItem.mediaId.toLongOrNull() ?: 0L,
                name = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                url = mediaItem.localConfiguration?.uri?.toString() ?: ""
            )
            _currentTrack.value = null
        } else {
            val timeline = controller.currentTimeline
            val window = Timeline.Window()
            val currentIndex = controller.currentMediaItemIndex
            
            val uid = if (!timeline.isEmpty && currentIndex != -1 && currentIndex < timeline.windowCount) {
                timeline.getWindow(currentIndex, window).uid.toString()
            } else {
                "fallback_${mediaItem.mediaId}_$currentIndex"
            }
            
            _currentTrack.value = mediaToTrack(mediaItem).copy(uid = uid)
            _currentRadioStation.value = null
        }
    }

    private fun updatePlaylist() {
        val controller = controller ?: return
        val timeline = controller.currentTimeline
        val items = mutableListOf<Track>()
        val window = Timeline.Window()
        
        if (timeline.isEmpty) {
            for (i in 0 until controller.mediaItemCount) {
                val mediaItem = controller.getMediaItemAt(i)
                if (mediaItem.mediaMetadata.extras?.getBoolean("isRadio") != true) {
                    items.add(mediaToTrack(mediaItem).copy(uid = "fallback_${mediaItem.mediaId}_$i"))
                }
            }
        } else {
            for (i in 0 until timeline.windowCount) {
                timeline.getWindow(i, window)
                val mediaItem = window.mediaItem ?: continue
                if (mediaItem.mediaMetadata.extras?.getBoolean("isRadio") != true) {
                    items.add(mediaToTrack(mediaItem).copy(uid = window.uid.toString()))
                }
            }
        }
        _playlist.value = items
    }

    private fun mediaToTrack(mediaItem: MediaItem): Track {
        val extras = mediaItem.mediaMetadata.extras
        val path = extras?.getString("path") ?: ""
        val isManual = extras?.getBoolean("isManual") ?: false
        val duration = extras?.getLong("duration") ?: 0L

        return Track(
            id = mediaIdToLong(mediaItem.mediaId),
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
            duration = duration,
            contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = mediaItem.mediaMetadata.artworkUri,
            path = path,
            isManual = isManual
        )
    }

    private fun mediaIdToLong(mediaId: String): Long {
        return try {
            mediaId.toLong()
        } catch (e: Exception) {
            mediaId.hashCode().toLong()
        }
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
        _shuffleModeEnabled.value = false
        _isRadioMode.value = false
        
        val mediaItems = tracks.map { track -> createMediaItem(track) }
        
        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playRadio(station: RadioStation) {
        val controller = controller ?: return
        _isRadioMode.value = true
        _currentRadioStation.value = station
        _currentTrack.value = null

        val extras = Bundle().apply {
            putBoolean("isRadio", true)
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId("radio_${station.id}")
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("Radio Stream")
                    .setExtras(extras)
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun shuffleAndPlay(tracks: List<Track>) {
        val controller = controller ?: return
        originalPlaylist = tracks
        val shuffled = tracks.shuffled()
        _isRadioMode.value = false
        
        val mediaItems = shuffled.map { track -> createMediaItem(track) }
        
        controller.setMediaItems(mediaItems, 0, 0L)
        controller.prepare()
        controller.play()
        _shuffleModeEnabled.value = true
    }

    private fun createMediaItem(track: Track, isManual: Boolean = false): MediaItem {
        val extras = Bundle().apply {
            putString("path", track.path)
            putBoolean("isManual", isManual)
            putLong("duration", track.duration)
            putBoolean("isRadio", false)
        }
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.albumArtUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun addToQueue(track: Track, showToast: Boolean = false) {
        val controller = controller ?: return
        if (_isRadioMode.value) return // Don't add to queue in radio mode

        val currentIdx = controller.currentMediaItemIndex
        if (currentIdx == -1) return

        var insertPos = currentIdx + 1
        val currentList = _playlist.value
        
        while (insertPos < currentList.size && currentList[insertPos].isManual) {
            insertPos++
        }

        controller.addMediaItem(insertPos, createMediaItem(track, isManual = true))
        
        if (showToast) {
            viewModelScope.launch {
                _toastEvent.emit("Added to queue: ${track.title}")
            }
        }
    }

    fun addTracksToQueue(tracks: List<Track>) {
        val controller = controller ?: return
        if (_isRadioMode.value) return

        val currentIdx = controller.currentMediaItemIndex
        if (currentIdx == -1) return

        var insertPos = currentIdx + 1
        val currentList = _playlist.value
        
        while (insertPos < currentList.size && currentList[insertPos].isManual) {
            insertPos++
        }

        val mediaItems = tracks.map { createMediaItem(it, isManual = true) }
        controller.addMediaItems(insertPos, mediaItems)

        viewModelScope.launch {
            _toastEvent.emit("Added ${tracks.size} tracks to queue")
        }
    }

    fun removeFromQueue(track: Track) {
        val controller = controller ?: return
        val currentList = _playlist.value
        val index = currentList.indexOfFirst { it.uid == track.uid }

        if (index != -1) {
            controller.removeMediaItem(index)
            viewModelScope.launch {
                _toastEvent.emit("Removed from queue: ${track.title}")
            }
        }
    }

    fun prepareForDeletion(tracksToDelete: List<Track>) {
        val controller = controller ?: return
        val currentTrackId = _currentTrack.value?.id
        val currentPlaylist = _playlist.value
        
        val indicesToRemove = tracksToDelete.mapNotNull { toDelete ->
            val idx = currentPlaylist.indexOfFirst { it.id == toDelete.id }
            if (idx != -1) idx else null
        }.distinct().sortedDescending()

        if (indicesToRemove.isEmpty()) return

        val isCurrentPlayingDeleted = tracksToDelete.any { it.id == currentTrackId }

        if (isCurrentPlayingDeleted) {
            val currentIndex = controller.currentMediaItemIndex
            val totalItems = controller.mediaItemCount
            
            if (totalItems > 1) {
                if (currentIndex == totalItems - 1) {
                    controller.seekToPreviousMediaItem()
                } else {
                    controller.seekToNextMediaItem()
                }
            }
        }

        indicesToRemove.forEach { index ->
            controller.removeMediaItem(index)
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

    fun setSystemVolume(vol: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (vol * max).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        _systemVolume.value = vol
    }

    fun skipToNext() { controller?.seekToNext() }
    
    fun skipToPrevious() { 
        val controller = controller ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
            _progress.value = 0L
        } else {
            controller.seekToPrevious()
        }
    }

    fun toggleShuffle() {
        val controller = controller ?: return
        if (_isRadioMode.value) return
        val currentlyEnabled = _shuffleModeEnabled.value
        
        if (!currentlyEnabled) {
            val currentItems = _playlist.value.toMutableList()
            if (currentItems.isEmpty()) return
            
            val curTrack = _currentTrack.value
            val currentIndex = currentItems.indexOfFirst { it.uid == curTrack?.uid }
            
            val currentPosition = controller.currentPosition
            val removedTrack = if (currentIndex != -1) currentItems.removeAt(currentIndex) else null
            currentItems.shuffle()
            if (removedTrack != null) currentItems.add(0, removedTrack)
            
            val mediaItems = currentItems.map { createMediaItem(it, isManual = it.isManual) }
            controller.setMediaItems(mediaItems, 0, currentPosition)
            _shuffleModeEnabled.value = true
        } else {
            if (originalPlaylist.isNotEmpty()) {
                val curTrack = _currentTrack.value
                val indexInOriginal = originalPlaylist.indexOfFirst { it.id == curTrack?.id }.coerceAtLeast(0)
                val currentPosition = controller.currentPosition
                
                val mediaItems = originalPlaylist.map { createMediaItem(it, isManual = false) }
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
        try {
            getApplication<Application>().unregisterReceiver(volumeReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
