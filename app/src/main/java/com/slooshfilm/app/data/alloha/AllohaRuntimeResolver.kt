package com.slooshfilm.app.data.alloha

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AllohaRuntimeResolver(private val context: Context) {
    companion object {
        private val iframeCache = ConcurrentHashMap<String, Pair<Map<String, Any>, Long>>()
        private const val CACHE_TTL_MS = 20_000L // 20 seconds (matches iOS)

        fun invalidateCache(iframeUrl: String) {
            iframeCache.remove(iframeUrl)
        }

        fun invalidateAllCache() {
            iframeCache.clear()
        }

        private var uaIndex = 0
        private val userAgents = listOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
        )

        @Synchronized
        fun nextUserAgent(): String {
            val idx = uaIndex % userAgents.size
            uaIndex++
            return userAgents[idx]
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var finished = false
    private val headers = linkedMapOf<String, String>()
    private val pendingPayloads = ArrayDeque<String>()
    private var bestMasterPayload: String? = null
    private var bestHlsSourcePayload: String? = null
    private var bestDirectPayload: String? = null
    private var baseUrl: String? = null
    private var deferred: CompletableDeferred<Map<String, Any>>? = null
    private var timeoutRunnable: Runnable? = null
    private var fallbackRunnable: Runnable? = null
    var activeUserAgent: String = nextUserAgent()
        private set

    suspend fun resolve(iframeUrl: String): Map<String, Any> {
        val cached = iframeCache[iframeUrl]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            android.util.Log.d("AllohaResolver", "Using cached stream resolution for $iframeUrl")
            return cached.first
        }

        URL(iframeUrl)
        baseUrl = iframeUrl
        val waiter = CompletableDeferred<Map<String, Any>>()
        deferred = waiter
        withContext(Dispatchers.Main) { start(iframeUrl) }
        return waiter.await()
    }

    fun cancel() {
        if (!finished) {
            finishError("Операция отменена")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun start(iframeUrl: String) {
        val wv = WebView(context)
        webView = wv
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        wv.settings.userAgentString = activeUserAgent

        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(wv, true)

        wv.addJavascriptInterface(Bridge(), "AndroidAllohaResolver")
        wv.webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ALPHA_8)
            }
        }
        wv.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        }
        startTimeout()
        wv.loadDataWithBaseURL(iframeUrl, wrapperHtml(iframeUrl), "text/html", "UTF-8", iframeUrl)
    }

    private fun startTimeout() {
        val timeout = Runnable {
            if (!finished) finishError("Поток временно недоступен (таймаут ответа)")
        }
        timeoutRunnable = timeout
        mainHandler.postDelayed(timeout, 20_000)
    }

    private inner class Bridge {
        @JavascriptInterface
        fun post(raw: String?) {
            if (raw.isNullOrBlank() || finished) return
            runCatching {
                val obj = JSONObject(raw)
                val incomingHeaders = obj.optJSONObject("headers")
                if (incomingHeaders != null) {
                    val keys = incomingHeaders.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        headers[key.lowercase(Locale.ROOT)] = incomingHeaders.optString(key)
                    }
                    resolveBestPayloadIfReady()
                }

                val payload = obj.optString("payload")
                if (payload.isBlank()) return@runCatching
                pendingPayloads.addLast(payload)
                while (pendingPayloads.size > 12) pendingPayloads.removeFirst()

                if (payload.trimStart().startsWith("{") && payload.contains("\"hlsSource\"")) {
                    bestHlsSourcePayload = payload
                    scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 800L else 2500L)
                    if (hasAllohaPlaybackHeaders()) {
                        resolveBestAvailablePayload(payload)
                    }
                    return@runCatching
                }

                if (isMasterPlaylistPayload(payload)) {
                    bestMasterPayload = payload
                    scheduleFallbackResolve(payload, if (bestHlsSourcePayload == null) 2400L else 800L)
                    return@runCatching
                }

                resolveIfReady(payload)
            }
        }
    }

    private fun resolveIfReady(payload: String) {
        if (finished) return
        when {
            payload.trimStart().startsWith("{") && payload.contains("\"hlsSource\"") -> {
                bestHlsSourcePayload = payload
                scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 1000L else 3000L)
            }
            isPlayablePayload(payload) -> {
                bestDirectPayload = payload
                scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 1500L else 4000L)
            }
        }
    }

    private fun resolveBestPayloadIfReady() {
        if (finished) return
        if (hasAllohaPlaybackHeaders() && (bestHlsSourcePayload != null || bestMasterPayload != null)) {
            resolveBestAvailablePayload(bestHlsSourcePayload ?: bestMasterPayload.orEmpty())
        } else if (hasAllohaPlaybackHeaders() && bestDirectPayload != null) {
            resolveBestAvailablePayload(bestDirectPayload.orEmpty())
        }
    }

    private fun isCleanVideoUrl(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val trimmed = candidate.trim()
        if (trimmed.length > 4096) return false
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        if (trimmed.any { it <= ' ' || it == '<' || it == '>' || it == '"' || it == '\'' || it == '{' || it == '}' || it == '[' || it == ']' }) {
            return false
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.contains("<html") || lower.contains("<!doctype") || lower.contains("function(") || lower.contains("var ")) {
            return false
        }
        return lower.contains(".m3u8") ||
                lower.contains(".mp4") ||
                lower.contains(".mpd") ||
                lower.contains("vkvideo") ||
                lower.contains("mail.ru") ||
                lower.contains("stravers") ||
                lower.contains("alloha") ||
                lower.contains("/video") ||
                lower.contains("/stream") ||
                lower.contains("/hls")
    }

    private fun resolveBestAvailablePayload(fallback: String) {
        val base = baseUrl ?: return
        val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload, fallback.ifBlank { null })
        val seen = mutableSetOf<String>()

        for (payload in payloads) {
            if (!seen.add(payload)) continue
            val parsed = AllohaRuntimeParser.parsePayload(payload, base, headers) ?: continue

            val audioVariants = (parsed["audioVariants"] as? List<*>)?.mapNotNull { raw ->
                val item = raw as? Map<*, *> ?: return@mapNotNull null
                val url = item["url"] as? String ?: return@mapNotNull null
                if (!isCleanVideoUrl(url)) return@mapNotNull null
                val title = (item["title"] as? String)?.trim().orEmpty().ifBlank { "Unknown" }
                val qualityVariants = (item["qualityVariants"] as? List<*>) ?: emptyList<Any>()
                mapOf(
                    "title" to title,
                    "url" to url,
                    "qualityVariants" to qualityVariants
                )
            }.orEmpty()

            val chosenUrl: String? = (parsed["videoURL"] as? String)?.takeIf { isCleanVideoUrl(it) }
                ?: audioVariants.firstOrNull()?.get("url") as? String
                ?: bestMasterPayload?.takeIf { isCleanVideoUrl(it) }

            if (chosenUrl != null) {
                finishOk(mapOf(
                    "url" to chosenUrl,
                    "subtitles" to (parsed["subtitles"] ?: emptyList<Any>()),
                    "audioVariants" to audioVariants,
                    "qualityVariants" to (parsed["qualityVariants"] ?: emptyList<Any>()),
                    "headers" to headers,
                    "introRange" to (parsed["introRange"] ?: emptyMap<String, Double>()),
                    "outroRange" to (parsed["outroRange"] ?: emptyMap<String, Double>())
                ))
                return
            }
        }

        // Direct fallback to bestMasterPayload
        val masterUrl = bestMasterPayload
        if (isCleanVideoUrl(masterUrl)) {
            finishOk(mapOf(
                "url" to masterUrl!!,
                "subtitles" to emptyList<Any>(),
                "audioVariants" to emptyList<Any>(),
                "qualityVariants" to emptyList<Any>(),
                "headers" to headers,
                "introRange" to emptyMap<String, Double>(),
                "outroRange" to emptyMap<String, Double>()
            ))
            return
        }

        // Direct fallback to bestDirectPayload
        val directUrl = bestDirectPayload
        if (isCleanVideoUrl(directUrl)) {
            finishOk(mapOf(
                "url" to directUrl!!,
                "subtitles" to emptyList<Any>(),
                "audioVariants" to emptyList<Any>(),
                "qualityVariants" to emptyList<Any>(),
                "headers" to headers,
                "introRange" to emptyMap<String, Double>(),
                "outroRange" to emptyMap<String, Double>()
            ))
            return
        }
    }

    private fun scheduleFallbackResolve(payload: String, delayMs: Long) {
        fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            if (!finished) resolveBestAvailablePayload(payload)
        }
        fallbackRunnable = task
        mainHandler.postDelayed(task, delayMs)
    }

    private fun hasAllohaPlaybackHeaders(): Boolean =
        !headers["authorizations"].isNullOrBlank() || !headers["accepts-controls"].isNullOrBlank() || !headers["authorization"].isNullOrBlank()

    private fun isMasterPlaylistPayload(payload: String): Boolean {
        if (payload.contains("<html", ignoreCase = true) || payload.contains("<!doctype", ignoreCase = true)) return false
        return payload.contains("master.m3u8", ignoreCase = true)
    }

    private fun isPlayablePayload(payload: String): Boolean {
        if (payload.contains("<html", ignoreCase = true) || payload.contains("<!doctype", ignoreCase = true)) return false
        val lower = payload.lowercase(Locale.ROOT)
        if (lower.contains("blank.mp4") || lower.contains("cdn.plyr.io")) return false
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd")
    }

    private fun finishOk(result: Map<String, Any>) {
        if (finished) return
        finished = true
        baseUrl?.let {
            iframeCache[it] = Pair(result, System.currentTimeMillis())
        }
        cleanup()
        deferred?.complete(result)
        deferred = null
    }

    private fun finishError(message: String) {
        if (finished) return
        finished = true
        cleanup()
        deferred?.completeExceptionally(Exception(message))
        deferred = null
    }

    private fun cleanup() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        fallbackRunnable = null
        val view = webView
        webView = null
        if (view != null) {
            mainHandler.post {
                runCatching {
                    view.stopLoading()
                    view.clearHistory()
                    view.clearCache(true)
                    view.clearFormData()
                    view.removeJavascriptInterface("AndroidAllohaResolver")
                    view.webViewClient = WebViewClient()
                    view.webChromeClient = WebChromeClient()
                    view.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null)
                    view.destroy()
                }
            }
        }
    }

    private fun wrapperHtml(url: String): String {
        return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
            <style>html, body, iframe { margin:0; width:100%; height:100%; background:#000; overflow:hidden; }</style>
        </head>
        <body>
            <iframe id="alloha_iframe" src="$url" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen frameborder="0"></iframe>
            <script>
            (function() {
              if (window.__neoAllohaResolverInstalled) return;
              window.__neoAllohaResolverInstalled = true;
              var capturedHeaders = {};
              var lastPayload = '';
              var lastM3u8 = '';
              function post(type, payload) {
                try {
                  AndroidAllohaResolver.post(JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders }));
                } catch(e) {}
              }
              function putHeader(name, value) {
                if (!name || !value) return;
                capturedHeaders[String(name).toLowerCase()] = String(value);
              }
              function defaultHeaders(win) {
                try {
                  if (!win || win === window) return;
                  putHeader('origin', win.location.origin);
                  putHeader('referer', win.location.origin + '/');
                  putHeader('user-agent', win.navigator.userAgent);
                  putHeader('accept', '*/*');
                  putHeader('sec-fetch-dest', 'empty');
                  putHeader('sec-fetch-mode', 'cors');
                  putHeader('sec-fetch-site', 'cross-site');
                } catch(e) {}
              }
              function looksPlayable(text) {
                if (typeof text !== 'string' || !text) return false;
                return text.indexOf('hlsSource') !== -1 ||
                  text.indexOf('.m3u8') !== -1 ||
                  text.indexOf('.mp4') !== -1 ||
                  text.indexOf('.vtt') !== -1;
              }
              function report(payload) {
                if (!looksPlayable(payload)) return;
                if (payload === lastPayload) return;
                lastPayload = payload;
                post('payload', payload);
              }
              function scan(win) {
                try {
                  if (!win || win === window) return;
                  defaultHeaders(win);
                  var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
                  for (var i = 0; i < media.length; i++) {
                    var src = media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '';
                    if (src && looksPlayable(src)) post('payload', src);
                  }
                  if (win.performance && win.performance.getEntriesByType) {
                    var entries = win.performance.getEntriesByType('resource');
                    for (var p = 0; p < entries.length; p++) {
                      var entryName = entries[p].name || '';
                      if (entryName && looksPlayable(entryName)) post('payload', entryName);
                    }
                  }
                  if (win.hlsSource) {
                    try { post('payload', JSON.stringify({ hlsSource: win.hlsSource })); } catch(e) {}
                  }
                  if (win.document && win.document.documentElement) {
                    var html = win.document.documentElement.outerHTML || '';
                    if (html.indexOf('hlsSource') !== -1 || html.indexOf('/bnsi/') !== -1) {
                      report(html);
                    }
                  }
                } catch(e) {}
              }
              function install(win) {
                try {
                  if (!win || win.__neoAllohaHooksInstalled) return;
                  win.__neoAllohaHooksInstalled = true;
                  if (win !== window) defaultHeaders(win);
                  var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
                  var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
                  if (originalOpen && originalSetHeader) {
                    win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
                      this.__neoAllohaUrl = requestUrl || '';
                      this.addEventListener('load', function() {
                        var responseUrl = this.responseURL || this.__neoAllohaUrl || '';
                        var responseText = '';
                        try { responseText = this.responseText || ''; } catch(e) {}
                        if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText);
                        if (responseText && responseText.indexOf('hlsSource') !== -1) report(responseText);
                        if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) {
                          lastM3u8 = responseUrl;
                          post('payload', responseUrl);
                        }
                      });
                      return originalOpen.apply(this, arguments);
                    };
                    win.XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                      putHeader(name, value);
                      return originalSetHeader.apply(this, arguments);
                    };
                  }
                  var originalFetch = win.fetch;
                  if (originalFetch) {
                    win.fetch = function(input, init) {
                      try {
                        var requestUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                        if (init && init.headers) {
                          if (typeof init.headers.forEach === 'function') init.headers.forEach(function(value, name) { putHeader(name, value); });
                          else for (var key in init.headers) putHeader(key, init.headers[key]);
                        }
                        if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(value, name) { putHeader(name, value); });
                        if (looksPlayable(requestUrl)) post('payload', requestUrl);
                      } catch(e) {}
                      return originalFetch.apply(this, arguments).then(function(response) {
                        try {
                          var responseUrl = response.url || '';
                          if (looksPlayable(responseUrl)) post('payload', responseUrl);
                          var clone = response.clone();
                          clone.text().then(function(text) {
                            if (text.indexOf('hlsSource') !== -1) report(text);
                          }).catch(function(){});
                        } catch(e) {}
                        return response;
                      });
                    };
                  }
                  var originalSend = win.WebSocket && win.WebSocket.prototype.send;
                  if (originalSend) {
                    win.WebSocket.prototype.send = function(data) {
                      if (!this.__neoAllohaWsHooked) {
                        this.__neoAllohaWsHooked = true;
                        this.addEventListener('message', function(event) {
                          try {
                            var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
                            if (msg && msg.type === 'config_update' && msg.edge_hash) {
                              putHeader('accepts-controls', msg.edge_hash);
                              if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
                              post('headers', '');
                            }
                            if (typeof event.data === 'string' && looksPlayable(event.data)) {
                              report(event.data);
                            }
                          } catch(e) {
                            if (typeof event.data === 'string' && looksPlayable(event.data)) {
                              report(event.data);
                            }
                          }
                        });
                      }
                      return originalSend.apply(this, arguments);
                    };
                  }
                } catch(e) {}
              }
              function tick() {
                install(window);
                try {
                  var frames = document.querySelectorAll('iframe');
                  for (var i = 0; i < frames.length; i++) {
                    install(frames[i].contentWindow);
                    scan(frames[i].contentWindow);
                  }
                } catch(e) {}
              }
              tick();
              setInterval(tick, 500);
              window.addEventListener('load', tick);
            })();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
