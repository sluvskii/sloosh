
package com.slooshfilm.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.R
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val localStorage = LocalStorage(requireContext())
                val currentThemeMode = localStorage.getThemeMode()
                val isSystemDark = isSystemInDarkTheme()

                val useDarkTheme = when (currentThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> isSystemDark
                }

                SlooshTheme(
                    darkTheme = useDarkTheme,
                    isAmoled = currentThemeMode == LocalStorage.THEME_MODE_AMOLED
                ) {
                    SettingsScreen(
                        navController = findNavController(),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    var selectedMirror by remember { mutableStateOf(localStorage.getString("forced_mirror", "auto")) }

    fun handleMirrorSelection(mirror: String) {
        if (mirror == "auto") {
            localStorage.remove("forced_mirror")
        } else {
            localStorage.putString("forced_mirror", mirror)
        }
        selectedMirror = mirror
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
    ) {
        TopBar(title = "Настройки", onBackClick = { navController.popBackStack() })
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Зеркало") {
                SettingsRadioItem(
                    text = "Авто (рекомендуется)",
                    selected = selectedMirror == "auto",
                    onClick = { handleMirrorSelection("auto") }
                )
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsRadioItem(
                    text = "rezka.ag",
                    selected = selectedMirror == "https://rezka.ag",
                    onClick = { handleMirrorSelection("https://rezka.ag") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Расширенные настройки") {
                SettingsNavigationItem(text = "Внешний вид", icon = painterResource(id = R.drawable.ic_style)) {
                    navController.navigate(R.id.action_settingsFragment_to_settingsAppearanceFragment)
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsNavigationItem(text = "Общие", icon = painterResource(id = R.drawable.solar_settings_linear)) {
                    navController.navigate(R.id.action_settingsFragment_to_settingsGeneralFragment)
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsNavigationItem(text = "Плеер", icon = painterResource(id = R.drawable.ic_play_arrow)) {
                    navController.navigate(R.id.action_settingsFragment_to_settingsPlayerFragment)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "", shape = RoundedCornerShape(50.dp)) {
                SettingsNavigationItem(text = "О приложении", icon = painterResource(id = R.drawable.ic_info)) {
                    navController.navigate(R.id.action_settingsFragment_to_settingsAboutFragment)
                }
            }
        }
    }
}
