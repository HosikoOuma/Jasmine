package com.nkds.hosikoouma.jasmine

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramArtFetcher
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JasmineApp : Application() {
    
    @Inject lateinit var telegramRepository: TelegramRepository

    override fun onCreate() {
        super.onCreate()
        
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
