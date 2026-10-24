package com.slooshfilm.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class LocalStorage(context: Context) {
    internal val prefs: SharedPreferences = context.getSharedPreferences("sloosh_prefs", Context.MODE_PRIVATE)

    companion object {
        const val GRID_COLUMNS_KEY = "grid_columns"
        const val THEME_MODE_KEY = "theme_mode"
        const val THEME_MODE_AMOLED = 999
        const val START_SCREEN_KEY = "start_screen"
        const val AUTO_NEXT_KEY = "auto_next_enabled"
        const val SEEK_FORWARD_KEY = "seek_forward_interval"
        const val SEEK_BACKWARD_KEY = "seek_backward_interval"
        const val DEFAULT_QUALITY_KEY = "default_quality"
        const val CARD_STYLE_KEY = "card_style"
        const val CARD_STYLE_CLASSIC = "classic"
        const val CARD_STYLE_OVERLAY = "overlay"

        const val CARD_DENSITY_KEY = "card_density"
        const val CARD_DENSITY_REGULAR = "regular"
        const val CARD_DENSITY_COMPACT = "compact"

        const val SHOW_VOTES_KEY = "show_votes"
        const val UPDATE_URL_KEY = "update_url"
        const val PROGRESSIVE_BLUR_KEY = "progressive_blur_enabled"
        const val LIQUID_GLASS_BLUR_KEY = "liquid_glass_blur_opacity"
        const val GLASS_ENABLED_KEY = "glass_enabled"

        /** Последняя выбранная озвучка (глобально) — как iOS `alloha_last_translation_name`. */
        const val LAST_VOICEOVER_KEY = "alloha_last_translation_name"
    }

    fun saveGlassEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(GLASS_ENABLED_KEY, enabled).apply()
    }

    fun getGlassEnabled(): Boolean {
        return prefs.getBoolean(GLASS_ENABLED_KEY, true)
    }

    fun saveLiquidGlassBlur(opacity: Float) {
        prefs.edit().putFloat(LIQUID_GLASS_BLUR_KEY, opacity).apply()
    }

    fun getLiquidGlassBlur(): Float {
        return prefs.getFloat(LIQUID_GLASS_BLUR_KEY, 0.70f)
    }

    fun saveProgressiveBlurEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PROGRESSIVE_BLUR_KEY, enabled).apply()
    }

    fun getProgressiveBlurEnabled(): Boolean {
        return prefs.getBoolean(PROGRESSIVE_BLUR_KEY, true)
    }

    fun saveCardStyle(style: String) {
        prefs.edit().putString(CARD_STYLE_KEY, style).apply()
    }

    fun getCardStyle(): String {
        return prefs.getString(CARD_STYLE_KEY, CARD_STYLE_CLASSIC) ?: CARD_STYLE_CLASSIC
    }

    fun saveCardDensity(density: String) {
        prefs.edit().putString(CARD_DENSITY_KEY, density).apply()
    }

    fun getCardDensity(): String {
        return prefs.getString(CARD_DENSITY_KEY, CARD_DENSITY_REGULAR) ?: CARD_DENSITY_REGULAR
    }

    fun saveGridColumns(columns: Int) {
        prefs.edit().putInt(GRID_COLUMNS_KEY, columns).apply()
    }

    fun getGridColumns(): Int {
        return prefs.getInt(GRID_COLUMNS_KEY, 2)
    }

    fun saveThemeMode(mode: Int) {
        prefs.edit().putInt(THEME_MODE_KEY, mode).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun saveStartScreen(screen: String) {
        prefs.edit().putString(START_SCREEN_KEY, screen).apply()
    }

    fun getStartScreen(): String {
        return prefs.getString(START_SCREEN_KEY, "home") ?: "home"
    }

    fun saveAutoNextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_NEXT_KEY, enabled).apply()
    }

    fun getAutoNextEnabled(): Boolean {
        return prefs.getBoolean(AUTO_NEXT_KEY, true)
    }

    fun saveSeekForwardInterval(seconds: Int) {
        prefs.edit().putInt(SEEK_FORWARD_KEY, seconds).apply()
    }

    fun getSeekForwardInterval(): Int {
        return prefs.getInt(SEEK_FORWARD_KEY, 10)
    }

    fun saveSeekBackwardInterval(seconds: Int) {
        prefs.edit().putInt(SEEK_BACKWARD_KEY, seconds).apply()
    }

    fun getSeekBackwardInterval(): Int {
        return prefs.getInt(SEEK_BACKWARD_KEY, 10)
    }

    fun saveDefaultQuality(quality: String) {
        prefs.edit().putString(DEFAULT_QUALITY_KEY, quality).apply()
    }

    fun getDefaultQuality(): String {
        return prefs.getString(DEFAULT_QUALITY_KEY, "1080p") ?: "1080p"
    }

    /**
     * Запоминает озвучку для конкретного тайтла и глобально (1:1 с iOS
     * `PlaybackProgressStore.saveLastVoiceover`). Ключ тайтла — mediaId без сезона/серии.
     */
    fun saveLastVoice(mediaId: String, voice: String) {
        if (voice.isBlank()) return
        prefs.edit()
            .putString("alloha.lastVoiceover.$mediaId", voice)
            .putString(LAST_VOICEOVER_KEY, voice)
            .apply()
    }

    fun getLastVoice(mediaId: String): String? {
        return prefs.getString("alloha.lastVoiceover.$mediaId", null)?.takeIf { it.isNotBlank() }
    }

    fun getGlobalLastVoice(): String? {
        return prefs.getString(LAST_VOICEOVER_KEY, null)?.takeIf { it.isNotBlank() }
    }

    fun setShowVotes(show: Boolean) {
        prefs.edit().putBoolean(SHOW_VOTES_KEY, show).apply()
    }

    fun getShowVotes(): Boolean {
        return prefs.getBoolean(SHOW_VOTES_KEY, true)
    }

    fun setUpdateUrl(url: String) {
        prefs.edit().putString(UPDATE_URL_KEY, url).apply()
    }

    fun getUpdateUrl(): String? {
        return prefs.getString(UPDATE_URL_KEY, null)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String = ""): String {
        return prefs.getString(key, defValue) ?: defValue
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun saveTabBarShowsLabels(enabled: Boolean) {
        prefs.edit().putBoolean("tab_bar_shows_labels", enabled).apply()
    }

    fun getTabBarShowsLabels(): Boolean {
        return prefs.getBoolean("tab_bar_shows_labels", false)
    }

    fun saveTabBarMinimizeOnScroll(enabled: Boolean) {
        prefs.edit().putBoolean("tab_bar_minimize_on_scroll", enabled).apply()
    }

    fun getTabBarMinimizeOnScroll(): Boolean {
        return prefs.getBoolean("tab_bar_minimize_on_scroll", true)
    }

    fun saveShowOriginalTitle(enabled: Boolean) {
        prefs.edit().putBoolean("show_original_title", enabled).apply()
    }

    fun getShowOriginalTitle(): Boolean {
        return prefs.getBoolean("show_original_title", true)
    }

    fun saveAutoAdvanceBackdrops(enabled: Boolean) {
        prefs.edit().putBoolean("auto_advance_backdrops", enabled).apply()
    }

    fun getAutoAdvanceBackdrops(): Boolean {
        return prefs.getBoolean("auto_advance_backdrops", false)
    }

    fun savePlayerVolumeBrightnessGestures(enabled: Boolean) {
        prefs.edit().putBoolean("player_volume_brightness_gestures", enabled).apply()
    }

    fun getPlayerVolumeBrightnessGestures(): Boolean {
        return prefs.getBoolean("player_volume_brightness_gestures", true)
    }
}
