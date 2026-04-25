package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3UManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val playlistsDir: File
        get() {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Jasmine/Playlists")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun savePlaylist(name: String, tracks: List<Track>, coverUri: String? = null) {
        val file = File(playlistsDir, "$name.m3u")
        val content = StringBuilder("#EXTM3U\n")
        
        coverUri?.let {
            content.append("#EXTALBUMARTURL:$it\n")
        }
        
        tracks.forEach { track ->
            content.append("#EXTINF:${track.duration / 1000},${track.artist} - ${track.title}\n")
            content.append("${track.path}\n")
        }
        file.writeText(content.toString())
    }

    fun getCoverFileForPlaylist(name: String): File {
        return File(playlistsDir, "$name.jpg")
    }
    
    fun findCoverForPlaylist(name: String): File? {
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        for (ext in extensions) {
            val file = File(playlistsDir, "$name.$ext")
            if (file.exists()) return file
        }
        return null
    }

    fun renamePlaylistFile(oldName: String, newName: String) {
        val oldFile = File(playlistsDir, "$oldName.m3u")
        val newFile = File(playlistsDir, "$newName.m3u")
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
        
        // Find and rename any supported image format
        findCoverForPlaylist(oldName)?.let { oldCover ->
            val ext = oldCover.extension
            val newCover = File(playlistsDir, "$newName.$ext")
            oldCover.renameTo(newCover)
        }
    }

    fun deletePlaylistFile(name: String) {
        val file = File(playlistsDir, "$name.m3u")
        if (file.exists()) file.delete()
        
        findCoverForPlaylist(name)?.delete()
    }

    fun getAllM3UFiles(): List<File> {
        return playlistsDir.listFiles { file -> file.extension.lowercase() == "m3u" }?.toList() ?: emptyList()
    }

    fun parseM3U(file: File): List<String> = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.trim() }
    
    fun parseM3UCover(file: File): String? {
        return file.readLines().find { it.startsWith("#EXTALBUMARTURL:") }?.substringAfter(":")
    }

    fun parseM3UFromUri(uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readLines().filter { it.isNotBlank() && !it.startsWith("#") }.map { it.trim() }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
