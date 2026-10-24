package com.slooshfilm.app.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.slooshfilm.app.R
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.utils.AppUpdateInfo
import com.slooshfilm.app.utils.AppUpdater
import com.slooshfilm.app.utils.UpdateConfig
import kotlinx.coroutines.launch

@Composable
fun SettingsAboutScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val pm = context.packageManager
    val pi = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    val versionName = pi.versionName ?: "2.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode

    SettingsScaffold(
        title = "О приложении",
        onBackClick = { navController.popBackStack() }
    ) {
        Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sloosh_logo),
                    contentDescription = "sloosh",
                    modifier = Modifier
                        .width(200.dp)
                        .height(51.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Версия $versionName ($versionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingsSection(title = "") {
                SettingsNavigationItem(
                    text = "Telegram-канал",
                    icon = painterResource(id = R.drawable.ic_telegram)
                ) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/slooshapp"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show()
                    }
                }

                SettingsDivider()

                SettingsNavigationItem(
                    text = if (isCheckingUpdate) "Проверка..." else "Проверить обновление",
                    icon = painterResource(id = R.drawable.ic_sf_info_circle_fill)
                ) {
                    if (isCheckingUpdate) return@SettingsNavigationItem
                    isCheckingUpdate = true
                    scope.launch {
                        try {
                            val updater = AppUpdater(context)
                            val info = updater.checkGithubLatest("sluvskii", "sloosh", versionName)
                            if (info != null) {
                                availableUpdate = info
                            } else {
                                Toast.makeText(context, "У вас установлена актуальная версия", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка проверки: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                }
            }
        }

        availableUpdate?.let { info ->
            val updateMessage = buildString {
                append("Версия сборки: ${info.versionCode}")
                if (!info.description.isNullOrBlank()) {
                    append("\n\n")
                    append(info.description)
                }
            }
            com.slooshfilm.app.ui.components.LiquidGlassAlertDialog(
                onDismissRequest = { availableUpdate = null },
                title = "Доступно обновление",
                message = updateMessage,
                confirmButtonText = "Скачать",
                dismissButtonText = "Позже",
                confirmButtonColor = com.slooshfilm.app.ui.theme.SlooshAccent,
                onConfirm = {
                    val url = info.url
                    availableUpdate = null
                    AppUpdater(context).downloadAndInstall(url)
                }
            )
        }
    }
