package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.Size
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
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val nomedia = File(dir, ".nomedia")
        if (!nomedia.exists()) {
            try {
                nomedia.createNewFile()
            } catch (e: Exception) {
                Log.e("CoverCache", "Failed to create .nomedia", e)
            }
        }
        return dir
    }

    fun getCoverUri(albumId: Long): Uri? {
        val file = File(rootDir, "album_$albumId.jpg")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun saveCoverIfMissing(albumId: Long, trackUri: Uri) {
        val dir = ensureRootDir()
        val file = File(dir, "album_$albumId.jpg")
        if (file.exists()) return

        try {
            val bitmap = context.contentResolver.loadThumbnail(trackUri, Size(600, 600), null)
            bitmap.let {
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                Log.d("CoverCache", "Saved cover for album $albumId")
            }
        } catch (e: Exception) {
            Log.v("CoverCache", "No cover found for $trackUri")
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
            // После очистки гарантируем наличие .nomedia
            ensureRootDir()
            true
        } catch (e: Exception) {
            false
        }
    }
}
