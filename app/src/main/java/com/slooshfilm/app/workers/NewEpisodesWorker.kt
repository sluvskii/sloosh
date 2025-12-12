package com.slooshfilm.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.slooshfilm.app.MainActivity
import com.slooshfilm.app.R
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.data.hdrezka.Notification
import com.slooshfilm.app.data.hdrezka.NotificationItem

class NewEpisodesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as SlooshApplication
            val repository = app.hdRezkaRepository
            val localStorage = LocalStorage(applicationContext)

            // If repository state isn't initialized fully, we might not know if logged in.
            // But isLoggedIn is a StateFlow, so .value gives current state.
            if (!repository.isLoggedIn.value) {
                return Result.success()
            }

            val notifications = repository.getNotifications()
            if (notifications.isEmpty()) {
                return Result.success()
            }

            val latestNotification = notifications.first()
            val topItem = latestNotification.items.firstOrNull() ?: return Result.success()
            
            val uniqueKey = "${latestNotification.date}|${topItem.name}|${topItem.info}"
            val lastKey = localStorage.getString("last_notification_key", "")

            if (uniqueKey != lastKey) {
                showNotification(topItem.name, topItem.info)
                localStorage.putString("last_notification_key", uniqueKey)
                localStorage.putString("last_notification_date", latestNotification.date)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "new_episodes_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "New Episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) 
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
