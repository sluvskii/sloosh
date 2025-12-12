package com.slooshfilm.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.slooshfilm.app.utils.UpdateConfig
import com.slooshfilm.app.MainActivity
import com.slooshfilm.app.R
import com.slooshfilm.app.utils.AppUpdater

class AppUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val pm = applicationContext.packageManager
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(applicationContext.packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(applicationContext.packageName, 0)
            }
            val versionName = pi.versionName ?: ""

            val updater = AppUpdater(applicationContext)
            val owner = UpdateConfig.GITHUB_OWNER
            val repo = UpdateConfig.GITHUB_REPO
            val info = updater.checkGithubLatest(owner, repo, versionName)
            if (info != null) {
                showUpdateNotification(info.url)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showUpdateNotification(downloadUrl: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Обновления", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Уведомления об обновлениях"
            ch.enableLights(true)
            ch.lightColor = Color.YELLOW
            nm.createNotificationChannel(ch)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("UPDATE_URL", downloadUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Доступно обновление Sloosh")
            .setContentText("Нажмите, чтобы скачать и установить")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        nm.notify(2001, notif)
    }
}
