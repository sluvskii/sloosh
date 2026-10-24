package com.slooshfilm.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.SlooshBottomBar
import com.slooshfilm.app.ui.continue_watching.ContinueScreen
import com.slooshfilm.app.ui.details.DetailsScreen
import com.slooshfilm.app.ui.details.MediaCatalogScreen
import com.slooshfilm.app.ui.home.HomeScreen
import com.slooshfilm.app.ui.messenger.MessengerScreen
import com.slooshfilm.app.ui.player.PlayerScreen
import com.slooshfilm.app.ui.profile.ProfileScreen
import com.slooshfilm.app.ui.search.SearchScreen
import com.slooshfilm.app.ui.search.rememberSearchState
import com.slooshfilm.app.ui.settings.*
import androidx.compose.ui.platform.LocalContext
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.LocalCardDensity
import com.slooshfilm.app.ui.components.LocalCardStyle
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.LocalPosterQuality
import com.slooshfilm.app.ui.components.LocalShowOriginalTitle
import com.slooshfilm.app.ui.components.LocalTabBarShowsLabels
import com.slooshfilm.app.ui.components.LocalGlassBlurMultiplier
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshTheme
import kotlin.math.roundToInt

private val IosNavEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun isMainTab(route: String?): Boolean {
    if (route == null) return false
    return route == Screen.Home.route ||
           route == Screen.Search.route ||
           route == Screen.Messenger.route ||
           route == Screen.ContinueWatching.route ||
           route == Screen.Profile.route
}

private fun isPlayer(route: String?): Boolean {
    return route?.startsWith("player") == true
}

