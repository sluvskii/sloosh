package com.slooshfilm.app.data.hdrezka

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.slooshfilm.app.data.LocalStorage

object UserData {
    private const val USER_ID = "user_id"
    private const val USER_NAME = "user_name"
    private const val USER_HASH = "user_hash"
    private const val SESSION_ID = "session_id"
    private const val IS_LOGGED_IN = "is_logged_in"
    private const val USER_AVATAR = "user_avatar"

    var isLoggedIn: Boolean? = null
    var avatarLink: String? = null
    var userName: String? = null

    fun init(context: Context) {
        val localStorage = LocalStorage(context)
        isLoggedIn = localStorage.getBoolean(IS_LOGGED_IN, false)
        avatarLink = localStorage.getString(USER_AVATAR, null)
        userName = localStorage.getString(USER_NAME, null)

        if (isLoggedIn == true) {
            try {
                val userId = CookieStorage.getCookie(getProvider(localStorage), "dle_user_id")
                if (userId.isNullOrEmpty()) {
                    loadCookies(context)
                }
            } catch (e: Exception) {
                loadCookies(context)
            }
        }
    }

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        isLoggedIn = loggedIn
        LocalStorage(context).putBoolean(IS_LOGGED_IN, loggedIn)
    }

    fun setAvatar(avatarLink: String?, context: Context) {
        if (avatarLink != null) {
            this.avatarLink = avatarLink
            LocalStorage(context).putString(USER_AVATAR, avatarLink)
        }
    }

    fun setCookies(user_id: String?, user_name: String?, password: String?, session: String?, context: Context, isSave: Boolean) {
        val cm = CookieManager.getInstance()
        val provider = getProvider(LocalStorage(context))

        user_id?.let { cm.setCookie(provider, "dle_user_id=$it") }
        password?.let { cm.setCookie(provider, "dle_password=$it") }
        session?.let { cm.setCookie(provider, "PHPSESSID=$it") }
        cm.acceptCookie()

        if (isSave) {
            saveCookies(user_id, user_name, password, session, context)
        }
    }

    private fun saveCookies(user_id: String?, user_name: String?, password: String?, session: String?, context: Context) {
        val localStorage = LocalStorage(context)
        user_id?.let { localStorage.putString(USER_ID, it) }
        user_name?.let { localStorage.putString(USER_NAME, it) }
        password?.let { localStorage.putString(USER_HASH, it) }
        session?.let { localStorage.putString(SESSION_ID, it) }
        userName = user_name
    }

    private fun loadCookies(context: Context) {
        val localStorage = LocalStorage(context)
        val id = localStorage.getString(USER_ID, null)
        val name = localStorage.getString(USER_NAME, null)
        val hash = localStorage.getString(USER_HASH, null)
        val session = localStorage.getString(SESSION_ID, null)

        userName = name

        setCookies(id, name, hash, session, context, false)
    }

    fun reset(context: Context) {
        isLoggedIn = false
        val cm = CookieManager.getInstance()
        cm.setCookie(getProvider(LocalStorage(context)), null)
        cm.removeAllCookies(null)
        cm.flush()
        WebStorage.getInstance().deleteAllData()
        val localStorage = LocalStorage(context)
        localStorage.remove(IS_LOGGED_IN)
        localStorage.remove(USER_AVATAR)
        localStorage.remove(USER_ID)
        localStorage.remove(USER_NAME)
        localStorage.remove(USER_HASH)
        localStorage.remove(SESSION_ID)

        avatarLink = null
        userName = null
    }

    private fun getProvider(localStorage: LocalStorage): String {
        return if (localStorage.isOfficialMode()) {
            "https://rezka.fi"
        } else {
            // Вернуть другой URL, если не официальный режим
            "https://hdrezka.ag"
        }
    }
}

// Добавьте эти методы в ваш класс LocalStorage
fun LocalStorage.putBoolean(key: String, value: Boolean) {
    prefs.edit().putBoolean(key, value).apply()
}

fun LocalStorage.getBoolean(key: String, defaultValue: Boolean): Boolean {
    return prefs.getBoolean(key, defaultValue)
}

fun LocalStorage.putString(key: String, value: String) {
    prefs.edit().putString(key, value).apply()
}

fun LocalStorage.getString(key: String, defaultValue: String?): String? {
    return prefs.getString(key, defaultValue)
}

fun LocalStorage.remove(key: String) {
    prefs.edit().remove(key).apply()
}
