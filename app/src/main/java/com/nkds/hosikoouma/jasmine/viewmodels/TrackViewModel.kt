package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.TrackScanner
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
        
    val blacklistedFolders = settingsRepository.blacklistedFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favoriteTrackIds: StateFlow<Set<String>> = favoritesRepository.favoriteTrackIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val filtersFlow = combine(minDurationLimit, blacklistedFolders) { dur, blacklist ->
        Pair(dur, blacklist)
    }

    val filteredTracks: StateFlow<List<Track>> = combine(
        _tracks, _searchQuery, _sortType, _isReversed, filtersFlow
    ) { tracks, query, sort, reversed, filters ->
        val (minDur, blacklist) = filters
        val folderFiltered = tracks.filter { track ->
            blacklist.none { blacklistedPath -> track.path.startsWith(blacklistedPath) }
        }
        val timeFiltered = folderFiltered.filter { it.duration >= minDur * 1000L }
        filterAndSort(timeFiltered, query, sort, reversed)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteTracks: StateFlow<List<Track>> = combine(
        _tracks, favoriteTrackIds, _searchQuery, filtersFlow
    ) { tracks, favIds, query, filters ->
        val (minDur, blacklist) = filters
        val folderFiltered = tracks.filter { track ->
            blacklist.none { blacklistedPath -> track.path.startsWith(blacklistedPath) }
        }
        val favorites = folderFiltered.filter { favIds.contains(it.id.toString()) && it.duration >= minDur * 1000L }
        if (query.isBlank()) favorites else favorites.filter { 
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<List<Album>> = combine(filteredTracks, _sortType, _isReversed) { tracks, sort, reversed ->
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

    val artists: StateFlow<List<Artist>> = combine(filteredTracks, _sortType, _isReversed) { tracks, sort, reversed ->
        val grouped = tracks.groupBy { it.artist }
            .map { Artist(it.key, it.value) }
        
        val sorted = when (sort) {
            SortType.BY_NAME, SortType.BY_ARTIST -> grouped.sortedBy { it.name.lowercase() }
            SortType.BY_DATE -> grouped.sortedByDescending { it.tracks.maxOfOrNull { t -> t.id } ?: 0L }
            SortType.BY_DURATION -> grouped.sortedBy { it.tracks.sumOf { t -> t.duration } }
        }
        if (reversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folders: StateFlow<List<Folder>> = combine(_tracks, _sortType, _isReversed, blacklistedFolders) { tracks, sort, reversed, blacklist ->
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

    fun addFolderToBlacklist(path: String) = viewModelScope.launch { settingsRepository.addFolderToBlacklist(path) }
    fun removeFolderFromBlacklist(path: String) = viewModelScope.launch { settingsRepository.removeFolderFromBlacklist(path) }

    // Playlist Operations
    fun createPlaylist(name: String) = viewModelScope.launch { playlistRepository.createPlaylist(name) }
    
    fun deletePlaylist(playlistId: Long) = viewModelScope.launch { 
        val currentPlaylists = playlistRepository.allPlaylists.first()
        val entity = currentPlaylists.find { it.id == playlistId }
        entity?.let { playlistRepository.deletePlaylist(it) }
    }
    
    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        val track = _tracks.value.find { it.id == trackId } ?: return@launch
        val added = playlistRepository.addTrackToPlaylist(playlistId, track)
        if (!added) {
            Toast.makeText(getApplication(), "Track already in playlist", Toast.LENGTH_SHORT).show()
        } else {
            // Update M3U file
            val pTracks = getTracksForPlaylist(playlistId).first()
            val playlist = playlists.value.find { it.id == playlistId }
            if (playlist != null) {
                playlistRepository.updateM3UFile(playlist.name, pTracks)
            }
        }
    }

    fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        val addedCount = playlistRepository.addTracksToPlaylist(playlistId, tracks)
        val pTracks = getTracksForPlaylist(playlistId).first()
        val playlist = playlists.value.find { it.id == playlistId }
        if (playlist != null) {
            playlistRepository.updateM3UFile(playlist.name, pTracks)
        }
        
        if (addedCount < tracks.size) {
            Toast.makeText(getApplication(), "Added $addedCount tracks (some already existed)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "Added $addedCount tracks", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeTracksFromPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        playlistRepository.removeTracksFromPlaylist(playlistId, tracks.map { it.id })
        val remainingTracks = getTracksForPlaylist(playlistId).first()
        val playlist = playlists.value.find { it.id == playlistId }
        if (playlist != null) {
            playlistRepository.updateM3UFile(playlist.name, remainingTracks)
        }
    }

    fun exportPlaylist(playlistId: Long, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val tracks = getTracksForPlaylist(playlistId).first()
        val content = StringBuilder("#EXTM3U\n")
        tracks.forEach { track ->
            content.append("#EXTINF:${track.duration / 1000},${track.artist} - ${track.title}\n")
            content.append("${track.path}\n")
        }
        
        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                FileOutputStream(fd.fileDescriptor).use { os ->
                    os.write(content.toString().toByteArray())
                }
            }
            launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Exported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importPlaylists() = viewModelScope.launch {
        playlistRepository.importM3UPlaylists(_tracks.value)
    }

    fun importPlaylistFromUri(uri: Uri, name: String) = viewModelScope.launch {
        playlistRepository.importPlaylistFromUri(uri, name, _tracks.value)
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
