package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PlaylistRepository(private val context: Context) {
    private val playlistDao = PlaylistDatabase.getDatabase(context).playlistDao()
    private val m3uManager = M3UManager(context)

    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
        m3uManager.savePlaylist(name, emptyList())
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val oldPlaylist = allPlaylists.first().find { it.id == playlistId }
        playlistDao.updatePlaylistName(playlistId, newName)
        
        oldPlaylist?.let {
            m3uManager.renamePlaylistFile(it.name, newName)
        }
    }

    suspend fun updatePlaylistCover(playlistId: Long, bitmap: Bitmap?) {
        val playlist = allPlaylists.first().find { it.id == playlistId } ?: return
        
        if (bitmap == null) {
            playlistDao.updatePlaylistCover(playlistId, null)
            val coverFile = m3uManager.getCoverFileForPlaylist(playlist.name)
            if (coverFile.exists()) coverFile.delete()
            return
        }

        withContext(Dispatchers.IO) {
            val coverFile = m3uManager.getCoverFileForPlaylist(playlist.name)
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            // Добавляем timestamp для сброса кэша изображений (Coil/Glide)
            val coverUri = "${Uri.fromFile(coverFile)}?t=${System.currentTimeMillis()}"
            playlistDao.updatePlaylistCover(playlistId, coverUri)
        }
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
        m3uManager.deletePlaylistFile(playlist.name)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track): Boolean {
        val currentIds = playlistDao.getTrackIdsForPlaylist(playlistId).first()
        if (currentIds.contains(track.id)) return false
        
        playlistDao.addTrackToPlaylist(PlaylistTrackEntity(playlistId, track.id))
        return true
    }

    suspend fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>): Int {
        val currentIds = playlistDao.getTrackIdsForPlaylist(playlistId).first()
        val newTracks = tracks.filter { !currentIds.contains(it.id) }
        
        newTracks.forEach { 
            playlistDao.addTrackToPlaylist(PlaylistTrackEntity(playlistId, it.id))
        }
        return newTracks.size
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<Long>) {
        trackIds.forEach { trackId ->
            playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    suspend fun updateM3UFile(playlistName: String, tracks: List<Track>, coverUri: String? = null) {
        m3uManager.savePlaylist(playlistName, tracks, coverUri)
    }

    fun getTrackIdsForPlaylist(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getTrackIdsForPlaylist(playlistId)
    }

    suspend fun importM3UPlaylists(allMediaStoreTracks: List<Track>) {
        val files = m3uManager.getAllM3UFiles()
        files.forEach { importOne(it.nameWithoutExtension, m3uManager.parseM3U(it), allMediaStoreTracks) }
    }

    suspend fun importPlaylistFromUri(uri: Uri, name: String, allTracks: List<Track>) {
        val paths = m3uManager.parseM3UFromUri(uri)
        importOne(name, paths, allTracks)
    }

    private suspend fun importOne(name: String, paths: List<String>, allTracks: List<Track>) {
        val currentPlaylists = allPlaylists.first()
        if (currentPlaylists.none { it.name == name }) {
            val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = name))
            paths.forEach { path ->
                val track = allTracks.find { it.path == path }
                if (track != null) {
                    playlistDao.addTrackToPlaylist(PlaylistTrackEntity(playlistId, track.id))
                }
            }
        }
    }
}
