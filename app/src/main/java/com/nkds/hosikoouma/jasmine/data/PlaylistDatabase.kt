package com.nkds.hosikoouma.jasmine.data

import androidx.room.*

@Database(
    entities = [
        PlaylistEntity::class, 
        PlaylistTrackEntity::class, 
        RadioStation::class, 
        QueueTrackEntity::class,
        PlayHistoryEntity::class,
        TrackStatsEntity::class,
        TelegramSongEntity::class,
        TelegramChannelEntity::class,
        TelegramTopicEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun radioDao(): RadioDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun telegramDao(): TelegramDao
}
