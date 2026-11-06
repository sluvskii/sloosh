package com.slooshfilm.app.data.alloha

import android.content.Context
import android.util.Log
import com.slooshfilm.app.data.api.MoviesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

private const val TAG = "AllohaRepository"

fun injectTranslationId(id: String, urlString: String): String {
    return try {
        val clean = urlString.replace(Regex("[?&]translation=[^&]*"), "")
        val sep = if (clean.contains("?")) "&" else "?"
        "$clean${sep}translation=$id"
    } catch (_: Exception) {
        urlString
    }
}

fun injectSeasonEpisode(season: Int, episode: Int, urlString: String): String {
    return try {
        val clean = urlString
            .replace(Regex("[?&]season=[^&]*"), "")
            .replace(Regex("[?&]episode=[^&]*"), "")
        val sep = if (clean.contains("?")) "&" else "?"
        "$clean${sep}season=$season&episode=$episode"
    } catch (_: Exception) {
        urlString
    }
}

class AllohaRepository(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AllohaRepository? = null

        private const val BALANCER_PREFS_KEY = "sloosh_cached_balancer_base_url"
        private const val FALLBACK_TOKENS_KEY = "sloosh_cached_fallback_stream_tokens"

        @Volatile
        var activeBalancerBaseURL: String = "https://api.alloha.tv"
            private set

        fun setBalancerBaseURL(context: Context, url: String) {
            val clean = if (url.endsWith("/")) url.dropLast(1) else url
            if (clean.isNotBlank() && clean.startsWith("http")) {
                activeBalancerBaseURL = clean
                context.getSharedPreferences("sloosh_alloha_config", Context.MODE_PRIVATE)
                    .edit()
                    .putString(BALANCER_PREFS_KEY, clean)
                    .apply()
            }
        }

        fun getInstance(context: Context): AllohaRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AllohaRepository(context.applicationContext).also {
                    val prefs = context.applicationContext.getSharedPreferences("sloosh_alloha_config", Context.MODE_PRIVATE)
                    val cachedUrl = prefs.getString(BALANCER_PREFS_KEY, null)
                    if (!cachedUrl.isNullOrBlank()) {
                        activeBalancerBaseURL = cachedUrl
                    }
                    INSTANCE = it
                }
            }
        }

        // Основной клиент с нормальной верификацией сертификатов. Резервное зеркало
        // (alloha.tv) иногда отдаёт самоподписанный сертификат — для него есть
        // точечный ретрай на unsafeFallbackClient в точке запроса.
        val sharedOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }

        private val unsafeFallbackClient: OkHttpClient by lazy {
            createUnsafeOkHttpClient()
        }

        fun createUnsafeOkHttpClient(): OkHttpClient {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    private val cache = ConcurrentHashMap<String, Pair<AllohaApiResult, Long>>()
    private val cacheTtlMs = 5 * 60 * 1000L

    private val client: OkHttpClient get() = sharedOkHttpClient

    fun invalidateCache() {
        cache.clear()
        Log.d(TAG, "Alloha cache invalidated")
    }

    private var remoteTokens: List<String> = emptyList()
    private var lastTokensFetchTime = 0L
    private val tokensMutex = Mutex()
    private val failedTokens = ConcurrentHashMap<String, Long>()
    private val tokenCooldownMs = 5 * 60 * 1000L

    // Zero Static Secrets: токены балансера не хранятся в коде. Пул выдаётся бэкендом только по
    // валидному Firebase ID токену; последний успешно полученный пул персистится локально
    // и служит офлайн-фоллбэком до восстановления связи.
    private var fallbackTokens: List<String> = emptyList()

    init {
        val prefs = context.getSharedPreferences("sloosh_alloha_config", Context.MODE_PRIVATE)
        val savedFallback = prefs.getStringSet(FALLBACK_TOKENS_KEY, null)?.toList()
        if (!savedFallback.isNullOrEmpty()) {
            fallbackTokens = savedFallback
        }
    }

    private fun persistTokens(tokens: List<String>) {
        context.getSharedPreferences("sloosh_alloha_config", Context.MODE_PRIVATE)
            .edit()
            .putStringSet(FALLBACK_TOKENS_KEY, tokens.toSet())
            .apply()
    }

    private suspend fun ensureTokensLoaded(): List<String> = tokensMutex.withLock {
        val now = System.currentTimeMillis()
        if (remoteTokens.isEmpty() || now - lastTokensFetchTime > 10 * 60 * 1000L) {
            try {
                val envelope = MoviesApi.service.getStreamTokens()
                val fetched = envelope.data?.tokens?.filter { it.isNotBlank() } ?: emptyList()
                if (fetched.isNotEmpty()) {
                    remoteTokens = fetched
                    lastTokensFetchTime = now
                    persistTokens(fetched)
                    Log.d(TAG, "Loaded ${fetched.size} Alloha tokens from server")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch remote Alloha tokens, using fallback tokens: ${e.message}")
                if (remoteTokens.isEmpty()) {
                    remoteTokens = fallbackTokens
                }
            }
        }

        val allTokens = remoteTokens.ifEmpty { fallbackTokens }
        val available = allTokens.filter { token ->
            val failTime = failedTokens[token] ?: 0L
            now - failTime > tokenCooldownMs
        }
        if (available.isNotEmpty()) available else allTokens
    }

    suspend fun fetchAllohaData(
        mediaId: String,
        explicitKpId: Int? = null,
        imdbId: String? = null,
        tmdbId: Int? = null,
        title: String? = null,
        originalTitle: String? = null,
        year: Int? = null,
        isTv: Boolean? = null,
        forceRefresh: Boolean = false
    ): AllohaApiResult? = withContext(Dispatchers.IO) {
        if (forceRefresh) {
            cache.clear()
        }
        val isExplicitKp = mediaId.startsWith("kp_")
        val isTvFromId = if (mediaId.startsWith("tv_")) true else if (mediaId.startsWith("movie_")) false else null
        val effectiveIsTv = isTv ?: isTvFromId
        val categoryParam = effectiveIsTv?.let { if (it) "2" else "1" }

        val validKp = explicitKpId?.takeIf { it > 0 }
            ?: if (isExplicitKp) mediaId.removePrefix("kp_").trim().toIntOrNull()?.takeIf { it > 0 } else null

        val effectiveTmdb = tmdbId?.takeIf { it > 0 }
            ?: if (!isExplicitKp) mediaId.replace("tv_", "").replace("movie_", "").trim().toIntOrNull()?.takeIf { it > 0 } else null
        val validTmdb = effectiveTmdb?.takeIf { it > 0 }

        // Helper to query with category and validate media type
        suspend fun query(baseParams: Map<String, String>): AllohaApiResult? {
            val params = baseParams.toMutableMap()
            if (categoryParam != null) {
                params["category"] = categoryParam
            }
            val res = performAllohaQueryParams(params, forceRefresh)
            return if (res != null && isValidType(res, effectiveIsTv)) res else null
        }

        // 1. Поиск по Kinopoisk ID
        if (validKp != null) {
            val res = query(mapOf("kp" to validKp.toString()))
            if (res != null) return@withContext res
        }

        // 2. Поиск по TMDB ID
        if (validTmdb != null) {
            val res = query(mapOf("tmdb" to validTmdb.toString()))
            if (res != null) return@withContext res
        }

        // 3. Поиск по IMDB ID
        if (!imdbId.isNullOrBlank()) {
            val res = query(mapOf("imdb" to imdbId.trim()))
            if (res != null) return@withContext res
        }

        // 4. Поиск по названию + году
        if (!title.isNullOrBlank() && !title.startsWith("Без названия")) {
            val cleanTitle = title.trim()
            if (year != null && year > 1900) {
                val res = query(mapOf("name" to cleanTitle, "year" to year.toString()))
                if (res != null) return@withContext res
            }
            val res = query(mapOf("name" to cleanTitle))
            if (res != null) return@withContext res
        }

        // 5. Поиск по оригинальному названию
        if (!originalTitle.isNullOrBlank() && originalTitle != title) {
            val cleanOrig = originalTitle.trim()
            if (year != null && year > 1900) {
                val res = query(mapOf("name" to cleanOrig, "year" to year.toString()))
                if (res != null) return@withContext res
            }
            val res = query(mapOf("name" to cleanOrig))
            if (res != null) return@withContext res
        }

        null
    }

    private fun isValidType(result: AllohaApiResult, isTv: Boolean?): Boolean {
        if (isTv != null) {
            if (isTv && !result.isSerial) return false
            if (!isTv && result.isSerial) return false
        }
        return true
    }

    suspend fun fetchMedia(
        kpId: Int? = null,
        tmdbId: Int? = null,
        imdbId: String? = null,
        title: String? = null,
        originalTitle: String? = null,
        year: Int? = null,
        isTv: Boolean? = null
    ): AllohaApiResult? {
        return fetchAllohaData(
            mediaId = if (kpId != null && kpId > 0) "kp_$kpId" else (tmdbId ?: 0).toString(),
            explicitKpId = kpId,
            imdbId = imdbId,
            tmdbId = tmdbId,
            title = title,
            originalTitle = originalTitle,
            year = year,
            isTv = isTv
        )
    }

    private suspend fun performAllohaQuery(param: String, value: String, forceRefresh: Boolean = false): AllohaApiResult? {
        return performAllohaQueryParams(mapOf(param to value), forceRefresh)
    }

    private suspend fun performAllohaQueryParams(params: Map<String, String>, forceRefresh: Boolean = false): AllohaApiResult? {
        val cacheKey = params.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
        if (!forceRefresh) {
            val cached = cache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.second < cacheTtlMs) {
                return cached.first
            }
        }

        val candidates = ensureTokensLoaded()
        if (candidates.isEmpty()) return null

        val queryBuilder = StringBuilder()
        for ((k, v) in params) {
            if (queryBuilder.isNotEmpty()) queryBuilder.append("&")
            queryBuilder.append(k).append("=").append(URLEncoder.encode(v, "UTF-8"))
        }
        val queryString = queryBuilder.toString()
        val streamSource = context.getSharedPreferences("sloosh_prefs", Context.MODE_PRIVATE).getString("stream_source", "Основной")
        val baseUrl = if (streamSource == "Резервный") "https://alloha.tv" else activeBalancerBaseURL

        for (candidateToken in candidates) {
            val url = "$baseUrl/?token=$candidateToken&$queryString"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1")
                .build()

            try {
                val response = try {
                    client.newCall(request).execute()
                } catch (e: javax.net.ssl.SSLException) {
                    // Резервное зеркало может отдавать невалидный сертификат —
                    // один ретрай без верификации, только для этого запроса.
                    unsafeFallbackClient.newCall(request).execute()
                }
                val code = response.code
                if (code in listOf(401, 403, 429) || code >= 500) {
                    failedTokens[candidateToken] = System.currentTimeMillis()
                    response.close()
                    continue
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    failedTokens[candidateToken] = System.currentTimeMillis()
                    continue
                }

                val json = JSONObject(body)
                val status = json.optString("status", "").lowercase(Locale.ROOT)
                if (status == "error") {
                    val errorInfo = json.optString("error_info", "").lowercase(Locale.ROOT)
                    if (errorInfo.contains("not movie")) {
                        // Релиз просто отсутствует в базе Alloha по данному запросу —
                        // токен полностью рабочий (iOS markTokenSuccess), подтверждаем его и переходим к след. параметру
                        failedTokens.remove(candidateToken)
                        return null
                    }
                    failedTokens[candidateToken] = System.currentTimeMillis()
                    continue
                }

                val data = json.optJSONObject("data") ?: continue
                val result = parseAllohaData(data)
                if (result != null) {
                    cache[cacheKey] = Pair(result, System.currentTimeMillis())
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Query error for token: ${e.message}")
            }
        }
        return null
    }

    private fun parseAllohaData(data: JSONObject): AllohaApiResult? = try {
        val title = data.optString("name", "Без названия")
        val isSerial = data.optBoolean("is_serial", false) || data.has("seasons")

        if (isSerial) {
            val seasonsList = mutableListOf<AllohaSeason>()
            val seasonsObj = data.optJSONObject("seasons")
            if (seasonsObj != null) {
                val sKeys = seasonsObj.keys()
                while (sKeys.hasNext()) {
                    val sKey = sKeys.next()
                    val sNum = sKey.toIntOrNull() ?: continue
                    val sData = seasonsObj.optJSONObject(sKey) ?: continue
                    val episodesObj = sData.optJSONObject("episodes") ?: continue
                    val episodesList = mutableListOf<AllohaEpisode>()

                    val eKeys = episodesObj.keys()
                    while (eKeys.hasNext()) {
                        val eKey = eKeys.next()
                        val eNum = eKey.toIntOrNull() ?: continue
                        val eData = episodesObj.optJSONObject(eKey) ?: continue
                        val translationsList = mutableListOf<AllohaTranslation>()

                        // 1. Проверяем объект или массив "translation"
                        val tObj = eData.optJSONObject("translation")
                        val tArr = eData.optJSONArray("translation")

                        if (tObj != null) {
                            val tKeys = tObj.keys()
                            while (tKeys.hasNext()) {
                                val tKey = tKeys.next()
                                val tVal = tObj.opt(tKey)
                                var transName = ""
                                var rawIframe = ""

                                if (tVal is JSONObject) {
                                    rawIframe = tVal.optString("iframe", tVal.optString("url", ""))
                                    transName = tVal.optString("translation", tVal.optString("name", tVal.optString("title", "")))
                                }

                                // A series-level episode iframe is not a per-translation iframe.
                                // Reusing it here creates duplicate-looking voice options that all
                                // resolve to the same stream. iOS only accepts a translation entry
                                // when that entry carries its own iframe.
                                if (rawIframe.isNotBlank()) {
                                    if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                                    rawIframe = injectTranslationId(tKey, rawIframe)
                                    rawIframe = injectSeasonEpisode(sNum, eNum, rawIframe)

                                    val cleanTitle = AllohaTranslationMatcher.normalizedAllohaTranslationName(if (transName.isBlank()) "Озвучка $tKey" else transName)
                                    val lower = cleanTitle.lowercase(Locale.ROOT)
                                    if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                                        translationsList.add(AllohaTranslation(id = tKey, name = cleanTitle, iframeUrl = rawIframe))
                                    }
                                }
                            }
                        } else if (tArr != null) {
                            for (i in 0 until tArr.length()) {
                                val item = tArr.optJSONObject(i) ?: continue
                                var rawIframe = item.optString("iframe", item.optString("url", ""))
                                if (rawIframe.isNotBlank()) {
                                    if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                                    val tId = item.optString("id", item.optString("translation_id", i.toString()))
                                    rawIframe = injectTranslationId(tId, rawIframe)
                                    rawIframe = injectSeasonEpisode(sNum, eNum, rawIframe)

                                    val transName = item.optString("translation", item.optString("name", "Озвучка $tId"))
                                    val cleanTitle = AllohaTranslationMatcher.normalizedAllohaTranslationName(transName)
                                    val lower = cleanTitle.lowercase(Locale.ROOT)
                                    if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                                        translationsList.add(AllohaTranslation(id = tId, name = cleanTitle, iframeUrl = rawIframe))
                                    }
                                }
                            }
                        }

                        // Если translationsList пустой, но есть общий iframe у серии
                        if (translationsList.isEmpty()) {
                            var directIframe = eData.optString("iframe", "")
                            if (directIframe.isNotBlank()) {
                                if (directIframe.startsWith("//")) directIframe = "https:$directIframe"
                                directIframe = injectSeasonEpisode(sNum, eNum, directIframe)
                                translationsList.add(AllohaTranslation(id = "default", name = "По умолчанию", iframeUrl = directIframe))
                            }
                        }

                        if (translationsList.isNotEmpty()) {
                            // Сортируем озвучки по рангу (русский дубляж = 1, топ-студии, проф)
                            translationsList.sortBy { AllohaTranslationMatcher.translationRank(it.name) }
                            episodesList.add(AllohaEpisode(season = sNum, episode = eNum, translations = translationsList))
                        }
                    }
                    episodesList.sortBy { it.episode }
                    if (episodesList.isNotEmpty()) {
                        seasonsList.add(AllohaSeason(season = sNum, episodes = episodesList))
                    }
                }
                seasonsList.sortBy { it.season }
            }
            AllohaApiResult(title = title, isSerial = true, movie = null, seasons = seasonsList)
        } else {
            // Фильм
            val translationsList = mutableListOf<AllohaTranslation>()
            val baseIframe = data.optString("iframe", "")

            // 1. Проверяем translation_iframe (основной формат Alloha для фильмов)
            val transIframeObj = data.optJSONObject("translation_iframe")
            val transIframeArr = data.optJSONArray("translation_iframe")
            val nameDict = data.optJSONObject("translation") ?: data.optJSONObject("translations")

            if (transIframeObj != null) {
                val tKeys = transIframeObj.keys()
                while (tKeys.hasNext()) {
                    val tKey = tKeys.next()
                    val tVal = transIframeObj.opt(tKey)
                    var rawIframe = ""
                    var transName = ""

                    if (tVal is JSONObject) {
                        rawIframe = tVal.optString("iframe", tVal.optString("url", ""))
                        transName = tVal.optString("name", tVal.optString("translation", tVal.optString("title", "")))
                    } else if (tVal is String) {
                        rawIframe = tVal
                    }

                    if (rawIframe.isBlank()) rawIframe = baseIframe
                    if (rawIframe.isNotBlank()) {
                        if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                        rawIframe = injectTranslationId(tKey, rawIframe)

                        if (transName.isBlank() && nameDict != null) {
                            val nVal = nameDict.opt(tKey)
                            if (nVal is String) {
                                transName = nVal
                            } else if (nVal is JSONObject) {
                                transName = nVal.optString("translation", nVal.optString("name", ""))
                            }
                        }

                        val cleanTitle = AllohaTranslationMatcher.normalizedAllohaTranslationName(if (transName.isBlank()) "Озвучка $tKey" else transName)
                        val lower = cleanTitle.lowercase(Locale.ROOT)
                        if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                            translationsList.add(AllohaTranslation(id = tKey, name = cleanTitle, iframeUrl = rawIframe))
                        }
                    }
                }
            } else if (transIframeArr != null) {
                for (i in 0 until transIframeArr.length()) {
                    val item = transIframeArr.optJSONObject(i) ?: continue
                    var rawIframe = item.optString("iframe", item.optString("url", ""))
                    if (rawIframe.isBlank()) rawIframe = baseIframe
                    if (rawIframe.isNotBlank()) {
                        if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                        val tId = item.optString("id", item.optString("translation_id", i.toString()))
                        rawIframe = injectTranslationId(tId, rawIframe)

                        val transName = item.optString("translation", item.optString("name", "Озвучка $tId"))
                        val cleanTitle = AllohaTranslationMatcher.normalizedAllohaTranslationName(transName)
                        val lower = cleanTitle.lowercase(Locale.ROOT)
                        if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                            translationsList.add(AllohaTranslation(id = tId, name = cleanTitle, iframeUrl = rawIframe))
                        }
                    }
                }
            }

            // 2. Если translation_iframe не было, проверяем translation словарь
            if (translationsList.isEmpty() && nameDict != null && baseIframe.isNotBlank()) {
                val tKeys = nameDict.keys()
                while (tKeys.hasNext()) {
                    val tKey = tKeys.next()
                    val nVal = nameDict.opt(tKey)
                    val transName = if (nVal is String) nVal else (nVal as? JSONObject)?.optString("name", "") ?: ""
                    var rawIframe = baseIframe
                    if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                    rawIframe = injectTranslationId(tKey, rawIframe)

                    val cleanTitle = AllohaTranslationMatcher.normalizedAllohaTranslationName(if (transName.isBlank()) "Озвучка $tKey" else transName)
                    val lower = cleanTitle.lowercase(Locale.ROOT)
                    if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                        translationsList.add(AllohaTranslation(id = tKey, name = cleanTitle, iframeUrl = rawIframe))
                    }
                }
            }

            // 3. Fallback: если ничего не найдено, но есть базовый iframe
            if (translationsList.isEmpty() && baseIframe.isNotBlank()) {
                var rawIframe = baseIframe
                if (rawIframe.startsWith("//")) rawIframe = "https:$rawIframe"
                translationsList.add(AllohaTranslation(id = "default", name = "По умолчанию", iframeUrl = rawIframe))
            }

            var safeBaseIframe = baseIframe
            if (safeBaseIframe.startsWith("//")) safeBaseIframe = "https:$safeBaseIframe"

            if (translationsList.isNotEmpty()) {
                translationsList.sortBy { AllohaTranslationMatcher.translationRank(it.name) }
            }

            val movieIframe = if (safeBaseIframe.isNotBlank()) safeBaseIframe else (translationsList.firstOrNull()?.iframeUrl ?: "")
            val movie = AllohaMovie(title = title, iframeUrl = movieIframe, translations = translationsList)
            AllohaApiResult(title = title, isSerial = false, movie = movie, seasons = emptyList())
        }
    } catch (e: Exception) {
        Log.e(TAG, "parseAllohaData error: ${e.message}", e)
        null
    }
}
