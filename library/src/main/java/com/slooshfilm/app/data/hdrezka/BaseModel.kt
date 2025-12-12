package com.slooshfilm.app.data.hdrezka

import androidx.media3.common.util.Util
import org.jsoup.Connection
import org.jsoup.Jsoup

object BaseModel {
    fun getJsoup(link: String?, context: android.content.Context): Connection {
        if (link.isNullOrBlank()) {
            throw IllegalArgumentException("Link cannot be null or empty")
        }
        val connection = Jsoup.connect(link.replace(" ", "").replace("\n", ""))
            .userAgent(Util.getUserAgent(context, "Sloosh-Film-App"))
            .ignoreContentType(true)
            .timeout(30000)

        return connection
    }
}