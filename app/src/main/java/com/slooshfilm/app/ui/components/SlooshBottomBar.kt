package com.slooshfilm.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import com.slooshfilm.app.R
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.emptyBackdrop
import com.slooshfilm.app.ui.navigation.Screen
import com.slooshfilm.app.ui.search.SearchState
import com.slooshfilm.app.ui.search.rememberSearchState
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceDark
import com.slooshfilm.app.ui.theme.LiquidGlassSurfaceLight
import com.slooshfilm.app.ui.theme.RatingIosRed
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import com.slooshfilm.app.ui.theme.TextPrimaryDark
import com.slooshfilm.app.ui.theme.TextSecondaryDark
import com.slooshfilm.app.ui.theme.TextTertiaryDark

/**
 * Flagship dynamic bottom navigation bar matching authentic iOS Apple TV+ Liquid Glass.
 *
 * States:
 * 1. Normal Mode:
 *    - Left: 4-tab capsule with authentic liquid optics & droplet slider (Home, Messenger, Continue, Profile).
 *    - Right: 54dp floating circular glass search button.
 * 2. Search Mode (Resting / Discovery):
 *    - Left: 54dp circular glass button with previous tab icon (clicking it smoothly returns to that tab).
 *    - Right: 54dp liquid glass search bar capsule with Search icon and placeholder.
 * 3. Search Mode (Active typing / keyboard open):
 *    - Left minimized tab collapses away.
 *    - Search bar capsule expands all the way to the left edge and elevates directly above keyboard.
 *    - Right: 54dp floating circular glass Close button with 'X' appears.
 * 4. Search Mode (Results active, keyboard dismissed):
 *    - Left minimized tab visible (54dp).
 *    - Search bar capsule in center with query and cutout clear icon.
 *    - Right 54dp floating circular glass Close button visible.
 */
