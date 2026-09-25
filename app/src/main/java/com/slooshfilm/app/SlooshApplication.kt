package com.slooshfilm.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.slooshfilm.app.data.api.MoviesApi
import coil.intercept.Interceptor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SlooshApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()
        MoviesApi.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(Interceptor { chain ->
                    val request = chain.request
                    val data = request.data
                    if (data is String) {
                        val resolved = MoviesApi.resolveEffectiveImageUrl(data)
                        if (resolved != null && resolved != data) {
                            return@Interceptor chain.proceed(request.newBuilder().data(resolved).build())
                        }
                    }
                    chain.proceed(request)
                })
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun setupGlobalCrashHandler() {
        try {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    Log.e("SlooshAppCrash", "Uncaught exception in thread ${thread.name}", throwable)

                    val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                    val timestamp = sdf.format(Date())
                    val crashFile = File(filesDir, "crash_$timestamp.txt")
                    val content = StringBuilder()
                    content.append("Thread: ").append(thread.name).append('\n')
                    content.append("Time: ").append(timestamp).append('\n')
                    content.append('\n')
                    content.append(throwable.stackTraceToString())
                    crashFile.writeText(content.toString())
                } catch (t: Throwable) {
                    Log.e("SlooshAppCrash", "Failed to write crash file", t)
                } finally {
                    try {
                        previousHandler?.uncaughtException(thread, throwable)
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("SlooshAppCrash", "Failed to set default uncaught exception handler", t)
        }
    }
}
