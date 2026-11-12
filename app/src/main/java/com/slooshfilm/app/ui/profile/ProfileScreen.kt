package com.slooshfilm.app.ui.profile

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.components.rememberCutoutHorizontalPadding
import com.kyant.shapes.Capsule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import com.slooshfilm.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.repository.AuthRepository
import com.slooshfilm.app.data.repository.CloudSyncService
import com.slooshfilm.app.data.storage.FavoritesStore
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.EmptyStateView
import com.slooshfilm.app.ui.components.LiquidGlassAlertDialog
import com.slooshfilm.app.ui.components.LiquidGlassDialog
import com.slooshfilm.app.ui.components.LiquidGlassTokens
import com.slooshfilm.app.ui.components.LocalCardDensity
import com.slooshfilm.app.ui.components.MediaCard
import com.slooshfilm.app.ui.components.LiquidGlassDualButton
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.plusBlendMode
import com.slooshfilm.app.ui.components.rememberNavBarBottomPadding
import androidx.compose.ui.graphics.RectangleShape
import com.slooshfilm.app.ui.backdrop.drawBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.layerBackdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberLayerBackdrop
import com.slooshfilm.app.ui.components.LocalBackdrop
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.ProgressiveBlurTokens
import com.slooshfilm.app.ui.components.iosProgressiveBlur
import com.slooshfilm.app.ui.theme.*

enum class FavoriteCategory(val title: String) {
    ALL("Все"),
    MOVIES("Фильмы"),
    SERIES("Сериалы"),
    CARTOONS("Мульты")
}

