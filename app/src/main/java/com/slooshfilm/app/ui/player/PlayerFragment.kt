package com.slooshfilm.app.ui.player

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.slooshfilm.app.SlooshApplication
import com.slooshfilm.app.ui.player.compose.PlayerScreen
import com.slooshfilm.app.data.hdrezka.StreamInfo
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import com.slooshfilm.app.data.hdrezka.Subtitle
import java.io.Serializable

class PlayerFragment : Fragment() {

    private val viewModel: PlayerViewModel by viewModels { (requireActivity().application as SlooshApplication).viewModelFactory }
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private val postId: String by lazy { arguments?.getString(ARG_POST_ID) ?: "" }
    private val translatorId: String by lazy { arguments?.getString(ARG_TRANSLATOR_ID) ?: "" }
    private val season: String? by lazy { arguments?.getString(ARG_SEASON) }
    private val episode: String? by lazy { arguments?.getString(ARG_EPISODE) }
    private val streamInfo: StreamInfo by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_STREAMS, StreamInfo::class.java) ?: StreamInfo(emptyMap())
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_STREAMS) as? StreamInfo ?: StreamInfo(emptyMap())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var player by remember { mutableStateOf<MediaController?>(null) }
                var hasNext by remember { mutableStateOf(false) }
                var hasPrevious by remember { mutableStateOf(false) }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(context, ComponentName(context, PlayerService::class.java))
                    controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                    controllerFuture.addListener(
                        {
                            val newPlayer = controllerFuture.get()
                            player = newPlayer
                            
                            newPlayer.addListener(object : Player.Listener {
                                override fun onEvents(player: Player, events: Player.Events) {
                                    hasNext = player.hasNextMediaItem()
                                    hasPrevious = player.hasPreviousMediaItem()
                                }
                            })

                        },
                        ContextCompat.getMainExecutor(context)
                    )

                    onDispose {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }

                player?.let { p ->
                    PlayerScreen(
                        player = p,
                        streamInfo = streamInfo,
                        onQualitySelected = { newUrl -> changeStream(p, newUrl) },
                        onSubtitleSelected = { subtitle -> changeSubtitle(p, subtitle) },
                        onPipClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                requireActivity().enterPictureInPictureMode()
                            }
                        },
                        onNextClick = { p.seekToNext() },
                        onPreviousClick = { p.seekToPrevious() },
                        hasNext = hasNext,
                        hasPrevious = hasPrevious
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterFullScreen()
        view.keepScreenOn = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::controllerFuture.isInitialized && controllerFuture.isDone) {
            val player = controllerFuture.get()
            val position = player.currentPosition
            val duration = player.duration
            if (position > 5000 && duration > 0 && position < duration - 5000) {
                viewModel.addWatchLater(postId)
            }
            if (position > 0 && duration > 0) {
                 viewModel.updateTime(postId, translatorId, season, episode, position / 1000)
            }
        }
        view?.keepScreenOn = false
        exitFullScreen()
    }

    private fun changeStream(player: MediaController, newUrl: String) {
        val currentPosition = player.currentPosition
        val wasPlaying = player.playWhenReady
        val currentMediaItem = player.currentMediaItem

        val newMediaItemBuilder = MediaItem.Builder().setUri(newUrl)
        currentMediaItem?.mediaMetadata?.let { metadata ->
            newMediaItemBuilder.setMediaMetadata(metadata)
        }
        
        // Preserve subtitle configuration if exists
        currentMediaItem?.localConfiguration?.subtitleConfigurations?.let {
            newMediaItemBuilder.setSubtitleConfigurations(it)
        }

        player.setMediaItem(newMediaItemBuilder.build(), currentPosition)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    private fun changeSubtitle(player: MediaController, subtitle: Subtitle?) {
        val currentPosition = player.currentPosition
        val wasPlaying = player.playWhenReady
        val currentMediaItem = player.currentMediaItem ?: return
        val currentUri = currentMediaItem.localConfiguration?.uri ?: return

        val newMediaItemBuilder = MediaItem.Builder().setUri(currentUri)
        currentMediaItem.mediaMetadata.let { metadata ->
            newMediaItemBuilder.setMediaMetadata(metadata)
        }

        if (subtitle != null) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                .setMimeType(MimeTypes.TEXT_VTT) // Default to VTT, but usually Rezka sends .vtt
                .setLanguage(subtitle.languageCode)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            newMediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        } else {
             newMediaItemBuilder.setSubtitleConfigurations(emptyList())
        }

        player.setMediaItem(newMediaItemBuilder.build(), currentPosition)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    private fun enterFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = requireActivity().window
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun exitFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = requireActivity().window
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_TRANSLATOR_ID = "translator_id"
        private const val ARG_SEASON = "season"
        private const val ARG_EPISODE = "episode"
        private const val ARG_STREAMS = "streams"

        fun newInstance(postId: String, translatorId: String?, season: Int?, episode: Int?, streamInfo: StreamInfo): PlayerFragment {
            return PlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_TRANSLATOR_ID, translatorId)
                    if (season != null) putString(ARG_SEASON, season.toString())
                    if (episode != null) putString(ARG_EPISODE, episode.toString())
                    putSerializable(ARG_STREAMS, streamInfo)
                }
            }
        }
    }
}