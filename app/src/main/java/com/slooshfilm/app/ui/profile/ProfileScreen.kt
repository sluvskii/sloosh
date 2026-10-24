package com.slooshfilm.app.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.kyant.shapes.Capsule
import androidx.compose.ui.res.painterResource
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
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.storage.FavoritesStore
import com.slooshfilm.app.data.storage.LocalStorage
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

    var selectedCategory by remember { mutableStateOf(FavoriteCategory.ALL) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showDownloadsDialog by remember { mutableStateOf(false) }

    val filteredFavorites = remember(favorites, selectedCategory) {
        when (selectedCategory) {
            FavoriteCategory.ALL -> favorites
            FavoriteCategory.MOVIES -> favorites.filter { it.type == "movie" && !it.isCartoon }
            FavoriteCategory.SERIES -> favorites.filter { it.type == "tv" && !it.isCartoon }
            FavoriteCategory.CARTOONS -> favorites.filter { it.isCartoon || it.type == "cartoon" }
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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomPadding = rememberNavBarBottomPadding()
    val topHeaderHeight = statusBarPadding + 106.dp
    val headerBlurHeight = statusBarPadding + 140.dp
    val bottomTotalPadding = navBarBottomPadding + 72.dp

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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_bookmark_fill),
                                contentDescription = null,
                                tint = TextTertiaryDark,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Пока ничего не добавлено",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Сохраняйте фильмы и сериалы в избранное, чтобы быстро возвращаться к ним позже.",
                                fontSize = 14.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (filteredFavorites.isEmpty()) {
                    // Category empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topHeaderHeight, bottom = bottomTotalPadding, start = 32.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_film_fill),
                                contentDescription = null,
                                tint = TextTertiaryDark,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "В этой подборке пока пусто",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Попробуйте открыть другую вкладку или добавьте что-нибудь в избранное.",
                                fontSize = 14.sp,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val cardDensity = LocalCardDensity.current
                    val isCompact = cardDensity == LocalStorage.CARD_DENSITY_COMPACT
                    val gridSpacing = if (isCompact) 6.dp else 8.dp
                    val gridSidePadding = if (isCompact) 10.dp else 12.dp

                    // 3-Column Poster Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            top = topHeaderHeight + 8.dp,
                            bottom = bottomTotalPadding,
                            start = gridSidePadding,
                            end = gridSidePadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFavorites, key = { it.favoriteIdentity }) { media ->
                            MediaCard(
                                media = media,
                                onClick = { onNavigateToDetails(media) },
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
                if (LocalProgressiveBlur.current) {
                    val isLightTheme = !LocalSlooshColors.current.isDark
                    // Progressive blur layer: true physical variable radius blur (decreasing smoothly from 4dp to 0dp)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                    .statusBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Header Bar: Left Avatar | Center Title | Right Download+Settings Capsule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Left Avatar Button (44dp circular button, styled like search button)
                        LiquidGlassFloatingButton(
                            onClick = { showAccountDialog = true },
                            backdrop = sceneBackdrop,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sf_person_fill),
                                contentDescription = "Профиль",
                                tint = LocalContentColor.current,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Center Title (iOS 1:1: font 20 medium)
                        Text(
                            text = "Профиль",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center)
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
                                Icon(
                                    painter = painterResource(R.drawable.ic_sf_arrow_down_circle_fill),
                                    contentDescription = "Загрузки",
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            secondContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sf_gearshape_fill),
                                    contentDescription = "Настройки",
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(24.dp)
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

        // Account Dialog (Coming Soon)
        if (showAccountDialog) {
            LiquidGlassDialog(
                title = "Аккаунт sloosh",
                onDismissRequest = { showAccountDialog = false },
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
                            .background(SlooshAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_person_fill),
                            contentDescription = null,
                            tint = SlooshAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "В разработке",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Понятно",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
                            .background(SlooshAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_arrow_down_circle_fill),
                            contentDescription = null,
                            tint = SlooshAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "В разработке",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(Capsule())
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Понятно",
                            color = TextPrimary,
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
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    fontSize = 28.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.plusBlendMode()
                )
            }
        }
    }
}
