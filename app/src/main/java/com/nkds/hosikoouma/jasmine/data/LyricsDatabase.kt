package com.nkds.hosikoouma.jasmine.data

import androidx.room.*

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val trackId: String,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE trackId = :id")
    suspend fun getLyrics(id: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache WHERE trackId = :id")
    suspend fun deleteLyrics(id: String)
}

@Database(entities = [LyricsCacheEntity::class], version = 1)
abstract class LyricsDatabase : RoomDatabase() {
    abstract fun lyricsDao(): LyricsDao
}
