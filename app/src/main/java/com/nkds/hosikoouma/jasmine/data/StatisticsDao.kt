package com.nkds.hosikoouma.jasmine.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayEvent(event: PlayHistoryEntity): Long

    @Query("UPDATE play_history SET playedDuration = :duration WHERE id = :id")
    suspend fun updatePlayDuration(id: Long, duration: Long)

    @Query("SELECT * FROM play_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlayHistoryEntity>>

    // Считаем прослушиванием только те записи, где реально слушали больше 10 секунд
    @Query("SELECT trackId, COUNT(*) as playCount FROM play_history WHERE playedDuration >= 10000 GROUP BY trackId ORDER BY playCount DESC LIMIT :limit")
    fun getTopTracks(limit: Int): Flow<List<TrackPlayCount>>

    @Query("SELECT SUM(playedDuration) FROM play_history")
    fun getTotalListeningTime(): Flow<Long?>

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()
}
