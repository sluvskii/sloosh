package com.slooshfilm.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.slooshfilm.app.R
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsGeneralFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val localStorage = LocalStorage(requireContext())
                val currentThemeMode = localStorage.getThemeMode()
                // dynamic color removed; keep theme consistent
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
                    SettingsGeneralScreen(
                        onBackClick = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGeneralScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    var startScreen by remember { mutableStateOf(localStorage.getStartScreen()) }
    var showVotes by remember { mutableStateOf(localStorage.getShowVotes()) }
    val showClearCacheDialog = remember { mutableStateOf(false) }

    fun handleStartScreenSelection(screenId: Int) {
        localStorage.saveStartScreen(screenId)
        startScreen = screenId
    }

    fun handleShowVotesToggle(show: Boolean) {
        localStorage.setShowVotes(show)
        showVotes = show
    }

    fun clearGlideCache() {
        CoroutineScope(Dispatchers.IO).launch {
            Glide.get(context.applicationContext).clearDiskCache()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Кэш изображений очищен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showClearCacheDialog.value) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Очистка кэша")
            .setMessage("Вы уверены, что хотите удалить все кэшированные изображения? Это действие нельзя отменить.")
            .setNegativeButton("Отмена") { dialog, _ ->
                showClearCacheDialog.value = false
                dialog.dismiss()
            }
            .setPositiveButton("Очистить") { dialog, _ ->
                clearGlideCache()
                showClearCacheDialog.value = false
                dialog.dismiss()
            }
            .setOnDismissListener { showClearCacheDialog.value = false }
            .show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopBar(title = "Общие", onBackClick = onBackClick)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            val startScreenOptions = mapOf(
                "Главная" to R.id.navigation_home,
                "Поиск" to R.id.navigation_search,
                "Досмотреть" to R.id.navigation_library,
                "Профиль" to R.id.navigation_profile
            )

            SettingsSection(title = "Стартовый экран") {
                startScreenOptions.entries.forEachIndexed { index, entry ->
                    SettingsRadioItem(
                        text = entry.key,
                        selected = startScreen == entry.value,
                        onClick = { handleStartScreenSelection(entry.value) }
                    )
                    if (index < startScreenOptions.size - 1) {
                        Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Отображение", shape = RoundedCornerShape(50.dp)) {
                 SettingsSwitchItem(
                    title = "Показывать количество голосов",
                    subtitle = "",
                    checked = showVotes,
                    onCheckedChange = { handleShowVotesToggle(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "Данные", shape = RoundedCornerShape(50.dp)) {
                Column(
                    modifier = Modifier.clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showClearCacheDialog.value = true }
                ) {
                    Text(
                        text = "Очистить кэш изображений",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                         modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp)
                    )
                }
            }
            Text(
                text = "Удалить все загруженные обложки и постеры",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp)
            )
        }
    }
}