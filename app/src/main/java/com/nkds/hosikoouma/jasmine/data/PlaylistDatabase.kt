package com.nkds.hosikoouma.jasmine.data

import androidx.room.*

@Database(
    entities = [
        PlaylistEntity::class, 
        PlaylistTrackEntity::class, 
        RadioStation::class, 
        QueueTrackEntity::class,
        TelegramSongEntity::class,
        TelegramChannelEntity::class,
        TelegramTopicEntity::class
    ],
    version = 7, // Повышаем версию для миграции
    exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun radioDao(): RadioDao
    abstract fun telegramDao(): TelegramDao
}
