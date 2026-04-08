package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import com.nkds.hosikoouma.jasmine.datamodels.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class LyricsRepository(private val context: Context) {

    private val lyricsDao = LyricsDatabase.getDatabase(context).lyricsDao()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "JasmineMusicPlayer/1.0 (https://github.com/hosikoouma/Jasmine)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val lrcLibService: LrcLibService by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibService::class.java)
    }

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
            // 2. Ищем в сети (GET)
            val durationInSec = if (actualDuration > 0) (actualDuration / 1000).toInt() else null
            val response = lrcLibService.getLyrics(
                trackName = track.title,
                artistName = track.artist,
                duration = durationInSec
            )
            
            var result: Lyrics? = null
            if (response.isSuccessful) {
                result = response.body()
            } else {
                // Fallback поиск (SEARCH)
                val searchResponse = lrcLibService.searchLyrics("${track.artist} ${track.title}")
                if (searchResponse.isSuccessful) {
                    result = searchResponse.body()?.firstOrNull { it.syncedLyrics != null || it.plainLyrics != null }
                }
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
