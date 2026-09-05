package com.nkds.hosikoouma.jasmine.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramDao {
    @Query("SELECT * FROM telegram_songs ORDER BY date_added DESC")
    fun getAllTelegramSongs(): Flow<List<TelegramSongEntity>>

    @Query("SELECT * FROM telegram_songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY date_added DESC")
    fun searchSongs(query: String): Flow<List<TelegramSongEntity>>

    @Query("SELECT * FROM telegram_songs WHERE id IN (:ids)")
    fun getSongsByIds(ids: List<String>): Flow<List<TelegramSongEntity>>

    @Query("SELECT * FROM telegram_songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): TelegramSongEntity?

    @Query("SELECT * FROM telegram_songs WHERE chat_id = :chatId ORDER BY date_added DESC")
    suspend fun getSongsByChatId(chatId: Long): List<TelegramSongEntity>

    @Query("SELECT * FROM telegram_songs WHERE chat_id = :chatId ORDER BY date_added DESC")
    fun getSongsByChatIdFlow(chatId: Long): Flow<List<TelegramSongEntity>>

    @Query("SELECT * FROM telegram_songs WHERE chat_id = :chatId AND thread_id = :threadId ORDER BY date_added DESC")
    suspend fun getSongsByTopicId(chatId: Long, threadId: Long): List<TelegramSongEntity>

    @Query("SELECT * FROM telegram_songs WHERE file_id = :fileId LIMIT 1")
    suspend fun getSongByFileId(fileId: Int): TelegramSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<TelegramSongEntity>)

    @Query("DELETE FROM telegram_songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM telegram_songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: TelegramChannelEntity)

    @Query("SELECT * FROM telegram_channels ORDER BY title ASC")
    fun getAllChannels(): Flow<List<TelegramChannelEntity>>

    @Query("DELETE FROM telegram_channels WHERE chat_id = :chatId")
    suspend fun deleteChannel(chatId: Long)

    @Query("DELETE FROM telegram_songs WHERE chat_id = :chatId")
    suspend fun deleteSongsByChatId(chatId: Long)

    @Query("DELETE FROM telegram_songs WHERE chat_id = :chatId AND thread_id = :threadId")
    suspend fun deleteSongsByTopicId(chatId: Long, threadId: Long)

    @Query("DELETE FROM telegram_songs")
    suspend fun clearAll()

    // ─── Topic methods ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TelegramTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TelegramTopicEntity>)

    @Query("SELECT * FROM telegram_topics WHERE chat_id = :chatId ORDER BY name ASC")
    fun getTopicsByChannel(chatId: Long): Flow<List<TelegramTopicEntity>>

    @Query("SELECT * FROM telegram_topics WHERE chat_id = :chatId ORDER BY name ASC")
    suspend fun getTopicsByChannelOnce(chatId: Long): List<TelegramTopicEntity>

    @Query("SELECT * FROM telegram_topics WHERE id = :id")
    suspend fun getTopicById(id: String): TelegramTopicEntity?

    @Query("DELETE FROM telegram_topics WHERE chat_id = :chatId")
    suspend fun deleteTopicsByChannel(chatId: Long)

    @Query("SELECT * FROM telegram_topics ORDER BY name ASC")
    fun getAllTopics(): Flow<List<TelegramTopicEntity>>

    @Query("DELETE FROM telegram_topics WHERE id = :id")
    suspend fun deleteTopic(id: String)
    @Transaction
    suspend fun updateChannelSongs(chatId: Long, songs: List<TelegramSongEntity>) {
        val existing = getSongsByChatId(chatId)
        val newIds = songs.map { it.id }.toSet()
        val toDelete = existing.filter { it.id !in newIds }.map { it.id }
        if (toDelete.isNotEmpty()) deleteSongsByIds(toDelete)
        insertSongs(songs)
    }
}
