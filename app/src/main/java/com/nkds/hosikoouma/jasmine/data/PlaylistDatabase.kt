package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, RadioStation::class],
    version = 3, // Увеличили версию с 2 до 3 из-за добавления coverUri
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
                .fallbackToDestructiveMigration() // Позволяет Room пересоздать базу при несовпадении версий
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
