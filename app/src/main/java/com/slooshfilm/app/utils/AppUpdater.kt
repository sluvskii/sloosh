package com.slooshfilm.app.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

object UpdateConfig {
    const val GITHUB_OWNER: String = "your_github_owner"
    const val GITHUB_REPO: String = "your_repo"
}

data class AppUpdateInfo(
    val versionCode: Int,
    val url: String,
    val description: String?
)

class AppUpdater(private val context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkUpdate(currentVersionCode: Int, updateUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(updateUrl).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("AppUpdater", "Bad response: ${response.code}")
                return@withContext null
            }
            val jsonStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(jsonStr)
            val remoteVersion = json.getInt("versionCode")
            if (remoteVersion > currentVersionCode) {
                return@withContext AppUpdateInfo(
                    versionCode = remoteVersion,
                    url = json.getString("url"),
                    description = json.optString("description")
                )
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Check update failed", e)
        }
        return@withContext null
    }

    suspend fun checkGithubLatest(owner: String, repo: String, currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("AppUpdater", "GitHub API error: ${response.code}")
                return@withContext null
            }
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val tag = json.optString("tag_name", json.optString("name", ""))
            if (tag.isBlank()) return@withContext null
            val latestVersionName = normalizeTag(tag)
            if (!isRemoteVersionNewer(latestVersionName, currentVersionName)) {
                return@withContext null
            }
            val assets = json.optJSONArray("assets")
            if (assets == null || assets.length() == 0) return@withContext null
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val contentType = a.optString("content_type", "")
                val name = a.optString("name", "")
                val url = a.optString("browser_download_url", "")
                if (contentType.contains("android.package-archive") || name.endsWith(".apk") || name.endsWith(".aab")) {
                    apkUrl = url
                    break
                }
            }
            if (apkUrl.isNullOrBlank()) return@withContext null
            val desc = json.optString("body", null)
            return@withContext AppUpdateInfo(
                versionCode = parseVersionCode(latestVersionName),
                url = apkUrl!!,
                description = desc
            )
        } catch (e: Exception) {
            Log.e("AppUpdater", "GitHub latest check failed", e)
            return@withContext null
        }
    }

    private fun normalizeTag(tag: String): String {
        return tag.lowercase().removePrefix("v").trim()
    }

    private fun parseVersionCode(versionName: String): Int {
        val parts = versionName.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    private fun isRemoteVersionNewer(remote: String, local: String): Boolean {
        fun parse(v: String): List<Int> {
            return v.split('.').map { it.toIntOrNull() ?: 0 }
        }
        val r = parse(remote)
        val l = parse(local)
        val max = maxOf(r.size, l.size)
        for (i in 0 until max) {
            val ri = r.getOrNull(i) ?: 0
            val li = l.getOrNull(i) ?: 0
            if (ri != li) return ri > li
        }
        return false
    }

    fun downloadAndInstall(url: String, fileName: String = "update.apk") {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Update")
            .setDescription("Please wait...")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(destination)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AppUpdater", "Failed to open unknown sources settings", e)
                }
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Install APK intent failed", e)
        }
    }
}
