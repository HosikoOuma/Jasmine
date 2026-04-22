package com.nkds.hosikoouma.jasmine

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JasmineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Увеличиваем кэш в памяти до 30% (было 25%)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    // Увеличиваем дисковый кэш до 250 МБ (было 50 МБ)
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            // Включаем поддержку работы с MediaStore и метаданными
            .allowHardware(true)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