@Composable
fun SlooshApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar on all screens except the full-screen video player
    val isPlayerScreen = currentRoute?.startsWith("player") == true
    val showBottomBar = !isPlayerScreen

    val searchState = rememberSearchState()

    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    val initialStartScreen = remember {
        val saved = localStorage.getStartScreen()
        if (Screen.mainScreens.any { it.route == saved }) saved else Screen.Home.route
    }

    var currentCardStyle by remember { mutableStateOf(localStorage.getCardStyle()) }
    var currentCardDensity by remember { mutableStateOf(localStorage.getCardDensity()) }
    var currentProgressiveBlur by remember { mutableStateOf(localStorage.getProgressiveBlurEnabled()) }
    var currentPosterQuality by remember { mutableStateOf(localStorage.getString("poster_quality", "Высокое")) }
    var currentShowOriginalTitle by remember { mutableStateOf(localStorage.getShowOriginalTitle()) }
    var currentTabBarShowsLabels by remember { mutableStateOf(localStorage.getTabBarShowsLabels()) }
    var currentGlassBlurMultiplier by remember { mutableFloatStateOf(localStorage.getLiquidGlassBlur()) }
    var currentGlassEnabled by remember { mutableStateOf(localStorage.getGlassEnabled()) }

    DisposableEffect(localStorage) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                LocalStorage.CARD_STYLE_KEY -> currentCardStyle = localStorage.getCardStyle()
                LocalStorage.CARD_DENSITY_KEY -> currentCardDensity = localStorage.getCardDensity()
                LocalStorage.PROGRESSIVE_BLUR_KEY -> currentProgressiveBlur = localStorage.getProgressiveBlurEnabled()
                "poster_quality" -> currentPosterQuality = localStorage.getString("poster_quality", "Высокое")
                "show_original_title" -> currentShowOriginalTitle = localStorage.getShowOriginalTitle()
                "tab_bar_shows_labels" -> currentTabBarShowsLabels = localStorage.getTabBarShowsLabels()
                LocalStorage.LIQUID_GLASS_BLUR_KEY -> currentGlassBlurMultiplier = localStorage.getLiquidGlassBlur()
                LocalStorage.GLASS_ENABLED_KEY -> currentGlassEnabled = localStorage.getGlassEnabled()
            }
        }
        localStorage.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            localStorage.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var previousTabRoute by rememberSaveable { mutableStateOf(initialStartScreen) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != null && Screen.bottomBarTabs.any { it.route == currentRoute }) {
            previousTabRoute = currentRoute
        }
    }

    // Freeze the root backdrop recording during push/pop transitions: the incoming screen's
    // first composition is the most expensive frame of the app, and re-recording the whole
    // NavHost into an offscreen layer on top of it doubles the compositing cost. The recorded
    // layer keeps its last frame, so the bottom bar glass samples a stale (visually identical,
    // static) frame for the 280ms of the slide. Tab fades (140ms) keep live recording because
    // the droplet pill animates then.
    var lastRouteForTransition by remember { mutableStateOf<String?>(null) }
    var isPushPopTransition by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        val previous = lastRouteForTransition
        lastRouteForTransition = currentRoute
        if (previous != null && currentRoute != null && previous != currentRoute) {
            val bothTabs = isMainTab(previous) && isMainTab(currentRoute)
            if (!bothTabs) {
                isPushPopTransition = true
                kotlinx.coroutines.delay(340)
                isPushPopTransition = false
            }
        }
    }

    val previousTab = remember(previousTabRoute) {
        Screen.bottomBarTabs.find { it.route == previousTabRoute } ?: Screen.Home
    }

    val isDarkTheme = LocalSlooshColors.current.isDark
    val view = LocalView.current
    LaunchedEffect(currentRoute, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val isImmersiveDarkScreen = currentRoute?.startsWith("details") == true || currentRoute?.startsWith("player") == true
            val forceDarkScreen = isDarkTheme || isImmersiveDarkScreen
            insetsController.isAppearanceLightStatusBars = !forceDarkScreen
            insetsController.isAppearanceLightNavigationBars = !forceDarkScreen
        }
    }

    val screenBg = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val navHostModifier = if (showBottomBar && currentGlassEnabled && !isPushPopTransition) {
        Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
    } else {
        Modifier.fillMaxSize()
    }

    CompositionLocalProvider(
        LocalCardStyle provides currentCardStyle,
        LocalCardDensity provides currentCardDensity,
        LocalGlassEnabled provides currentGlassEnabled,
        LocalProgressiveBlur provides (currentProgressiveBlur && currentGlassEnabled),
        LocalPosterQuality provides currentPosterQuality,
        LocalShowOriginalTitle provides currentShowOriginalTitle,
        LocalTabBarShowsLabels provides currentTabBarShowsLabels,
        LocalGlassBlurMultiplier provides currentGlassBlurMultiplier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            NavHost(
                navController = navController,
                startDestination = initialStartScreen,
                modifier = navHostModifier.clipToBounds(),
            enterTransition = {
                val targetRoute = targetState.destination.route
                val initialRoute = initialState.destination.route
                when {
                    isPlayer(targetRoute) -> {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(280, easing = IosNavEasing)
                        ) + fadeIn(animationSpec = tween(180))
                    }
                    isMainTab(initialRoute) && isMainTab(targetRoute) -> {
                        fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing))
                    }
                    else -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(280, easing = IosNavEasing)
                        ) + fadeIn(
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
                    }
                }
            },
            exitTransition = {
                val targetRoute = targetState.destination.route
                val initialRoute = initialState.destination.route
                when {
                    isPlayer(targetRoute) -> {
                        fadeOut(animationSpec = tween(150))
                    }
                    isMainTab(initialRoute) && isMainTab(targetRoute) -> {
                        fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))
                    }
                    else -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -(fullWidth * 0.22f).roundToInt() },
                            animationSpec = tween(280, easing = IosNavEasing)
                        ) + fadeOut(
                            animationSpec = tween(180, easing = FastOutLinearInEasing)
                        )
                    }
                }
            },
            popEnterTransition = {
                val targetRoute = targetState.destination.route
                val initialRoute = initialState.destination.route
                when {
                    isPlayer(initialRoute) -> {
                        fadeIn(animationSpec = tween(180))
                    }
                    isMainTab(initialRoute) && isMainTab(targetRoute) -> {
                        fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing))
                    }
                    else -> {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -(fullWidth * 0.22f).roundToInt() },
                            animationSpec = tween(280, easing = IosNavEasing)
                        ) + fadeIn(
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        )
                    }
                }
            },
            popExitTransition = {
                val targetRoute = targetState.destination.route
                val initialRoute = initialState.destination.route
                when {
                    isPlayer(initialRoute) -> {
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(260, easing = IosNavEasing)
                        ) + fadeOut(animationSpec = tween(150))
                    }
                    isMainTab(initialRoute) && isMainTab(targetRoute) -> {
                        fadeOut(animationSpec = tween(100, easing = FastOutLinearInEasing))
                    }
                    else -> {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(280, easing = IosNavEasing)
                        ) + fadeOut(
                            animationSpec = tween(180, easing = FastOutLinearInEasing)
                        )
                    }
                }
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onMediaClick = { media ->
                        val type = if (media.isTvSeries) "tv" else "movie"
                        navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                    },
                    onWatchClick = { media ->
                        val type = if (media.isTvSeries) "tv" else "movie"
                        navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onMediaClick = { media ->
                        val type = if (media.isTvSeries) "tv" else "movie"
                        navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                    },
                    onWatchClick = { media ->
                        val type = if (media.isTvSeries) "tv" else "movie"
                        navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                    },
                    searchState = searchState,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Messenger.route) {
                MessengerScreen(
                    onMediaClick = { mediaId, isTv ->
                        val type = if (isTv) "tv" else "movie"
                        val cleanId = mediaId.replace("tv_", "").replace("movie_", "")
                        navController.navigate(Screen.Details.createRoute(cleanId, type))
                    }
                )
            }

            composable(Screen.ContinueWatching.route) {
                ContinueScreen(
                    onPlayItem = { item ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                id = item.mediaId,
                                type = item.type,
                                season = item.season,
                                episode = item.episode,
                                voice = item.voice
                            )
                        )
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToDetails = { media ->
                        val type = if (media.isTvSeries) "tv" else "movie"
                        navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "movie" }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("id") ?: ""
                val mediaType = backStackEntry.arguments?.getString("type") ?: "movie"
                SlooshTheme(darkTheme = true) {
                    DetailsScreen(
                        mediaId = mediaId,
                        mediaType = mediaType,
                        onBack = { navController.popBackStack() },
                        onPlay = { season, episode, voice, translationId, iframeUrl ->
                            navController.navigate(
                                Screen.Player.createRoute(
                                    id = mediaId,
                                    type = mediaType,
                                    season = season,
                                    episode = episode,
                                    voice = voice,
                                    translationId = translationId,
                                    iframeUrl = iframeUrl
                                )
                            )
                        },
                        onSimilarClick = { similar ->
                            val type = if (similar.isTvSeries) "tv" else "movie"
                            navController.navigate(Screen.Details.createRoute(similar.cleanId, type))
                        },
                        onGenreClick = { genre, type ->
                            navController.navigate(Screen.Catalog.createGenreRoute(genre, type))
                        },
                        onStudioClick = { studioId, studioName ->
                            navController.navigate(Screen.Catalog.createStudioRoute(studioId, studioName))
                        }
                    )
                }
            }

            composable(
                route = Screen.Catalog.route,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("mediaType") { type = NavType.StringType; defaultValue = "all" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                SlooshTheme(darkTheme = true) {
                    MediaCatalogScreen(
                        kind = backStackEntry.arguments?.getString("kind").orEmpty(),
                        id = backStackEntry.arguments?.getString("id").orEmpty(),
                        name = backStackEntry.arguments?.getString("name").orEmpty(),
                        mediaType = backStackEntry.arguments?.getString("mediaType"),
                        onBack = { navController.popBackStack() },
                        onMediaClick = { media ->
                            val type = if (media.isTvSeries) "tv" else "movie"
                            navController.navigate(Screen.Details.createRoute(media.cleanId, type))
                        }
                    )
                }
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType; defaultValue = "movie" },
                    navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("voice") { type = NavType.StringType; defaultValue = "" },
                    navArgument("tid") { type = NavType.StringType; defaultValue = "" },
                    navArgument("iframe") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("id") ?: ""
                val mediaType = backStackEntry.arguments?.getString("type") ?: "movie"
                val seasonArg = backStackEntry.arguments?.getInt("season", -1)?.takeIf { it != -1 }
                val episodeArg = backStackEntry.arguments?.getInt("episode", -1)?.takeIf { it != -1 }
                val voiceArg = backStackEntry.arguments?.getString("voice")?.takeIf { it.isNotBlank() }?.let {
                    runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
                }
                val translationIdArg = backStackEntry.arguments?.getString("tid")?.takeIf { it.isNotBlank() }?.let {
                    runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
                }
                val iframeArg = backStackEntry.arguments?.getString("iframe")?.takeIf { it.isNotBlank() }?.let {
                    runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
                }

                SlooshTheme(darkTheme = true) {
                    // PlayerScreen's first composition is the heaviest in the app (ExoPlayer build,
                    // WebView stream resolution kick-off, full gesture/glass tree). Defer it until the
                    // 280ms slide-up transition settles: the transition animates a black placeholder
                    // (pixel-identical to the player's initial look), then the heavy frame composes
                    // while the screen is already static. The flag lives OUTSIDE key(...) so in-player
                    // episode/voice switches (which re-key PlayerScreen) don't re-trigger the delay.
                    var playerComposed by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(340)
                        playerComposed = true
                    }
                    key(mediaId, mediaType, seasonArg, episodeArg, voiceArg, translationIdArg, iframeArg) {
                        if (playerComposed) {
                            PlayerScreen(
                                mediaId = mediaId,
                                mediaType = mediaType,
                                initialSeason = seasonArg,
                                initialEpisode = episodeArg,
                                initialVoice = voiceArg,
                                initialTranslationId = translationIdArg,
                                initialIframeUrl = iframeArg,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black))
                        }
                    }
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    onGlassEnabledChange = { currentGlassEnabled = it }
                )
            }

            composable(Screen.SettingsAppearance.route) {
                SettingsScreen(
                    navController = navController,
                    onGlassEnabledChange = { currentGlassEnabled = it }
                )
            }

            composable(Screen.SettingsGeneral.route) {
                SettingsScreen(
                    navController = navController,
                    onGlassEnabledChange = { currentGlassEnabled = it }
                )
            }

            composable(Screen.SettingsPlayer.route) {
                SettingsScreen(
                    navController = navController,
                    onGlassEnabledChange = { currentGlassEnabled = it }
                )
            }

            composable(Screen.SettingsAbout.route) {
                SettingsScreen(
                    navController = navController,
                    onGlassEnabledChange = { currentGlassEnabled = it }
                )
            }
        }

        // Floating Bottom Bar Island Overlay
        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SlooshBottomBar(
                currentRoute = currentRoute,
                previousTab = previousTab,
                searchState = searchState,
                backdrop = backdrop,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(initialStartScreen) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Оффлайн-плашка: единый индикатор потери сети для всех экранов
        com.slooshfilm.app.ui.components.OfflineBanner(
            backdrop = if (currentGlassEnabled) backdrop else null,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
}
