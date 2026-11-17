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

    /** Самый свежий отчёт о падении — показываем всегда, чтобы его точно можно было прислать. */
    fun pending(context: Context): Report? {
        val file = newestFile(context) ?: return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
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
