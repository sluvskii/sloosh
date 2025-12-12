package com.slooshfilm.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.slooshfilm.app.data.hdrezka.SerializableCookie
import com.slooshfilm.app.data.model.Movie
import okhttp3.Cookie

class LocalStorage(context: Context) {
    internal val prefs: SharedPreferences = context.getSharedPreferences("sloosh_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAllCookies(cookies: List<Cookie>) {
        val serializableCookies = cookies.map { SerializableCookie(
            name = it.name,
            value = it.value,
            expiresAt = it.expiresAt,
            domain = it.domain,
            path = it.path,
            secure = it.secure,
            httpOnly = it.httpOnly,
            hostOnly = it.hostOnly
        ) }
        val json = gson.toJson(serializableCookies)
        prefs.edit().putString(ALL_COOKIES_KEY, json).apply()
    }

    fun getAllCookies(): List<Cookie> {
        val json = prefs.getString(ALL_COOKIES_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<SerializableCookie>>() {}.type
            val serializableCookies: List<SerializableCookie> = gson.fromJson(json, type)
            serializableCookies.map { it.toCookie() }
        } else {
            emptyList()
        }
    }

    fun clearAllCookies() {
        prefs.edit().remove(ALL_COOKIES_KEY).apply()
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
        return prefs.getInt(THEME_MODE_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DYNAMIC_COLOR_KEY, enabled).apply()
    }

    fun isDynamicColorEnabled(): Boolean {
        return prefs.getBoolean(DYNAMIC_COLOR_KEY, false)
    }

    fun setOfficialMode(enabled: Boolean) {
        prefs.edit().putBoolean(OFFICIAL_MODE_KEY, enabled).apply()
    }

    fun isOfficialMode(): Boolean {
        return prefs.getBoolean(OFFICIAL_MODE_KEY, true)
    }

    fun saveStartScreen(screenId: Int) {
        prefs.edit().putInt(START_SCREEN_KEY, screenId).apply()
    }

    fun getStartScreen(): Int {
        return prefs.getInt(START_SCREEN_KEY, Ids.NAVIGATION_HOME)
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

    fun getSearchHistory(): List<String> {
        val json = prefs.getString(SEARCH_HISTORY_KEY, "[]")
        val type = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveSearchQuery(query: String) {
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        val json = gson.toJson(history.take(10))
        prefs.edit().putString(SEARCH_HISTORY_KEY, json).apply()
    }

    fun removeSearchQuery(query: String) {
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        val json = gson.toJson(history)
        prefs.edit().putString(SEARCH_HISTORY_KEY, json).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(SEARCH_HISTORY_KEY).apply()
    }

    fun setShowVotes(show: Boolean) {
        prefs.edit().putBoolean(SHOW_VOTES_KEY, show).apply()
    }

    fun getShowVotes(): Boolean {
        return prefs.getBoolean(SHOW_VOTES_KEY, false)
    }

    fun isLoggedIn(): Boolean {
        return getAllCookies().any { it.name == "dle_user_id" }
    }

    // Generic helpers used across the codebase
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun setUpdateUrl(url: String) {
        prefs.edit().putString(UPDATE_URL_KEY, url).apply()
    }

    fun getUpdateUrl(): String? {
        return prefs.getString(UPDATE_URL_KEY, null)
    }

    companion object {
        const val THEME_MODE_AMOLED = -101
        const val GRID_COLUMNS_KEY = "grid_columns"
        const val THEME_MODE_KEY = "theme_mode"
        const val DYNAMIC_COLOR_KEY = "dynamic_color"
        const val OFFICIAL_MODE_KEY = "official_mode"
        const val SEARCH_HISTORY_KEY = "search_history"
        const val START_SCREEN_KEY = "start_screen"
        const val AUTO_NEXT_KEY = "auto_next"
        const val SEEK_FORWARD_KEY = "seek_forward_interval"
        const val SEEK_BACKWARD_KEY = "seek_backward_interval"
        const val SHOW_VOTES_KEY = "show_votes"
        const val ALL_COOKIES_KEY = "all_cookies"
        const val LAST_WORKING_MIRROR_KEY = "last_working_mirror"
        const val LAST_WORKING_MIRROR_TS_KEY = "last_working_mirror_ts"
        const val UPDATE_URL_KEY = "update_url"
    }
}
