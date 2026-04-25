package com.nkds.hosikoouma.jasmine.data.telegram

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Папка с музыкой находится в кэше
    private val telegramCacheDir get() = File(context.cacheDir, "tdlib_files")

    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        getFolderSize(telegramCacheDir)
    }

    private fun getFolderSize(file: File): Long {
        if (file.exists()) {
            if (file.isDirectory) {
                return file.listFiles()?.sumOf { getFolderSize(it) } ?: 0L
            }
            return file.length()
        }
        return 0L
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        // Очищаем только папку с файлами, не трогая БД в filesDir
        deleteFolderContent(telegramCacheDir)
    }

    private fun deleteFolderContent(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFolder(it) }
            return true
        }
        return false
    }

    private fun deleteFolder(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteFolder(it) }
        }
        return file.delete()
    }
}
