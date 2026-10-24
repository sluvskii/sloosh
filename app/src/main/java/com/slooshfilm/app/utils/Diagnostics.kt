package com.slooshfilm.app.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Сбор текстового диагностического отчёта для «Поделиться логами» (1:1 с iOS AppDiagnostics):
 * шапка с версией/устройством + последние краш-логи из filesDir.
 */
object Diagnostics {

    private const val MAX_CRASH_FILES = 5
    private const val MAX_FILE_CHARS = 100_000

    fun collectReport(context: Context): String {
        val sb = StringBuilder()
        val pm = context.packageManager
        val pkg = context.packageName
        val versionName = runCatching { pm.getPackageInfo(pkg, 0).versionName }.getOrNull() ?: "?"
        val versionCode = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(pkg, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).versionCode.toLong()
            }
        }.getOrDefault(-1L)

        sb.appendLine("sloosh Android $versionName ($versionCode)")
        sb.appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("Сгенерировано: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine()

        val crashFiles = context.filesDir
            .listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        if (crashFiles.isEmpty()) {
            sb.appendLine("Крашей не зафиксировано.")
            return sb.toString()
        }

        sb.appendLine("Краш-логи (${crashFiles.size}):")
        for (file in crashFiles.take(MAX_CRASH_FILES)) {
            sb.appendLine()
            sb.appendLine("──────── ${file.name} ────────")
            sb.appendLine(
                runCatching { file.readText().take(MAX_FILE_CHARS) }
                    .getOrDefault("<не удалось прочитать файл>")
            )
        }
        if (crashFiles.size > MAX_CRASH_FILES) {
            sb.appendLine()
            sb.appendLine("…и ещё ${crashFiles.size - MAX_CRASH_FILES} файлов")
        }
        return sb.toString()
    }

    /** Удалить сохранённые краш-логи (например, после успешной отправки). */
    fun clearCrashLogs(context: Context) {
        context.filesDir
            .listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".txt") }
            ?.forEach { runCatching { it.delete() } }
    }
}
