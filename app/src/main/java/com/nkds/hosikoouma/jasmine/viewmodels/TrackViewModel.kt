package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.TrackScanner
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class SortType {
    BY_NAME,
    BY_ARTIST,
    BY_DATE,
    BY_DURATION
}

data class Album(val name: String, val artist: String, val tracks: List<Track>)
data class Artist(val name: String, val tracks: List<Track>)
data class Folder(val name: String, val path: String, val tracks: List<Track>)
data class Playlist(val id: Long, val name: String, val tracks: List<Track>, val createdAt: Long = 0)

class TrackViewModel(application: Application) : AndroidViewModel(application) {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks = _tracks.asStateFlow()
    
    private val trackScanner = TrackScanner(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val playlistRepository = PlaylistRepository(application)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.BY_DATE)
    val sortType = _sortType.asStateFlow()

    private val _isReversed = MutableStateFlow(false)
    val isReversed = _isReversed.asStateFlow()

    private val _pendingDeleteIntent = MutableSharedFlow<IntentSender>()
    val pendingDeleteIntent = _pendingDeleteIntent.asSharedFlow()

    val minDurationLimit = settingsRepository.minTrackDuration
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val favoriteTrackIds: StateFlow<Set<String>> = favoritesRepository.favoriteTrackIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val filteredTracks: StateFlow<List<Track>> = combine(
        _tracks, _searchQuery, _sortType, _isReversed, minDurationLimit
    ) { tracks, query, sort, reversed, minDur ->
        val timeFiltered = tracks.filter { it.duration >= minDur * 1000L }
        filterAndSort(timeFiltered, query, sort, reversed)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteTracks: StateFlow<List<Track>> = combine(
        _tracks, favoriteTrackIds, _searchQuery, minDurationLimit
    ) { tracks, favIds, query, minDur ->
        val favorites = tracks.filter { favIds.contains(it.id.toString()) && it.duration >= minDur * 1000L }
        if (query.isBlank()) favorites else favorites.filter { 
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Сортировка для альбомов
    val albums: StateFlow<List<Album>> = combine(_tracks, _sortType, _isReversed) { tracks, sort, reversed ->
        val grouped = tracks.groupBy { it.album }
            .map { (name, albumTracks) -> Album(name, albumTracks.first().artist, albumTracks) }
        
        val sorted = when (sort) {
            SortType.BY_NAME -> grouped.sortedBy { it.name.lowercase() }
            SortType.BY_ARTIST -> grouped.sortedBy { it.artist.lowercase() }
            SortType.BY_DATE -> grouped.sortedByDescending { it.tracks.maxOfOrNull { t -> t.id } ?: 0L }
            SortType.BY_DURATION -> grouped.sortedBy { it.tracks.sumOf { t -> t.duration } }
        }
        if (reversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Сортировка для артистов
    val artists: StateFlow<List<Artist>> = combine(_tracks, _sortType, _isReversed) { tracks, sort, reversed ->
        val grouped = tracks.groupBy { it.artist }
            .map { Artist(it.key, it.value) }
        
        val sorted = when (sort) {
            SortType.BY_NAME, SortType.BY_ARTIST -> grouped.sortedBy { it.name.lowercase() }
            SortType.BY_DATE -> grouped.sortedByDescending { it.tracks.maxOfOrNull { t -> t.id } ?: 0L }
            SortType.BY_DURATION -> grouped.sortedBy { it.tracks.sumOf { t -> t.duration } }
        }
        if (reversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Сортировка для папок
    val folders: StateFlow<List<Folder>> = combine(_tracks, _sortType, _isReversed) { tracks, sort, reversed ->
        val grouped = tracks.groupBy { 
            val file = File(it.path)
            file.parent ?: "Unknown"
        }.map { Folder(it.key.substringAfterLast("/"), it.key, it.value) }
        
        val sorted = when (sort) {
            SortType.BY_NAME -> grouped.sortedBy { it.name.lowercase() }
            SortType.BY_ARTIST -> grouped.sortedBy { it.tracks.first().artist.lowercase() }
            SortType.BY_DATE -> grouped.sortedByDescending { it.tracks.maxOfOrNull { t -> t.id } ?: 0L }
            SortType.BY_DURATION -> grouped.sortedBy { it.tracks.sumOf { t -> t.duration } }
        }
        if (reversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Сортировка для плейлистов
    val playlists: StateFlow<List<Playlist>> = combine(playlistRepository.allPlaylists, _sortType, _isReversed) { entities, sort, reversed ->
        val grouped = entities.map { Playlist(it.id, it.name, emptyList(), it.createdAt) }
        val sorted = when (sort) {
            SortType.BY_NAME -> grouped.sortedBy { it.name.lowercase() }
            SortType.BY_DATE -> grouped.sortedByDescending { it.createdAt }
            else -> grouped.sortedBy { it.name.lowercase() }
        }
        if (reversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadTracks()
        loadSortSettings()
    }

    private fun loadSortSettings() {
        viewModelScope.launch {
            val defaultSort = settingsRepository.defaultSortType.first()
            _sortType.value = try { SortType.valueOf(defaultSort) } catch (e: Exception) { SortType.BY_DATE }
            _isReversed.value = settingsRepository.isDefaultSortReversed.first()
        }
    }

    private fun filterAndSort(tracks: List<Track>, query: String, sort: SortType, reversed: Boolean): List<Track> {
        var result = if (query.isBlank()) tracks else tracks.filter { 
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
        }
        result = when (sort) {
            SortType.BY_NAME -> result.sortedBy { it.title.lowercase() }
            SortType.BY_ARTIST -> result.sortedBy { it.artist.lowercase() }
            SortType.BY_DATE -> result.sortedBy { it.id }
            SortType.BY_DURATION -> result.sortedBy { it.duration }
        }
        return if (reversed) result.reversed() else result
    }

    fun loadTracks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val trackList = trackScanner.scanTracks()
            _tracks.value = trackList
            _isRefreshing.value = false
            _isLoaded.value = true
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track.id.toString())
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    fun toggleReverse() {
        _isReversed.value = !_isReversed.value
    }

    fun saveDefaultSortSettings(type: SortType, reversed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDefaultSortType(type.name)
            settingsRepository.setDefaultSortReversed(reversed)
        }
    }

    // Playlist Operations
    fun createPlaylist(name: String) = viewModelScope.launch { playlistRepository.createPlaylist(name) }
    
    fun deletePlaylist(playlistId: Long) = viewModelScope.launch { 
        val currentPlaylists = playlistRepository.allPlaylists.first()
        val entity = currentPlaylists.find { it.id == playlistId }
        entity?.let { playlistRepository.deletePlaylist(it) }
    }
    
    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        playlistRepository.addTrackToPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return combine(playlistRepository.getTrackIdsForPlaylist(playlistId), _tracks) { ids, tracks ->
            ids.mapNotNull { id -> tracks.find { it.id == id } }
        }
    }

    fun deleteTracks(tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            val uris = tracks.map { ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.id) }
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                _pendingDeleteIntent.emit(pendingIntent.intentSender)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
