package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, RadioStation::class, QueueTrackEntity::class],
    version = 4, // Увеличили с 3 до 4 из-за добавления QueueTrackEntity
    exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun radioDao(): RadioDao

    companion object {
        @Volatile
        private var INSTANCE: PlaylistDatabase? = null

        fun getDatabase(context: Context): PlaylistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaylistDatabase::class.java,
                    "playlist_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
