package com.slooshfilm.app.data.alloha

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.net.URL
import java.util.Locale

class AllohaParser(context: Context) {

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
    )
    private var uaIndex = (0 until userAgents.size).random()
    val userAgent get() = userAgents[uaIndex]
    val lastUserAgent get() = userAgent

    var lastIframeUrl: String = ""

    fun rotateUserAgent() {
        uaIndex = (uaIndex + 1) % userAgents.size
        Handler(Looper.getMainLooper()).post {
            webView.settings.userAgentString = userAgent
        }
        Log.d("AllohaParser", "UA rotated to: ${userAgent.take(60)}")
    }

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgent
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
        }
        keepScreenOn = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(BOOTSTRAP_SCRIPT, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(BOOTSTRAP_SCRIPT, null)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                // Bypass CORS for Android WebView default video poster
                if (url.startsWith("android-webview-video-poster")) {
                    val posterHeaders = mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Content-Type" to "image/png"
                    )
                    return WebResourceResponse(
                        "image/png",
                        "UTF-8",
                        200,
                        "OK",
                        posterHeaders,
                        java.io.ByteArrayInputStream(ByteArray(0))
                    )
                }

                return super.shouldInterceptRequest(view, request)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): Bitmap {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.TRANSPARENT)
                }
            }
        }

        // Inject script at document start for ALL frames (main and cross-origin iframes)
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(
                    this,
                    BOOTSTRAP_SCRIPT,
                    setOf("*")
                )
                Log.d("AllohaParser", "addDocumentStartJavaScript registered successfully")
            }
        } catch (e: Exception) {
            Log.w("AllohaParser", "Failed to register addDocumentStartJavaScript: ${e.message}")
        }
    }

    interface Callback {
        fun onHlsLinksReceived(json: String, extraHeaders: Map<String, String>)
        fun onConfigUpdate(edgeHash: String, ttlSeconds: Int, extraHeaders: Map<String, String>)
        fun onM3u8Refreshed(url: String, extraHeaders: Map<String, String>)
        fun onStreamHeadersUpdated(extraHeaders: Map<String, String>) {}
        fun onError(error: String)
    }

    @SuppressLint("AddJavascriptInterface")
    fun parse(iframeUrl: String, callback: Callback) {
        lastIframeUrl = iframeUrl
        Handler(Looper.getMainLooper()).post {
            webView.onResume()
            webView.resumeTimers()

            webView.removeJavascriptInterface("AndroidBridge")
            webView.addJavascriptInterface(object : Any() {
                private var isParsed = false

                @JavascriptInterface
                fun onReady(jsonResponse: String, headersJson: String) {
                    if (isParsed) return
                    isParsed = true
                    val extraHeaders = parseHeaders(headersJson)
                    Handler(Looper.getMainLooper()).post {
                        CookieManager.getInstance().flush()
                        callback.onHlsLinksReceived(jsonResponse, extraHeaders)
                    }
                }

                @JavascriptInterface
                fun onConfigUpdate(edgeHash: String, ttl: Int, headersJson: String) {
                    val extraHeaders = parseHeaders(headersJson)
                    Log.d("AllohaParser", "config_update hash=$edgeHash ttl=${ttl}s")
                    Handler(Looper.getMainLooper()).post {
                        callback.onConfigUpdate(edgeHash, ttl, extraHeaders)
                    }
                }

                @JavascriptInterface
                fun onM3u8Refreshed(url: String, headersJson: String) {
                    val extraHeaders = parseHeaders(headersJson)
                    Log.d("AllohaParser", "m3u8 refreshed url=$url")
                    Handler(Looper.getMainLooper()).post {
                        callback.onM3u8Refreshed(url, extraHeaders)
                    }
                }

                @JavascriptInterface
                fun onLog(msg: String) {
                    Log.d("AllohaParserJS", msg)
                }

                private fun parseHeaders(headersJson: String): Map<String, String> {
                    val map = mutableMapOf<String, String>()
                    try {
                        val obj = JSONObject(headersJson)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = obj.getString(k)
                        }
                    } catch (_: Exception) {}
                    return map
                }

                @JavascriptInterface
                fun onStreamHeaders(headersJson: String) {
                    val extraHeaders = parseHeaders(headersJson)
                    Handler(Looper.getMainLooper()).post {
                        CookieManager.getInstance().flush()
                        callback.onStreamHeadersUpdated(extraHeaders)
                    }
                }
            }, "AndroidBridge")

            val wrapperHtml = buildWrapperHtml(iframeUrl)
            val parsedUrl = URL(iframeUrl)
            val baseUrl = "${parsedUrl.protocol}://${parsedUrl.host.lowercase(Locale.ROOT)}/"
            webView.loadDataWithBaseURL(baseUrl, wrapperHtml, "text/html", "UTF-8", null)
        }
    }

    fun detachFromParent() {
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
    }

    fun release() {
        Handler(Looper.getMainLooper()).post {
            detachFromParent()
            webView.destroy()
        }
    }

    private fun buildWrapperHtml(iframeUrl: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script>
                $BOOTSTRAP_SCRIPT
            </script>
        </head>
        <body style="margin:0;padding:0;background:black;overflow:hidden;">
            <iframe id="alloha_iframe" src="$iframeUrl" width="100%" height="100%" frameborder="0" allow="autoplay; fullscreen" allowfullscreen></iframe>
            <script>
                try {
                    var iframe = document.getElementById('alloha_iframe');
                    iframe.onload = function() {
                        try {
                            var iframeWin = iframe.contentWindow;
                            if (iframeWin && !iframeWin.__slooshAllohaHooked) {
                                var s = iframeWin.document.createElement('script');
                                s.textContent = $BOOTSTRAP_SCRIPT_JSON;
                                iframeWin.document.documentElement.appendChild(s);
                            }
                        } catch(e) {}
                    };
                } catch(e) {}
            </script>
        </body>
        </html>
    """.trimIndent()

    companion object {
        val BOOTSTRAP_SCRIPT = """
            (function() {
                if (window.__slooshAllohaHooked) return;
                window.__slooshAllohaHooked = true;

                var capturedHeaders = {};
                var isDone = false;
                var lastM3u8Url = null;

                try {
                    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                    Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                } catch(e) {}

                // Prevent background WebView from spawning duplicate MediaCodec hardware decoders & Mali graphic errors
                try {
                    HTMLMediaElement.prototype.play = function() {
                        this.muted = true;
                        return Promise.resolve();
                    };
                } catch(e) {}

                // Strip poster attributes and properties to silence android-webview-video-poster CORS blocks
                try {
                    if (typeof HTMLVideoElement !== 'undefined' && HTMLVideoElement.prototype) {
                        var desc = Object.getOwnPropertyDescriptor(HTMLVideoElement.prototype, 'poster');
                        if (!desc || desc.configurable) {
                            Object.defineProperty(HTMLVideoElement.prototype, 'poster', {
                                get: function() { return ''; },
                                set: function(v) { /* no-op */ },
                                configurable: true
                            });
                        }
                    }
                } catch(e) {}
                try {
                    var observer = new MutationObserver(function() {
                        var videos = document.querySelectorAll('video');
                        for (var i = 0; i < videos.length; i++) {
                            if (videos[i].hasAttribute('poster')) {
                                videos[i].removeAttribute('poster');
                            }
                        }
                    });
                    observer.observe(document.documentElement, { childList: true, subtree: true });
                } catch(e) {}

                function putHeader(name, value) {
                    if (!name || !value) return;
                    capturedHeaders[String(name).toLowerCase()] = String(value);
                    try {
                        if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onStreamHeaders) {
                            AndroidBridge.onStreamHeaders(JSON.stringify(capturedHeaders));
                        }
                    } catch(e) {}
                }

                try {
                    putHeader('origin', window.location.origin);
                    putHeader('referer', window.location.origin + '/');
                    putHeader('user-agent', navigator.userAgent);
                    putHeader('accept', '*/*');
                    putHeader('sec-fetch-dest', 'empty');
                    putHeader('sec-fetch-mode', 'cors');
                    putHeader('sec-fetch-site', 'cross-site');
                } catch(e) {}

                function checkPayload(text) {
                    if (!text || isDone) return;
                    if (text.indexOf('hlsSource') !== -1 || text.indexOf('/bnsi/') !== -1) {
                        isDone = true;
                        try {
                            if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onReady) {
                                AndroidBridge.onReady(text, JSON.stringify(capturedHeaders));
                            }
                        } catch(e) {}
                    }
                }

                // Scan globals
                try {
                    if (window.hlsSource) {
                        checkPayload(JSON.stringify({ hlsSource: window.hlsSource }));
                    }
                } catch(e) {}

                function isPlayableM3u8(u) {
                    if (!u || typeof u !== 'string') return false;
                    var lower = u.toLowerCase();
                    if (lower.indexOf('blank.mp4') !== -1 || lower.indexOf('cdn.plyr.io') !== -1) return false;
                    return lower.indexOf('.m3u8') !== -1 || lower.indexOf('/hls/') !== -1;
                }

                // Hook XMLHttpRequest
                try {
                    var origOpen = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        this.__slooshUrl = url || '';
                        if (isPlayableM3u8(url) && url !== lastM3u8Url) {
                            lastM3u8Url = url;
                            try {
                                if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onM3u8Refreshed) {
                                    AndroidBridge.onM3u8Refreshed(url, JSON.stringify(capturedHeaders));
                                }
                            } catch(e) {}
                        }
                        this.addEventListener('load', function() {
                            var rUrl = this.responseURL || this.__slooshUrl || '';
                            var respText = '';
                            try { respText = this.responseText || ''; } catch(e) {}

                            if (rUrl.indexOf('/bnsi/') !== -1 || respText.indexOf('hlsSource') !== -1) {
                                checkPayload(respText);
                            }
                            if (isPlayableM3u8(rUrl) && rUrl !== lastM3u8Url) {
                                lastM3u8Url = rUrl;
                                try {
                                    if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onM3u8Refreshed) {
                                        AndroidBridge.onM3u8Refreshed(rUrl, JSON.stringify(capturedHeaders));
                                    }
                                } catch(e) {}
                            }
                        });
                        return origOpen.apply(this, arguments);
                    };

                    var origSetHeader = XMLHttpRequest.prototype.setRequestHeader;
                    XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                        putHeader(name, value);
                        return origSetHeader.apply(this, arguments);
                    };
                } catch(e) {}

                // Hook Fetch
                try {
                    var origFetch = window.fetch;
                    if (origFetch) {
                        window.fetch = function(input, init) {
                            var url = '';
                            try {
                                url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                                if (init && init.headers) {
                                    if (typeof init.headers.forEach === 'function') {
                                        init.headers.forEach(function(v, k) { putHeader(k, v); });
                                    } else {
                                        for (var hk in init.headers) { putHeader(hk, init.headers[hk]); }
                                    }
                                }
                                if (isPlayableM3u8(url) && url !== lastM3u8Url) {
                                    lastM3u8Url = url;
                                    if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onM3u8Refreshed) {
                                        AndroidBridge.onM3u8Refreshed(url, JSON.stringify(capturedHeaders));
                                    }
                                }
                            } catch(e) {}

                            return origFetch.apply(this, arguments).then(function(resp) {
                                try {
                                    var rUrl = resp.url || '';
                                    if (rUrl.indexOf('/bnsi/') !== -1 || isPlayableM3u8(rUrl)) {
                                        resp.clone().text().then(function(t) {
                                            checkPayload(t);
                                            if (isPlayableM3u8(rUrl) && rUrl !== lastM3u8Url) {
                                                lastM3u8Url = rUrl;
                                                if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onM3u8Refreshed) {
                                                    AndroidBridge.onM3u8Refreshed(rUrl, JSON.stringify(capturedHeaders));
                                                }
                                            }
                                        }).catch(function(){});
                                    }
                                } catch(e) {}
                                return resp;
                            });
                        };
                    }
                } catch(e) {}

                // Hook WebSocket — wrap constructor to attach message listener BEFORE server sends config_update.
                // The Alloha server sends config_update immediately on connect, before the client calls send().
                // Hooking prototype.send misses this initial message. Constructor wrapping fixes the race condition.
                try {
                    var OrigWebSocket = window.WebSocket;
                    var lastEdgeHash = null;
                    var heartbeatTimer = null;

                    window.WebSocket = function(url, protocols) {
                        var ws = protocols !== undefined ? new OrigWebSocket(url, protocols) : new OrigWebSocket(url);
                        var sessionStart = Date.now();

                        // Attach message listener immediately at construction time — before any data arrives
                        ws.addEventListener('message', function(event) {
                            try {
                                var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
                                if (msg && msg.type === 'config_update' && msg.edge_hash) {
                                    if (msg.edge_hash !== lastEdgeHash) {
                                        lastEdgeHash = msg.edge_hash;
                                        capturedHeaders['accepts-controls'] = msg.edge_hash;
                                        var ttl = msg.ttl || 120;
                                        try {
                                            if (typeof AndroidBridge !== 'undefined' && AndroidBridge.onConfigUpdate) {
                                                AndroidBridge.onConfigUpdate(msg.edge_hash, ttl, JSON.stringify(capturedHeaders));
                                            }
                                        } catch(e) {}
                                    }
                                }
                                if (typeof event.data === 'string' && event.data.indexOf('hlsSource') !== -1) {
                                    checkPayload(event.data);
                                }
                            } catch(e) {}
                        });

                        ws.addEventListener('close', function() {
                            if (heartbeatTimer) clearInterval(heartbeatTimer);
                        });

                        if (heartbeatTimer) clearInterval(heartbeatTimer);
                        heartbeatTimer = setInterval(function() {
                            if (ws.readyState !== 1) return;
                            var t = Math.floor((Date.now() - sessionStart) / 1000);
                            try {
                                ws.send(JSON.stringify({
                                    type: 'playing',
                                    current_time: t,
                                    resolution: '1080',
                                    track_id: '1',
                                    speed: 1,
                                    subtitle: 0,
                                    ts: Date.now()
                                }));
                            } catch(e) {}
                        }, 18000);

                        return ws;
                    };
                    // Copy static properties from original WebSocket
                    window.WebSocket.prototype = OrigWebSocket.prototype;
                    window.WebSocket.CONNECTING = OrigWebSocket.CONNECTING;
                    window.WebSocket.OPEN = OrigWebSocket.OPEN;
                    window.WebSocket.CLOSING = OrigWebSocket.CLOSING;
                    window.WebSocket.CLOSED = OrigWebSocket.CLOSED;
                } catch(e) {}

                // Safe play button trigger to initiate HLS manifest fetch
                function triggerPlay() {
                    try {
                        var btn = document.querySelector('.allplay__play-btn') || 
                                  document.querySelector('.play-btn') || 
                                  document.querySelector('[class*="play"]');
                        if (btn && typeof btn.click === 'function') btn.click();
                        var video = document.querySelector('video');
                        if (video) {
                            video.muted = true;
                        }
                    } catch(e) {}
                }

                triggerPlay();
                var pollCount = 0;
                var pollInterval = setInterval(function() {
                    pollCount++;
                    if (!isDone && pollCount < 20) {
                        triggerPlay();
                    } else {
                        clearInterval(pollInterval);
                    }
                }, 500);
            })();
        """.trimIndent()

        val BOOTSTRAP_SCRIPT_JSON = JSONObject.quote(BOOTSTRAP_SCRIPT)
    }
}
