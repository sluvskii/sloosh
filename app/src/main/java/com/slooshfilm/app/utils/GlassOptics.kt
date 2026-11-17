package com.slooshfilm.app.utils

import android.content.Context

/**
 * Предохранитель оптики стекла.
 *
 * Контекст проблемы: экран настроек падал **нативно**, без Java-отчёта, — это GPU-путь
 * движка (`RuntimeShader`/`RenderEffect`: линзы контролов, преломление). Такое падение
 * нельзя перехватить `Thread.setDefaultUncaughtExceptionHandler`, поэтому «логи не выходят».
 *
 * Механика:
 * 1. [isDisabled] по умолчанию `true` — оптика выключена, стекло как материал работает.
 * 2. Пользователь может включить преломление обратно ([armTrial]).
 * 3. Если после включения приложение прожило сессию и экран настроек отрисовался
 *    ([confirmHealthy]), попытка считается удачной.
 * 4. Если попытка не подтвердилась (процесс умер) — следующий запуск ([onStartup]) сам
 *    возвращает безопасный режим и пишет отчёт «падение нативно», который наконец видно
 *    в «Настройки → Диагностика → Логи падений».
 */
object GlassOptics {

    private const val PREFS = "sloosh_prefs"
    private const val KEY_DISABLED = "glass_optics_disabled"
    /** Взведённая попытка: оптика включена, но сессия ещё не подтвердила, что она жива. */
    private const val KEY_TRIAL_ARMED = "glass_optics_trial_armed"

    /** true — преломление и линзы выключены (безопасный режим, используется по умолчанию). */
    fun isDisabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISABLED, true)

    fun setDisabled(context: Context, disabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DISABLED, disabled).apply()
        // Включили оптику обратно — взводим попытку: если процесс умрёт, вернёмся сами.
        if (!disabled) armTrial(context) else confirmHealthy(context)
    }

    private fun armTrial(context: Context) {
        prefs(context).edit().putBoolean(KEY_TRIAL_ARMED, true).apply()
    }

    /** Экран с включённой оптикой отрисовался — попытка удалась, откатывать нечего. */
    fun confirmHealthy(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_TRIAL_ARMED, false)) {
            prefs.edit().putBoolean(KEY_TRIAL_ARMED, false).apply()
        }
    }

    /**
     * Вызывается один раз при старте процесса. Если предыдущая попытка с включённой оптикой
     * не подтвердилась — значит, процесс умер нативно: выключаем оптику и пишем отчёт.
     *
     * @return true, если предохранитель сработал (оптика была выключена автоматически).
     */
    fun onStartup(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_TRIAL_ARMED, false)) return false
        if (prefs.getBoolean(KEY_DISABLED, true)) {
            // Оптика уже выключена — подтверждать нечего.
            prefs.edit().putBoolean(KEY_TRIAL_ARMED, false).apply()
            return false
        }
        prefs.edit()
            .putBoolean(KEY_DISABLED, true)
            .putBoolean(KEY_TRIAL_ARMED, false)
            .apply()
        CrashLogs.writeNativeSuspect(
            context,
            "Приложение завершилось нативно при включённом преломлении стекла " +
                "(RuntimeShader/RenderEffect). Преломление выключено автоматически, " +
                "интерфейс продолжает работать без линз."
        )
        return true
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
