package com.nkds.hosikoouma.jasmine.data

import com.nkds.hosikoouma.jasmine.datamodels.Lyrics
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibService {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): Response<Lyrics>

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<Lyrics>>
}
