package com.nkds.hosikoouma.jasmine.data.telegram

import android.net.Uri
import android.util.Log
import com.nkds.hosikoouma.jasmine.data.TelegramChannelEntity
import com.nkds.hosikoouma.jasmine.data.TelegramDao
import com.nkds.hosikoouma.jasmine.data.TelegramSongEntity
import com.nkds.hosikoouma.jasmine.data.TelegramTopicEntity
import com.nkds.hosikoouma.jasmine.datamodels.Track
import com.nkds.hosikoouma.jasmine.data.toTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.isActive
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class TelegramRepository @Inject constructor(
    private val clientManager: TelegramClientManager,
    private val dao: TelegramDao
) {
    private companion object {
        private const val TAG = "TelegramRepository"
        private const val AUTH_REQUEST_TIMEOUT_MS = 20_000L
    }

    val authorizationState: Flow<TdApi.AuthorizationState?> = clientManager.authorizationState
    val authErrors: SharedFlow<TdApi.Error> = clientManager.errors

    fun isReady(): Boolean = clientManager.isReady()

    suspend fun awaitReady(timeoutMs: Long = 30_000L): Boolean =
        clientManager.awaitReady(timeoutMs)

    suspend fun sendPhoneNumberAwait(
        phoneNumber: String,
        timeoutMs: Long = AUTH_REQUEST_TIMEOUT_MS
    ): Result<Unit> = runAuthRequest(timeoutMs) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        clientManager.sendRequest<TdApi.Ok>(
            TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)
        )
    }

    suspend fun checkAuthenticationCodeAwait(
        code: String,
        timeoutMs: Long = AUTH_REQUEST_TIMEOUT_MS
    ): Result<Unit> = runAuthRequest(timeoutMs) {
        clientManager.sendRequest<TdApi.Ok>(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun checkAuthenticationPasswordAwait(
        password: String,
        timeoutMs: Long = AUTH_REQUEST_TIMEOUT_MS
    ): Result<Unit> = runAuthRequest(timeoutMs) {
        clientManager.sendRequest<TdApi.Ok>(TdApi.CheckAuthenticationPassword(password))
    }

    fun logout() {
        clientManager.logout()
    }

    private suspend fun runAuthRequest(
        timeoutMs: Long,
        block: suspend () -> TdApi.Object
    ): Result<Unit> {
        return try {
            withTimeout(timeoutMs) { block() }
            Result.success(Unit)
        } catch (timeout: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Telegram did not respond", timeout))
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    suspend fun searchPublicChat(username: String): TdApi.Chat? {
        return try {
            clientManager.sendRequest(TdApi.SearchPublicChat(username))
        } catch (e: Exception) {
            Log.e(TAG, "Error searching public chat: $username", e)
            null
        }
    }

    // --- Channel Management ---

    val allChannels: Flow<List<TelegramChannelEntity>> = dao.getAllChannels()

    suspend fun addChannel(chat: TdApi.Chat) {
        val username = try {
            val type = chat.type
            val field = type.javaClass.getField("username")
            field.isAccessible = true
            field.get(type) as? String
        } catch (e: Exception) {
            null
        }

        val photoPath = downloadChatPhoto(chat)

        val entity = TelegramChannelEntity(
            chatId = chat.id,
            title = chat.title,
            username = username,
            photoPath = photoPath
        )
        dao.insertChannel(entity)
        syncChannel(chat.id)
    }

    private suspend fun downloadChatPhoto(chat: TdApi.Chat): String? {
        val fileId = chat.photo?.small?.id ?: return null
        var file = getFile(fileId)
        
        if (file?.local?.isDownloadingCompleted == false) {
            file = downloadFile(fileId, 1)
            withTimeoutOrNull(5000) {
                while (file?.local?.isDownloadingCompleted == false) {
                    delay(200)
                    file = getFile(fileId)
                }
            }
        }
        return file?.local?.path?.takeIf { it.isNotEmpty() && File(it).exists() }
    }

    suspend fun removeChannel(chatId: Long) {
        // Очищаем кэш всех треков канала перед удалением
        val songs = dao.getSongsByChatId(chatId)
        songs.forEach { deleteFileFromCache(it.fileId) }
        
        dao.deleteChannel(chatId)
        dao.deleteSongsByChatId(chatId)
    }

    suspend fun syncChannel(chatId: Long) {
        try {
            val chat = clientManager.sendRequest<TdApi.Chat>(TdApi.GetChat(chatId))
            val photoPath = downloadChatPhoto(chat)
            val existing = dao.getAllChannels().first().find { it.chatId == chatId }
            if (existing != null) {
                dao.insertChannel(existing.copy(
                    title = chat.title,
                    photoPath = photoPath ?: existing.photoPath
                ))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh chat info for $chatId")
        }

        // Получаем актуальный список треков из Telegram
        val currentRemoteTracks = getAudioMessagesEntities(chatId)
        val remoteIds = currentRemoteTracks.map { it.id }.toSet()
        
        // Получаем то, что у нас в базе сейчас
        val localSongs = dao.getSongsByChatId(chatId)
        
        // Находим те, что были удалены в Telegram
        val songsToDelete = localSongs.filter { it.id !in remoteIds }
        
        // Удаляем файлы из кэша и записи из БД
        songsToDelete.forEach { song ->
            deleteFileFromCache(song.fileId)
            dao.deleteSong(song.id)
        }

        // Сохраняем новые / обновляем старые
        dao.insertSongs(currentRemoteTracks)

        val channel = dao.getAllChannels().first().find { it.chatId == chatId }
        if (channel != null) {
            dao.insertChannel(channel.copy(
                songCount = currentRemoteTracks.size,
                lastSyncTime = System.currentTimeMillis()
            ))
        }
    }

    // --- Audio Messages Fetching ---

    /**
     * Возвращает список сущностей напрямую для внутреннего использования при синхронизации.
     */
    private suspend fun getAudioMessagesEntities(chatId: Long): List<TelegramSongEntity> {
        try {
            clientManager.sendRequest<TdApi.Ok>(TdApi.OpenChat(chatId))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open chat: $chatId")
        }

        val allEntities = mutableListOf<TelegramSongEntity>()
        var nextFromMessageId = 0L
        val batchSize = 100

        try {
            while (true) {
                val request = TdApi.SearchChatMessages().apply {
                    this.chatId = chatId
                    this.query = ""
                    this.senderId = null
                    this.fromMessageId = nextFromMessageId
                    this.offset = 0
                    this.limit = batchSize
                    this.filter = TdApi.SearchMessagesFilterAudio()
                }

                val response = clientManager.sendRequest<TdApi.FoundChatMessages>(request)
                if (response.messages.isEmpty()) break

                response.messages.mapNotNull { mapMessageToEntity(it) }.let {
                    allEntities.addAll(it)
                }

                nextFromMessageId = response.nextFromMessageId
                if (nextFromMessageId == 0L) break
            }
            return allEntities
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat history for chat $chatId", e)
            return emptyList()
        }
    }

    suspend fun getAudioMessages(chatId: Long): List<Track> {
        val entities = getAudioMessagesEntities(chatId)
        dao.insertSongs(entities)
        return entities.map { it.toTrack() }
    }

    private fun mapMessageToEntity(message: TdApi.Message): TelegramSongEntity? {
        val content = message.content
        if (content !is TdApi.MessageAudio) return null
        val audio = content.audio

        var artUri: String? = null
        val thumbnail = audio.albumCoverThumbnail
        if (thumbnail != null) {
            artUri = "telegram_art://${message.chatId}/${message.id}"
        }

        return TelegramSongEntity(
            id = "${message.chatId}_${message.id}",
            chatId = message.chatId,
            messageId = message.id,
            fileId = audio.audio.id,
            title = audio.title.ifEmpty { audio.fileName.substringBeforeLast('.') },
            artist = audio.performer.ifEmpty { "Telegram Artist" },
            duration = audio.duration * 1000L,
            filePath = audio.audio.local.path,
            mimeType = audio.mimeType,
            dateAdded = message.date.toLong(),
            albumArtUriString = artUri,
            threadId = null
        )
    }

    // --- File & Art Management ---

    suspend fun downloadFile(fileId: Int, priority: Int = 1): TdApi.File? {
        return try {
            clientManager.sendRequest(TdApi.DownloadFile(fileId, priority, 0, 0, false))
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: $fileId", e)
            null
        }
    }

    suspend fun deleteFileFromCache(fileId: Int) {
        try {
            clientManager.sendRequest<TdApi.Ok>(TdApi.DeleteFile(fileId))
            Log.d(TAG, "File deleted from Telegram cache: $fileId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete file from cache: $fileId")
        }
    }

    suspend fun getFile(fileId: Int): TdApi.File? {
        return try {
            clientManager.sendRequest(TdApi.GetFile(fileId))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMessage(chatId: Long, messageId: Long): TdApi.Message? {
        return try {
            clientManager.sendRequest(TdApi.GetMessage(chatId, messageId))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isFileReady(fileId: Int): Boolean {
        val file = getFile(fileId) ?: return false
        return file.local.isDownloadingCompleted && 
               file.local.path.isNotEmpty() && 
               File(file.local.path).exists()
    }

    suspend fun getArtworkFile(chatId: Long, messageId: Long): File? {
        val message = getMessage(chatId, messageId) ?: return null
        val content = message.content
        val fileId = when (content) {
            is TdApi.MessageAudio -> content.audio.albumCoverThumbnail?.file?.id
            is TdApi.MessageDocument -> content.document.thumbnail?.file?.id
            else -> null
        } ?: return null

        var file = getFile(fileId)
        if (file?.local?.isDownloadingCompleted == false) {
            file = downloadFile(fileId, 1)
            withTimeoutOrNull(10000) {
                while (file?.local?.isDownloadingCompleted == false) {
                    delay(200)
                    file = getFile(fileId)
                }
            }
        }
        
        return file?.local?.path?.let { if (it.isNotEmpty()) File(it) else null }
    }
}
