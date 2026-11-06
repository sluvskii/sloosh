package com.slooshfilm.app.data.alloha

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AllohaRuntimeResolver(private val context: Context) {
    companion object {
        private val iframeCache = ConcurrentHashMap<String, Pair<Map<String, Any>, Long>>()
        private const val CACHE_TTL_MS = 20_000L // 20 seconds (matches iOS 1:1)

        fun invalidateCache(iframeUrl: String) {
            iframeCache.remove(iframeUrl)
            DynamicHttpDataSource.invalidateMasterCache()
        }

        fun invalidateAllCache() {
            iframeCache.clear()
            DynamicHttpDataSource.invalidateMasterCache()
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
    private var hostView: View? = null
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

    private fun findActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun start(iframeUrl: String) {
        val wv = WebView(context)
        webView = wv
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            userAgentString = activeUserAgent
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(wv, true)

        wv.addJavascriptInterface(Bridge(), "AndroidAllohaResolver")

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            runCatching {
                WebViewCompat.addDocumentStartJavaScript(wv, BOOTSTRAP_SCRIPT, setOf("*"))
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): Bitmap? {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            }
        }
        wv.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(BOOTSTRAP_SCRIPT, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(BOOTSTRAP_SCRIPT, null)
            }
        }

        // Attach to View hierarchy to prevent Chromium from throttling background timers/WebSockets
        val activity = findActivity(context)
        val decorView = activity?.window?.decorView as? ViewGroup
        if (decorView != null) {
            val host = FrameLayout(context).apply {
                visibility = View.INVISIBLE
                alpha = 0f
            }
            decorView.addView(host, ViewGroup.LayoutParams(2, 2))
            host.addView(wv, ViewGroup.LayoutParams(2, 2))
            hostView = host
        }

        startTimeout()

        val baseOrigin = try {
            val uri = Uri.parse(iframeUrl)
            "${uri.scheme ?: "https"}://${uri.authority}/"
        } catch (_: Exception) {
            iframeUrl
        }
        wv.loadDataWithBaseURL(baseOrigin, wrapperHtml(iframeUrl), "text/html", "UTF-8", iframeUrl)
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
                    scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 500L else 2500L)
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
                scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 500L else 3000L)
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
        val baseUri = try { URI(base) } catch (_: Exception) { null }

        // Collect subtitles from ALL payloads matching iOS 1:1
        val allSubtitles = mutableListOf<Map<String, String>>()
        if (baseUri != null) {
            val payloadsToScan = (pendingPayloads + listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload, fallback.ifBlank { null }))
            for (p in payloadsToScan) {
                allSubtitles.addAll(AllohaRuntimeParser.subtitleTracks(p, baseUri))
            }
        }
        val seenSubUrls = mutableSetOf<String>()
        val uniqueSubtitles = allSubtitles.filter { sub ->
            val u = sub["url"].orEmpty()
            u.isNotBlank() && seenSubUrls.add(u)
        }

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
                    "subtitles" to (if (uniqueSubtitles.isNotEmpty()) uniqueSubtitles else (parsed["subtitles"] ?: emptyList<Any>())),
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
                "subtitles" to uniqueSubtitles,
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
                "subtitles" to uniqueSubtitles,
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
        val host = hostView
        hostView = null
        if (view != null || host != null) {
            mainHandler.post {
                runCatching {
                    host?.let {
                        (it.parent as? ViewGroup)?.removeView(it)
                        (it as? ViewGroup)?.removeAllViews()
                    }
                    if (view != null) {
                        (view.parent as? ViewGroup)?.removeView(view)
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
    }

    private fun wrapperHtml(url: String): String {
        return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
            <meta name="referrer" content="unsafe-url">
            <style>html, body, iframe { margin:0; width:100%; height:100%; background:#000; overflow:hidden; }</style>
        </head>
        <body>
            <iframe id="alloha_iframe" src="$url" referrerpolicy="unsafe-url" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen frameborder="0"></iframe>
        </body>
        </html>
        """.trimIndent()
    }

    private val BOOTSTRAP_SCRIPT = """
    (function() {
      if (window.__neoAllohaResolverInstalled) return;
      window.__neoAllohaResolverInstalled = true;

      var capturedHeaders = {};
      var lastPayload = '';
      var lastM3u8 = '';

      function post(type, payload) {
        try {
          if (window.AndroidAllohaResolver && window.AndroidAllohaResolver.post) {
            window.AndroidAllohaResolver.post(JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders }));
          }
        } catch(e) {}
      }

      function putHeader(name, value) {
        if (!name || !value) return;
        capturedHeaders[String(name).toLowerCase()] = String(value);
      }

      function defaultHeaders(win) {
        try {
          if (!win || !win.location) return;
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
        return typeof text === 'string' && (
          text.indexOf('hlsSource') !== -1 ||
          text.indexOf('.m3u8') !== -1 ||
          text.indexOf('.mp4') !== -1 ||
          text.indexOf('.vtt') !== -1 ||
          text.indexOf('.srt') !== -1 ||
          text.indexOf('subtitles') !== -1 ||
          text.indexOf('subtitle') !== -1
        );
      }

      function report(payload) {
        if (!looksPlayable(payload)) return;
        if (payload === lastPayload) return;
        lastPayload = payload;
        post('payload', payload);
      }

      function scan(win) {
        try {
          if (!win) return;
          defaultHeaders(win);
          var chunks = [];
          if (win.location && win.location.href) chunks.push(win.location.href);
          if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
          var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
          for (var i = 0; i < media.length; i++) chunks.push(media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '');
          if (win.performance && win.performance.getEntriesByType) {
            var entries = win.performance.getEntriesByType('resource');
            for (var p = 0; p < entries.length; p++) chunks.push(entries[p].name || '');
          }
          if (win.hlsSource) {
            try { chunks.push(JSON.stringify({ hlsSource: win.hlsSource })); } catch(e) {}
          }
          try {
            var tracks = win.document ? win.document.querySelectorAll('track') : [];
            var extractedTracks = [];
            for (var t = 0; t < tracks.length; t++) {
              var tr = tracks[t];
              var src = tr.src || tr.getAttribute('src');
              if (src) {
                var label = tr.label || tr.getAttribute('label') || tr.srclang || 'Субтитры';
                extractedTracks.push({ name: label, url: src });
              }
            }
            if (extractedTracks.length > 0) {
              chunks.push(JSON.stringify({ subtitles: extractedTracks }));
            }
            if (win.playerjs_subtitle || win.pjssubtitle) {
              chunks.push(JSON.stringify({ subtitles: win.playerjs_subtitle || win.pjssubtitle }));
            }
            if (win.player && (win.player.subtitle || win.player.subtitles || win.player.tracks)) {
              chunks.push(JSON.stringify({ subtitles: win.player.subtitle || win.player.subtitles || win.player.tracks }));
            }
            if (win.alloha && (win.alloha.subtitles || win.alloha.subtitle)) {
              chunks.push(JSON.stringify({ subtitles: win.alloha.subtitles || win.alloha.subtitle }));
            }
            var scripts = win.document ? win.document.querySelectorAll('script') : [];
            for (var s = 0; s < scripts.length; s++) {
              var st = scripts[s].textContent || '';
              if (looksPlayable(st)) {
                chunks.push(st);
              }
            }
          } catch(e) {}
          report(chunks.join('\n'));
        } catch(e) {}
      }

      function install(win) {
        try {
          if (!win || win.__neoAllohaHooksInstalled) return;
          win.__neoAllohaHooksInstalled = true;
          defaultHeaders(win);

          try {
            var _pjs = win.Playerjs;
            if (_pjs && !win.__neoPlayerjsHooked) {
              win.__neoPlayerjsHooked = true;
              win.Playerjs = function(cfg) {
                try {
                  if (cfg) {
                    if (cfg.file) report(cfg.file);
                    if (cfg.subtitle) report(JSON.stringify({ subtitles: cfg.subtitle }));
                    if (cfg.subtitles) report(JSON.stringify({ subtitles: cfg.subtitles }));
                  }
                } catch(e) {}
                return new _pjs(cfg);
              };
              win.Playerjs.prototype = _pjs.prototype;
            }
          } catch(e) {}

          var OriginalWS = win.WebSocket;
          if (OriginalWS && !win.__neoAllohaWsCtorHooked) {
            win.__neoAllohaWsCtorHooked = true;
            var PatchedWS = function(url, protocols) {
              var ws = protocols !== undefined ? new OriginalWS(url, protocols) : new OriginalWS(url);
              try {
                ws.addEventListener('message', function(event) {
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
              } catch(e) {}
              return ws;
            };
            PatchedWS.prototype = OriginalWS.prototype;
            for (var prop in OriginalWS) {
              if (OriginalWS.hasOwnProperty(prop)) {
                try { PatchedWS[prop] = OriginalWS[prop]; } catch(e) {}
              }
            }
            win.WebSocket = PatchedWS;
          }

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
                if (looksPlayable(responseText)) report(responseText);
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
                  clone.text().then(function(text) { report(text); }).catch(function(){});
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
        scan(window);
        try {
          var frames = document.querySelectorAll('iframe');
          for (var i = 0; i < frames.length; i++) {
            try {
              install(frames[i].contentWindow);
              scan(frames[i].contentWindow);
            } catch(e) {}
          }
        } catch(e) {}
      }

      install(window);
      scan(window);
      tick();
      setInterval(tick, 300);
      window.addEventListener('load', tick);
    })();
    """.trimIndent()
}
