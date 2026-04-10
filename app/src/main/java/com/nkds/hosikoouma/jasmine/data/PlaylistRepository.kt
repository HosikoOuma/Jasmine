package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(context: Context) {
    private val playlistDao = PlaylistDatabase.getDatabase(context).playlistDao()

    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.addTrackToPlaylist(PlaylistTrackEntity(playlistId, trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTrackIdsForPlaylist(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getTrackIdsForPlaylist(playlistId)
    }
}
