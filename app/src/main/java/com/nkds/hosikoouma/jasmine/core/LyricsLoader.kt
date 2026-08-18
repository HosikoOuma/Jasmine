package com.nkds.hosikoouma.jasmine.core

import com.nkds.hosikoouma.jasmine.data.LyricsRepository
import com.nkds.hosikoouma.jasmine.data.LyricsHelper
import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import com.nkds.hosikoouma.jasmine.datamodels.LyricsLine
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LyricsLoader(private val lyricsRepository: LyricsRepository) {

    private val _localLyrics = MutableStateFlow<String?>(null)
    val localLyrics: StateFlow<String?> = _localLyrics.asStateFlow()

    private val _remoteLyrics = MutableStateFlow<Lyrics?>(null)
    val remoteLyrics: StateFlow<Lyrics?> = _remoteLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private var lyricsJob: Job? = null
    private var lastLoadedTrackId: String? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val syncedLocalLyrics: StateFlow<List<LyricsLine>?> = _localLyrics
        .map { withContext(Dispatchers.Default) { LyricsHelper.parseLrc(it) } }
        .stateIn(scope, SharingStarted.Lazily, null)

    val syncedRemoteLyrics: StateFlow<List<LyricsLine>?> = _remoteLyrics
        .map { withContext(Dispatchers.Default) { LyricsHelper.parseLrc(it?.syncedLyrics) } }
        .stateIn(scope, SharingStarted.Lazily, null)

    fun loadLyrics(track: Track?, viewModelScope: CoroutineScope) {
        val track = track ?: return
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

    fun reset() {
        lastLoadedTrackId = null
        _localLyrics.value = null
        _remoteLyrics.value = null
    }
}
