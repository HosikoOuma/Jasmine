package com.nkds.hosikoouma.jasmine.data

import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LrcLibService(private val client: HttpClient) {
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        duration: Int? = null
    ): Lyrics? {
        return try {
            val response = client.get("get") {
                parameter("track_name", trackName)
                parameter("artist_name", artistName)
                if (albumName != null) parameter("album_name", albumName)
                if (duration != null) parameter("duration", duration)
            }
            if (response.status.value in 200..299) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchLyrics(query: String): List<Lyrics> {
        return try {
            val response = client.get("search") {
                parameter("q", query)
            }
            if (response.status.value in 200..299) response.body() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
