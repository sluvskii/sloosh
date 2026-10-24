package com.slooshfilm.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.LiquidSlider
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.ui.theme.SlooshAccent
import kotlin.math.roundToInt

@Composable
fun SettingsAppearanceScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }

    var currentThemeMode by remember { mutableIntStateOf(localStorage.getThemeMode()) }
    var gridColumns by remember { mutableIntStateOf(localStorage.getGridColumns()) }
    var isProgressiveBlurEnabled by remember { mutableStateOf(localStorage.getProgressiveBlurEnabled()) }
    var glassBlur by remember { mutableFloatStateOf(localStorage.getLiquidGlassBlur()) }
    var isGlassEnabled by remember { mutableStateOf(localStorage.getGlassEnabled()) }

    fun handleThemeSelection(mode: Int) {
        if (currentThemeMode == mode) return
        localStorage.saveThemeMode(mode)
        currentThemeMode = mode
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun handleGridSelection(columns: Int) {
        if (gridColumns == columns) return
        localStorage.saveGridColumns(columns)
        gridColumns = columns
    }

    SettingsScaffold(
        title = "Внешний вид",
        onBackClick = onBackClick
    ) {
        Spacer(modifier = Modifier.height(16.dp))

            val themeOptions = listOf("Системная", "Тёмная", "Светлая")
            val selectedTheme = when (currentThemeMode) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "Системная"
                AppCompatDelegate.MODE_NIGHT_YES, LocalStorage.THEME_MODE_AMOLED -> "Тёмная"
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
                                "Тёмная" -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> AppCompatDelegate.MODE_NIGHT_NO
                            }
                            handleThemeSelection(mode)
                        }
                    )
                    if (index < themeOptions.size - 1) {
                        SettingsDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "") {
                SettingsSwitchItem(
                    title = "AMOLED тема",
                    subtitle = "Абсолютно чёрный фон для OLED-экранов",
                    checked = currentThemeMode == LocalStorage.THEME_MODE_AMOLED,
                    onCheckedChange = { checked ->
                        val newMode = if (checked) LocalStorage.THEME_MODE_AMOLED else AppCompatDelegate.MODE_NIGHT_YES
                        handleThemeSelection(newMode)
                    }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    title = "Прогрессивный блюр",
                    subtitle = "Физическое размытие хедера (при выключении остаётся градиент)",
                    checked = isProgressiveBlurEnabled,
                    onCheckedChange = { checked ->
                        localStorage.saveProgressiveBlurEnabled(checked)
                        isProgressiveBlurEnabled = checked
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val blurPercentText = "${(glassBlur * 100).roundToInt()}%"
            val glassFooterText = if (isGlassEnabled) {
                "Плавная регулировка от абсолютно прозрачного стекла (0%) до сплошного непрозрачного размытия (100%)."
            } else {
                "В режиме без стекла преломления, аберрации и размытия отключены для максимальной плавности. Все анимации и инерция ползунка сохранены."
            }

            SettingsSection(
                title = "Жидкое стекло",
                footer = glassFooterText
            ) {
                SettingsSwitchItem(
                    title = "Режим без стекла",
                    subtitle = "Отключает эффекты стекла для слабых устройств",
                    checked = !isGlassEnabled,
                    onCheckedChange = { noGlass ->
                        val enabled = !noGlass
                        localStorage.saveGlassEnabled(enabled)
                        isGlassEnabled = enabled
                    }
                )
                SettingsDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Размытие стекла",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = blurPercentText,
                            color = if (glassBlur <= 0.01f) MaterialTheme.colorScheme.onSurfaceVariant else SlooshAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LiquidSlider(
                        value = glassBlur,
                        onValueChange = { newBlur ->
                            glassBlur = newBlur
                            localStorage.saveLiquidGlassBlur(newBlur)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val columnOptions = listOf("Два", "Три")
            val selectedColumn = if (gridColumns == 2) "Два" else "Три"

            SettingsSection(title = "Количество столбцов в списках") {
                columnOptions.forEachIndexed { index, option ->
                    SettingsRadioItem(
                        text = option,
                        selected = selectedColumn == option,
                        onClick = { handleGridSelection(if (option == "Два") 2 else 3) }
                    )
                    if (index < columnOptions.size - 1) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
