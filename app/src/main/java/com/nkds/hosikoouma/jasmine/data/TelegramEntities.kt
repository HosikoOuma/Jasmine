package com.nkds.hosikoouma.jasmine.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nkds.hosikoouma.jasmine.datamodels.Track
import java.io.File
import kotlin.math.absoluteValue

@Entity(tableName = "telegram_channels")
data class TelegramChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id") val chatId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "username") val username: String? = null,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long = 0,
    @ColumnInfo(name = "photo_path") val photoPath: String? = null
)

@Entity(
    tableName = "telegram_songs",
    indices = [
        Index(value = ["chat_id"]),
        Index(value = ["message_id"]),
        Index(value = ["file_id"]),
        Index(value = ["chat_id", "message_id"]),
        Index(value = ["thread_id"])
    ]
)
data class TelegramSongEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String, // format: "chatId_messageId"
    @ColumnInfo(name = "chat_id") val chatId: Long,
    @ColumnInfo(name = "message_id") val messageId: Long,
    @ColumnInfo(name = "file_id") val fileId: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "file_path") val filePath: String, // Empty if not downloaded
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "album_art_uri") val albumArtUriString: String? = null,
    @ColumnInfo(name = "thread_id") val threadId: Long? = null
)

@Entity(
    tableName = "telegram_topics",
    indices = [
        Index(value = ["chat_id"])
    ]
)
data class TelegramTopicEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String, // format: "chatId_threadId"
    @ColumnInfo(name = "chat_id") val chatId: Long,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: Long = 0,
    @ColumnInfo(name = "icon_emoji") val iconEmoji: String? = null
)

fun TelegramSongEntity.toTrack(channelTitle: String? = null, topicName: String? = null): Track {
    val albumLabel = topicName ?: channelTitle ?: "Telegram Cloud"
    
    // Мы ВСЕГДА используем схему telegram://, чтобы трек проходил через PlayerViewModel 
    // и резолвился через прокси. Это предотвращает ошибку FileNotFound, если кэш был очищен.
    val contentUri = Uri.parse("telegram://${this.chatId}/${this.messageId}")
    
    val artUri = albumArtUriString?.let { Uri.parse(it) }
        ?: if (filePath.isNotEmpty() && File(filePath).exists()) Uri.parse("telegram_art://$chatId/$messageId") 
        else null

    return Track(
        id = -(this.id.hashCode().toLong().absoluteValue),
        title = this.title,
        artist = this.artist,
        album = albumLabel,
        duration = this.duration,
        contentUri = contentUri,
        albumArtUri = artUri,
        path = this.filePath,
        uid = this.id,
        isManual = false,
        dateModified = this.dateAdded
    )
}
