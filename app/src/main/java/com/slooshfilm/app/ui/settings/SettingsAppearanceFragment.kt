package com.slooshfilm.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme
import androidx.compose.material3.Switch

class SettingsAppearanceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val localStorage = LocalStorage(requireContext())
                val currentThemeMode = localStorage.getThemeMode()
                // dynamic color setting removed: keep theme consistent across app
                // val useDynamicColor = localStorage.isDynamicColorEnabled()
                val isSystemDark = isSystemInDarkTheme()

                val useDarkTheme = when (currentThemeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> isSystemDark
                }

                SlooshTheme(
                    darkTheme = useDarkTheme,
                    // dynamicColor removed
                    isAmoled = currentThemeMode == LocalStorage.THEME_MODE_AMOLED
                ) {
                    SettingsAppearanceScreen(
                        localStorage = localStorage,
                        onBackClick = { parentFragmentManager.popBackStack() },
                        recreateActivity = { requireActivity().recreate() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsAppearanceScreen(
    localStorage: LocalStorage,
    onBackClick: () -> Unit,
    recreateActivity: () -> Unit
) {
    var currentThemeMode by remember { mutableStateOf(localStorage.getThemeMode()) }
    var gridColumns by remember { mutableStateOf(localStorage.getGridColumns()) }

    fun handleThemeSelection(mode: Int) {
        if (currentThemeMode == mode) return
        localStorage.saveThemeMode(mode)
        recreateActivity() // Immediate recreate is the only correct way
    }

    fun handleGridSelection(columns: Int) {
        if (gridColumns == columns) return
        localStorage.saveGridColumns(columns)
        gridColumns = columns // Update state locally, NO recreate
    }

    // dynamic color setting removed

    // Simple, stable UI without backdrop or LiquidToggle
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopBar("Внешний вид", onBackClick)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            val themeOptions = listOf("Системная", "Темная", "Светлая")
            val selectedTheme = when (currentThemeMode) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "Системная"
                AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> "Темная"
                else -> "Светлая"
            }

            SettingsSection(title = "Тема оформления") {
                themeOptions.forEachIndexed { index, option ->
                    SettingsRadioItem(
                        text = option,
                        selected = selectedTheme == option,
                        onClick = {
                            val mode = when (option) {
                                "Системная" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                "Темная" -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> AppCompatDelegate.MODE_NIGHT_NO
                            }
                            handleThemeSelection(mode)
                        }
                    )
                    if (index < themeOptions.size - 1) {
                        Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "", shape = RoundedCornerShape(50.dp)) {
                SettingsSwitchItem(
                    title = "Amoled тема",
                    subtitle = "",
                    checked = currentThemeMode == LocalStorage.THEME_MODE_AMOLED,
                    onCheckedChange = { checked ->
                        val newMode = if (checked) LocalStorage.THEME_MODE_AMOLED else AppCompatDelegate.MODE_NIGHT_YES
                        handleThemeSelection(newMode)
                    }
                )
            }
            Text(
                text = "Для OLED-экранов",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val columnOptions = listOf("Два", "Три")
            val selectedColumn = if (gridColumns == 2) "Два" else "Три"

            SettingsSection(title = "Кол-во столбцов в списках") {
                columnOptions.forEachIndexed { index, option ->
                    SettingsRadioItem(
                        text = option,
                        selected = selectedColumn == option,
                        onClick = { handleGridSelection(if (option == "Два") 2 else 3) }
                    )
                    if (index < columnOptions.size - 1) {
                        Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}