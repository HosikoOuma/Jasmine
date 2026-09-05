package com.nkds.hosikoouma.jasmine

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nkds.hosikoouma.jasmine.core.CrashHandler
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramArtFetcher
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class JasmineApp : Application() {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface JasmineAppEntryPoint {
        fun telegramRepository(): TelegramRepository
    }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Global Crash Handler инициализируем во всех процессах
        CrashHandler.initialize(this)

        // 2. Тяжелую инициализацию (Telegram, Coil) делаем ТОЛЬКО в основном процессе
        if (isMainProcess()) {
            setupMainProcess()
        }
    }

    private fun isMainProcess(): Boolean {
        return try {
            val processName = getProcessName()
            processName == packageName
        } catch (e: Exception) {
            true
        }
    }

    private fun setupMainProcess() {
        // Получаем репозиторий через EntryPoint только в основном процессе
        val entryPoint = EntryPointAccessors.fromApplication(this, JasmineAppEntryPoint::class.java)
        val telegramRepository = entryPoint.telegramRepository()

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(TelegramArtFetcher.Factory(telegramRepository))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
