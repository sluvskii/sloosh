package com.slooshfilm.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.slooshfilm.app.ui.navigation.SlooshApp
import com.slooshfilm.app.ui.splash.LaunchSplashView
import com.slooshfilm.app.ui.theme.SlooshTheme

import androidx.appcompat.app.AppCompatDelegate
import com.slooshfilm.app.data.storage.LocalStorage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localStorage = LocalStorage(this)
        AppCompatDelegate.setDefaultNightMode(localStorage.getThemeMode())

        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isInitialDark = when (localStorage.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemDark
        }
        enableEdgeToEdge(
            statusBarStyle = if (isInitialDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (isInitialDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = params
        }
        enableHighRefreshRate()

        try {
            val authRepo = com.slooshfilm.app.data.repository.AuthRepository.getInstance(this)
            if (authRepo.isAuthenticated) {
                com.slooshfilm.app.data.repository.CloudSyncService.getInstance(this).syncAllData()
            }
        } catch (_: Exception) {}

        setContent {
            val localStorage = androidx.compose.runtime.remember { LocalStorage(this) }
            var currentThemeMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(localStorage.getThemeMode()) }

            androidx.compose.runtime.DisposableEffect(localStorage) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == LocalStorage.THEME_MODE_KEY) {
                        currentThemeMode = localStorage.getThemeMode()
                    }
                }
                localStorage.prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    localStorage.prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (currentThemeMode) {
                AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> isSystemDark
            }

            SlooshTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SlooshApp()

                    var isSplashActive by rememberSaveable { mutableStateOf(true) }
                    if (isSplashActive) {
                        LaunchSplashView(
                            onDismiss = { isSplashActive = false }
                        )
                    }

                    // Привязка ТВ-устройства: deep link sloosh://pair?code=XXXX-XXXX
                    val pairCodeState = remember { androidx.compose.runtime.mutableStateOf(extractPairingCode(intent)) }
                    pairCodeForwarder = { pairCodeState.value = it }
                    pairCodeState.value?.let { code ->
                        com.slooshfilm.app.ui.pairing.PairConfirmScreen(
                            initialCode = code,
                            onDismiss = { pairCodeState.value = null }
                        )
                    }
                }
            }
        }
    }

    private var pairCodeForwarder: ((String?) -> Unit)? = null

    private fun extractPairingCode(intent: android.content.Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "sloosh" && data.host == "pair") {
            return data.getQueryParameter("code")?.trim()?.takeIf { it.isNotBlank() } ?: ""
        }
        return null
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        extractPairingCode(intent)?.let { pairCodeForwarder?.invoke(it) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enableHighRefreshRate()
    }

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
    }

    private fun enableHighRefreshRate() {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = display?.supportedModes
            val highestMode = modes?.maxByOrNull { it.refreshRate }
            if (highestMode != null && highestMode.refreshRate >= 80f) {
                val params = window.attributes
                params.preferredDisplayModeId = highestMode.modeId
                @Suppress("DEPRECATION")
                params.preferredRefreshRate = highestMode.refreshRate
                window.attributes = params
            }
        } catch (_: Exception) {
        }
    }
}
