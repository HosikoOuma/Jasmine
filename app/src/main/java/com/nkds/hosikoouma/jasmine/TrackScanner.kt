package com.nkds.hosikoouma.jasmine

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TrackScanner(private val context: Context) {
    
    private val baseAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val baseAlbumArtUri = Uri.parse("content://media/external/audio/albumart")

    fun scanTracksFlow(): Flow<List<Track>> = flow {
        val tracks = mutableListOf<Track>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                var count = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    
                    val track = Track(
                        id = id,
                        title = cursor.getString(titleColumn) ?: "Unknown Title",
                        artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                        album = cursor.getString(albumColumn) ?: "Unknown Album",
                        duration = cursor.getLong(durationColumn),
                        contentUri = ContentUris.withAppendedId(baseAudioUri, id),
                        albumArtUri = ContentUris.withAppendedId(baseAlbumArtUri, albumId),
                        path = cursor.getString(dataColumn) ?: ""
                    )
                    tracks.add(track)
                    count++

                    // Эмитим пачки по 50 треков для быстрого отклика UI
                    if (count % 50 == 0) {
                        emit(ArrayList(tracks))
                    }
                }
                // Финальный список
                emit(tracks)
            }
        } catch (e: Exception) {
            Log.e("TrackScanner", "Error scanning tracks", e)
        }
    }

    // Оставляем старый метод для совместимости или удаляем, если переписали всё
    fun scanTracks(): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    
                    tracks.add(Track(
                        id, 
                        cursor.getString(titleColumn) ?: "Unknown Title", 
                        cursor.getString(artistColumn) ?: "Unknown Artist", 
                        cursor.getString(albumColumn) ?: "Unknown Album", 
                        cursor.getLong(durationColumn), 
                        ContentUris.withAppendedId(baseAudioUri, id), 
                        ContentUris.withAppendedId(baseAlbumArtUri, albumId), 
                        cursor.getString(dataColumn) ?: ""
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("TrackScanner", "Error scanning tracks", e)
        }
        return tracks
    }
}