@Composable
fun ProfileScreen(
    onNavigateToDetails: (MediaDto) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoritesStore = remember { FavoritesStore.getInstance(context) }
    val favorites by favoritesStore.favoritesFlow.collectAsStateWithLifecycle()

    val authRepo = remember { AuthRepository.getInstance(context) }
    val currentUser by authRepo.currentUser.collectAsStateWithLifecycle()
    val isAuthenticated = !currentUser.isAnonymous
    val cloudSync = remember { CloudSyncService.getInstance(context) }

    var selectedCategory by remember { mutableStateOf(FavoriteCategory.ALL) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSignOutAlert by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }
    var longClickedMedia by remember { mutableStateOf<MediaDto?>(null) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            cloudSync.syncAllData()
        }
    }

    val filteredFavorites = remember(favorites, selectedCategory) {
        when (selectedCategory) {
            FavoriteCategory.ALL -> favorites
            FavoriteCategory.MOVIES -> favorites.filter { !it.isTvSeries && !it.isCartoon }
            FavoriteCategory.SERIES -> favorites.filter { it.isTvSeries && !it.isCartoon }
            FavoriteCategory.CARTOONS -> favorites.filter { it.isCartoon || it.isAnime || it.type == "cartoon" }
        }
    }

    val screenBg = MaterialTheme.colorScheme.background
    val contentBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }
    val sceneBackdrop = rememberLayerBackdrop {
        drawRect(screenBg)
        drawContent()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val rawStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarPadding = if (rawStatusBarPadding > 0.dp) rawStatusBarPadding else (if (isLandscape) 8.dp else 0.dp)
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + (if (isLandscape) 80.dp else 106.dp)
    val headerBlurHeight = statusBarPadding + (if (isLandscape) 96.dp else 140.dp)
    val bottomTotalPadding = if (isLandscape) navBarBottomPadding + 48.dp else navBarBottomPadding + 72.dp

    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val headerFadeDistancePx = remember(density) { with(density) { 56.dp.toPx() } }
    val headerScrollFraction by remember(gridState, headerFadeDistancePx) {
        derivedStateOf {
            val offsetPx = if (gridState.firstVisibleItemIndex > 0) {
                headerFadeDistancePx
            } else {
                gridState.firstVisibleItemScrollOffset.toFloat()
            }
            (offsetPx / headerFadeDistancePx).coerceIn(0f, 1f)
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        animationSpec = tween(durationMillis = 200),
        label = "profileHeaderAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // 1. Edge-to-Edge Content and Progressive Blur Header captured in sceneBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(sceneBackdrop)
        ) {
            // Scrollable Content Layer captured in contentBackdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(contentBackdrop)
            ) {
                // Solid opaque background so posters and cards blur completely
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(screenBg)
                )

                if (favorites.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topHeaderHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            iconSfName = "bookmark.fill",
                            title = "Пока ничего не добавлено",
                            message = "Сохраняйте фильмы и сериалы в избранное, чтобы быстро возвращаться к ним позже."
                        )
                    }
                } else if (filteredFavorites.isEmpty()) {
                    // Category empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topHeaderHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            iconSfName = "film.fill",
                            title = "В этой подборке пока пусто",
                            message = "Попробуйте открыть другую вкладку или добавьте что-нибудь в избранное."
                        )
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val isTablet = configuration.screenWidthDp >= 600
                    val cardDensity = LocalCardDensity.current
                    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
                    val cutoutPadding = rememberCutoutHorizontalPadding()
                    val gridSpacing = if (isTablet) 10.dp else if (isCompact) 6.dp else 8.dp
                    val gridSidePadding = (if (isTablet) 20.dp else if (isCompact) 10.dp else 12.dp) + cutoutPadding
                    val columnsCount = if (isTablet) 6 else if (isLandscape) 4 else 3
                    val lastScrollTimestamp = remember { java.util.concurrent.atomic.AtomicLong(0L) }
                    val scrollTrackingConnection = remember(gridState) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                                return Offset.Zero
                            }

                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                                return Offset.Zero
                            }

                            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                                lastScrollTimestamp.set(android.os.SystemClock.uptimeMillis())
                                return Velocity.Zero
                            }
                        }
                    }

                    val onMediaCardClick = remember<(MediaDto) -> Unit>(onNavigateToDetails, gridState) {
                        { media ->
                            val now = android.os.SystemClock.uptimeMillis()
                            val wasRecentlyScrolling = (now - lastScrollTimestamp.get()) < 350L
                            if (!gridState.isScrollInProgress && !wasRecentlyScrolling) {
                                onNavigateToDetails(media)
                            }
                        }
                    }
                    val onMediaCardLongClick = remember<(MediaDto) -> Unit>(gridState) {
                        { media ->
                            val now = android.os.SystemClock.uptimeMillis()
                            val wasRecentlyScrolling = (now - lastScrollTimestamp.get()) < 350L
                            if (!gridState.isScrollInProgress && !wasRecentlyScrolling) {
                                longClickedMedia = media
                            }
                        }
                    }

                    // Adaptive Poster Grid (3 cols on phones, 4/5 in landscape, 6/7 on tablets)
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(
                            top = topHeaderHeight + 8.dp,
                            bottom = bottomTotalPadding,
                            start = gridSidePadding,
                            end = gridSidePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollTrackingConnection)
                    ) {
                        items(filteredFavorites, key = { it.favoriteIdentity }) { media ->
                            MediaCard(
                                media = media,
                                onClick = onMediaCardClick,
                                onLongClick = onMediaCardLongClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Floating Progressive Blur Top Header (Apple TV+ Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBlurHeight)
                    .align(Alignment.TopCenter)
            ) {
                if (LocalProgressiveBlur.current && headerAlpha > 0.001f) {
                    val isLightTheme = !LocalSlooshColors.current.isDark
                    // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = headerAlpha }
                            .drawBackdrop(
                                backdrop = contentBackdrop,
                                shape = { RectangleShape },
                                effects = {
                                    val topOffsetPx = statusBarPadding.toPx()
                                    iosProgressiveBlur(
                                        maxRadius = ProgressiveBlurTokens.MaxRadius.toPx(),
                                        topOffset = topOffsetPx,
                                        falloffDistance = (size.height - topOffsetPx).coerceAtLeast(1f),
                                        isLight = isLightTheme
                                    )
                                },
                                highlight = null,
                                shadow = null
                            )
                    )
                }

                // Ambient gradient overlay for typography contrast & seamless edge blending
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = headerAlpha }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    screenBg,
                                    screenBg.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }
        }

        // 2. Floating Liquid Glass Header matching iOS 1:1 (refracting sceneBackdrop)
        CompositionLocalProvider(LocalBackdrop provides sceneBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarPadding)
                    .padding(bottom = if (isLandscape) 4.dp else 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Header Bar: Left Avatar | Center Title | Right Download+Settings Capsule
                    val cutoutPadding = rememberCutoutHorizontalPadding()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 44.dp else 56.dp)
                            .padding(horizontal = 16.dp + cutoutPadding)
                    ) {
                        // Left Avatar / Sign-In Button (iOS 1:1)
                        if (isAuthenticated) {
                            LiquidGlassFloatingButton(
                                onClick = { showAccountDialog = true },
                                backdrop = sceneBackdrop,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.CenterStart)
                            ) {
                                val photoUrl = currentUser.photoURL
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Профиль",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val initials = currentUser.avatarInitials
                                    if (initials.isNotEmpty() && initials != "👤") {
                                        Text(
                                            text = initials,
                                            color = SlooshAccent,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        SfIcon(
                                            systemName = "person.fill",
                                            contentDescription = "Профиль",
                                            tint = LocalContentColor.current,
                                            size = 22.dp
                                        )
                                    }
                                }
                            }
                        } else {
                            LiquidGlassFloatingButton(
                                onClick = { showAuthSheet = true },
                                backdrop = sceneBackdrop,
                                shape = Capsule(),
                                modifier = Modifier
                                    .height(44.dp)
                                    .align(Alignment.CenterStart)
                            ) {
                                Text(
                                    text = "Войти",
                                    color = SlooshAccent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }
                        }

                        // Center Title (iOS 1:1: font 20 medium)
                        val headerTitle = if (isAuthenticated) {
                            if (!currentUser.tag.isNullOrBlank()) {
                                currentUser.displayTag
                            } else {
                                currentUser.displayTitle
                            }
                        } else {
                            "Профиль"
                        }

                        Text(
                            text = headerTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 80.dp)
                        )

                        // Right Capsule: Download + Settings side-by-side (LiquidGlassDualButton, 44dp height)
                        LiquidGlassDualButton(
                            firstOnClick = { showDownloadsDialog = true },
                            secondOnClick = { onNavigateToSettings() },
                            backdrop = sceneBackdrop,
                            shape = Capsule(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(44.dp),
                            firstContent = {
                                SfIcon(
                                    systemName = "arrow.down.circle.fill",
                                    contentDescription = "Загрузки",
                                    tint = LocalContentColor.current,
                                    size = 24.dp
                                )
                            },
                            secondContent = {
                                SfIcon(
                                    systemName = "gearshape.fill",
                                    contentDescription = "Настройки",
                                    tint = LocalContentColor.current,
                                    size = 24.dp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Typography Category Tabs ("Все", "Фильмы", "Сериалы", "Мульты")
                    ProfileCategoryTextTabs(
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )
                }
            }
        }

        // Account Dialog (Authenticated user profile details & cloud sync)
        if (showAccountDialog && isAuthenticated) {
            ProfileAccountDialog(
                user = currentUser,
                onDismissRequest = { showAccountDialog = false },
                onSignOutClick = {
                    showAccountDialog = false
                    showSignOutAlert = true
                },
                backdrop = sceneBackdrop
            )
        }

        // Sign Out Confirmation Alert
        if (showSignOutAlert) {
            LiquidGlassAlertDialog(
                title = "Выйти из аккаунта?",
                message = "Вы действительно хотите выйти из своего аккаунта?",
                confirmButtonText = "Выйти",
                dismissButtonText = "Отмена",
                isDestructive = true,
                backdrop = sceneBackdrop,
                onConfirm = {
                    showSignOutAlert = false
                    authRepo.signOut()
                },
                onDismissRequest = {
                    showSignOutAlert = false
                }
            )
        }

        // Auth Sheet (Full modal sheet matching iOS AuthView.swift 1:1)
        if (showAuthSheet) {
            AuthSheet(
                onDismissRequest = { showAuthSheet = false },
                backdrop = sceneBackdrop
            )
        }

        // Downloads Dialog (Coming Soon)
        if (showDownloadsDialog) {
            LiquidGlassDialog(
                title = "Загрузки",
                onDismissRequest = { showDownloadsDialog = false },
                backdrop = sceneBackdrop,
                showCloseButton = false
            ) { dismiss ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SfIcon(
                            systemName = "arrow.down.circle.fill",
                            contentDescription = null,
                            tint = Color.White,
                            size = 28.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Скоро будет доступно",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.60f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Сохранение фильмов и сериалов для просмотра без интернета появится в следующем обновлении.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(Color.White)
                            .clickable { dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Понятно",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Long-Press Action Sheet Dialog for Favorite item
        val selectedMedia = longClickedMedia
        if (selectedMedia != null) {
            LiquidGlassDialog(
                title = selectedMedia.displayTitle,
                onDismissRequest = { longClickedMedia = null },
                backdrop = sceneBackdrop,
                showCloseButton = false
            ) { dismiss ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open Details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .clickable {
                                dismiss()
                                onNavigateToDetails(selectedMedia)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SfIcon(
                                systemName = "info.circle.fill",
                                contentDescription = null,
                                tint = TextPrimary,
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Подробнее",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Remove from favorites
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(RatingIosRed.copy(alpha = 0.15f))
                            .clickable {
                                dismiss()
                                favoritesStore.removeFavorite(selectedMedia)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SfIcon(
                                systemName = "trash.fill",
                                contentDescription = null,
                                tint = RatingIosRed,
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Удалить из избранного",
                                color = RatingIosRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cancel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                            .clickable { dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Отмена",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large typography text tabs matching iOS ProfileCategoryTextTabs 1:1.
 */
@Composable
private fun ProfileCategoryTextTabs(
    selectedCategory: FavoriteCategory,
    onSelectCategory: (FavoriteCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val cutoutPadding = rememberCutoutHorizontalPadding()
    val tabSpacing = if (isLandscape) 10.dp else 12.dp
    val titleFontSize = if (isLandscape) 22.sp else 28.sp

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(start = 16.dp + cutoutPadding, end = 16.dp + cutoutPadding),
        horizontalArrangement = Arrangement.spacedBy(tabSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(FavoriteCategory.values()) { category ->
            val isSelected = selectedCategory == category
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val textColor by animateColorAsState(
                targetValue = if (isSelected) TextPrimary.copy(alpha = 0.90f) else TextPrimary.copy(alpha = 0.45f),
                animationSpec = spring(),
                label = "profileCategoryColor"
            )

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1.0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                label = "profileTabScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelectCategory(category) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.title,
                    color = textColor,
                    fontSize = titleFontSize,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.plusBlendMode()
                )
            }
        }
    }
}
