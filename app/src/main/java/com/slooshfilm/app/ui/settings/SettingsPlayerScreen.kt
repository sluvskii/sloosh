package com.slooshfilm.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.theme.BackgroundDark

@Composable
fun SettingsPlayerScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }

    var seekForward by remember { mutableIntStateOf(localStorage.getSeekForwardInterval()) }
    var seekBackward by remember { mutableIntStateOf(localStorage.getSeekBackwardInterval()) }
    var autoNext by remember { mutableStateOf(localStorage.getAutoNextEnabled()) }
    var defaultQuality by remember { mutableStateOf(localStorage.getDefaultQuality()) }

    SettingsScaffold(
        title = "Плеер",
        onBackClick = onBackClick
    ) {
        Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Интервалы перемотки") {
                IntervalStepper(
                    label = "Вперёд",
                    value = seekForward,
                    onValueChange = {
                        val newValue = (seekForward + it).coerceIn(5, 60)
                        localStorage.saveSeekForwardInterval(newValue)
                        seekForward = newValue
                    }
                )
                SettingsDivider()
                IntervalStepper(
                    label = "Назад",
                    value = seekBackward,
                    onValueChange = {
                        val newValue = (seekBackward + it).coerceIn(5, 60)
                        localStorage.saveSeekBackwardInterval(newValue)
                        seekBackward = newValue
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Автоматизация") {
                SettingsSwitchItem(
                    title = "Следующая серия",
                    subtitle = "Автоматически включать следующую серию сериала",
                    checked = autoNext,
                    onCheckedChange = {
                        localStorage.saveAutoNextEnabled(it)
                        autoNext = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val qualityOptions = listOf("4K", "1080p Ultra", "1080p", "720p", "480p")
            SettingsSection(title = "Качество по умолчанию") {
                qualityOptions.forEachIndexed { index, quality ->
                    SettingsRadioItem(
                        text = quality,
                        selected = defaultQuality == quality,
                        onClick = {
                            localStorage.saveDefaultQuality(quality)
                            defaultQuality = quality
                        }
                    )
                    if (index < qualityOptions.size - 1) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
