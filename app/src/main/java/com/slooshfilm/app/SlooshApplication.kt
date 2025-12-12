package com.slooshfilm.app

import android.app.Application
import androidx.room.Room
import com.slooshfilm.app.data.AppDatabase
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.data.hdrezka.UserData
import com.slooshfilm.app.data.hdrezka.UserModel
import com.slooshfilm.app.data.repository.HdRezkaRepository
import com.slooshfilm.app.data.repository.WatchHistoryRepository
import com.slooshfilm.app.ui.AppViewModelFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.slooshfilm.app.workers.NewEpisodesWorker
import com.slooshfilm.app.workers.AppUpdateWorker
import java.util.concurrent.TimeUnit

class SlooshApplication : Application() {

    lateinit var hdRezkaRepository: HdRezkaRepository
        private set

    lateinit var watchHistoryRepository: WatchHistoryRepository
        private set

    lateinit var viewModelFactory: AppViewModelFactory
        private set

    lateinit var localStorage: LocalStorage
        private set

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()

        localStorage = LocalStorage(applicationContext)
        hdRezkaRepository = HdRezkaRepository(localStorage)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "sloosh-db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        watchHistoryRepository = WatchHistoryRepository(db.watchHistoryDao(), db.movieDao())

        viewModelFactory = AppViewModelFactory(hdRezkaRepository, watchHistoryRepository)

        // Initialize UserData from storage
        UserData.init(applicationContext)

        // If cookies indicate logged-in but we don't have saved username/avatar, try fetching them in background
        if (localStorage.getAllCookies().any { it.name == "dle_user_id" }) {
            val savedName = localStorage.getString("user_name", null)
            val savedAvatar = localStorage.getString("user_avatar", null)

            if (savedName.isNullOrEmpty() || savedAvatar.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val name = UserModel.getUserName(applicationContext)
                        val avatar = UserModel.getUserAvatarLink(applicationContext)

                                // Do not persist auto-fetched profile data to prefs — avoid accidental "ghost" login.
                                // Only keep it in-memory (UserData) so UI can optionally show avatar/name when explicitly queried,
                                // but saved username (used to decide logged-in UI) remains authoritative.
                                if (!name.isNullOrEmpty()) {
                                    UserData.userName = name
                                }
                                if (!avatar.isNullOrEmpty()) {
                                    UserData.avatarLink = avatar
                                }
                    } catch (_: Exception) {
                        // ignore background fetch errors
                    }
                }
            }
        }
        
        val workRequest = PeriodicWorkRequestBuilder<NewEpisodesWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "NewEpisodesCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val updateWork = PeriodicWorkRequestBuilder<AppUpdateWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AppUpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWork
        )
    }

    private fun setupGlobalCrashHandler() {
        try {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    // Log to logcat
                    Log.e("SlooshAppCrash", "Uncaught exception in thread ${thread.name}", throwable)

                    // Write a crash file to internal storage for later inspection
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
                    // Best-effort: if logging fails, avoid masking original throwable
                    Log.e("SlooshAppCrash", "Failed to write crash file", t)
                } finally {
                    // Delegate to previous handler (this may show system dialog / crash UI)
                    try {
                        previousHandler?.uncaughtException(thread, throwable)
                    } catch (_: Throwable) {
                        // ignore delegate errors
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("SlooshAppCrash", "Failed to set default uncaught exception handler", t)
        }
    }
}
