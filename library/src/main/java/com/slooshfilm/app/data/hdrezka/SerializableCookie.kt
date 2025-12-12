package com.slooshfilm.app.data.hdrezka

import okhttp3.Cookie

// A serializable version of OkHttp's Cookie class
data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
) {
    fun toCookie(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .expiresAt(expiresAt)
            .path(path)

        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }
        if (secure) {
            builder.secure()
        }
        if (httpOnly) {
            builder.httpOnly()
        }
        return builder.build()
    }
}