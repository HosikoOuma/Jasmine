package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsDao: LyricsDao,
    private val lrcLibService: LrcLibService
) {

    suspend fun getLocalLyrics(track: Track): String? = withContext(Dispatchers.IO) {
        try {
            val physicalPath = getRealPathFromURI(track.contentUri) ?: return@withContext null
            val trackFile = File(physicalPath)

            val lrcFile = File(trackFile.parent, trackFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists()) return@withContext lrcFile.readText()

            val audioFile = AudioFileIO.read(trackFile)
            return@withContext audioFile.tag?.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error reading local lyrics", e)
            null
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        if (contentUri.scheme != "content") return contentUri.path
        val proj = arrayOf(MediaStore.Audio.Media.DATA)
        return context.contentResolver.query(contentUri, proj, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            if (columnIndex != -1 && cursor.moveToFirst()) cursor.getString(columnIndex) else null
        }
    }

    suspend fun getRemoteLyrics(track: Track, actualDuration: Long = 0): Lyrics? = withContext(Dispatchers.IO) {
        val cacheId = "${track.artist}_${track.title}"
        
        // 1. Проверяем кэш
        val cached = lyricsDao.getLyrics(cacheId)
        if (cached != null) {
            Log.d("LyricsRepository", "Found cached lyrics for ${track.title}")
            return@withContext Lyrics(
                plainLyrics = cached.plainLyrics,
                syncedLyrics = cached.syncedLyrics,
                name = track.title,
                artistName = track.artist
            )
        }

        try {
            // 2. Ищем в сети (Ktor Service)
            val durationInSec = if (actualDuration > 0) (actualDuration / 1000).toInt() else null
            var result = lrcLibService.getLyrics(
                trackName = track.title,
                artistName = track.artist,
                duration = durationInSec
            )
            
            if (result == null) {
                // Fallback поиск (SEARCH)
                val searchResults = lrcLibService.searchLyrics("${track.artist} ${track.title}")
                result = searchResults.firstOrNull { it.syncedLyrics != null || it.plainLyrics != null }
            }

            // 3. Сохраняем в кэш
            if (result != null) {
                lyricsDao.insertLyrics(
                    LyricsCacheEntity(
                        trackId = cacheId,
                        plainLyrics = result.plainLyrics,
                        syncedLyrics = result.syncedLyrics
                    )
                )
            }
            return@withContext result

        } catch (e: Exception) {
            Log.e("LyricsRepository", "Error fetching/caching lyrics", e)
        }
        null
    }
}
