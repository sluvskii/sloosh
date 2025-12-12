package com.slooshfilm.app.data.hdrezka

import android.webkit.CookieManager
import java.net.HttpCookie

object CookieStorage {
    fun getCookie(siteName: String?, cookieName: String?): String? {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(siteName) ?: return null

        return cookies.split(";").mapNotNull { 
            try {
                HttpCookie.parse(it).firstOrNull()
            } catch (e: IllegalArgumentException) {
                null
            }
        }.firstOrNull { it.name == cookieName }?.value
    }
}