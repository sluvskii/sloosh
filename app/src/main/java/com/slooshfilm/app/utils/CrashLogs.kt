package com.slooshfilm.app.utils

import android.content.Context
import java.io.File

/**
 * Отчёты о падении, которые пишет [com.slooshfilm.app.SlooshApplication][setupGlobalCrashHandler].
 *
 * Файлы `crash_yyyy-MM-dd_HH-mm-ss.txt` лежат в `filesDir`. Экран настроек может сам падать,
 * поэтому отчёт показывается диалогом на старте приложения — оттуда его можно скопировать
 * и прислать разработчику, не подключая телефон к компьютеру.
 */
object CrashLogs {

    data class Report(val fileName: String, val text: String)

    /**
     * Самый свежий отчёт о падении **текущей** сборки.
     *
     * Отчёты прошлых версий удаляются: их номера строк относятся к другому коду, и такой
     * отчёт выглядит как «всё ещё падает», хотя причина уже устранена.
     */
    fun pending(context: Context): Report? {
        val currentVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        val file = newestFile(context) ?: return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        val reportVersion = text.lineSequence()
            .firstOrNull { it.startsWith("Version:") }
            ?.removePrefix("Version:")
            ?.trim()
        if (currentVersion == null || reportVersion != currentVersion) {
            // Отчёт другой сборки (или без версии — совсем старый): он больше не актуален.
            runCatching { file.delete() }
            return pending(context)
        }
        return Report(file.name, text.trim())
    }

    /** Удаляет все файлы отчётов — после отправки они больше не нужны. */
    fun clear(context: Context) {
        crashFiles(context).forEach { runCatching { it.delete() } }
    }

    private fun newestFile(context: Context): File? = crashFiles(context)
        .filter { it.isFile && it.length() > 0 }
        .maxByOrNull { it.lastModified() }

    private fun crashFiles(context: Context): List<File> =
        runCatching {
            (context.filesDir?.listFiles() ?: emptyArray())
                .filter { it.name.startsWith("crash_") && it.name.endsWith(".txt") }
                .toList()
        }.getOrDefault(emptyList())
}
