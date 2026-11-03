package dev.readtracker.lite

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import android.content.Context
import dev.readtracker.lite.data.Store

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // прогреваем хранилище при старте
        Store.get(this)
    }

    override fun onPause() {
        super.onPause()
        // сохранение при сворачивании приложения (II-12)
        val s = Store.get(this)
        s.saveTitles()
        s.saveSettings()
    }

    /** Coil с диском кэшем изображений (для сетевых обложек). */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.10).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("img_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()
}
