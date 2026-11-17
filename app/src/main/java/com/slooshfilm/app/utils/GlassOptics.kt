package com.slooshfilm.app.utils

import android.content.Context

/**
 * Предохранитель оптики стекла — три уровня деградации.
 *
 * Уровни:
 * - [LEVEL_FULL] — всё включено: размытие + преломление (линзы контролов).
 * - [LEVEL_NO_LENSES] — линзы выключены, размытие работает. **Уровень по умолчанию.**
 * - [LEVEL_NO_OPTICS] — оптика выключена целиком, остаётся материал стекла.
 *
 * Почему по умолчанию именно [LEVEL_NO_LENSES]: каскад `nativeControlLens` (5-7
 * `RuntimeShader` на контрол) падает **нативно** на Adreno 610 (подтверждено на
 * Xiaomi 23043RP34G, Redmi Pad SE, Android 14). Такое падение Java-обработчик не видит,
 * поэтому приложение умирает молча, без отчёта. Линзы — тонкая косметика преломления
 * на переключателе/ползунке/сегментах, а размытие стекла даёт почти весь вид. Меняем
 * косметику на стабильность, сохраняя вид.
 *
 * Самовосстановление: когда пользователь включает преломление, попытка «взводится».
 * Если процесс умер нативно, следующий запуск понижает уровень на шаг и пишет отчёт,
 * который наконец виден в «Настройки → Диагностика → Логи падений».
 */
object GlassOptics {

    private const val PREFS = "sloosh_prefs"
    private const val KEY_LEVEL = "glass_optics_level"
    private const val KEY_TRIAL_ARMED = "glass_optics_trial_armed"
    /** Предохранитель понизил уровень сам — надо один раз объяснить это пользователю. */
    private const val KEY_AUTO_DOWNGRADED = "glass_optics_auto_downgraded"

    const val LEVEL_FULL = 0
    const val LEVEL_NO_LENSES = 1
    const val LEVEL_NO_OPTICS = 2

    /** Текущий уровень деградации. */
    fun level(context: Context): Int {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_LEVEL)) {
            // Миграция: у кого стоял безопасный режим, получаем размытие обратно,
            // но линзы оставляем выключенными — они и падали.
            return LEVEL_NO_LENSES
        }
        return prefs.getInt(KEY_LEVEL, LEVEL_NO_LENSES).coerceIn(LEVEL_FULL, LEVEL_NO_OPTICS)
    }

    /** Линзы преломления на контролах разрешены только на полном уровне. */
    fun lensesEnabled(context: Context): Boolean = level(context) == LEVEL_FULL

    /** Размытие и материалы стекла разрешены, пока оптика не выключена целиком. */
    fun backdropEnabled(context: Context): Boolean = level(context) != LEVEL_NO_OPTICS

    fun setLevel(context: Context, level: Int) {
        val target = level.coerceIn(LEVEL_FULL, LEVEL_NO_OPTICS)
        prefs(context).edit()
            .putInt(KEY_LEVEL, target)
            .putBoolean(KEY_TRIAL_ARMED, target == LEVEL_FULL)
            .apply()
    }

    /** Пользователь включил преломление: взводим попытку, чтобы откатиться при падении. */
    fun armTrial(context: Context) {
        prefs(context).edit().putBoolean(KEY_TRIAL_ARMED, true).apply()
    }

    /** Экран с включённым преломлением отрисовался — попытка удалась, откат не нужен. */
    fun confirmHealthy(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_TRIAL_ARMED, false) &&
            prefs.getInt(KEY_LEVEL, LEVEL_NO_LENSES) == LEVEL_FULL
        ) {
            prefs.edit().putBoolean(KEY_TRIAL_ARMED, false).apply()
        }
    }

    /**
     * Вызывается один раз при старте процесса. Если предыдущая попытка работать на полном
     * уровне не подтвердилась — процесс умер нативно: понижаем уровень и пишем отчёт.
     *
     * @return true, если предохранитель сработал.
     */
    fun onStartup(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_TRIAL_ARMED, false)) return false
        val current = level(context)
        prefs.edit().putBoolean(KEY_TRIAL_ARMED, false).apply()
        if (current != LEVEL_FULL) return false

        // Полный уровень не подтвердился — значит, падают линзы: уходим на уровень без них.
        prefs.edit()
            .putInt(KEY_LEVEL, LEVEL_NO_LENSES)
            .putBoolean(KEY_AUTO_DOWNGRADED, true)
            .apply()
        CrashLogs.writeNativeSuspect(
            context,
            "Приложение завершилось нативно при включённом преломлении стекла " +
                "(каскад RuntimeShader в nativeControlLens). Преломление выключено " +
                "автоматически, размытие стекла продолжает работать."
        )
        return true
    }

    /** Сработал ли предохранитель автоматически (уровень понижен без ведома пользователя). */
    fun wasAutoDowngraded(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_DOWNGRADED, false)

    fun clearAutoDowngrade(context: Context) {
        prefs(context).edit().putBoolean(KEY_AUTO_DOWNGRADED, false).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
