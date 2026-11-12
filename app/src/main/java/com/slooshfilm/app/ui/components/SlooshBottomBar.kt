package com.slooshfilm.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.emptyBackdrop
import com.slooshfilm.app.ui.navigation.Screen
import com.slooshfilm.app.ui.search.SearchState
import com.slooshfilm.app.ui.search.rememberSearchState
import com.slooshfilm.app.ui.sficons.SfIcon
import com.slooshfilm.app.ui.sficons.SfSymbol
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.SlooshFontFamily
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.glass.GlassSurfaceStyle
import io.github.neoruaa.glasmio.ui.CompactBottomControlSize
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBarWithTrailingItem
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import io.github.neoruaa.glasmio.ui.NavigationBarSurfaceHeight

/**
 * Аутентичная панель навигации sloosh на базе движка Glasmio (iOS 27 Liquid Glass 1:1).
 *
 * 1. Обычный режим:
 *    - GlasmioNavigationBarWithTrailingItem с контактным жидким стеклом (BottomNavigationContact)
 *    - 4 таба (Главная, Чаты, Продолжить, Профиль) на единой стеклянной подложке
 *    - Активный ползунок-капля (nativeControlLens) с физикой DampedDragAnimation и раздуванием на касание (platterPuff)
 *    - Вспомогательная круглая кнопка Поиск на базе FloatingNavigationControl
 *    - Контактный мениск и объединенная тень между плашкой и поиском
 * 2. Режим поиска:
 *    - Слева: компактная круглая стеклянная кнопка возврата к предыдущему табу (GlasmioIconButton)
 *    - В центре: поле поиска на жидком стекле (GlassSurface Navigation) с автоподъемом над клавиатурой
 *    - Справа: кнопка закрытия поиска (GlasmioIconButton)
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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentTab = remember(currentRoute, previousTab) {
        if (isSearchActive) {
            previousTab
        } else {
            tabs.firstOrNull { it.route == currentRoute } ?: previousTab
        }
    }

    val activeBackdrop = backdrop ?: emptyBackdrop()
    val glasmioBackdrop = rememberGlasmioBackdrop(activeBackdrop)
    val isLightTheme = !LocalSlooshColors.current.isDark

    val navBarBottomPadding = rememberNavBarBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val barMetrics = rememberAdaptiveBarMetrics()

    // Плавный подъем бара над клавиатурой
    val animatedImeBottom by animateDpAsState(
        targetValue = imeBottomDp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "imeBottom"
    )

    val glasmioTabItems = remember(currentTab, unreadMessageCount) {
        listOf(
            GlasmioTabItem(
                key = Screen.Home,
                label = "Главная",
                symbol = if (currentTab == Screen.Home) SfSymbol.HouseFilled else SfSymbol.House,
            ),
            GlasmioTabItem(
                key = Screen.Messenger,
                label = "Чаты",
                symbol = if (currentTab == Screen.Messenger) SfSymbol.BubbleLeftAndBubbleRightFilled else SfSymbol.BubbleLeftAndBubbleRight,
                badgeCount = unreadMessageCount,
            ),
            GlasmioTabItem(
                key = Screen.ContinueWatching,
                label = "Продолжить",
                symbol = if (currentTab == Screen.ContinueWatching) SfSymbol.PlayCircleFilled else SfSymbol.PlayCircle,
            ),
            GlasmioTabItem(
                key = Screen.Profile,
                label = "Профиль",
                symbol = if (currentTab == Screen.Profile) SfSymbol.PersonFilled else SfSymbol.Person,
            ),
        )
    }

    val trailingSearchItem = remember {
        GlasmioTabItem(
            key = Screen.Search,
            label = "Поиск",
            symbol = SfSymbol.Search,
        )
    }

    Box(
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
        AnimatedContent(
            targetState = isSearchActive,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = 320f, dampingRatio = 0.82f)) +
                 scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = 320f, dampingRatio = 0.82f)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = 320f, dampingRatio = 0.82f)) +
                        scaleOut(targetScale = 0.96f, animationSpec = spring(stiffness = 320f, dampingRatio = 0.82f))
                    )
            },
            label = "BottomBarMode"
        ) { searchActive ->
            if (!searchActive) {
                // =========================================================================
                // 1. АУТЕНТИЧНЫЙ НАВБАР GLASMIO (1:1 iOS 27 Liquid Glass)
                // =========================================================================
                GlasmioNavigationBarWithTrailingItem(
                    items = glasmioTabItems,
                    trailingItem = trailingSearchItem,
                    selectedKey = currentTab,
                    onSelected = { key ->
                        if (key == Screen.Search) {
                            onNavigate(Screen.Search)
                        } else {
                            onNavigate(key)
                        }
                    },
                    onExpand = {},
                    compactProgress = remember { mutableFloatStateOf(0f) },
                    backdrop = glasmioBackdrop,
                    detachTrailingItem = true,
                    compactSize = CompactBottomControlSize,
                    maxMainWidth = barMetrics.compactTabsWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // =========================================================================
                // 2. РЕЖИМ ПОИСКА (iOS Liquid Glass Search Toolbar)
                // =========================================================================
                val colors = LocalGlassColors.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NavigationBarSurfaceHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка возврата к предыдущему табу (сворачивается при вводе)
                    AnimatedVisibility(
                        visible = !isSearchFocused && query.isEmpty(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        GlasmioIconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                searchState.isFocused = false
                                onNavigate(previousTab)
                            },
                            backdrop = glasmioBackdrop,
                            style = GlassSurfaceStyle.Navigation,
                            modifier = Modifier.size(NavigationBarSurfaceHeight)
                        ) {
                            val iconSf = when (previousTab) {
                                Screen.Home -> SfSymbol.House
                                Screen.Messenger -> SfSymbol.BubbleLeftAndBubbleRight
                                Screen.ContinueWatching -> SfSymbol.PlayCircle
                                Screen.Profile -> SfSymbol.Person
                                else -> SfSymbol.House
                            }
                            SfIcon(
                                symbol = iconSf,
                                contentDescription = previousTab.title,
                                tint = colors.content,
                                size = 24.dp
                            )
                        }
                    }

                    // Капсула поля поиска на жидком стекле
                    GlassSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(NavigationBarSurfaceHeight),
                        backdrop = glasmioBackdrop,
                        shape = Capsule(),
                        style = GlassSurfaceStyle.Navigation
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SfIcon(
                                symbol = SfSymbol.Search,
                                contentDescription = "Поиск",
                                tint = colors.secondaryContent,
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Фильмы и сериалы...",
                                        color = colors.tertiaryContent,
                                        fontSize = 15.sp,
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
                                        color = colors.content,
                                        fontSize = 15.sp,
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
                            if (query.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable(
                                            interactionSource = null,
                                            indication = null
                                        ) {
                                            searchState.clearQuery()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CutoutClearIcon(
                                        circleColor = if (isLightTheme) Color.Black else Color.White,
                                        sizeDp = 14.dp,
                                        strokeWidthDp = 1.6.dp
                                    )
                                }
                            }
                        }
                    }

                    // Кнопка закрытия поиска ('X')
                    AnimatedVisibility(
                        visible = isSearchFocused || query.isNotEmpty(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        GlasmioIconButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                searchState.isFocused = false
                                if (query.isNotEmpty()) {
                                    searchState.clearQuery()
                                } else {
                                    onNavigate(previousTab)
                                }
                            },
                            backdrop = glasmioBackdrop,
                            style = GlassSurfaceStyle.Navigation,
                            modifier = Modifier.size(NavigationBarSurfaceHeight)
                        ) {
                            SfIcon(
                                symbol = SfSymbol.Close,
                                contentDescription = "Закрыть",
                                tint = colors.content,
                                size = 20.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
