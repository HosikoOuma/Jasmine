package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.IntentSender
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.TrackScanner
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.data.*
import com.nkds.hosikoouma.jasmine.datamodels.*
import com.nkds.hosikoouma.jasmine.ui.components.ToastData
import com.nkds.hosikoouma.jasmine.ui.components.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.AndroidArtwork
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TrackFilters(
    val query: String = "",
    val sortType: SortType = SortType.BY_DATE,
    val isReversed: Boolean = false,
    val minDuration: Int = 0,
    val blacklist: Set<String> = emptySet()
)

@HiltViewModel
class TrackViewModel @Inject constructor(
    application: Application,
    private val trackRepository: TrackRepository,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
    private val playlistRepository: PlaylistRepository,
    private val coverCacheManager: CoverCacheManager
) : AndroidViewModel(application) {
    
    val allTracks = trackRepository.allTracks
    val isLoaded = trackRepository.isLoaded

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.BY_DATE)
    val sortType = _sortType.asStateFlow()

    private val _isReversed = MutableStateFlow(false)
    val isReversed = _isReversed.asStateFlow()

    private val _pendingDeleteIntent = MutableSharedFlow<IntentSender>()
    val pendingDeleteIntent = _pendingDeleteIntent.asSharedFlow()

    private val _appToast = MutableStateFlow<ToastData?>(null)
    val appToast = _appToast.asStateFlow()

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

    val filteredTracks: StateFlow<List<Track>> = combine(allTracks, filters) { tracks, f ->
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

    val favoriteTracks: StateFlow<List<Track>> = combine(allTracks, favoriteTrackIds) { tracks, favIds ->
        tracks.filter { track -> favIds.contains(track.id.toString()) }.sortedBy { it.title.lowercase() }
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

    // Все папки, которые есть в MediaStore (без фильтрации ЧС)
    val allFolders: StateFlow<List<Folder>> = allTracks.map { tracks ->
        withContext(Dispatchers.Default) {
            tracks.groupBy { File(it.path).parent ?: "Unknown" }
                .map { (path, folderTracks) -> Folder(path.substringAfterLast("/"), path, folderTracks) }
                .sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Только те папки, которые НЕ в черном списке
    val folders: StateFlow<List<Folder>> = combine(allFolders, blacklistedFolders) { folderList, blacklist ->
        folderList.filter { folder -> blacklist.none { folder.path.startsWith(it) } }
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
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                trackRepository.loadTracks()
                delay(1000)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- Actions ---

    fun showToast(track: Track?, type: ToastType, message: String? = null) {
        _appToast.value = ToastData(track, type, message)
    }

    fun clearToast() { _appToast.value = null }

    fun toggleFavorite(track: Track) = viewModelScope.launch { 
        favoritesRepository.toggleFavorite(track.id.toString()) 
    }
    
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
        val track = allTracks.value.find { it.id == trackId } ?: return@launch
        val playlistName = getPlaylistNameSync(playlistId) ?: "Playlist"
        if (!playlistRepository.addTrackToPlaylist(playlistId, track)) {
            showToast(track, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.already_in_playlist, playlistName))
        } else {
            updateM3U(playlistId)
            showToast(track, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.added_to_playlist_toast, playlistName))
        }
    }

    fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        val added = playlistRepository.addTracksToPlaylist(playlistId, tracks)
        val playlistName = getPlaylistNameSync(playlistId) ?: "Playlist"
        updateM3U(playlistId)
        
        if (tracks.size == 1) {
            val track = tracks.first()
            if (added == 0) {
                showToast(track, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.already_in_playlist, playlistName))
            } else {
                showToast(track, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.added_to_playlist_toast, playlistName))
            }
        } else {
            if (added == 0) {
                showToast(null, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.already_in_playlist, playlistName))
            } else if (added < tracks.size) {
                // Часть треков добавлена, часть уже была
                showToast(null, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.added_x_tracks_to_playlist, added, playlistName))
            } else {
                showToast(null, ToastType.PLAYLIST_ADDED, getApplication<Application>().getString(R.string.added_x_tracks_to_playlist, added, playlistName))
            }
        }
    }

    fun removeTracksFromPlaylist(playlistId: Long, tracks: List<Track>) = viewModelScope.launch {
        playlistRepository.removeTracksFromPlaylist(playlistId, tracks.map { it.id })
        val playlistName = getPlaylistNameSync(playlistId) ?: "Playlist"
        updateM3U(playlistId)
        if (tracks.size == 1) {
            showToast(tracks.first(), ToastType.PLAYLIST_REMOVED, getApplication<Application>().getString(R.string.removed_from_playlist_toast, playlistName))
        } else {
            showToast(null, ToastType.PLAYLIST_REMOVED, getApplication<Application>().getString(R.string.removed_x_tracks_from_playlist, tracks.size, playlistName))
        }
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
            withContext(Dispatchers.Main) { showToast(null, ToastType.DELETE_SUCCESS, getApplication<Application>().getString(R.string.playlist_exported)) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { showToast(null, ToastType.DELETE_FAILED, getApplication<Application>().getString(R.string.export_failed)) }
        }
    }

    fun importPlaylistFromUri(uri: Uri, name: String) = viewModelScope.launch {
        playlistRepository.importPlaylistFromUri(uri, name, allTracks.value)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> = combine(
        playlistRepository.getTrackIdsForPlaylist(playlistId), allTracks
    ) { ids, tracks -> 
        withContext(Dispatchers.Default) {
            ids.mapNotNull { id -> tracks.find { it.id == id } }
        }
    }

    fun getPlaylistNameSync(playlistId: Long): String? = playlists.value.find { it.id == playlistId }?.name

    fun getPlaylistTracksSync(playlistId: Long): List<Track> {
        return runBlocking(Dispatchers.Default) {
            val ids = playlistRepository.getTrackIdsForPlaylist(playlistId).first()
            val tracks = allTracks.value
            ids.mapNotNull { id -> tracks.find { it.id == id } }
        }
    }

    fun getFolderTracksSync(path: String): List<Track> {
        val decodedPath = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (e: Exception) { path }
        return allTracks.value.filter { it.path.startsWith(decodedPath) }
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

    fun updateTrackMetadata(track: Track, title: String, artist: String, album: String, cover: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (track.path.isEmpty()) return@launch
                val file = File(track.path)
                if (!file.exists()) return@launch

                // 1. Обновление тегов в файле
                val audioFile = AudioFileIO.read(file)
                val tag = audioFile.tag ?: audioFile.createDefaultTag()
                tag.setField(FieldKey.TITLE, title)
                tag.setField(FieldKey.ARTIST, artist)
                tag.setField(FieldKey.ALBUM, album)
                
                cover?.let {
                    val stream = ByteArrayOutputStream()
                    it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val artwork = AndroidArtwork()
                    artwork.binaryData = stream.toByteArray()
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                    
                    // Индивидуальный кэш Jasmine
                    coverCacheManager.saveTrackBitmapToCache(track.id, it)
                }
                audioFile.commit()

                // 2. Обновление MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, title)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    put(MediaStore.Audio.Media.ALBUM, album)
                }
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, track.id)
                getApplication<Application>().contentResolver.update(uri, values, null, null)

                // 3. Ждем сканирования системы и обновляем UI
                MediaScannerConnection.scanFile(getApplication(), arrayOf(file.absolutePath), null) { _, _ ->
                    viewModelScope.launch {
                        delay(500)
                        loadTracks()
                        withContext(Dispatchers.Main) {
                            showToast(track, ToastType.DELETE_SUCCESS, getApplication<Application>().getString(R.string.metadata_updated))
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("TrackViewModel", "Update failed", e)
                withContext(Dispatchers.Main) {
                    showToast(track, ToastType.DELETE_FAILED, getApplication<Application>().getString(R.string.update_failed))
                }
            }
        }
    }
}
