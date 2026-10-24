package com.slooshfilm.app.data.alloha

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "AllohaSession"

class AllohaSessionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var parser: AllohaParser? = null
        private set

    val activeHeaders: Map<String, String> get() = _activeHeaders.toMap()
    private val _activeHeaders = ConcurrentHashMap<String, String>()

    var lastSelectedQuality: String = ""
        private set
    var lastQualityMap: Map<String, String> = emptyMap()
        private set

    var lastSkipRanges: List<LongRange> = emptyList()
        private set

    var latestMasterUrl: String? = null
        private set

    var onStreamReady: ((qualityMap: Map<String, String>, defaultUrl: String) -> Unit)? = null
    var onStreamReadyWithHeaders: ((qualityMap: Map<String, String>, defaultUrl: String, headers: Map<String, String>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onM3u8Updated: ((newUrl: String) -> Unit)? = null
    var onSessionRefreshed: ((qualityMap: Map<String, String>, defaultUrl: String, headers: Map<String, String>) -> Unit)? = null

    private var isRestarting = false
    private var lastRestartUrl: String? = null
    private var failureCount = 0

    fun ensureInitialized() {
        if (parser == null) parser = AllohaParser(context)
    }

    fun refreshSession(attempt: Int = 1) {
        val url = lastRestartUrl ?: return
        Log.d(TAG, "Proactively refreshing Alloha session for $url (attempt=$attempt)")
        startSession(url, isRestart = true, attempt = attempt)
    }

    fun startSession(iframeUrl: String, isRestart: Boolean = false, attempt: Int = 1) {
        ensureInitialized()
        lastRestartUrl = iframeUrl
        isRestarting = isRestart

        Log.d(TAG, "Starting Alloha session (attempt=$attempt, restart=$isRestart): $iframeUrl")

        val timeoutJob = scope.launch {
            delay(20_000)
            if (isActive) {
                Log.w(TAG, "Session init timed out after 20s (attempt $attempt)")
                failureCount++
                parser?.rotateUserAgent()
                if (attempt < 3) {
                    Log.d(TAG, "Retrying session init (attempt ${attempt + 1})...")
                    startSession(iframeUrl, isRestart, attempt = attempt + 1)
                } else {
                    onError?.invoke("Превышено время ожидания видеопотока")
                }
            }
        }

        parser?.parse(iframeUrl, object : AllohaParser.Callback {
            override fun onHlsLinksReceived(json: String, extraHeaders: Map<String, String>) {
                timeoutJob.cancel()
                failureCount = 0
                _activeHeaders.putAll(extraHeaders)

                val result = parseBnsiJson(json)
                if (result == null) {
                    if (attempt < 3) {
                        parser?.rotateUserAgent()
                        startSession(iframeUrl, isRestart, attempt = attempt + 1)
                    } else {
                        onError?.invoke("Ошибка декодирования потока")
                    }
                    return
                }

                lastQualityMap = result.qualityMap
                lastSkipRanges = result.skipRanges
                AllohaSessionHolder.skipRanges = result.skipRanges
                AllohaSessionHolder.qualityMap = result.qualityMap

                val initialQuality = pickBestQuality(result.qualityMap)
                val initialUrl = result.qualityMap[initialQuality] ?: result.defaultUrl
                lastSelectedQuality = initialQuality
                latestMasterUrl = initialUrl
                AllohaSessionHolder.latestMasterUrl = initialUrl

                if (isRestart) {
                    // Do NOT immediately push initialUrl when restarting/recovering, because initialUrl has the
                    // preliminary token! Wait for onM3u8Refreshed to deliver the live signed master URL.
                    scope.launch {
                        delay(1200L)
                        if (latestMasterUrl == initialUrl) {
                            Log.d(TAG, "Fallback to initialUrl after 1200ms delay: $initialUrl")
                            onM3u8Updated?.invoke(initialUrl)
                            onSessionRefreshed?.invoke(result.qualityMap, initialUrl, activeHeaders)
                        }
                    }
                } else {
                    onStreamReady?.invoke(result.qualityMap, initialUrl)
                    onStreamReadyWithHeaders?.invoke(result.qualityMap, initialUrl, activeHeaders)
                }
            }

            override fun onConfigUpdate(edgeHash: String, ttlSeconds: Int, extraHeaders: Map<String, String>) {
                _activeHeaders.putAll(extraHeaders)
                _activeHeaders["accepts-controls"] = edgeHash
                _activeHeaders["Accepts-Controls"] = edgeHash
                Log.d(TAG, "Dynamic edge_hash updated: $edgeHash (ttl=${ttlSeconds}s)")
            }

            override fun onM3u8Refreshed(url: String, extraHeaders: Map<String, String>) {
                timeoutJob.cancel()
                failureCount = 0
                _activeHeaders.putAll(extraHeaders)
                latestMasterUrl = url
                AllohaSessionHolder.latestMasterUrl = url
                Log.d(TAG, "CDN master URL refreshed: $url")
                onM3u8Updated?.invoke(url)
                if (isRestarting) {
                    isRestarting = false
                    onSessionRefreshed?.invoke(lastQualityMap, url, activeHeaders)
                }
            }

            override fun onStreamHeadersUpdated(extraHeaders: Map<String, String>) {
                _activeHeaders.putAll(extraHeaders)
            }

            override fun onError(error: String) {
                timeoutJob.cancel()
                if (attempt < 3) {
                    parser?.rotateUserAgent()
                    startSession(iframeUrl, isRestart, attempt = attempt + 1)
                } else {
                    this@AllohaSessionManager.onError?.invoke(error)
                }
            }
        })
    }

    fun switchQuality(qualityKey: String): String? {
        val url = lastQualityMap[qualityKey] ?: return null
        lastSelectedQuality = qualityKey
        return url
    }

    fun release() {
        scope.cancel()
        parser?.release()
        parser = null
        _activeHeaders.clear()
        Log.d(TAG, "AllohaSessionManager released")
    }

    data class BnsiResult(
        val qualityMap: Map<String, String>,
        val defaultUrl: String,
        val subtitles: List<Triple<String, String, String>> = emptyList(),
        val skipRanges: List<LongRange> = emptyList()
    )

    private fun extractHlsSource(rootObj: JSONObject): JSONArray? {
        if (rootObj.has("hlsSource")) {
            return rootObj.optJSONArray("hlsSource")
        }
        val wrapperKeys = listOf("data", "result", "payload", "response", "content", "body")
        for (key in wrapperKeys) {
            val nested = rootObj.optJSONObject(key)
            if (nested != null && nested.has("hlsSource")) {
                return nested.optJSONArray("hlsSource")
            }
        }
        return null
    }

    private fun parseBnsiJson(json: String): BnsiResult? = try {
        val trimmed = json.trim()
        val startIdx = trimmed.indexOf('{')
        val endIdx = trimmed.lastIndexOf('}')
        val cleanJson = if (startIdx != -1 && endIdx > startIdx) trimmed.substring(startIdx, endIdx + 1) else trimmed

        val root = JSONObject(cleanJson)
        val hlsSourceArray = extractHlsSource(root) ?: root.optJSONArray("hlsSource")
        val hlsSource = hlsSourceArray?.optJSONObject(0) ?: root

        val qualityMap = mutableMapOf<String, String>()
        var masterUrl: String? = null

        if (hlsSource.has("quality")) {
            val qualityObj = hlsSource.getJSONObject("quality")
            val keys = qualityObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val raw = qualityObj.getString(k)
                val url = if (raw.contains(" or ")) raw.split(" or ")[0].trim() else raw.trim()

                if (url.contains("master.m3u8", ignoreCase = true) && masterUrl == null) {
                    masterUrl = url
                }

                val normKey = when {
                    k.contains("2160") || k.contains("4k", ignoreCase = true) -> "4K"
                    k.contains("1440") || k.contains("2k", ignoreCase = true) -> "2K"
                    k.contains("1080") -> "1080p"
                    k.contains("720") -> "720p"
                    k.contains("480") -> "480p"
                    k.contains("360") -> "360p"
                    k.endsWith("p", ignoreCase = true) -> k
                    else -> "${k}p"
                }
                qualityMap[normKey] = url
            }
        }

        val defaultUrl = qualityMap["1080p"]
            ?: qualityMap["720p"]
            ?: qualityMap["480p"]
            ?: qualityMap["360p"]
            ?: qualityMap.values.firstOrNull { !it.contains("master.m3u8", ignoreCase = true) }
            ?: masterUrl
            ?: qualityMap.values.firstOrNull()
            ?: ""

        // Parse subtitles
        val subtitles = mutableListOf<Triple<String, String, String>>()
        if (hlsSource.has("tracks")) {
            val tracks = hlsSource.optJSONObject("tracks")
            if (tracks != null) {
                val tKeys = tracks.keys()
                while (tKeys.hasNext()) {
                    val lang = tKeys.next()
                    val trackUrl = tracks.optString(lang, "")
                    if (trackUrl.isNotBlank()) {
                        val label = when (lang.lowercase()) {
                            "rus", "ru" -> "Русские"
                            "eng", "en" -> "English"
                            "ukr", "uk" -> "Українська"
                            else -> lang.replaceFirstChar { it.uppercase() }
                        }
                        subtitles.add(Triple(lang, label, trackUrl))
                    }
                }
            }
        }

        // Parse skipTime (String, Array, or Object)
        val skipRanges = mutableListOf<LongRange>()
        val skipTimeRaw = hlsSource.opt("skipTime") ?: root.opt("skipTime")
        if (skipTimeRaw is String && skipTimeRaw.isNotBlank()) {
            // e.g. "0-90,3453-3607"
            val parts = skipTimeRaw.split(",")
            for (part in parts) {
                val times = part.split("-")
                if (times.size == 2) {
                    val s = times[0].trim().toDoubleOrNull()?.toLong() ?: -1L
                    val e = times[1].trim().toDoubleOrNull()?.toLong() ?: -1L
                    if (s in 0 until e) skipRanges.add(s..e)
                }
            }
        } else if (skipTimeRaw is JSONArray) {
            for (i in 0 until skipTimeRaw.length()) {
                val item = skipTimeRaw.opt(i)
                if (item is JSONObject) {
                    val start = item.optLong("start", -1L)
                    val end = item.optLong("end", -1L)
                    if (start in 0 until end) skipRanges.add(start..end)
                } else if (item is String) {
                    val times = item.split("-")
                    if (times.size == 2) {
                        val s = times[0].trim().toDoubleOrNull()?.toLong() ?: -1L
                        val e = times[1].trim().toDoubleOrNull()?.toLong() ?: -1L
                        if (s in 0 until e) skipRanges.add(s..e)
                    }
                }
            }
        } else if (skipTimeRaw is JSONObject) {
            val intro = skipTimeRaw.optJSONObject("intro")
            if (intro != null) {
                val s = intro.optDouble("start", -1.0).toLong()
                val e = intro.optDouble("end", -1.0).toLong()
                if (s in 0 until e) skipRanges.add(s..e)
            }
            val outro = skipTimeRaw.optJSONObject("outro")
            if (outro != null) {
                val s = outro.optDouble("start", -1.0).toLong()
                val e = outro.optDouble("end", -1.0).toLong()
                if (s in 0 until e) skipRanges.add(s..e)
            }
        }

        BnsiResult(qualityMap, defaultUrl, subtitles, skipRanges)
    } catch (e: Exception) {
        Log.e(TAG, "parseBnsiJson error: ${e.message}")
        null
    }

    private fun pickBestQuality(qualityMap: Map<String, String>): String {
        val preferred = listOf("1080p", "720p", "480p", "360p", "4K", "2K")
        return preferred.firstOrNull { qualityMap.containsKey(it) } ?: qualityMap.keys.firstOrNull() ?: "1080p"
    }
}
