package com.nkds.hosikoouma.jasmine.datamodels

data class Lyrics(
    val id: Long = 0,
    val name: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double = 0.0,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricsLine(
    val timestamp: Long, // in milliseconds
    val text: String
)
