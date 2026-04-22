package com.nkds.hosikoouma.jasmine

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.nkds.hosikoouma.jasmine.data.CoverCacheManager
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coverCacheManager: CoverCacheManager
) {
    
    private val baseAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val baseAlbumArtUri = Uri.parse("content://media/external/audio/albumart")

    fun scanTracksFlow(): Flow<List<Track>> = flow {
        val tracks = mutableListOf<Track>()
        // Кэшируем ID альбомов, которые уже проверили в текущем сеансе, чтобы не дергать File.exists()
        val checkedAlbumIds = mutableSetOf<Long>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            baseAudioUri
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(baseAudioUri, id)
                    
                    // Оптимизация: проверяем только если еще не видели этот альбом в текущем цикле
                    if (!checkedAlbumIds.contains(albumId)) {
                        coverCacheManager.saveCoverIfMissing(albumId, contentUri)
                        checkedAlbumIds.add(albumId)
                    }
                    
                    val artUri = coverCacheManager.getCoverUri(albumId) ?: ContentUris.withAppendedId(baseAlbumArtUri, albumId)

                    tracks.add(Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown Title",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        duration = cursor.getLong(durationCol),
                        contentUri = contentUri,
                        albumArtUri = artUri,
                        path = cursor.getString(dataCol) ?: ""
                    ))

                    // Эмитим реже (раз в 100 треков), чтобы снизить нагрузку на UI и ViewModel
                    if (tracks.size % 100 == 0) {
                        emit(tracks.toList())
                    }
                }
                emit(tracks.toList())
            }
        } catch (e: Exception) {
            Log.e("TrackScanner", "MediaStore query failed", e)
            emit(emptyList())
        }
    }

    fun scanTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val checkedAlbumIds = mutableSetOf<Long>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            baseAudioUri
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val contentUri = ContentUris.withAppendedId(baseAudioUri, id)
                    
                    if (!checkedAlbumIds.contains(albumId)) {
                        coverCacheManager.saveCoverIfMissing(albumId, contentUri)
                        checkedAlbumIds.add(albumId)
                    }

                    val artUri = coverCacheManager.getCoverUri(albumId) ?: ContentUris.withAppendedId(baseAlbumArtUri, albumId)

                    tracks.add(Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown Title",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        album = cursor.getString(albumCol) ?: "Unknown Album",
                        duration = cursor.getLong(durationCol),
                        contentUri = contentUri,
                        albumArtUri = artUri,
                        path = cursor.getString(dataCol) ?: ""
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("TrackScanner", "Sync scan failed", e)
        }
        return tracks
    }
}
