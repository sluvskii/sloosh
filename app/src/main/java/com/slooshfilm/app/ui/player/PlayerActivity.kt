package com.slooshfilm.app.ui.player

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.slooshfilm.app.R
import com.slooshfilm.app.data.hdrezka.StreamInfo
import java.io.Serializable

class PlayerActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        if (savedInstanceState == null) {
            val postId = intent.getStringExtra(EXTRA_POST_ID)
            val season = intent.getIntExtra(EXTRA_SEASON, -1).takeIf { it != -1 }
            val episode = intent.getIntExtra(EXTRA_EPISODE, -1).takeIf { it != -1 }
            val translatorId = intent.getStringExtra(EXTRA_TRANSLATOR_ID)
            @Suppress("DEPRECATION")
            val streamInfo = intent.getSerializableExtra(EXTRA_STREAMS) as? StreamInfo

            if (postId != null && streamInfo != null) {
                val fragment =  PlayerFragment.newInstance(postId, translatorId, season, episode, streamInfo)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializeController()
    }

    override fun onStop() {
        super.onStop()
        if (::controllerFuture.isInitialized && controllerFuture.isDone) {
            controllerFuture.get().playWhenReady = false
        }
        releaseController()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            stopService(Intent(this, PlayerService::class.java))
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (::controllerFuture.isInitialized && controllerFuture.isDone && controllerFuture.get().isPlaying) {
                enterPictureInPictureMode(buildPipParams())
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // The PlayerFragment will handle its own UI changes via Compose now.
    }

    private fun buildPipParams(): PictureInPictureParams {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(true)
                .build()
        } else {
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
        }
    }

    private fun initializeController() {
        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                controller.playWhenReady = true

                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
                if (videoUrl != null && controller.currentMediaItem == null) {
                    val title = intent.getStringExtra(EXTRA_TITLE)
                    val season = intent.getIntExtra(EXTRA_SEASON, -1)
                    val episode = intent.getIntExtra(EXTRA_EPISODE, -1)

                    val subtitle = if (season != -1 && episode != -1) "Сезон $season Серия $episode" else null

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_POST_ID = "post_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SEASON = "season"
        private const val EXTRA_EPISODE = "episode"
        private const val EXTRA_TRANSLATOR_ID = "translator_id"
        private const val EXTRA_STREAMS = "streams"

        fun newIntent(
            context: Context, 
            videoUrl: String, 
            postId: String, 
            title: String?, 
            season: Int?, 
            episode: Int?,
            translatorId: String?,
            streamInfo: StreamInfo
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_TITLE, title)
                season?.let { putExtra(EXTRA_SEASON, it) }
                episode?.let { putExtra(EXTRA_EPISODE, it) }
                translatorId?.let { putExtra(EXTRA_TRANSLATOR_ID, it) }
                putExtra(EXTRA_STREAMS, streamInfo as Serializable)
            }
        }
    }
}