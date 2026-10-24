package com.slooshfilm.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.slooshfilm.app.data.api.MoviesApi
import coil.intercept.Interceptor
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SlooshApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()
        MoviesApi.init(this)
        com.slooshfilm.app.utils.NetworkMonitor.start(this)
        checkForUpdatesOnStart()
    }

    /**
     * Тихая проверка обновлений при запуске (GitHub Releases, как в настройках).
     * Если есть новая версия — один тост с подсказкой, где обновиться.
     */
    private fun checkForUpdatesOnStart() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: return
        } catch (_: Exception) {
            return
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main).launch {
            val update = try {
                com.slooshfilm.app.utils.AppUpdater(this@SlooshApplication).checkGithubLatest(
                    com.slooshfilm.app.utils.UpdateConfig.GITHUB_OWNER,
                    com.slooshfilm.app.utils.UpdateConfig.GITHUB_REPO,
                    versionName
                )
            } catch (_: Exception) {
                null
            }
            if (update != null) {
                try {
                    android.widget.Toast.makeText(
                        this@SlooshApplication,
                        "Доступна новая версия sloosh. Обновить: Настройки → О приложении",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowHardware(true) // Native GPU HardwareBuffer textures for buttery smooth 120 FPS rendering
            .components {
                add(Interceptor { chain ->
                    val request = chain.request
                    val data = request.data
                    if (data is String) {
                        val resolved = MoviesApi.resolveEffectiveImageUrl(data)
                        if (!resolved.isNullOrBlank() && resolved != data) {
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
                val dir = cacheDir.resolve("image_cache")
                dir.mkdirs()
                DiskCache.Builder()
                    .directory(dir)
                    .maxSizeBytes(100L * 1024 * 1024)
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
