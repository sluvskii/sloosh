package com.slooshfilm.app.utils

import android.content.Context
import java.io.File

/**
 * Отчёты о падении, которые пишет `SlooshApplication.setupGlobalCrashHandler`.
 *
 * Файлы `crash_yyyy-MM-dd_HH-mm-ss.txt` лежат в `filesDir`. Экран настроек может сам падать,
 * поэтому отчёт можно посмотреть и скопировать внутри приложения, без подключения к компьютеру.
 *
 * Правила показа (чтобы окно не было навязчивым):
 * 1. Диалог на запуске показывается **только** для непросмотренного отчёта текущей сборки.
 * 2. Как только отчёт показан — он помечается просмотренным и больше не всплывает,
 *    в том числе после смены системной темы (это пересоздание Activity).
 * 3. Новое падение пишет новый файл — он снова непросмотренный.
 * 4. Отчёты других версий не показываются и удаляются: их номера строк относятся к другому коду.
 * 5. Все логи в любой момент доступны в «Настройки → Диагностика → Логи падений».
 */
object CrashLogs {

    private const val PREFS = "sloosh_prefs"
    /** Ключ «этот отчёт уже показывали»; сбрасывается при записи нового отчёта. */
    const val KEY_SEEN = "crash_report_seen"
    private const val MAX_KEPT = 5

    data class Report(val fileName: String, val text: String)

    /**
     * Непросмотренный отчёт текущей сборки или `null`.
     *
     * Вызывается один раз на старте Activity. Побочных эффектов, кроме удаления отчётов
     * чужих версий, нет: пометка «просмотрено» ставится явно в [markSeen].
     */
    fun pendingUnseen(context: Context): Report? {
        val currentVersion = versionName(context)
        val file = newestFile(context) ?: return null
        val text = runCatching { file.readText() }.getOrNull() ?: return null
        if (currentVersion == null || reportVersion(text) != currentVersion) {
            // Отчёт другой сборки (или совсем старый, без версии): к текущему коду не относится.
            runCatching { file.delete() }
            return pendingUnseen(context)
        }
        if (isSeen(context, file.name)) return null
        return Report(file.name, text.trim())
    }

    /** Помечает отчёт просмотренным: диалог больше не появится ни на одном запуске. */
    fun markSeen(context: Context, report: Report) {
        prefs(context).edit().putString(KEY_SEEN, report.fileName).apply()
    }

    /** Все сохранённые отчёты (свежие сверху) — для экрана «Логи падений». */
    fun all(context: Context): List<Report> = crashFiles(context)
        .sortedByDescending { it.lastModified() }
        .mapNotNull { file ->
            runCatching { file.readText() }.getOrNull()?.let { Report(file.name, it.trim()) }
        }

    /** Самый свежий отчёт без учёта «просмотрено» — для ручного просмотра из настроек. */
    fun newest(context: Context): Report? = all(context).firstOrNull()

    /**
     * Пишет короткий отчёт о запуске: сборка, устройство, версия Android, GPU-рендеринг.
     *
     * Нужен потому, что часть падений происходит **нативно** (в HWUI/шейдерах): Java-обработчик
     * таких исключений не видит, файла о падении не появляется. Такой «отчёт о запуске» рядом
     * с отсутствием файла о падении показывает, что последний запуск не дошёл до Java-краша.
     */
    fun logEnvironment(context: Context) {
        runCatching {
            val versionName = versionName(context) ?: "?"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US)
            val timestamp = sdf.format(java.util.Date())
            val file = File(context.filesDir, "crash_boot_$timestamp.txt")
            val text = buildString {
                appendLine("Version: $versionName")
                appendLine("Thread: main")
                appendLine("Time: $timestamp")
                appendLine()
                appendLine("Это отчёт о запуске, а не о падении.")
                appendLine("Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
                appendLine("HWUI: ${runCatching { android.os.Build.HARDWARE }.getOrNull()}")
                appendLine("Оптика движка на этом экране: выключена (reduceTransparency)")
                appendLine("UI-поток: ${runCatching { android.os.Looper.getMainLooper().thread.name }.getOrNull()}")
            }
            file.writeText(text)
            trim(context)
        }
    }

    /**
     * Отчёт о падении, которое Java-обработчик перехватить не мог (нативный GPU-сбой).
     *
     * Пишется, когда предохранитель оптики откатился: файл создаётся с `Version:` текущей
     * сборки, поэтому диалог на запуске покажет его один раз — как обычный отчёт.
     */
    fun writeNativeSuspect(context: Context, details: String) {
        runCatching {
            val versionName = versionName(context) ?: "?"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US)
            val timestamp = sdf.format(java.util.Date())
            val file = File(context.filesDir, "crash_native_$timestamp.txt")
            val text = buildString {
                appendLine("Version: $versionName")
                appendLine("Thread: main")
                appendLine("Time: $timestamp")
                appendLine()
                appendLine("Причина: нативное падение (Java-исключения нет, отчёт о нём невозможен).")
                appendLine(details)
                appendLine()
                appendLine("Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("ABI: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
            }
            file.writeText(text)
            // Новый отчёт должен быть непросмотренным — иначе диалог его не покажет.
            prefs(context).edit().remove(KEY_SEEN).apply()
            trim(context)
        }
    }

    /** Удаляет все файлы отчётов — после отправки они больше не нужны. */
    fun clear(context: Context) {
        crashFiles(context).forEach { runCatching { it.delete() } }
        prefs(context).edit().remove(KEY_SEEN).apply()
    }

    /** Оставляет не больше [MAX_KEPT] самых свежих файлов, чтобы каталог не разрастался. */
    fun trim(context: Context) {
        crashFiles(context)
            .sortedByDescending { it.lastModified() }
            .drop(MAX_KEPT)
            .forEach { runCatching { it.delete() } }
    }

    private fun isSeen(context: Context, fileName: String): Boolean =
        prefs(context).getString(KEY_SEEN, null) == fileName

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun versionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    private fun reportVersion(text: String): String? = text.lineSequence()
        .firstOrNull { it.startsWith("Version:") }
        ?.removePrefix("Version:")
        ?.trim()

    private fun newestFile(context: Context): File? = crashFiles(context)
        .filter { it.isFile && it.length() > 0 }
        .maxByOrNull { it.lastModified() }

    /**
     * Файлы отчётов. `crash_boot_*` — это отметки о запуске, а не падения: они нужны, чтобы
     * отличить «нативное падение без Java-обработчика» от «падения не было».
     */
    private fun crashFiles(context: Context): List<File> =
        runCatching {
            (context.filesDir?.listFiles() ?: emptyArray())
                .filter {
                    it.name.startsWith("crash_") && it.name.endsWith(".txt") &&
                        !it.name.startsWith("crash_boot_")
                }
                .toList()
        }.getOrDefault(emptyList())

    /** Есть ли отметка о запуске текущей сборки (значит, приложение стартовало). */
    fun hasBootRecord(context: Context): Boolean = runCatching {
        context.filesDir?.listFiles()?.any {
            it.name.startsWith("crash_boot_") && it.name.endsWith(".txt")
        } == true
    }.getOrDefault(false)
}
