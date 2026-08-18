package com.nkds.hosikoouma.jasmine.core

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.nkds.hosikoouma.jasmine.core.utils.QueueUtils
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramStreamProxy
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.ui.components.ToastData
import com.nkds.hosikoouma.jasmine.ui.components.ToastType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class QueueManager(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val telegramDao: TelegramDao,
    private val telegramStreamProxy: TelegramStreamProxy,
    private val placeholderArtwork: ByteArray
) {

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _appToast = MutableStateFlow<ToastData?>(null)
    val appToast: StateFlow<ToastData?> = _appToast.asStateFlow()

    @Volatile
    var originalTrackList: List<Track> = emptyList()
        private set

    var playlistIndexByUid: Map<String, Track> = emptyMap()
        private set

    private var updatePlaylistJob: Job? = null
    private var lastPlaylistHash: Int = 0

    fun showToast(track: Track?, type: ToastType, message: String? = null) { _appToast.value = ToastData(track, type, message) }
    fun clearToast() { _appToast.value = null }

    fun updateCurrentTrackFromMedia(mediaItem: MediaItem?): Track? {
        if (mediaItem == null) return null
        return mediaToTrack(mediaItem).copy(uid = mediaItem.mediaId)
    }

    fun updatePlaylist(controller: MediaController, scope: CoroutineScope, onCurrentTrackUpdate: (MediaItem?) -> Unit) {
        updatePlaylistJob?.cancel()
        updatePlaylistJob = scope.launch {
            delay(50)

            val mediaItemsWithUids = withContext(Dispatchers.Main) {
                val result = mutableListOf<Pair<MediaItem, String>>()
                val timeline = controller.currentTimeline
                val window = Timeline.Window()
                for (i in 0 until timeline.windowCount) {
                    timeline.getWindow(i, window)
                    window.mediaItem?.let { result.add(it to it.mediaId) }
                }
                result
            }

            val newHash = mediaItemsWithUids.hashCode()
            if (newHash == lastPlaylistHash) return@launch
            lastPlaylistHash = newHash

            val items = withContext(Dispatchers.Default) {
                mediaItemsWithUids.map { (item, uid) -> mediaToTrack(item).copy(uid = uid) }
            }

            val currentSize = _playlist.value.size
            if (currentSize == items.size) {
                var same = true
                for (i in items.indices) {
                    if (_playlist.value[i].uid != items[i].uid) { same = false; break }
                }
                if (same) return@launch
            }

            withContext(Dispatchers.Main) {
                _playlist.value = items
                playlistIndexByUid = items.associateBy { it.uid }
                onCurrentTrackUpdate(controller.currentMediaItem)
            }
        }
    }

    fun playTracks(controller: MediaController, tracks: List<Track>, startIndex: Int, sourceName: String?, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val mediaItems = tracks.map { createMediaItem(it, sourceName = sourceName) }
            val savedRepeatMode = settingsRepository.repeatMode.first()
            withContext(Dispatchers.Main) {
                originalTrackList = emptyList()
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.shuffleModeEnabled = false
                controller.repeatMode = savedRepeatMode
                controller.prepare()
                controller.play()
            }
        }
    }

    fun playRadio(controller: MediaController, targetStation: RadioStation, allStations: List<RadioStation>, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val mediaItems = allStations.map { station ->
                val extras = Bundle().apply { putBoolean("isRadio", true) }
                MediaItem.Builder()
                    .setMediaId("radio_${station.id}")
                    .setUri(station.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(station.name)
                            .setArtist("Radio Stream")
                            .setArtworkData(placeholderArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .setExtras(extras)
                            .build()
                    ).build()
            }
            val startIndex = allStations.indexOfFirst { it.id == targetStation.id }.coerceAtLeast(0)
            withContext(Dispatchers.Main) {
                originalTrackList = emptyList()
                controller.setMediaItems(mediaItems, startIndex, 0L)
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
                controller.prepare()
                controller.play()
            }
        }
    }

    fun shuffleAndPlay(controller: MediaController, tracks: List<Track>, sourceName: String?, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            val shuffled = QueueUtils.fisherYatesCopy(tracks)
            val mediaItems = shuffled.map { createMediaItem(it, sourceName = sourceName) }
            val savedRepeatMode = settingsRepository.repeatMode.first()
            withContext(Dispatchers.Main) {
                originalTrackList = tracks.map { it.copy(uid = "${it.id}_original") }
                controller.setMediaItems(mediaItems, 0, 0L)
                controller.shuffleModeEnabled = false
                controller.repeatMode = savedRepeatMode
                controller.prepare()
                controller.play()
            }
        }
    }

    fun addToQueue(controller: MediaController, track: Track, sourceName: String?, isRadioMode: Boolean, scope: CoroutineScope, showToast: Boolean = false) {
        if (isRadioMode || controller.currentMediaItemIndex == -1) return
        var insertPos = controller.currentMediaItemIndex + 1
        val currentList = _playlist.value
        while (insertPos < currentList.size && currentList[insertPos].isManual) { insertPos++ }
        scope.launch(Dispatchers.Default) {
            val mediaItem = createMediaItem(track, isManual = true, sourceName = sourceName)
            withContext(Dispatchers.Main) {
                controller.addMediaItem(insertPos, mediaItem)
                if (showToast) _appToast.value = ToastData(track, ToastType.ADDED)
            }
        }
    }

    fun addTracksToQueue(controller: MediaController, tracks: List<Track>, sourceName: String?, isRadioMode: Boolean, scope: CoroutineScope) {
        if (isRadioMode || controller.currentMediaItemIndex == -1) return
        var insertPos = controller.currentMediaItemIndex + 1
        val currentList = _playlist.value
        while (insertPos < currentList.size && currentList[insertPos].isManual) { insertPos++ }
        scope.launch(Dispatchers.Default) {
            val mediaItems = tracks.map { createMediaItem(it, isManual = true, sourceName = sourceName) }
            withContext(Dispatchers.Main) {
                controller.addMediaItems(insertPos, mediaItems)
                if (tracks.isNotEmpty()) _appToast.value = ToastData(tracks.first(), ToastType.ADDED)
            }
        }
    }

    fun removeFromQueue(controller: MediaController, track: Track) {
        val index = _playlist.value.indexOfFirst { it.uid == track.uid }
        if (index != -1) {
            controller.removeMediaItem(index)
            _appToast.value = ToastData(track, ToastType.REMOVED)
        }
    }

    fun stopAndClearQueue(controller: MediaController) {
        controller.stop()
        controller.clearMediaItems()
        _playlist.value = emptyList()
        originalTrackList = emptyList()
    }

    fun moveTrack(controller: MediaController, fromIndex: Int, toIndex: Int) { controller.moveMediaItem(fromIndex, toIndex) }
    fun skipToQueueItem(controller: MediaController, index: Int) { controller.seekTo(index, 0L) }

    fun prepareForDeletion(controller: MediaController, tracksToDelete: List<Track>, currentTrackId: Long?) {
        val currentPlaylist = _playlist.value
        val indicesToRemove = tracksToDelete.mapNotNull { toDelete ->
            val idx = currentPlaylist.indexOfFirst { it.id == toDelete.id }
            if (idx != -1) idx else null
        }.distinct().sortedDescending()
        if (indicesToRemove.isEmpty()) return
        val isCurrentPlayingDeleted = tracksToDelete.any { it.id == currentTrackId }
        if (isCurrentPlayingDeleted && controller.mediaItemCount > 1) {
            if (controller.currentMediaItemIndex == controller.mediaItemCount - 1) controller.seekToPreviousMediaItem()
            else controller.seekToNextMediaItem()
        }
        indicesToRemove.forEach { controller.removeMediaItem(it) }
    }

    fun toggleShuffle(controller: MediaController, isRadioMode: Boolean, currentTrack: Track?, sourceName: String?, isShuffleCurrentlyEnabled: Boolean, scope: CoroutineScope, onShuffleChanged: (Boolean) -> Unit) {
        if (isRadioMode || _playlist.value.isEmpty() || currentTrack == null) return
        val currentPos = controller.currentPosition

        scope.launch(Dispatchers.Default) {
            val newList: List<Track>
            val shuffleEnabled: Boolean

            if (isShuffleCurrentlyEnabled) {
                if (originalTrackList.isNotEmpty()) {
                    newList = originalTrackList
                } else {
                    withContext(Dispatchers.Main) { onShuffleChanged(false) }
                    return@launch
                }
                shuffleEnabled = false
            } else {
                originalTrackList = _playlist.value
                val currentIndex = _playlist.value.indexOfFirst { it.uid == currentTrack.uid }.coerceAtLeast(0)
                newList = QueueUtils.buildAnchoredShuffleQueueSuspending(_playlist.value, currentIndex, startAtZero = true)
                shuffleEnabled = true
            }

            val newIndex = newList.indexOfFirst { it.uid == currentTrack.uid }.coerceAtLeast(0)
            val mediaItems = newList.map { track ->
                createMediaItem(track, track.isManual, existingUid = track.uid, sourceName = sourceName)
            }

            withContext(Dispatchers.Main) {
                controller.shuffleModeEnabled = false
                controller.setMediaItems(mediaItems, newIndex, currentPos)
                onShuffleChanged(shuffleEnabled)
            }
        }
    }

    suspend fun createMediaItem(track: Track, isManual: Boolean = false, existingUid: String? = null, sourceName: String? = null): MediaItem {
        val uid = existingUid ?: "${track.id}_${UUID.randomUUID()}"
        var playbackUri = track.contentUri

        if (track.contentUri.scheme == "telegram") {
            val song = telegramDao.getSongById(track.uid)
            if (song != null) {
                val proxyUrl = telegramStreamProxy.getProxyUrl(song.fileId)
                playbackUri = Uri.parse(proxyUrl)
            }
        }

        val extras = Bundle(4).apply {
            putString("path", track.path)
            putBoolean("isManual", isManual)
            putLong("duration", track.duration)
            putBoolean("isRadio", false)
            sourceName?.let { putString("sourceName", it) }
        }

        val metaBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setExtras(extras)

        if (track.albumArtUri != null) {
            metaBuilder.setArtworkUri(track.albumArtUri)
        } else {
            metaBuilder.setArtworkData(placeholderArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        return MediaItem.Builder()
            .setMediaId(uid)
            .setUri(playbackUri)
            .setMediaMetadata(metaBuilder.build())
            .build()
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
            isManual = extras?.getBoolean("isManual") ?: false,
            uid = mediaItem.mediaId
        )
    }

    private fun mediaIdToIdString(mediaId: String): String {
        return if (mediaId.contains("_")) mediaId.split("_")[0] else mediaId
    }

    private fun mediaIdToLong(cleanId: String): Long = try {
        if (cleanId.startsWith("radio_")) cleanId.substring(6).toLong()
        else {
            val idToParse = if (cleanId.contains("_")) cleanId.split("_")[0] else cleanId
            idToParse.toLong()
        }
    } catch (e: Exception) { cleanId.hashCode().toLong() }
}
