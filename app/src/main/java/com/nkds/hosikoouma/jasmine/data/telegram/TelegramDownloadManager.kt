package com.nkds.hosikoouma.jasmine.data.telegram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nkds.hosikoouma.jasmine.R
import com.nkds.hosikoouma.jasmine.data.TelegramDao
import com.nkds.hosikoouma.jasmine.data.TelegramSongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filter
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TelegramRepository,
    private val clientManager: TelegramClientManager,
    private val dao: TelegramDao
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private companion object {
        const val CHANNEL_ID = "telegram_downloads"
        const val TAG = "TelegramDownloadManager"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.telegram_downloads_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun downloadTracks(songIds: List<String>) {
        managerScope.launch {
            songIds.forEach { id ->
                // Запускаем каждый трек в отдельной корутине для параллельной загрузки
                launch {
                    val song = dao.getSongById(id) ?: return@launch
                    downloadTrack(song)
                }
            }
        }
    }

    suspend fun downloadTrack(song: TelegramSongEntity) = withContext(Dispatchers.IO) {
        showProgressNotification(song, 0)

        try {
            var file = repository.getFile(song.fileId)
            if (file?.local?.isDownloadingCompleted == false) {
                repository.downloadFile(song.fileId, 3)
                
                try {
                    clientManager.updates
                        .filterIsInstance<TdApi.UpdateFile>()
                        .filter { it.file.id == song.fileId }
                        .collect { update ->
                            val currentFile = update.file
                            val progress = if (currentFile.expectedSize > 0) {
                                (currentFile.local.downloadedSize * 100 / currentFile.expectedSize).toInt()
                            } else 0
                            
                            showProgressNotification(song, progress)
                            
                            if (currentFile.local.isDownloadingCompleted) {
                                file = currentFile
                                throw CancellationException("Download completed")
                            }
                        }
                } catch (e: CancellationException) {
                    // Ожидаемое завершение загрузки
                }
            }

            val sourcePath = file?.local?.path
            if (sourcePath.isNullOrEmpty() || !File(sourcePath).exists()) {
                showErrorNotification(song, context.getString(R.string.download_error_path))
                return@withContext
            }

            val savedFile = saveToPublicMusic(song, File(sourcePath))
            if (savedFile != null) {
                showSuccessNotification(song)
                android.media.MediaScannerConnection.scanFile(context, arrayOf(savedFile.absolutePath), null, null)
            } else {
                showErrorNotification(song, context.getString(R.string.download_error_save))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            showErrorNotification(song, e.message ?: "Unknown error")
        }
    }

    private fun saveToPublicMusic(song: TelegramSongEntity, source: File): File? {
        val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Jasmine/Saved")
        if (!destDir.exists()) destDir.mkdirs()

        val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val cleanArtist = song.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        
        val extension = when (song.mimeType.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/x-flac", "audio/flac" -> "flac"
            "audio/aac" -> "aac"
            "audio/m4a", "audio/x-m4a", "audio/mp4" -> "m4a"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/opus" -> "opus"
            else -> song.mimeType.substringAfter("/", "mp3")
        }
        
        val destFile = File(destDir, "$cleanArtist - $cleanTitle.$extension")

        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            null
        }
    }

    private fun showProgressNotification(song: TelegramSongEntity, progress: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.downloading_track, song.title))
            .setContentText("$progress%")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        
        try {
            notificationManager.notify(song.fileId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing")
        }
    }

    private fun showSuccessNotification(song: TelegramSongEntity) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.download_complete))
            .setContentText(context.getString(R.string.download_success_desc, song.title))
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(song.fileId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing")
        }
    }

    private fun showErrorNotification(song: TelegramSongEntity, error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.download_failed))
            .setContentText("${song.title}: $error")
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(song.fileId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing")
        }
    }
}
