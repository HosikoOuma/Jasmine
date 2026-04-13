package com.nkds.hosikoouma.jasmine.datamodels

import com.google.gson.annotations.SerializedName

data class Lyrics(
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("artistName")
    val artistName: String = "",
    @SerializedName("albumName")
    val albumName: String = "",
    @SerializedName("duration")
    val duration: Double = 0.0,
    @SerializedName("instrumental")
    val instrumental: Boolean = false,
    @SerializedName("plainLyrics")
    val plainLyrics: String? = null,
    @SerializedName("syncedLyrics")
    val syncedLyrics: String? = null
)

data class LyricsLine(
    val timestamp: Long, // in milliseconds
    val text: String
)
