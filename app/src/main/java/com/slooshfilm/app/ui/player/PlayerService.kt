package com.slooshfilm.app.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.slooshfilm.app.R
import com.slooshfilm.app.data.LocalStorage

@UnstableApi
class PlayerService : MediaSessionService() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "sloosh_player_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Playback"
        private const val NOTIFICATION_ID = 0xCAFE
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Ensure notification channel exists (no-op on < O)
        createNotificationChannel()

        val localStorage = LocalStorage(this)
        val seekForwardMs = localStorage.getSeekForwardInterval() * 1000L
        val seekBackwardMs = localStorage.getSeekBackwardInterval() * 1000L

        val player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(seekBackwardMs)
            .setSeekForwardIncrementMs(seekForwardMs)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as foreground immediately to satisfy Android's requirement when startForegroundService() is used.
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.stop()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, PlayerActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.playback_running))
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
