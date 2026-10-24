package com.slooshfilm.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.components.LiquidGlassAlertDialog
import com.slooshfilm.app.ui.navigation.Screen
import com.slooshfilm.app.ui.theme.BackgroundDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsGeneralScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    var startScreen by remember { mutableStateOf(localStorage.getStartScreen()) }
    var showVotes by remember { mutableStateOf(localStorage.getShowVotes()) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    fun handleStartScreenSelection(screenRoute: String) {
        localStorage.saveStartScreen(screenRoute)
        startScreen = screenRoute
    }

    fun handleShowVotesToggle(show: Boolean) {
        localStorage.setShowVotes(show)
        showVotes = show
    }

    fun clearCoilCache() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Кэш изображений очищен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка очистки кэша: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showClearCacheDialog) {
        LiquidGlassAlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = "Очистка кэша",
            message = "Вы уверены, что хотите удалить все кэшированные постеры и изображения?",
            confirmButtonText = "Очистить",
            dismissButtonText = "Отмена",
            isDestructive = true,
            onConfirm = {
                clearCoilCache()
            }
        )
    }

    SettingsScaffold(
        title = "Общие",
        onBackClick = onBackClick
    ) {
        Spacer(modifier = Modifier.height(16.dp))

            val startScreenOptions = listOf(
                "Главная" to Screen.Home.route,
                "Поиск" to Screen.Search.route,
                "Продолжить" to Screen.ContinueWatching.route,
                "Профиль" to Screen.Profile.route
            )

            SettingsSection(title = "Стартовый экран") {
                startScreenOptions.forEachIndexed { index, (label, route) ->
                    SettingsRadioItem(
                        text = label,
                        selected = startScreen == route,
                        onClick = { handleStartScreenSelection(route) }
                    )
                    if (index < startScreenOptions.size - 1) {
                        SettingsDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Отображение") {
                SettingsSwitchItem(
                    title = "Показывать количество оценок",
                    subtitle = "Отображать число проголосовавших рядом с рейтингом",
                    checked = showVotes,
                    onCheckedChange = { handleShowVotesToggle(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Хранилище") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearCacheDialog = true }
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                ) {
                    Column {
                        Text(
                            text = "Очистить кэш изображений",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Удалить все сохранённые обложки и бэкдропы",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
