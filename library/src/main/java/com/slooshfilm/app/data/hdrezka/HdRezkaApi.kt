package com.slooshfilm.app.data.hdrezka

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.data.model.ActorDetails
import com.slooshfilm.app.data.model.ActorRole
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.EpisodeStatus
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.models.Bookmark
import com.slooshfilm.app.data.models.BookmarksPage
import com.slooshfilm.app.data.parser.ConfigProvider
import com.slooshfilm.app.data.parser.UniversalParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class Subtitle(
    val name: String,
    val languageCode: String,
    val url: String,
    val isDefault: Boolean = false
) : java.io.Serializable

data class StreamInfo(
    val streams: Map<String, String>,
    val subtitles: List<Subtitle> = emptyList(),
    val storyboardUrl: String? = null
) : java.io.Serializable

data class Notification(
    val date: String,
    val items: List<NotificationItem>
)

data class NotificationItem(
    val name: String,
    val link: String,
    val info: String
)

data class Comment(
    val id: String,
    val avatar: String,
    val author: String,
    val date: String,
    val text: String,
    val likes: Int,
    val indent: Int,
    val isSpoiler: Boolean
) : java.io.Serializable

class HdRezkaApi(
    private val localStorage: LocalStorage,
    private val universalParser: UniversalParser,
    private val configProvider: ConfigProvider
) {

    // How long to trust a last working mirror (ms). 6 hours by default for faster loading.
    private val LAST_MIRROR_TTL_MS = 6 * 60 * 60 * 1000L

    // In-memory cache for the working mirror (refreshed every 6 hours)
    private var cachedWorkingMirror: String? = null
    private var cachedMirrorTime: Long = 0

    val client: OkHttpClient

    private val defaultMirrors = listOf(
        "https://rezka.ag",
        "https://hdrezka.website",
        "https://rezka.fi",
        "https://rezka-ua.org"
    )

    private val officialMirrors = listOf(
        "https://hdrzk.org",
        "https://stepnet.video"
    )

    init {
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val currentCookies = localStorage.getAllCookies().toMutableList()
                currentCookies.removeAll { cookie -> cookies.any { it.name == cookie.name } }
                currentCookies.addAll(cookies)
                localStorage.saveAllCookies(currentCookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return localStorage.getAllCookies().filter { it.expiresAt > System.currentTimeMillis() }
            }
        }

        client = createUnsafeOkHttpClient().newBuilder()
            .cookieJar(cookieJar)
            .addInterceptor(createHeaderInterceptor())
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun getWorkingMirror(): String? = withContext(Dispatchers.IO) {
        if (cachedWorkingMirror != null && System.currentTimeMillis() - cachedMirrorTime < LAST_MIRROR_TTL_MS) {
            Log.d("HdRezkaApi", "Using in-memory cached mirror: $cachedWorkingMirror")
            return@withContext cachedWorkingMirror
        }

        try {
            val forcedMirror = localStorage.getString("forced_mirror", null)
            if (!forcedMirror.isNullOrBlank()) {
                Log.d("HdRezkaApi", "Using forced mirror: $forcedMirror")
                cachedWorkingMirror = forcedMirror
                cachedMirrorTime = System.currentTimeMillis()
                return@withContext forcedMirror
            }
        } catch (_: Exception) {}

        try {
            val last = localStorage.getString(LocalStorage.LAST_WORKING_MIRROR_KEY, null)
            val lastTs = localStorage.getString(LocalStorage.LAST_WORKING_MIRROR_TS_KEY, null)?.toLongOrNull()
            if (!last.isNullOrBlank() && lastTs != null && System.currentTimeMillis() - lastTs < LAST_MIRROR_TTL_MS) {
                try {
                    val req = Request.Builder().url(last).build()
                    client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            Log.d("HdRezkaApi", "Using cached mirror: $last")
                            cachedWorkingMirror = last
                            cachedMirrorTime = System.currentTimeMillis()
                            return@withContext last
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val mirrors = if (localStorage.isOfficialMode()) officialMirrors else defaultMirrors
        val mirrorChecks = mirrors.map { mirror ->
            async {
                try {
                    val req = Request.Builder().url(mirror).build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        res.close()
                        mirror to true
                    } else {
                        res.close()
                        mirror to false
                    }
                } catch (_: Exception) {
                    mirror to false
                }
            }
        }

        val results = awaitAll(*mirrorChecks.toTypedArray())
        val workingMirror = results.firstOrNull { it.second }?.first

        if (workingMirror != null) {
            try {
                localStorage.putString(LocalStorage.LAST_WORKING_MIRROR_KEY, workingMirror)
                localStorage.putString(LocalStorage.LAST_WORKING_MIRROR_TS_KEY, System.currentTimeMillis().toString())
                cachedWorkingMirror = workingMirror
                cachedMirrorTime = System.currentTimeMillis()
            } catch (_: Exception) {}
            return@withContext workingMirror
        }

        Log.w("HdRezkaApi", "No working mirror found")
        return@withContext null
    }

    suspend fun getEpisodes(postId: String, translatorId: String, refererUrl: String? = null): Map<Int, List<Episode>> = withContext(Dispatchers.IO) {
        try {
            val unixTime = System.currentTimeMillis()

            val formBody = FormBody.Builder()
                .add("id", postId)
                .add("translator_id", translatorId)
                .add("action", "get_episodes")
                .build()

            var responseBody: String? = null
            val primaryMirror = try {
                getWorkingMirror()
            } catch (e: Exception) {
                null
            }

            if (primaryMirror != null) {
                try {
                    val refererHeader = refererUrl ?: primaryMirror
                    val req = Request.Builder()
                        .url("${primaryMirror.trimEnd('/')}/ajax/get_cdn_series/?t=$unixTime")
                        .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Accept", "application/json, text/javascript, */*; q=0.01")
                        .apply { if (!refererHeader.isNullOrBlank()) header("Referer", refererHeader) }
                        .post(formBody)
                        .build()

                    client.newCall(req).execute().use { res ->
                        responseBody = res.body?.string()
                    }
                } catch (e: Exception) {
                    Log.w("HdRezkaApi", "Primary mirror $primaryMirror failed for getEpisodes, will try others", e)
                    responseBody = null
                }
            }

            if (responseBody.isNullOrEmpty()) {
                val mirrors = if (localStorage.isOfficialMode()) officialMirrors else defaultMirrors
                for (mirror in mirrors.distinct()) {
                    try {
                        val mirrorBase = mirror.trimEnd('/')
                        val refererHeader = refererUrl ?: mirrorBase
                        val altReq = Request.Builder()
                            .url("$mirrorBase/ajax/get_cdn_series/?t=$unixTime")
                            .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Accept", "application/json, text/javascript, */*; q=0.01")
                            .apply { if (!refererHeader.isNullOrBlank()) header("Referer", refererHeader) }
                            .post(formBody)
                            .build()

                        client.newCall(altReq).execute().use { altRes ->
                            val altBody = altRes.body?.string()
                            if (!altBody.isNullOrEmpty()) {
                                responseBody = altBody
                                break
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }

            if (responseBody.isNullOrEmpty()) {
                return@withContext emptyMap()
            }

            val resp = responseBody ?: ""
            var fullHtml = resp
            val trimmed = resp.trimStart()
            val doc: Document = if (trimmed.startsWith("{")) {
                try {
                    val json = JSONObject(resp)
                    val success = json.optBoolean("success", false)

                    if (!success) {
                        return@withContext emptyMap()
                    }

                    val seasonsHtml = json.optString("seasons", "")
                    val episodesHtml = json.optString("episodes", "")
                    fullHtml = "$seasonsHtml$episodesHtml"
                    Jsoup.parse(fullHtml)
                } catch (e: Exception) {
                    fullHtml = resp
                    Jsoup.parse(resp)
                }
            } else {
                fullHtml = resp
                Jsoup.parse(resp)
            }

            val episodes = mutableMapOf<Int, MutableList<Episode>>()

            val seasons = doc.select(".b-simple_season__item")

            if (seasons.isNotEmpty()) {
                seasons.forEach { season ->
                    val seasonId = season.attr("data-tab_id").toIntOrNull() ?: return@forEach
                    val episodeElements = doc.select("#simple-episodes-list-$seasonId li.b-simple_episode__item")

                    episodeElements.forEach { ep ->
                        val episodeId = ep.attr("data-episode_id").toIntOrNull() ?: return@forEach
                        var fullEpisodeText = ep.text().trim()
                        if (fullEpisodeText.isBlank()) {
                            val textFromChildren = ep.select("span, a, div").asSequence()
                                .map { it.text().trim() }
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                            fullEpisodeText = textFromChildren
                        }
                        val title = fullEpisodeText.takeIf { it.isNotBlank() } ?: "Серия $episodeId"
                        val status = EpisodeStatus.UPCOMING
                        episodes.getOrPut(seasonId) { mutableListOf() }
                               .add(Episode(seasonId, episodeId, title, "", null, status))
                    }
                }
            } else {
                doc.select("li.b-simple_episode__item").forEach { ep ->
                    val seasonNum = ep.attr("data-season_id").toIntOrNull() ?: 1
                    val episodeNum = ep.attr("data-episode_id").toIntOrNull() ?: return@forEach
                    var fullEpisodeText = ep.text().trim()
                    if (fullEpisodeText.isBlank()) {
                        fullEpisodeText = ep.select("span, a, div").asSequence()
                            .map { it.text().trim() }
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    }
                    val title = fullEpisodeText.takeIf { it.isNotBlank() } ?: "Серия $episodeNum"
                    val status = EpisodeStatus.UPCOMING
                    episodes.getOrPut(seasonNum) { mutableListOf() }
                           .add(Episode(seasonNum, episodeNum, title, "", null, status))
                }
            }

            episodes.forEach { (_, epList) ->
                epList.sortBy { it.number }
            }

            return@withContext episodes
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to get episodes for postId: $postId", e)
            return@withContext emptyMap()
        }
    }

    suspend fun getBookmarksPage(): BookmarksPage = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext BookmarksPage(emptyList(), emptyList())
        val html = getHtml("$baseUrl/favorites/")
        val doc = Jsoup.parse(html, baseUrl)

        val categories = doc.select("div.b-favorites_content__cats_list_item").mapNotNull { el ->
            val id = el.attr("data-cat_id")
            val linkElement = el.selectFirst("a.b-favorites_content__cats_list_link") ?: return@mapNotNull null
            val name = linkElement.selectFirst("span.name")?.text() ?: return@mapNotNull null
            val link = linkElement.attr("href")
            Bookmark(id, link, name, 0)
        }

        val films = parseFavoritesList(doc)

        BookmarksPage(categories, films)
    }

    suspend fun getFilmsFromBookmarkPage(url: String): List<Movie> = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext emptyList()
        val finalUrl = if (url.startsWith("http")) url else baseUrl + url
        val html = getHtml(finalUrl)
        val doc = Jsoup.parse(html, baseUrl)
        return@withContext parseFavoritesList(doc)
    }

    suspend fun getPage(pageUrl: String, page: Int): Pair<List<Movie>, Int> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext Pair(emptyList<Movie>(), 1)

            val base = baseUrl.trimEnd('/')
            var s = pageUrl.trim()
            if (!s.startsWith("/")) s = "/$s"
            val cleanPath = s

            fun buildWithQuery(path: String, pageNum: Int): String {
                val idx = path.indexOf('?')
                if (idx == -1) {
                    val p = path.trimEnd('/')
                    return if (pageNum > 1) "$p/page/$pageNum/" else "$p/"
                } else {
                    val pathPart = path.substring(0, idx).trimEnd('/')
                    val queryPart = path.substring(idx)
                    val p = if (pathPart.isEmpty()) "/" else pathPart
                    return if (pageNum > 1) "$p/page/$pageNum/$queryPart" else "$p$queryPart"
                }
            }

            val finalUrl = if (cleanPath.startsWith("http")) {
                buildWithQuery(cleanPath, page)
            } else {
                val combined = (base + cleanPath)
                buildWithQuery(combined, page)
            }

            Log.d("HdRezkaApi", "Loading page: $finalUrl")

            val request = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("HdRezkaApi", "Failed to load page: ${response.code}")
                response.close()
                return@withContext Pair(emptyList<Movie>(), 1)
            }

            val html = response.body?.string()
            response.close()

            if (html.isNullOrEmpty()) {
                Log.e("HdRezkaApi", "Empty response body")
                return@withContext Pair(emptyList<Movie>(), 1)
            }

            val doc = Jsoup.parse(html, baseUrl)

            val contentItems = doc.select("div.b-content__inline_item")
            if (contentItems.isEmpty()) {
                Log.d("HdRezkaApi", "No content items found on page")
                return@withContext Pair(emptyList<Movie>(), 1)
            }

            val pagination = doc.select("div.b-navigation")
            val currentPage = pagination.select("span.current").firstOrNull()?.text()?.toIntOrNull() ?: 1

            var totalPages = 1
            val navLinks = doc.select("div.b-navigation a")
            if (navLinks.size >= 2) {
                val maybe = navLinks[navLinks.size - 2].text()
                totalPages = maybe.toIntOrNull() ?: totalPages
            }

            return@withContext Pair(parseFilmsList(doc), totalPages)

        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Error loading page", e)
            return@withContext Pair(emptyList(), 1)
        }
    }

    suspend fun createBookmarkCategory(name: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val formBody = FormBody.Builder()
            .add("name", name)
            .add("action", "add_cat")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/ajax/favorites/")
            .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
            .post(formBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            json.optBoolean("success")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "createBookmarkCategory failed", e)
            false
        }
    }

    suspend fun renameBookmarkCategory(catId: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val formBody = FormBody.Builder()
            .add("cat_id", catId)
            .add("name", newName)
            .add("action", "edit_cat")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/ajax/favorites/")
            .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
            .post(formBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            json.optBoolean("success")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to rename bookmark category", e)
            false
        }
    }

    suspend fun deleteBookmarkCategory(catId: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val formBody = FormBody.Builder()
            .add("cat_id", catId)
            .add("action", "delete_cat")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/ajax/favorites/")
            .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
            .post(formBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            val json = JSONObject(bodyStr)
            json.optBoolean("success")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to delete bookmark category", e)
            false
        }
    }

    suspend fun getNotifications(): List<Notification> = withContext(Dispatchers.IO) {
        val mirror = getWorkingMirror() ?: return@withContext emptyList()
        val url = mirror.trimEnd('/') + "/"
        
        try {
            val req = Request.Builder()
                .url(url)
                .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
                .build()
                
            client.newCall(req).execute().use { res ->
                val html = res.body?.string() ?: return@withContext emptyList()
                val doc = Jsoup.parse(html)
                val notifications = mutableListOf<Notification>()
                
                doc.select(".b-seriesupdate__block").forEach { el ->
                    val date = el.select(".b-seriesupdate__block_date").text().replace(" развернуть", "").trim()
                    val items = el.select(".tracked").map { item ->
                        val season = item.select(".season").text().trim()
                        val episode = item.select(".cell-2").text().trim()
                        val info = "$season - $episode"
                        val linkEl = item.select(".b-seriesupdate__block_list_link")
                        val name = linkEl.text().trim()
                        val link = linkEl.attr("href")
                        NotificationItem(name, link, info)
                    }
                    notifications.add(Notification(date, items))
                }
                return@withContext notifications
            }
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to get notifications", e)
            return@withContext emptyList()
        }
    }

    @SuppressLint("TrustAllX509TrustManager")
    private fun createUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun createHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val builder = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")

            if (localStorage.isOfficialMode()) {
                builder.header("X-Hdrezka-Android-App", "1")
                builder.header("X-Hdrezka-Android-App-Version", "2.2.1")
            }

            chain.proceed(builder.build())
        }
    }

    suspend fun login(login: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val formBody = FormBody.Builder()
            .add("login_name", login)
            .add("login_password", password)
            .add("login_not_save", "0")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/ajax/login/")
            .post(formBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            val json = JSONObject(responseString)
            json.optBoolean("success")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Login failed", e)
            false
        }
    }

    fun logout() {
        localStorage.clearAllCookies()
    }

    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        return@withContext localStorage.isLoggedIn()
    }

    suspend fun getMovieDetails(pageUrl: String): MovieDetails? = withContext(Dispatchers.IO) {
        try {
            val html = getHtml(pageUrl)
            val config = configProvider.getConfig()
            val movieDetails = universalParser.parseMovieDetails(html, pageUrl, config.movieDetailsSelectors)

            if (movieDetails != null && !movieDetails.trailerDataId.isNullOrBlank()) {
                val trailerUrl = getTrailerUrl(movieDetails.trailerDataId, pageUrl)
                movieDetails.trailerUrl = trailerUrl
            }

            return@withContext movieDetails
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to get movie details", e)
            null
        }
    }

    private suspend fun getTrailerUrl(trailerId: String, refererUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext null
            val formBody = FormBody.Builder()
                .add("id", trailerId)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/engine/ajax/gettrailervideo.php")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", refererUrl)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()
            response.close()

            if (responseString.isNullOrEmpty() || !responseString.startsWith("{")) {
                return@withContext null
            }

            val json = JSONObject(responseString)
            if (json.optBoolean("success")) {
                var videoUrl = json.optString("url", null)
                if (videoUrl.isNullOrEmpty()) {
                    val codeHtml = json.optString("code", null)
                    if (!codeHtml.isNullOrEmpty()) {
                        val srcRegex = """src="([^"]+)"""".toRegex()
                        val matchResult = srcRegex.find(codeHtml)
                        videoUrl = matchResult?.groupValues?.get(1)
                    }
                }
                return@withContext videoUrl
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to get trailer url", e)
            return@withContext null
        }
    }

    suspend fun hasNextPage(pageUrl: String, currentPage: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext false
            val nextPage = currentPage + 1

            val testUrl = when {
                pageUrl.startsWith("http") -> "$pageUrl/page/$nextPage/"
                else -> "$baseUrl$pageUrl/page/$nextPage/"
            }

            val request = Request.Builder()
                .url(testUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val hasNext = response.isSuccessful &&
                         response.body?.string()?.let { html ->
                             val doc = Jsoup.parse(html, baseUrl)
                             doc.select("div.b-content__inline_item").isNotEmpty()
                         } ?: false

            response.close()
            return@withContext hasNext

        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Error checking next page", e)
            return@withContext false
        }
    }

    suspend fun getWatchLater(page: Int = 1): Pair<List<Movie>, Int> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext Pair(emptyList(), 0)
            val url = if (page > 1) "$baseUrl/continue/page/$page/" else "$baseUrl/continue/"
            val html = getHtml(url)
            val doc = Jsoup.parse(html, baseUrl)

            val items = doc.select(".b-videosaves__list_item")
            if (items.isEmpty()) return@withContext Pair(emptyList(), 0)

            val usable = if (items.size > 1) items.drop(1) else emptyList()

            val movies = usable.mapNotNull { el ->
                try {
                    val id = el.selectFirst(".delete")?.attr("data-id") ?: ""
                    val linkEl = el.selectFirst(".title a")
                    val link = linkEl?.absUrl("href") ?: ""
                    val image = linkEl?.attr("data-cover_url") ?: linkEl?.selectFirst("img")?.absUrl("src") ?: ""
                    var name = (el.selectFirst(".title")?.text() ?: "").trim()

                    if (link.isBlank() || name.isBlank()) return@mapNotNull null

                    val isSeries = link.contains("/series/")

                    val year = Regex("(\\d{4})").find(name)?.groupValues?.get(1)
                    if (year != null) {
                        name = name.replace(year, "").replace(",", "").trim()
                    }

                    Movie(
                        url = link.ifEmpty { "" },
                        postId = if (id.isNotBlank()) id else null,
                        title = name,
                        imageUrl = image,
                        year = year,
                        rating = null,
                        isSeries = isSeries
                    )
                } catch (e: Exception) {
                    Log.e("HdRezkaApi", "Failed to parse continue item", e)
                    null
                }
            }

            val totalPages = doc.select("div.b-navigation a").last()?.text()?.toIntOrNull() ?: 1

            return@withContext Pair(movies, totalPages)
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to load watch later/continue page", e)
            return@withContext Pair(emptyList(), 0)
        }
    }

    private suspend fun modifyFavorite(postId: String, catId: String, action: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val formBody = FormBody.Builder()
            .add("post_id", postId)
            .add("cat_id", catId)
            .add("action", action)
            .build()

        val request = Request.Builder()
            .url("$baseUrl/ajax/favorites/")
            .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "$baseUrl/favorites/")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .post(formBody)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            response.close()

            try {
                val json = JSONObject(bodyStr)
                return@withContext json.optBoolean("success", response.isSuccessful)
            } catch (e: Exception) {
                if (bodyStr.contains("success", ignoreCase = true) || bodyStr.contains("ok", ignoreCase = true)) {
                    return@withContext true
                }
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to $action favorite for postId: $postId", e)
            false
        }
    }

    suspend fun addBookmark(postId: String): Boolean {
        return modifyFavorite(postId, "1", "add_post")
    }

    suspend fun addBookmarkToCategory(postId: String, catId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            modifyFavorite(postId, catId, "add_post")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "addBookmarkToCategory failed", e)
            false
        }
    }

    suspend fun removeBookmark(postId: String): Boolean {
        return modifyFavorite(postId, "1", "delete_post")
    }

    suspend fun removeBookmarkFromCategory(postId: String, catId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            modifyFavorite(postId, catId, "delete_post")
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "removeBookmarkFromCategory failed", e)
            false
        }
    }

    suspend fun addWatchLater(postId: String): Boolean {
        return modifyFavorite(postId, "2", "add_post")
    }

    suspend fun removeWatchLater(postId: String): Boolean {
        return modifyFavorite(postId, "2", "delete_post")
    }

    suspend fun isBookmarked(postId: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val html = getHtml("$baseUrl/favorites/")
        val doc = Jsoup.parse(html, baseUrl)
        return@withContext doc.select("div.b-favorites_content__item[data-id=$postId]").isNotEmpty()
    }

    suspend fun isInWatchLater(postId: String): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext false
        val html = getHtml("$baseUrl/favorites/watching/")
        val doc = Jsoup.parse(html, baseUrl)
        return@withContext doc.select("div.b-favorites_content__item[data-id=$postId]").isNotEmpty()
    }

    private suspend fun getHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().body?.string() ?: ""
    }

    private fun parseFilmsList(doc: Document): List<Movie> {
        return doc.select("div.b-content__inline_item").mapNotNull { el ->
            try {
                val url = el.selectFirst("a")?.absUrl("href") ?: return@mapNotNull null
                val title = el.selectFirst(".b-content__inline_item-link a")?.text() ?: return@mapNotNull null
                val imageUrl = el.selectFirst("img")?.absUrl("src") ?: return@mapNotNull null

                val infoDiv = el.selectFirst(".b-content__inline_item-link div")?.text() ?: ""

                val year = Regex("(\\d{4})").find(infoDiv)?.groupValues?.get(1)

                val isSeries = when {
                    url.contains("/series/") -> true
                    infoDiv.contains(Regex("(?i)(сезон|серия|сериал)")) -> true
                    else -> false
                }

                Movie(
                    url = url,
                    postId = el.attr("data-id"),
                    title = title,
                    imageUrl = imageUrl,
                    year = year,
                    rating = null,
                    isSeries = isSeries
                )
            } catch (e: Exception) {
                Log.e("HdRezkaApi", "Error parsing movie item", e)
                null
            }
        }
    }

    private fun parseFavoritesList(doc: Document): List<Movie> {
        val candidateSelectors = listOf(
            "div.b-favorites_content__item",
            "div.b-content__inline_item",
            "div.b-videos__list_item",
            "div.b-content__item",
            "li.b-content__inline_item",
            "div[data-id]"
        )

        for (sel in candidateSelectors) {
            val els = doc.select(sel)
            if (els.isNotEmpty()) {
                return els.mapNotNull { el ->
                    try {
                        val linkEl = el.selectFirst("a[href]") ?: return@mapNotNull null
                        val url = linkEl.absUrl("href")

                        val titleElement = el.selectFirst(".b-favorites_content__item_title a")
                            ?: el.selectFirst(".b-content__inline_item-link a")
                            ?: el.selectFirst("a[title]")
                            ?: linkEl
                        val fullTitle = titleElement.text().trim()

                        val imageEl = el.selectFirst("img")
                        val imageUrl = imageEl?.absUrl("src") ?: linkEl.attr("data-cover_url") ?: ""

                        val id = el.attr("data-id").ifBlank { el.attr("data-post_id") }

                        val infoDiv = (el.selectFirst(".b-favorites_content__item_title div")?.text()
                            ?: el.selectFirst(".b-content__inline_item-info")?.text()
                            ?: "").trim()

                        val year = Regex("(\\d{4})").find(infoDiv)?.groupValues?.get(1)
                        val cleanTitle = if (year != null) fullTitle.replace(year, "").replace(",", "").trim() else fullTitle

                        Movie(
                            url = url,
                            postId = if (id.isNotBlank()) id else el.attr("data-post_id").ifBlank { el.attr("data-id") },
                            title = cleanTitle,
                            imageUrl = imageUrl,
                            year = year,
                            rating = null,
                            isSeries = url.contains("/series/")
                        )
                    } catch (e: Exception) {
                        Log.e("HdRezkaApi", "Failed to parse favorite item", e)
                        null
                    }
                }
            }
        }

        return doc.select("div.b-content__inline_item, li.b-content__inline_item").mapNotNull { el ->
            try {
                val url = el.selectFirst("a")?.absUrl("href") ?: return@mapNotNull null
                val title = el.selectFirst(".b-content__inline_item-link a")?.text() ?: el.selectFirst("a")?.text() ?: ""
                val imageUrl = el.selectFirst("img")?.absUrl("src") ?: ""
                val year = Regex("(\\d{4})").find(el.text())?.groupValues?.get(1)
                Movie(url = url, postId = el.attr("data-id"), title = title, imageUrl = imageUrl, year = year, rating = null, isSeries = url.contains("/series/"))
            } catch (e: Exception) {
                Log.e("HdRezkaApi", "Fallback parse favorites failed", e)
                null
            }
        }
    }

    suspend fun search(query: String): List<Movie> = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("$baseUrl/search/?do=search&subaction=search&q=$encodedQuery")
            .get()
            .build()
        val html = client.newCall(request).execute().body?.string() ?: return@withContext emptyList()
        val doc = Jsoup.parse(html, baseUrl)
        return@withContext parseFilmsList(doc)
    }

    suspend fun getActorDetails(link: String): ActorDetails? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext null
            val fullUrl = if (link.startsWith("http")) link else baseUrl + link
            val html = getHtml(fullUrl)
            val doc = Jsoup.parse(html, baseUrl)

            val name = doc.selectFirst(".b-post__title .t1")?.text() ?: return@withContext null
            val originalName = doc.selectFirst(".b-post__title .t2")?.text()
            val photo = doc.selectFirst(".b-sidecover img")?.absUrl("src")

            var dob: String? = null
            var birthPlace: String? = null
            var height: String? = null

            doc.select(".b-post__info tr").forEach { tr ->
                val tds = tr.select("td")
                if (tds.size >= 2) {
                    val key = tds[0].text().replace(":", "").trim()
                    val value = tds[1].text().trim()

                    when (key) {
                        "Дата рождения" -> dob = value
                        "Место рождения" -> birthPlace = value
                        "Рост" -> height = value
                    }
                }
            }

            val biography = doc.select(".b-post__description_text").text()
            val careers = mutableListOf<String>()
            doc.select(".b-post__title .t1 + span").forEach { span ->
                span.text()?.let { careers.add(it) }
            }

            val roles = doc.select(".b-person__career").mapNotNull { careerEl ->
                try {
                    val role = careerEl.selectFirst("h2")?.text() ?: return@mapNotNull null
                    val info = careerEl.selectFirst(".b-person__career_stats")?.text()

                    val films = careerEl.select(".b-content__inline_item").mapNotNull { filmEl ->
                        try {
                            val url = filmEl.selectFirst("a")?.absUrl("href") ?: return@mapNotNull null
                            val title = filmEl.selectFirst(".b-content__inline_item-link a")?.text() ?: return@mapNotNull null
                            val imageUrl = filmEl.selectFirst("img")?.absUrl("src") ?: return@mapNotNull null
                            val infoDiv = filmEl.selectFirst(".b-content__inline_item-link div")?.text() ?: ""
                            val year = Regex("(\\d{4})").find(infoDiv)?.groupValues?.get(1)

                            val isSeries = url.contains("/series/") || infoDiv.contains(Regex("(?i)(сезон|серия|сериал)"))

                            Movie(
                                title = title,
                                imageUrl = imageUrl,
                                url = url,
                                year = year,
                                isSeries = isSeries,
                                rating = null
                            )
                        } catch (e: Exception) {
                            Log.e("HdRezkaApi", "Failed to parse film in actor role", e)
                            null
                        }
                    }

                    ActorRole(
                        role = role,
                        info = info,
                        films = films
                    )
                } catch (e: Exception) {
                    Log.e("HdRezkaApi", "Failed to parse actor role", e)
                    null
                }
            }

            ActorDetails(
                name = name,
                originalName = originalName,
                photo = photo,
                careers = careers,
                dob = dob,
                birthPlace = birthPlace,
                height = height,
                biography = biography,
                roles = roles
            )
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to get actor details", e)
            null
        }
    }

    suspend fun getStreamsForPost(postId: String, translatorId: String, season: String? = null, episode: String? = null): StreamInfo = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext StreamInfo(emptyMap())
            val unixTime = System.currentTimeMillis()

            val formBodyBuilder = FormBody.Builder()
                .add("id", postId)
                .add("translator_id", translatorId)

            if (season != null && episode != null) {
                formBodyBuilder
                    .add("season", season)
                    .add("episode", episode)
                    .add("action", "get_stream")
            } else {
                formBodyBuilder.add("action", "get_movie")
            }

            val request = Request.Builder()
                .url("$baseUrl/ajax/get_cdn_series/?t=$unixTime")
                .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
                .post(formBodyBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()
            response.close()

            if (responseString.isNullOrEmpty() || !responseString.startsWith("{")) {
                Log.w("HdRezkaApi", "Expected JSON response but got: $responseString")
                return@withContext StreamInfo(emptyMap())
            }

            val json = JSONObject(responseString)
            if (!json.optBoolean("success", false)) {
                val msg = json.optString("message", "Unknown error")
                Log.w("HdRezkaApi", "Server returned error: $msg")
                return@withContext StreamInfo(emptyMap())
            }

            val streamsKey = when {
                json.has("url") -> "url"
                json.has("streams") -> "streams"
                else -> {
                    return@withContext StreamInfo(emptyMap())
                }
            }

            val rawStreams = json.getString(streamsKey)
            if (rawStreams.isBlank()) {
                return@withContext StreamInfo(emptyMap())
            }

            val decodedStreams = decodeStreamUrl(rawStreams)
            val streamsMap = parseStreams(decodedStreams)
            
            // Subtitles
            val subtitle = json.optString("subtitle")
            val subtitleDef = json.optString("subtitle_def")
            val subtitleLns = mutableMapOf<String, String>()
            val subtitleLnsJson = json.optJSONObject("subtitle_lns")
            if (subtitleLnsJson != null) {
                subtitleLnsJson.keys().forEach { key ->
                    subtitleLns[key] = subtitleLnsJson.getString(key)
                }
            }
            val subtitles = parseSubtitles(subtitle, subtitleDef, subtitleLns)

            // Storyboard
            val thumbnails = json.optString("thumbnails")
            
            val streamInfo = StreamInfo(
                streams = streamsMap,
                subtitles = subtitles,
                storyboardUrl = thumbnails.takeIf { it.isNotBlank() }
            )
            
            return@withContext modifyCDN(streamInfo)
        } catch (e: Exception) {
            return@withContext StreamInfo(emptyMap())
        }
    }

    private fun modifyCDN(streamInfo: StreamInfo): StreamInfo {
        // Implementation for CDN modification if needed
        // For now, we can check if a specific CDN is forced in settings
        val forcedCdn = localStorage.getString("forced_cdn", null)
        if (forcedCdn.isNullOrBlank() || forcedCdn == "auto") {
            return streamInfo
        }

        val newStreams = streamInfo.streams.mapValues { (_, url) ->
            try {
                // Replace the domain in the URL with the forced CDN
                // This is a simplified implementation. Real world usage might need more complex regex or logic
                // assuming urls are like https://stream.voidboost.cc/...
                val urlObj = java.net.URL(url)
                val newUrl = "${urlObj.protocol}://$forcedCdn${urlObj.file}"
                newUrl
            } catch (e: Exception) {
                url
            }
        }
        
        return streamInfo.copy(streams = newStreams)
    }

    private fun parseSubtitles(
        subtitle: String?,
        subtitleDef: String,
        subtitleLns: Map<String, String>
    ): List<Subtitle> {
        if (subtitle.isNullOrEmpty()) return emptyList()
        
        val rawSubtitles = mutableListOf<Pair<String, String>>()
        try {
            subtitle.split(",").forEach { str ->
                val bracketIndex = str.indexOf(']')
                if (bracketIndex != -1) {
                    val language = str.substring(1, bracketIndex)
                    val url = str.substring(bracketIndex + 1)
                    rawSubtitles.add(language to url)
                }
            }
            
            return rawSubtitles.map { (name, url) ->
                Subtitle(
                    name = name,
                    languageCode = subtitleLns[name] ?: "",
                    url = url,
                    isDefault = subtitleLns[name] == subtitleDef
                )
            }
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to parse subtitles", e)
            return emptyList()
        }
    }

    private fun decodeStreamUrl(str: String): String {
        return HdRezkaDecryptor.decrypt(str) ?: str
    }

    private fun parseStreams(streams: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            streams.split(",").forEach { stream ->
                try {
                    val qualityStart = stream.indexOf('[')
                    val qualityEnd = stream.indexOf(']')

                    if (qualityStart != -1 && qualityEnd != -1 && qualityEnd > qualityStart) {
                        val quality = stream.substring(qualityStart + 1, qualityEnd)
                        val cleanQuality = Jsoup.parse(quality).text().trim()

                        val urlPart = stream.substring(qualityEnd + 1)

                        val urls = urlPart.split(" or ")
                        val bestUrl = urls.firstOrNull()?.trim()

                        if (!bestUrl.isNullOrBlank() && !cleanQuality.isBlank()) {
                            map[cleanQuality] = bestUrl
                        }
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        return map
    }

    suspend fun getCommentsCount(postId: String, movieUrl: String? = null): Int = withContext(Dispatchers.IO) {
        try {
            val (comments, totalPages) = getComments(postId, 1)
            if (comments.isNotEmpty()) {
                val estimatedCount = comments.size * totalPages

                if (movieUrl != null) {
                    try {
                        val html = getHtml(movieUrl)
                        val doc = Jsoup.parse(html)

                        val commentsCountText = doc.select(".b-comments__count, .comments-count, [data-comments-count], .b-comments__title, .b-post__comments_count")
                            .firstOrNull()?.text()

                        if (commentsCountText != null) {
                            val regex = Regex("(\\d+)")
                            val match = regex.find(commentsCountText)
                            match?.groupValues?.get(1)?.toIntOrNull()?.let {
                                return@withContext it
                            }
                        }
                    } catch (e: Exception) {
                        return@withContext estimatedCount
                    }
                }

                return@withContext estimatedCount
            }

            if (movieUrl != null) {
                val html = getHtml(movieUrl)
                val doc = Jsoup.parse(html)

                val commentsCountText = doc.select(".b-comments__count, .comments-count, [data-comments-count], .b-comments__title, .b-post__comments_count")
                    .firstOrNull()?.text()

                if (commentsCountText != null) {
                    val regex = Regex("(\\d+)")
                    val match = regex.find(commentsCountText)
                    return@withContext match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
            }

            return@withContext 0
        } catch (e: Exception) {
            return@withContext 0
        }
    }

    suspend fun getComments(postId: String, page: Int = 1): Pair<List<Comment>, Int> = withContext(Dispatchers.IO) {
        val baseUrl = getWorkingMirror() ?: return@withContext Pair(emptyList(), 1)

        try {
            val url = "$baseUrl/ajax/get_comments".toHttpUrlOrNull()?.newBuilder()?.apply {
                addQueryParameter("t", System.currentTimeMillis().toString())
                addQueryParameter("news_id", postId)
                addQueryParameter("cstart", page.toString())
                addQueryParameter("type", "0")
                addQueryParameter("comment_id", "0")
                addQueryParameter("skin", "hdrezka")
            }?.build() ?: return@withContext Pair(emptyList(), 1)

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string() ?: return@withContext Pair(emptyList(), 1)
            val json = JSONObject(jsonStr)

            val commentsHtml = json.optString("comments", "")
            val navigationHtml = json.optString("navigation", "")

            if (commentsHtml.isEmpty()) {
                return@withContext Pair(emptyList(), 1)
            }

            val commentsDoc = Jsoup.parse(commentsHtml)
            val navDoc = Jsoup.parse(navigationHtml)

            val comments = commentsDoc.select(".comments-tree-item").mapNotNull { el ->
                try {
                    val textEl = el.select(".text").firstOrNull()
                    val text = textEl?.text() ?: ""
                    val isSpoiler = textEl?.hasClass("spoiler") == true ||
                                   text.contains("спойлер", ignoreCase = true) ||
                                   el.hasClass("spoiler")

                    Comment(
                        id = el.attr("data-id"),
                        avatar = el.select(".ava img").firstOrNull()?.absUrl("src") ?: "",
                        author = el.select(".name").firstOrNull()?.text() ?: "",
                        date = el.select(".date").firstOrNull()?.text()?.replace("оставлен ", "") ?: "",
                        text = text,
                        likes = el.select(".b-comment__like_it").firstOrNull()?.attr("data-likes_num")?.toIntOrNull() ?: 0,
                        indent = el.attr("data-indent").toIntOrNull() ?: 0,
                        isSpoiler = isSpoiler
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val navLinks = navDoc.select(".b-navigation a")
            val totalPages = if (navLinks.size >= 2) {
                navLinks[navLinks.size - 2].text().toIntOrNull() ?: 1
            } else {
                1
            }

            return@withContext Pair(comments, totalPages)
        } catch (e: Exception) {
            return@withContext Pair(emptyList(), 1)
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext emptyList()
            val formBody = FormBody.Builder()
                .add("q", query)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/engine/ajax/search.php")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()
            val doc = Jsoup.parse(html)
            
            return@withContext doc.select("span.search-title").map { it.text() }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    suspend fun updateTime(postId: String, translatorId: String, season: String?, episode: String?, timeInSeconds: Long) = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getWorkingMirror() ?: return@withContext
            
            val formBodyBuilder = FormBody.Builder()
                .add("post_id", postId)
                .add("translator_id", translatorId)
                .add("season", season ?: "0")
                .add("episode", episode ?: "0")
                .add("current_time", timeInSeconds.toString())
                
            val request = Request.Builder()
                .url("$baseUrl/ajax/send_save/")
                .header("Cookie", localStorage.getAllCookies().joinToString("; ") { "${it.name}=${it.value}" })
                .header("X-Requested-With", "XMLHttpRequest")
                .post(formBodyBuilder.build())
                .build()
            
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to update time", e)
        }
    }
}
