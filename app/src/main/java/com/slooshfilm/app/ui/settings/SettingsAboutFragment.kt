package com.slooshfilm.app.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.slooshfilm.app.R
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.ui.theme.SlooshTheme
import com.slooshfilm.app.utils.AppUpdater
import com.slooshfilm.app.utils.AppUpdateInfo
import com.slooshfilm.app.utils.UpdateConfig
import kotlinx.coroutines.launch

class SettingsAboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val localStorage = LocalStorage(requireContext())
                val currentThemeMode = localStorage.getThemeMode()
                // dynamic color removed
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
                    SettingsAboutScreen(
                        navController = findNavController()
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsAboutScreen(navController: NavController) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    val scope = rememberCoroutineScope()
    var showUpdateUrlDialog by remember { mutableStateOf(false) }
    var updateUrlInput by remember { mutableStateOf(localStorage.getUpdateUrl() ?: "") }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    val pm = context.packageManager
    val pi = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    val versionName = pi.versionName ?: ""
    val versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopBar(title = "О приложении", onBackClick = { navController.popBackStack() })
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_sloosh),
                    contentDescription = "Логотип Sloosh",
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Версия $versionName ($versionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "") {
                SettingsNavigationItem(text = "Telegram канал", icon = painterResource(id = R.drawable.ic_telegram)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/slooshapp"))
                    context.startActivity(intent)
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsNavigationItem(text = "Лицензии", icon = painterResource(id = R.drawable.ic_licenses)) {
                     navController.navigate(R.id.action_settingsAboutFragment_to_settingsLicensesFragment)
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsNavigationItem(text = "Поддержать проект", icon = painterResource(id = R.drawable.ic_favorite)) {
                     val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.donationalerts.com/r/sluvskii"))
                     context.startActivity(intent)
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
                SettingsNavigationItem(text = "Проверить обновление", icon = painterResource(id = R.drawable.ic_info)) {
                    val updateUrl = localStorage.getUpdateUrl()
                    scope.launch {
                        val updater = AppUpdater(context)
                        val owner = UpdateConfig.GITHUB_OWNER
                        val repo = UpdateConfig.GITHUB_REPO
                        val info = updater.checkGithubLatest(owner, repo, versionName)
                        if (info != null) {
                            availableUpdate = info
                        } else {
                            android.widget.Toast.makeText(context, "У вас актуальная версия", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)
            }
        }

        if (showUpdateUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateUrlDialog = false },
                title = { Text("Источник обновлений") },
                text = {
                    Column {
                        Text(text = "Введите URL JSON файла с данными обновления")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = updateUrlInput, onValueChange = { updateUrlInput = it })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        localStorage.setUpdateUrl(updateUrlInput)
                        showUpdateUrlDialog = false
                    }) { Text("Сохранить") }
                },
                dismissButton = {
                    Button(onClick = { showUpdateUrlDialog = false }) { Text("Отмена") }
                }
            )
        }

        availableUpdate?.let { info ->
            AlertDialog(
                onDismissRequest = { availableUpdate = null },
                title = { Text("Доступно обновление") },
                text = {
                    Column {
                        Text(text = "Версия: ${info.versionCode}")
                        if (!info.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = info.description!!)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        availableUpdate = null
                        AppUpdater(context).downloadAndInstall(info.url)
                    }) { Text("Скачать") }
                },
                dismissButton = {
                    Button(onClick = { availableUpdate = null }) { Text("Позже") }
                }
            )
        }
    }
}
