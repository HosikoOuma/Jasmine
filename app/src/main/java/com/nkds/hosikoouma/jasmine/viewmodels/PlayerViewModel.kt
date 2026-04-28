package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nkds.hosikoouma.jasmine.PlaybackService
import com.nkds.hosikoouma.jasmine.core.utils.QueueUtils
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramStreamProxy
import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import com.nkds.hosikoouma.jasmine.datamodels.LyricsLine
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.ToastData
import com.nkds.hosikoouma.jasmine.ui.components.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val favoritesRepository: FavoritesRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    private val telegramDao: TelegramDao,
    private val telegramStreamProxy: TelegramStreamProxy
) : AndroidViewModel(application) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) {
            try { controllerFuture?.get() } catch (e: Exception) { null }
        } else null

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _currentRadioStation = MutableStateFlow<RadioStation?>(null)
    val currentRadioStation = _currentRadioStation.asStateFlow()

    private var _radioTrackTitle = MutableStateFlow<String?>(null)
    private var _radioTrackArtist = MutableStateFlow<String?>(null)
    val radioTrackTitle = _radioTrackTitle.asStateFlow()
    val radioTrackArtist = _radioTrackArtist.asStateFlow()

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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch = _playbackPitch.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private val _appToast = MutableStateFlow<ToastData?>(null)
    val appToast = _appToast.asStateFlow()

    private val _localLyrics = MutableStateFlow<String?>(null)
    val localLyrics = _localLyrics.asStateFlow()

    private val _remoteLyrics = MutableStateFlow<Lyrics?>(null)
    val remoteLyrics = _remoteLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics = _isLoadingLyrics.asStateFlow()

    private val _currentSource = MutableStateFlow<String?>(null)
    val currentSource = _currentSource.asStateFlow()

    val syncedLocalLyrics: StateFlow<List<LyricsLine>?> = _localLyrics
        .map { withContext(Dispatchers.Default) { LyricsHelper.parseLrc(it) } }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val syncedRemoteLyrics: StateFlow<List<LyricsLine>?> = _remoteLyrics
        .map { withContext(Dispatchers.Default) { LyricsHelper.parseLrc(it?.syncedLyrics) } }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isCurrentFavorite: StateFlow<Boolean> = combine(
        currentTrack,
        favoritesRepository.favoriteTrackIds
    ) { track, favoriteIds ->
        track?.let { favoriteIds.contains(it.id.toString()) } ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private var progressJob: Job? = null
    private var lyricsJob: Job? = null
    private var lastLoadedTrackId: String? = null
    private var originalTrackList: List<Track> = emptyList()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") updateVolumeState()
        }
    }

    init {
        initializeController()
        setupVolumeReceiver()
        telegramStreamProxy.start()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({ setupController() }, MoreExecutors.directExecutor())
    }

    private fun setupVolumeReceiver() {
        updateVolumeState()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplication<Application>().registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                getApplication<Application>().registerReceiver(volumeReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Failed to register volume receiver", e)
        }
    }

    private fun updateVolumeState() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        _systemVolume.value = current.toFloat() / max.toFloat()
    }

    fun loadLyricsForCurrentTrack() {
        val track = _currentTrack.value ?: return
        val trackUniqueId = "${track.id}_${track.title}"
        if (lastLoadedTrackId == trackUniqueId) return

        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoadingLyrics.value = true
            _localLyrics.value = null
            _remoteLyrics.value = null
            
            val local = lyricsRepository.getLocalLyrics(track)
            val remote = lyricsRepository.getRemoteLyrics(track, track.duration)
            
            withContext(Dispatchers.Main) {
                lastLoadedTrackId = trackUniqueId
                _localLyrics.value = local
                _remoteLyrics.value = remote
                _isLoadingLyrics.value = false
            }
        }
    }

    private fun setupController() {
        val controller = controller ?: return
        _repeatMode.value = controller.repeatMode
        _playbackSpeed.value = controller.playbackParameters.speed
        _playbackPitch.value = controller.playbackParameters.pitch
        _isPlaying.value = controller.isPlaying
        _duration.value = if (controller.duration > 0) controller.duration else 0L
        _progress.value = if (controller.currentPosition > 0) controller.currentPosition else 0L
        
        updatePlaylist()
        updateCurrentTrack(controller.currentMediaItem)
        
        if (controller.isPlaying) startProgressUpdate()

        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrack(mediaItem)
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
                    _progress.value = controller.currentPosition
                }
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (_isRadioMode.value) {
                    parseRadioMetadata(mediaMetadata)
                } else {
                    updateCurrentTrack(controller.currentMediaItem)
                }
            }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                _playbackSpeed.value = params.speed
                _playbackPitch.value = params.pitch
            }
            override fun onRepeatModeChanged(mode: Int) { _repeatMode.value = mode }
        })
    }

    private fun parseRadioMetadata(mediaMetadata: MediaMetadata) {
        val title = mediaMetadata.title?.toString()
        val artist = mediaMetadata.artist?.toString()
        if (artist == "Radio Stream" && title != null && title.contains(" - ")) {
            val parts = title.split(" - ", limit = 2)
            _radioTrackArtist.value = parts[0].trim()
            _radioTrackTitle.value = parts[1].trim()
        } else {
            _radioTrackTitle.value = title ?: _currentRadioStation.value?.name
            _radioTrackArtist.value = if (!artist.isNullOrBlank()) artist else "Radio Stream"
        }
    }

    private fun updateCurrentTrack(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            resetCurrentTrackState()
            return
        }
        
        val extras = mediaItem.mediaMetadata.extras
        val isRadio = extras?.getBoolean("isRadio") ?: (mediaItem.mediaId.startsWith("radio_"))
        _isRadioMode.value = isRadio
        _currentSource.value = extras?.getString("sourceName")

        if (isRadio) {
            val station = RadioStation(
                id = mediaIdToLong(mediaIdToIdString(mediaItem.mediaId)),
                name = mediaItem.mediaMetadata.station?.toString() ?: mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                url = mediaItem.localConfiguration?.uri?.toString() ?: ""
            )
            if (_currentRadioStation.value?.id != station.id) {
                _currentRadioStation.value = station
                _currentTrack.value = null
            }
            parseRadioMetadata(mediaItem.mediaMetadata)
        } else {
            val uid = mediaItem.mediaId
            val trackFromMedia = mediaToTrack(mediaItem).copy(uid = uid)
            
            val trackFromPlaylist = _playlist.value.find { it.uid == uid }
            val newTrack = if (trackFromMedia.title == "Unknown" && trackFromPlaylist != null) trackFromPlaylist else trackFromMedia

            val isDataImproved = (_currentTrack.value?.title == "Unknown" || _currentTrack.value?.title == null) && newTrack.title != "Unknown"
            
            if (_currentTrack.value?.uid != newTrack.uid || _currentTrack.value?.id != newTrack.id || isDataImproved) {
                _currentTrack.value = newTrack
                _currentRadioStation.value = null
                _radioTrackTitle.value = null
                _radioTrackArtist.value = null
                
                if (lastLoadedTrackId != "${newTrack.id}_${newTrack.title}") {
                    _localLyrics.value = null
                    _remoteLyrics.value = null
                }
            }
        }
    }

    private fun mediaIdToIdString(mediaId: String): String {
        return if (mediaId.contains("_")) {
            val parts = mediaId.split("_")
            parts[0] // Всегда возвращаем только первую часть (оригинальный ID)
        } else mediaId
    }

    private fun resetCurrentTrackState() {
        _currentTrack.value = null
        _isRadioMode.value = false
        _currentRadioStation.value = null
        _radioTrackTitle.value = null
        _radioTrackArtist.value = null
        lastLoadedTrackId = null
        _currentSource.value = null
        _localLyrics.value = null
        _remoteLyrics.value = null
    }

    private var updatePlaylistJob: Job? = null
    private fun updatePlaylist() {
        val controller = controller ?: return
        val mediaItemsWithUids = mutableListOf<Pair<MediaItem, String>>()
        val timeline = controller.currentTimeline
        
        if (timeline.isEmpty) {
            for (i in 0 until controller.mediaItemCount) {
                val item = controller.getMediaItemAt(i)
                mediaItemsWithUids.add(item to item.mediaId)
            }
        } else {
            val window = Timeline.Window()
            for (i in 0 until timeline.windowCount) {
                timeline.getWindow(i, window)
                window.mediaItem?.let { mediaItemsWithUids.add(it to it.mediaId) }
            }
        }
        
        updatePlaylistJob?.cancel()
        updatePlaylistJob = viewModelScope.launch(Dispatchers.Default) {
            val items = mediaItemsWithUids.map { (item, uid) -> mediaToTrack(item).copy(uid = uid) }
            if (_playlist.value != items) {
                _playlist.value = items
                if (!_shuffleModeEnabled.value) {
                    originalTrackList = items
                }
                withContext(Dispatchers.Main) {
                    updateCurrentTrack(controller.currentMediaItem)
                }
            }
        }
    }

    private fun mediaToTrack(mediaItem: MediaItem): Track {
        val extras = mediaItem.mediaMetadata.extras
        return Track(
            id = mediaIdToLong(mediaIdToIdString(mediaId = mediaItem.mediaId)),
            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
            album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
            duration = extras?.getLong("duration") ?: 0L,
            contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = mediaItem.mediaMetadata.artworkUri,
            path = extras?.getString("path") ?: "",
            isManual = extras?.getBoolean("isManual") ?: false
        )
    }

    private fun mediaIdToLong(cleanId: String): Long = try {
        if (cleanId.startsWith("radio_")) {
            cleanId.substring(6).toLong()
        } else {
            // Если в ID остались подчеркивания (не должно быть после mediaIdToIdString), берем первую часть
            val idToParse = if (cleanId.contains("_")) cleanId.split("_")[0] else cleanId
            idToParse.toLong()
        }
    } catch (e: Exception) { cleanId.hashCode().toLong() }

    fun toggleFavoriteCurrent() {
        val track = currentTrack.value ?: return
        viewModelScope.launch { favoritesRepository.toggleFavorite(track.id.toString()) }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int, sourceName: String? = null) {
        val controller = controller ?: return
        _isRadioMode.value = false
        _currentSource.value = sourceName
        viewModelScope.launch(Dispatchers.Default) {
            val mediaItems = tracks.map { createMediaItem(it, sourceName = sourceName) }
            val tracksWithUids = tracks.zip(mediaItems).map { (t, m) -> t.copy(uid = m.mediaId) }
            val savedRepeatMode = settingsRepository.repeatMode.first()
            withContext(Dispatchers.Main) {
                originalTrackList = tracksWithUids
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.shuffleModeEnabled = false
                _shuffleModeEnabled.value = false
                controller.repeatMode = savedRepeatMode
                controller.prepare()
                controller.play()
            }
        }
    }

    fun playRadio(targetStation: RadioStation, allStations: List<RadioStation>) {
        val controller = controller ?: return
        _isRadioMode.value = true
        _currentRadioStation.value = targetStation
        _currentSource.value = "Radio"
        viewModelScope.launch(Dispatchers.Default) {
            val mediaItems = allStations.map { station ->
                val extras = Bundle().apply { putBoolean("isRadio", true) }
                MediaItem.Builder().setMediaId("radio_${station.id}").setUri(station.url).setMediaMetadata(MediaMetadata.Builder().setTitle(station.name).setArtist("Radio Stream").setExtras(extras).build()).build()
            }
            val startIndex = allStations.indexOfFirst { it.id == targetStation.id }.coerceAtLeast(0)
            withContext(Dispatchers.Main) {
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
                controller.prepare()
                controller.play()
            }
        }
    }

    fun shuffleAndPlay(tracks: List<Track>, sourceName: String? = null) {
        val controller = controller ?: return
        _isRadioMode.value = false
        _currentSource.value = sourceName
        viewModelScope.launch(Dispatchers.Default) {
            val shuffled = QueueUtils.fisherYatesCopy(tracks)
            val mediaItems = shuffled.map { createMediaItem(it, sourceName = sourceName) }
            val tracksWithUids = shuffled.zip(mediaItems).map { (t, m) -> t.copy(uid = m.mediaId) }
            val savedRepeatMode = settingsRepository.repeatMode.first()
            withContext(Dispatchers.Main) {
                originalTrackList = tracksWithUids
                controller.setMediaItems(mediaItems, 0, 0L)
                controller.shuffleModeEnabled = false
                _shuffleModeEnabled.value = true
                controller.repeatMode = savedRepeatMode
                controller.prepare()
                controller.play()
            }
        }
    }

    private suspend fun createMediaItem(track: Track, isManual: Boolean = false, existingUid: String? = null, sourceName: String? = null): MediaItem {
        val uid = existingUid ?: "${track.id}_${UUID.randomUUID()}"
        var playbackUri = track.contentUri

        if (track.contentUri.scheme == "telegram") {
            val song = telegramDao.getSongById(track.uid)
            if (song != null) {
                val proxyUrl = telegramStreamProxy.getProxyUrl(song.fileId)
                playbackUri = Uri.parse(proxyUrl)
            }
        }

        val extras = Bundle().apply { 
            putString("path", track.path)
            putBoolean("isManual", isManual)
            putLong("duration", track.duration)
            putBoolean("isRadio", false)
            sourceName?.let { putString("sourceName", it) }
        }
        return MediaItem.Builder()
            .setMediaId(uid)
            .setUri(playbackUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.albumArtUri)
                    .setExtras(extras)
                    .build()
            ).build()
    }

    fun addToQueue(track: Track, showToast: Boolean = false) {
        val controller = controller ?: return
        if (_isRadioMode.value || controller.currentMediaItemIndex == -1) return
        var insertPos = controller.currentMediaItemIndex + 1
        val currentList = _playlist.value
        while (insertPos < currentList.size && currentList[insertPos].isManual) { insertPos++ }
        viewModelScope.launch(Dispatchers.Default) {
            val mediaItem = createMediaItem(track, isManual = true, sourceName = _currentSource.value)
            withContext(Dispatchers.Main) { 
                controller.addMediaItem(insertPos, mediaItem) 
                if (showToast) {
                    _appToast.value = ToastData(track, ToastType.ADDED)
                }
            }
        }
    }

    fun addTracksToQueue(tracks: List<Track>) {
        val controller = controller ?: return
        if (_isRadioMode.value || controller.currentMediaItemIndex == -1) return
        var insertPos = controller.currentMediaItemIndex + 1
        val currentList = _playlist.value
        while (insertPos < currentList.size && currentList[insertPos].isManual) { insertPos++ }
        viewModelScope.launch(Dispatchers.Default) {
            val mediaItems = tracks.map { createMediaItem(it, isManual = true, sourceName = _currentSource.value) }
            withContext(Dispatchers.Main) { 
                controller.addMediaItems(insertPos, mediaItems) 
                if (tracks.isNotEmpty()) {
                    _appToast.value = ToastData(tracks.first(), ToastType.ADDED)
                }
            }
        }
    }

    fun showToast(track: Track?, type: ToastType, message: String? = null) {
        _appToast.value = ToastData(track, type, message)
    }

    fun clearAddedToast() {
        _appToast.value = null
    }

    fun removeFromQueue(track: Track) {
        val index = _playlist.value.indexOfFirst { it.uid == track.uid }
        if (index != -1) {
            controller?.removeMediaItem(index)
            _appToast.value = ToastData(track, ToastType.REMOVED)
        }
    }

    fun stopAndClearQueue() {
        controller?.let {
            it.stop()
            it.clearMediaItems()
        }
        resetCurrentTrackState()
        _playlist.value = emptyList()
        originalTrackList = emptyList()
    }

    fun prepareForDeletion(tracksToDelete: List<Track>) {
        val controller = controller ?: return
        val currentPlaylist = _playlist.value
        val indicesToRemove = tracksToDelete.mapNotNull { toDelete ->
            val idx = currentPlaylist.indexOfFirst { it.id == toDelete.id }
            if (idx != -1) idx else null
        }.distinct().sortedDescending()
        if (indicesToRemove.isEmpty()) return
        val isCurrentPlayingDeleted = tracksToDelete.any { it.id == _currentTrack.value?.id }
        if (isCurrentPlayingDeleted && controller.mediaItemCount > 1) {
            if (controller.currentMediaItemIndex == controller.mediaItemCount - 1) controller.seekToPreviousMediaItem()
            else controller.seekToNextMediaItem()
        }
        indicesToRemove.forEach { controller.removeMediaItem(it) }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) { controller?.moveMediaItem(fromIndex, toIndex) }
    fun skipToQueueItem(index: Int) { controller?.seekTo(index, 0L) }
    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(position: Long) { controller?.seekTo(position); _progress.value = position }
    fun setSystemVolume(vol: Float) { viewModelScope.launch(Dispatchers.IO) { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0); _systemVolume.value = vol } }
    fun setPlaybackSpeed(speed: Float) { controller?.let { it.playbackParameters = PlaybackParameters(speed, it.playbackParameters.pitch) } }
    fun setPlaybackPitch(pitch: Float) { controller?.let { it.playbackParameters = PlaybackParameters(it.playbackParameters.speed, pitch) } }
    fun skipToNext() { controller?.seekToNext() }
    fun skipToPrevious() { val controller = controller ?: return; if (controller.currentPosition > 3000L && !_isRadioMode.value) { controller.seekTo(0L); _progress.value = 0L } else controller.seekToPrevious() }

    fun toggleShuffle() {
        val controller = controller ?: return
        if (_isRadioMode.value || _playlist.value.isEmpty()) return
        
        val willEnable = !_shuffleModeEnabled.value
        val currentTracks = _playlist.value
        val currentTrack = _currentTrack.value ?: return
        val currentPos = controller.currentPosition
        
        viewModelScope.launch(Dispatchers.Default) {
            val newList: List<Track>
            
            if (willEnable) {
                originalTrackList = currentTracks
                val currentIndex = currentTracks.indexOfFirst { it.uid == currentTrack.uid }.coerceAtLeast(0)
                newList = QueueUtils.buildAnchoredShuffleQueueSuspending(currentTracks, currentIndex, startAtZero = true)
            } else {
                if (originalTrackList.isNotEmpty()) {
                    newList = originalTrackList
                } else {
                    withContext(Dispatchers.Main) { _shuffleModeEnabled.value = false }
                    return@launch
                }
            }
            
            val newIndex = newList.indexOfFirst { it.uid == currentTrack.uid }.coerceAtLeast(0)
            val mediaItems = newList.map { track -> 
                createMediaItem(track, track.isManual, existingUid = track.uid, sourceName = _currentSource.value) 
            }
            
            withContext(Dispatchers.Main) {
                controller.setMediaItems(mediaItems, newIndex, currentPos)
                _shuffleModeEnabled.value = willEnable
                controller.shuffleModeEnabled = false
            }
        }
    }

    fun toggleRepeatMode() { controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }

    private fun startProgressUpdate() { progressJob?.cancel(); progressJob = viewModelScope.launch { while (true) { controller?.let { _progress.value = it.currentPosition }; delay(1000) } } }
    private fun stopProgressUpdate() { progressJob?.cancel() }

    override fun onCleared() {
        super.onCleared()
        try {
            controller?.release()
        } catch (_: Exception) {}
        
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture!!)
        }
        
        stopProgressUpdate()
        try {
            getApplication<Application>().unregisterReceiver(volumeReceiver)
        } catch (_: Exception) {}
        telegramStreamProxy.stop()
    }
}
