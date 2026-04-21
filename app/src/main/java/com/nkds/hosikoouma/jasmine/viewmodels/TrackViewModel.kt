package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ContentUris
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.TrackScanner
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.datamodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class TrackFilters(
    val query: String = "",
    val sortType: SortType = SortType.BY_DATE,
    val isReversed: Boolean = false,
    val minDuration: Int = 0,
    val blacklist: Set<String> = emptySet()
)

class TrackViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repositories
    private val trackScanner = TrackScanner(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val playlistRepository = PlaylistRepository(application)

    // Raw Data
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks = _tracks.asStateFlow()

    // UI States
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

    // Filter Aggregation
    val minDurationLimit = settingsRepository.minTrackDuration
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        
    val blacklistedFolders = settingsRepository.blacklistedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isPlaylistsGridView = settingsRepository.isPlaylistsGridView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filters: StateFlow<TrackFilters> = combine(
        _searchQuery, _sortType, _isReversed,
        minDurationLimit,
        blacklistedFolders
    ) { query, sort, reversed, minDur, blacklist ->
        TrackFilters(query, sort, reversed, minDur, blacklist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackFilters())

    val favoriteTrackIds: StateFlow<Set<String>> = favoritesRepository.favoriteTrackIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // --- High Performance Filtered Lists ---

    val filteredTracks: StateFlow<List<Track>> = combine(_tracks, filters) { tracks, f ->
        if (tracks.isEmpty()) return@combine emptyList()
        
        withContext(Dispatchers.Default) {
            tracks.asSequence()
                .filter { track -> f.blacklist.none { track.path.startsWith(it) } }
                .filter { it.duration >= f.minDuration * 1000L }
                .filter { f.query.isBlank() || it.title.contains(f.query, true) || it.artist.contains(f.query, true) }
                .let { seq ->
                    when (f.sortType) {
                        SortType.BY_TITLE -> seq.sortedBy { it.title.lowercase() }
                        SortType.BY_ARTIST -> seq.sortedBy { it.artist.lowercase() }
                        SortType.BY_DATE -> seq.sortedByDescending { it.id }
                        SortType.BY_DURATION -> seq.sortedBy { it.duration }
                    }
                }
                .let { if (f.isReversed) it.toList().reversed() else it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteTracks: StateFlow<List<Track>> = combine(filteredTracks, favoriteTrackIds) { tracks, favIds ->
        tracks.filter { favIds.contains(it.id.toString()) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums: StateFlow<List<Album>> = filteredTracks.map { tracks ->
        withContext(Dispatchers.Default) {
            tracks.groupBy { it.album }
                .map { (name, albumTracks) -> Album(name, albumTracks.first().artist, albumTracks) }
                .sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artists: StateFlow<List<Artist>> = filteredTracks.map { tracks ->
        withContext(Dispatchers.Default) {
            tracks.groupBy { it.artist }
                .map { Artist(it.key, it.value) }
                .sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folders: StateFlow<List<Folder>> = _tracks.map { tracks ->
        withContext(Dispatchers.Default) {
            tracks.groupBy { File(it.path).parent ?: "Unknown" }
                .map { (path, folderTracks) -> Folder(path.substringAfterLast("/"), path, folderTracks) }
                .sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<Playlist>> = combine(playlistRepository.allPlaylists, _sortType, _isReversed) { entities, sort, reversed ->
        val mapped = entities.map { 
            Playlist(
                id = it.id, 
                name = it.name, 
                tracks = emptyList(), 
                createdAt = it.createdAt,
                coverUri = it.coverUri?.let { uriStr -> Uri.parse(uriStr) }
            ) 
        }
        val sorted = when (sort) {
            SortType.BY_TITLE -> mapped.sortedBy { it.name.lowercase() }
            SortType.BY_DATE -> mapped.sortedByDescending { it.createdAt }
            else -> mapped.sortedBy { it.name.lowercase() }
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

    fun loadTracks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            trackScanner.scanTracksFlow()
                .flowOn(Dispatchers.IO)
                .collect { trackList ->
                    _tracks.value = trackList
                    _isLoaded.value = true
                }
            _isRefreshing.value = false
        }
    }

    // --- Actions ---

    fun toggleFavorite(track: Track) = viewModelScope.launch { favoritesRepository.toggleFavorite(track.id.toString()) }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortType(type: SortType) { _sortType.value = type }
    fun toggleReverse() { _isReversed.value = !_isReversed.value }

    fun addFolderToBlacklist(path: String) = viewModelScope.launch { settingsRepository.addFolderToBlacklist(path) }
    fun removeFolderFromBlacklist(path: String) = viewModelScope.launch { settingsRepository.removeFolderFromBlacklist(path) }

    fun createPlaylist(name: String) = viewModelScope.launch { playlistRepository.createPlaylist(name) }
    
    fun renamePlaylist(playlistId: Long, newName: String) = viewModelScope.launch {
        playlistRepository.renamePlaylist(playlistId, newName)
    }

    fun updatePlaylistCover(playlistId: Long, bitmap: Bitmap?) = viewModelScope.launch {
        playlistRepository.updatePlaylistCover(playlistId, bitmap)
        updateM3U(playlistId)
    }

    fun setPlaylistsGridView(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPlaylistsGridView(enabled)
    }

    fun deletePlaylist(playlistId: Long) = viewModelScope.launch { 
        playlistRepository.allPlaylists.first().find { it.id == playlistId }?.let { playlistRepository.deletePlaylist(it) }
    }
    
    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        val track = _tracks.value.find { it.id == trackId } ?: return@launch
        if (!playlistRepository.addTrackToPlaylist(playlistId, track)) {
            withContext(Dispatchers.Main) { Toast.makeText(getApplication(), "Already in playlist", Toast.LENGTH_SHORT).show() }
        } else {
            updateM3U(playlistId)
        }
    }

    fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        val added = playlistRepository.addTracksToPlaylist(playlistId, tracks)
        updateM3U(playlistId)
        withContext(Dispatchers.Main) { Toast.makeText(getApplication(), "Added $added tracks", Toast.LENGTH_SHORT).show() }
    }

    fun removeTracksFromPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        playlistRepository.removeTracksFromPlaylist(playlistId, tracks.map { it.id })
        updateM3U(playlistId)
    }

    private suspend fun updateM3U(playlistId: Long) {
        val pTracks = getTracksForPlaylist(playlistId).first()
        val playlist = playlists.value.find { it.id == playlistId } ?: return
        playlistRepository.updateM3UFile(playlist.name, pTracks, playlist.coverUri?.toString())
    }

    fun exportPlaylist(playlistId: Long, uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val tracks = getTracksForPlaylist(playlistId).first()
        val content = StringBuilder("#EXTM3U\n")
        tracks.forEach { track -> content.append("#EXTINF:${track.duration / 1000},${track.artist} - ${track.title}\n${track.path}\n") }
        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                FileOutputStream(fd.fileDescriptor).use { it.write(content.toString().toByteArray()) }
            }
            withContext(Dispatchers.Main) { Toast.makeText(getApplication(), "Exported", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(getApplication(), "Export failed", Toast.LENGTH_SHORT).show() }
        }
    }

    fun importPlaylistFromUri(uri: Uri, name: String) = viewModelScope.launch {
        playlistRepository.importPlaylistFromUri(uri, name, _tracks.value)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> = combine(
        playlistRepository.getTrackIdsForPlaylist(playlistId), _tracks
    ) { ids, tracks -> 
        withContext(Dispatchers.Default) {
            ids.mapNotNull { id -> tracks.find { it.id == id } }
        }
    }

    // --- Синхронные методы для UI (заголовки, быстрые действия) ---

    fun getPlaylistNameSync(playlistId: Long): String? {
        return playlists.value.find { it.id == playlistId }?.name
    }

    fun getPlaylistTracksSync(playlistId: Long): List<Track> {
        return runBlocking(Dispatchers.Default) {
            val ids = playlistRepository.getTrackIdsForPlaylist(playlistId).first()
            val tracks = _tracks.value
            ids.mapNotNull { id -> tracks.find { it.id == id } }
        }
    }

    fun getFolderTracksSync(path: String): List<Track> {
        val decodedPath = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (e: Exception) { path }
        return _tracks.value.filter { it.path.startsWith(decodedPath) }
    }

    fun deleteTracks(tracks: List<Track>) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = tracks.map { ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.id) }
            try {
                val intentSender = MediaStore.createDeleteRequest(getApplication<Application>().contentResolver, uris).intentSender
                _pendingDeleteIntent.emit(intentSender)
            } catch (e: Exception) {
                Log.e("TrackViewModel", "Failed to create delete request", e)
            }
        }
    }
}
