package com.nkds.hosikoouma.jasmine.datamodels

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val path: String = "",
    val uid: String = id.toString(),
    val isManual: Boolean = false
)

data class Album(val name: String, val artist: String, val tracks: List<Track>)
data class Artist(val name: String, val tracks: List<Track>)
data class Folder(val name: String, val path: String, val tracks: List<Track>)
data class Playlist(
    val id: Long, 
    val name: String, 
    val tracks: List<Track>, 
    val createdAt: Long = 0,
    val coverUri: Uri? = null
)
