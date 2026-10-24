package com.slooshfilm.app.ui.components

import android.provider.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Detects whether the device is currently using Gesture Navigation (full screen gestures / home handle strip)
 * or 3-Button/2-Button Navigation.
 *
 * In Android:
 * - When 3-button navigation is active, the buttons are tappable system UI elements,
 *   so [WindowInsets.tappableElement] bottom is > 0 (typically ~48dp).
 * - When gesture navigation is active, the home pill strip is a gesture swipe zone rather than a tappable element,
 *   so [WindowInsets.tappableElement] bottom is 0dp.
 * - In addition, navigation bar height threshold (gesture <= 24dp vs 3-button >= 40dp)
 *   and system settings ("navigation_mode": 2 = gestural, 0 = 3-button) are checked as fallbacks.
 */
@Composable
fun rememberIsGestureNavigation(): Boolean {
    val context = LocalContext.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tappableBottom = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()

    return remember(navBarBottom, tappableBottom) {
        if (tappableBottom > 0.dp) {
            false
        } else if (navBarBottom > 32.dp) {
            false
        } else if (navBarBottom == 0.dp) {
            true
        } else {
            try {
                val mode = Settings.Secure.getInt(context.contentResolver, "navigation_mode", -1)
                if (mode == 2) true
                else if (mode == 0 || mode == 1) false
                else true
            } catch (e: Exception) {
                true
            }
        }
    }
}

/**
 * Returns the bottom padding for the floating navigation bar.
 * In gesture mode ("полоска для жеста домой"): returns [gestureBottomPadding] (default 24.dp).
 * In 3-button mode ("кнопки"): returns [navBarBottom + buttonSpacingAboveNavBar] (default 8.dp clearance above buttons),
 * avoiding excessive gaps above the button bar.
 */
@Composable
fun rememberNavBarBottomPadding(
    gestureBottomPadding: Dp = 24.dp,
    buttonSpacingAboveNavBar: Dp = 8.dp
): Dp {
    val isGesture = rememberIsGestureNavigation()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (isGesture) {
        gestureBottomPadding
    } else {
        navBarBottom + buttonSpacingAboveNavBar
    }
}

/**
 * Returns the bottom inset that should be added under floating UI elements.
 * For gesture navigation ("полоска для жеста домой"), returns 0.dp (no inset).
 * For 3-button navigation ("кнопки"), returns the height of the button bar (e.g. ~48dp).
 */
@Composable
fun rememberNavBarInset(): Dp {
    val isGesture = rememberIsGestureNavigation()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (isGesture) 0.dp else navBarBottom
}

