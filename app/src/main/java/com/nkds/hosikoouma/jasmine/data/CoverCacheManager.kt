package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootDir: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Jasmine/Covers")

    private fun ensureRootDir(): File {
        val dir = rootDir
        if (!dir.exists()) dir.mkdirs()
        val nomedia = File(dir, ".nomedia")
        if (!nomedia.exists()) {
            try { nomedia.createNewFile() } catch (e: Exception) { Log.e("CoverCache", "Failed to create .nomedia", e) }
        }
        return dir
    }

    // Получение URI обложки по trackId или albumId
    fun getTrackCoverUri(trackId: Long): Uri? {
        val file = File(rootDir, "track_$trackId.jpg")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun getAlbumCoverUri(albumId: Long): Uri? {
        if (albumId == -1L) return null
        val file = File(rootDir, "album_$albumId.jpg")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    // Сохранение обложки
    fun saveTrackBitmapToCache(trackId: Long, bitmap: Bitmap) {
        val dir = ensureRootDir()
        val file = File(dir, "track_$trackId.jpg")
        saveBitmapToFile(file, bitmap)
    }

    fun saveAlbumBitmapToCache(albumId: Long, bitmap: Bitmap) {
        if (albumId == -1L) return
        val dir = ensureRootDir()
        val file = File(dir, "album_$albumId.jpg")
        saveBitmapToFile(file, bitmap)
    }

    // Метод для извлечения обложки НАПРЯМУЮ из файла
    fun extractAndCacheEmbeddedArt(id: Long, filePath: String, isAlbum: Boolean = false): Uri? {
        if (filePath.isEmpty()) return null
        val fileName = if (isAlbum) "album_$id.jpg" else "track_$id.jpg"
        val cacheFile = File(rootDir, fileName)
        if (cacheFile.exists()) return Uri.fromFile(cacheFile)

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            retriever.release()

            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                ensureRootDir()
                saveBitmapToFile(cacheFile, bitmap)
                Uri.fromFile(cacheFile)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            Log.e("CoverCache", "Failed to save bitmap", e)
        }
    }

    fun getCacheInfo(): Pair<Int, Long> {
        val dir = rootDir
        if (!dir.exists()) return Pair(0, 0L)
        val files = dir.listFiles { f -> f.extension == "jpg" } ?: emptyArray()
        val size = files.sumOf { it.length() }
        return Pair(files.size, size)
    }

    fun clearCache(): Boolean {
        return try {
            val dir = rootDir
            dir.listFiles { f -> f.extension == "jpg" }?.forEach { it.delete() }
            ensureRootDir()
            true
        } catch (e: Exception) {
            false
        }
    }
}
