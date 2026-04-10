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

    fun savePlaylist(name: String, tracks: List<Track>) {
        val file = File(playlistsDir, "$name.m3u")
        val content = StringBuilder("#EXTM3U\n")
        tracks.forEach { track ->
            content.append("#EXTINF:${track.duration / 1000},${track.artist} - ${track.title}\n")
            content.append("${track.path}\n")
        }
        file.writeText(content.toString())
    }

    fun deletePlaylistFile(name: String) {
        val file = File(playlistsDir, "$name.m3u")
        if (file.exists()) file.delete()
    }

    fun getAllM3UFiles(): List<File> {
        return playlistsDir.listFiles { file -> file.extension.lowercase() == "m3u" }?.toList() ?: emptyList()
    }

    fun parseM3U(file: File): List<String> = parseLines(file.readLines())

    fun parseM3UFromUri(uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseLines(inputStream.bufferedReader().readLines())
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseLines(lines: List<String>): List<String> {
        val paths = mutableListOf<String>()
        lines.forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                paths.add(line.trim())
            }
        }
        return paths
    }
}
