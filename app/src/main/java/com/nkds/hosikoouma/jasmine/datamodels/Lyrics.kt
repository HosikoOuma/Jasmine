package com.nkds.hosikoouma.jasmine.datamodels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    @SerialName("id")
    val id: Long = 0,
    @SerialName("name")
    val name: String = "",
    @SerialName("artistName")
    val artistName: String = "",
    @SerialName("albumName")
    val albumName: String = "",
    @SerialName("duration")
    val duration: Double = 0.0,
    @SerialName("instrumental")
    val instrumental: Boolean = false,
    @SerialName("plainLyrics")
    val plainLyrics: String? = null,
    @SerialName("syncedLyrics")
    val syncedLyrics: String? = null
)

data class LyricsLine(
    val timestamp: Long, // in milliseconds
    val text: String
)
