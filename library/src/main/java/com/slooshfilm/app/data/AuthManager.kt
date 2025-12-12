package com.slooshfilm.app.data

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