@Composable
fun SlooshBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    previousTab: Screen = Screen.Home,
    searchState: SearchState = rememberSearchState(),
    unreadMessageCount: Int = 0,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val tabs = remember { Screen.bottomBarTabs }
    val isSearchActive = currentRoute == Screen.Search.route
    val isSearchFocused = searchState.isFocused
    val query = searchState.query
    val showCloseButton = isSearchActive && (isSearchFocused || query.isNotEmpty())

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(
            tabs.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
        )
    }

    LaunchedEffect(currentRoute) {
        val routeIndex = tabs.indexOfFirst { it.route == currentRoute }
        if (routeIndex >= 0 && routeIndex != selectedTabIndex) {
            selectedTabIndex = routeIndex
        }
    }

    val activeTabIndex = remember(currentRoute, previousTab, selectedTabIndex) {
        if (isSearchActive) {
            tabs.indexOfFirst { it == previousTab }.takeIf { it >= 0 } ?: selectedTabIndex
        } else {
            selectedTabIndex
        }
    }

    val activeBackdrop = backdrop ?: emptyBackdrop()
    val isLightTheme = !LocalSlooshColors.current.isDark
    val containerColor =
        if (isLightTheme) LiquidGlassSurfaceLight
        else LiquidGlassSurfaceDark

    val animationScope = rememberCoroutineScope()
    val minTabInteractiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val searchInteractiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val closeInteractiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val handleMinimizedClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        searchState.isFocused = false
        onNavigate(previousTab)
    }

    val navBarBottomPadding = rememberNavBarBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    // Адаптивные метрики бара: в ландшафте и на планшете бар compact (44dp) и ограничен
    // по ширине, в портрете телефона значения совпадают с прежними (54dp, без ограничения).
    val barMetrics = rememberAdaptiveBarMetrics()

    // Smooth keyboard elevation (GPU-accelerated via graphicsLayer translation to prevent remeasurement)
    val animatedImeBottom by animateDpAsState(
        targetValue = imeBottomDp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "imeBottom"
    )

    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(
            stiffness = 240f,
            dampingRatio = 0.74f
        ),
        label = "searchProgress"
    )

    val focusProgress by animateFloatAsState(
        targetValue = if (isSearchActive && isSearchFocused) 1f else 0f,
        animationSpec = spring(
            stiffness = 260f,
            dampingRatio = 0.76f
        ),
        label = "focusProgress"
    )

    val closeButtonProgress by animateFloatAsState(
        targetValue = if (showCloseButton) 1f else 0f,
        animationSpec = spring(
            stiffness = 240f,
            dampingRatio = 0.72f
        ),
        label = "closeButtonProgress"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = barMetrics.horizontalPadding,
                end = barMetrics.horizontalPadding,
                bottom = navBarBottomPadding,
                top = 4.dp
            )
            .graphicsLayer {
                val lift = if (animatedImeBottom > 0.dp) {
                    (animatedImeBottom - navBarBottomPadding + 8.dp).toPx().coerceAtLeast(0f)
                } else {
                    0f
                }
                translationY = -lift
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val normalButtonSize = barMetrics.buttonSize
        val compactButtonSize = 44.dp
        val normalBarHeight = barMetrics.barHeight
        val compactBarHeight = 44.dp

        // В ландшафте и на планшетах капсула табов остаётся компактной (360dp) слева,
        // а кнопка поиска прижата к правому краю. В портрете телефона бар занимает всю ширину.
        val maxNormalLeftWidth = (totalWidth - normalButtonSize - 10.dp).coerceAtLeast(0.dp)
        val normalLeftWidth = barMetrics.compactTabsWidth?.let { minOf(it, maxNormalLeftWidth) } ?: maxNormalLeftWidth
        val compactLeftWidth = compactButtonSize // 44.dp

        // Fluid droplet elasticity during morphing (organic overshoot and surface tension settling)
        val effectiveSearchProgress = if (searchProgress > 1f) {
            1f + (searchProgress - 1f) * 0.35f
        } else if (searchProgress < 0f) {
            searchProgress * 0.75f
        } else {
            searchProgress
        }

        // Left component width morphs: normalLeftWidth -> 44.dp -> 0.dp (when focused)
        val leftRestingWidth = lerp(normalLeftWidth, compactLeftWidth, effectiveSearchProgress)
        val animatedLeftWidth = lerp(leftRestingWidth, 0.dp, focusProgress).coerceAtLeast(0.dp)

        // Spacing between left element and search element during active search
        val activeSpacing = if (animatedLeftWidth > 0.5.dp) {
            lerp(10.dp, 8.dp, searchProgress.coerceIn(0f, 1f))
        } else {
            0.dp
        }

        // Close button width and spacing on the right
        val animatedCloseWidth = lerp(0.dp, compactButtonSize, closeButtonProgress).coerceAtLeast(0.dp)
        val closeSpacing = if (closeButtonProgress > 0.01f) (8.dp * closeButtonProgress).coerceAtLeast(0.dp) else 0.dp

        // Target search element width when fully active
        val targetSearchWidth = (totalWidth - animatedLeftWidth - activeSpacing - animatedCloseWidth - closeSpacing)
            .coerceIn(compactButtonSize, totalWidth)

        // Search element width: morphs smoothly from normalButtonSize to targetSearchWidth
        val animatedSearchWidth = lerp(normalButtonSize, targetSearchWidth, searchProgress.coerceIn(0f, 1f))
            .coerceIn(compactButtonSize, totalWidth)

        // Flexible spacing between left component and search component (in landscape, separates tabs on left from search on right)
        val middleSpacing = if (animatedLeftWidth > 0.5.dp) {
            (totalWidth - animatedLeftWidth - animatedSearchWidth - animatedCloseWidth - closeSpacing).coerceAtLeast(0.dp)
        } else {
            0.dp
        }

        // Heights
        val animatedLeftHeight = lerp(normalBarHeight, compactBarHeight, searchProgress)
        val animatedSearchHeight = lerp(normalBarHeight, compactBarHeight, searchProgress)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // =========================================================================
            // 1. LEFT ELEMENT: Tab Bar (Normal) physically morphing into Minimized Button (Search)
            // =========================================================================
            if (animatedLeftWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .width(animatedLeftWidth)
                        .height(animatedLeftHeight)
                        .then(
                            if (searchProgress >= 0.5f) {
                                Modifier
                                    .graphicsLayer {
                                        val width = size.width
                                        val height = size.height

                                        val progress = minTabInteractiveHighlight.pressProgress
                                        val scale = 1f + (4f.dp.toPx() / height) * progress

                                        val maxOffset = size.minDimension
                                        val initialDerivative = 0.05f
                                        val offset = minTabInteractiveHighlight.offset
                                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                                        val maxDragScale = 4f.dp.toPx() / height
                                        val offsetAngle = atan2(offset.y, offset.x)
                                        scaleX =
                                            scale +
                                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                                (width / height).fastCoerceAtMost(1f)
                                        scaleY =
                                            scale +
                                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                                (height / width).fastCoerceAtMost(1f)

                                        clip = true
                                        shape = SharedCapsule
                                    }
                                    .clip(SharedCapsule)
                                    .then(minTabInteractiveHighlight.modifier)
                                    .then(minTabInteractiveHighlight.gestureModifier)
                            } else {
                                Modifier
                            }
                        )
                        .then(if (searchProgress > 0f) Modifier.clip(SharedCapsule) else Modifier)
                        .clickable(
                            enabled = searchProgress >= 0.5f,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = handleMinimizedClick
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    LiquidBottomTabs(
                        selectedTabIndex = { selectedTabIndex },
                        onTabSelected = { index ->
                            selectedTabIndex = index
                            if (index in tabs.indices) {
                                onNavigate(tabs[index])
                            }
                        },
                        backdrop = activeBackdrop,
                        tabsCount = tabs.size,
                        modifier = Modifier.fillMaxSize(),
                        barHeight = animatedLeftHeight,
                        searchProgress = searchProgress,
                        focusProgress = focusProgress,
                        activeTabIndex = activeTabIndex,
                        normalWidth = normalLeftWidth,
                        accentColor = SlooshAccent
                    ) {
                        val showLabels = LocalTabBarShowsLabels.current
                        tabs.forEachIndexed { index, tab ->
                            LiquidBottomTab(
                                onClick = {
                                    selectedTabIndex = index
                                    onNavigate(tab)
                                }
                            ) {
                                val iconSize = if (showLabels) 22.dp else 26.dp
                                val tabIcon: @Composable () -> Unit = {
                                    tab.iconRes?.let { iconRes ->
                                        if (tab == Screen.Messenger && unreadMessageCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = RatingIosRed,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(
                                                            text = if (unreadMessageCount > 99) "99+" else "$unreadMessageCount",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(iconRes),
                                                    contentDescription = tab.title,
                                                    tint = LocalContentColor.current,
                                                    modifier = Modifier.size(iconSize)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = tab.title,
                                                tint = LocalContentColor.current,
                                                modifier = Modifier.size(iconSize)
                                            )
                                        }
                                    }
                                }

                                if (showLabels && searchProgress < 0.5f) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        tabIcon()
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = LocalContentColor.current,
                                            maxLines = 1,
                                            softWrap = false,
                                            letterSpacing = (-0.2).sp
                                        )
                                    }
                                } else {
                                    tabIcon()
                                }
                            }
                        }
                    }
                }

                if (middleSpacing > 0.dp) {
                    Spacer(modifier = Modifier.width(middleSpacing))
                }
            }

            // =========================================================================
            // 2. SEARCH ELEMENT: 54dp Search Button morphing into Search Bar Capsule
            // =========================================================================
            val searchBarShape = SharedCapsule
            val textAlpha = (searchProgress * 2.2f - 0.2f).coerceIn(0f, 1f)
            val searchIconSize = lerp(26.dp, 20.dp, searchProgress)
            val iconPaddingStart = lerp(13.5.dp, 14.dp, searchProgress)

            Box(
                modifier = Modifier
                    .width(animatedSearchWidth)
                    .height(animatedSearchHeight)
                    .graphicsLayer {
                        val width = size.width
                        val height = size.height

                        val progress = searchInteractiveHighlight.pressProgress
                        val scale = 1f + (4f.dp.toPx() / height) * progress

                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = searchInteractiveHighlight.offset
                        translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        val maxDragScale = 4f.dp.toPx() / height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)

                        clip = true
                        shape = searchBarShape
                    }
                    .clip(searchBarShape)
                    .liquidGlassSurface(
                        style = if (isSearchActive) LiquidGlassTokens.Bar else LiquidGlassTokens.Button,
                        shape = searchBarShape,
                        containerColor = containerColor,
                        backdrop = activeBackdrop
                    )
                    .then(searchInteractiveHighlight.modifier)
                    .then(searchInteractiveHighlight.gestureModifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isSearchActive) {
                            onNavigate(Screen.Search)
                        } else {
                            searchState.isFocused = true
                            searchState.focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedSearchHeight)
                        .padding(start = iconPaddingStart)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_magnifyingglass),
                        contentDescription = "Поиск",
                        tint = lerpColor(if (isLightTheme) Color.Black else Color.White, TextSecondaryDark, searchProgress),
                        modifier = Modifier.size(searchIconSize)
                    )

                    if (textAlpha > 0f) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = textAlpha },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Фильмы и сериалы...",
                                    color = TextTertiaryDark,
                                    fontSize = 14.sp,
                                    fontFamily = SlooshFontFamily,
                                    maxLines = 1
                                )
                            }

                            BasicTextField(
                                value = query,
                                onValueChange = {
                                    searchState.query = it
                                    searchState.doSearch(it, page = 1, isAppend = false)
                                },
                                textStyle = TextStyle(
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontFamily = SlooshFontFamily
                                ),
                                cursorBrush = SolidColor(SlooshAccent),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchState.focusRequester)
                                    .onFocusChanged { focusState ->
                                        searchState.isFocused = focusState.isFocused
                                    }
                            )
                        }

                        if (query.isNotEmpty() && searchProgress > 0.5f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        searchState.clearQuery()
                                    }
                                    .padding(start = 4.dp, end = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CutoutClearIcon(
                                    circleColor = if (isLightTheme) Color.Black else Color.White,
                                    sizeDp = 14.dp,
                                    strokeWidthDp = 1.6.dp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
            }

            // =========================================================================
            // 3. CLOSE BUTTON: Compact 44dp circular glass button emerging on the right
            // =========================================================================
            if (animatedCloseWidth > 0.5.dp) {
                if (closeSpacing > 0.dp) {
                    Spacer(modifier = Modifier.width(closeSpacing))
                }

                val closeShape = SharedCapsule
                Box(
                    modifier = Modifier
                        .width(animatedCloseWidth)
                        .height(compactButtonSize)
                        .clip(closeShape)
                        .liquidGlassInteractive(
                            interactiveHighlight = closeInteractiveHighlight,
                            style = LiquidGlassTokens.Button,
                            shape = closeShape,
                            containerColor = containerColor,
                            backdrop = activeBackdrop
                        )
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                searchState.isFocused = false
                                if (searchState.query.isNotEmpty()) {
                                    searchState.clearQuery()
                                } else {
                                    onNavigate(previousTab)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val closeIconScale = closeButtonProgress.coerceIn(0f, 1f)
                    Icon(
                        painter = painterResource(R.drawable.ic_sf_xmark),
                        contentDescription = "Закрыть",
                        tint = if (isLightTheme) Color.Black else Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = closeIconScale
                                scaleY = closeIconScale
                            }
                    )
                }
            }
        }
    }
}
