package com.nkds.hosikoouma.jasmine.data

import androidx.room.*

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val playedDuration: Long = 0 // Реально прослушанное время в мс
)

@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val trackId: Long,
    val playCount: Int = 0,
    val lastPlayed: Long = System.currentTimeMillis()
)

data class TrackPlayCount(
    val trackId: Long,
    val playCount: Int
)
