package com.nkds.hosikoouma.jasmine.data

import androidx.room.*

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, RadioStation::class, QueueTrackEntity::class],
    version = 4,
    exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun radioDao(): RadioDao
}
