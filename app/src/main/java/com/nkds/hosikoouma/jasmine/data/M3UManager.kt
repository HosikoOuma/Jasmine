package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.nkds.hosikoouma.jasmine.datamodels.Track
import java.io.File

class M3UManager(private val context: Context) {

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

    fun renamePlaylistFile(oldName: String, newName: String) {
        val oldFile = File(playlistsDir, "$oldName.m3u")
        val newFile = File(playlistsDir, "$newName.m3u")
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
        
        val oldCover = File(playlistsDir, "$oldName.jpg")
        val newCover = File(playlistsDir, "$newName.jpg")
        if (oldCover.exists()) {
            oldCover.renameTo(newCover)
        }
    }

    fun deletePlaylistFile(name: String) {
        val file = File(playlistsDir, "$name.m3u")
        if (file.exists()) file.delete()
        
        val coverFile = File(playlistsDir, "$name.jpg")
        if (coverFile.exists()) coverFile.delete()
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
