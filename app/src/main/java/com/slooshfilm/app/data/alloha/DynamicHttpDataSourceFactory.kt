package com.slooshfilm.app.data.alloha

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.net.URL
import java.util.TreeMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Dynamic HTTP Data Source for Media3 ExoPlayer.
 *
 * Provides dynamic headers (User-Agent, Referer, and conditionally Origin/accepts-controls)
 * on every HTTP request (master playlist, variant playlists, and media segments).
 *
 * Prevents 403 Forbidden errors by ensuring third-party CDNs (like vkvideo.cloud) do not receive
 * cross-origin 'Origin' or internal 'accepts-controls' headers.
 */
@OptIn(UnstableApi::class)
class DynamicHttpDataSource(
    private val headerProvider: () -> Map<String, String>,
    private val iframeUrlProvider: () -> String?,
    private val userAgentProvider: () -> String,
    private val defaultProperties: HttpDataSource.RequestProperties,
    private val tokenRefresher: ((String) -> String?)? = null,
    private val cookieProvider: ((String) -> String?)? = null
) : HttpDataSource {

    companion object {
        private val tokenRecoveryLock = Any()
    }

    private var activeDelegate: DefaultHttpDataSource? = null
    private val transferListeners = CopyOnWriteArrayList<TransferListener>()
    private val dynamicProperties = HttpDataSource.RequestProperties()

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners.add(transferListener)
        activeDelegate?.addTransferListener(transferListener)
    }

    private fun createDelegate(targetUri: Uri?): DefaultHttpDataSource {
        val dynamicHeaders = headerProvider()
        val ua = dynamicHeaders.entries.firstOrNull { it.key.equals("user-agent", ignoreCase = true) }?.value
            ?.takeIf { it.isNotBlank() }
            ?: userAgentProvider()

        val factory = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        // Case-insensitive map to prevent duplicate headers
        val caseInsensitiveHeaders = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        caseInsensitiveHeaders.putAll(defaultProperties.snapshot)
        caseInsensitiveHeaders.putAll(dynamicProperties.snapshot)

        // Forward all dynamic headers (Origin, Referer, User-Agent, accepts-controls, authorizations, etc.)
        for ((k, v) in dynamicHeaders) {
            val lower = k.lowercase()
            if (lower == "content-type" || lower == "content-length" || lower == "host") continue
            caseInsensitiveHeaders[k] = v
        }

        // Ensure Origin and Referer are set from iframeUrl if not already present in dynamicHeaders
        val iframeUrl = iframeUrlProvider()
        if (!iframeUrl.isNullOrBlank()) {
            val iframeOrigin = try {
                val uri = Uri.parse(iframeUrl)
                val scheme = uri.scheme ?: "https"
                val authority = uri.authority
                if (!authority.isNullOrBlank()) "$scheme://$authority" else null
            } catch (e: Exception) {
                null
            }
            if (iframeOrigin != null) {
                if (caseInsensitiveHeaders["Origin"].isNullOrBlank()) {
                    caseInsensitiveHeaders["Origin"] = iframeOrigin
                }
                if (caseInsensitiveHeaders["Referer"].isNullOrBlank()) {
                    caseInsensitiveHeaders["Referer"] = "$iframeOrigin/"
                }
            }
        }

        // Standard CORS and fetch headers required by streaming CDNs (e.g. vkvideo.cloud)
        if (caseInsensitiveHeaders["Accept"].isNullOrBlank()) {
            caseInsensitiveHeaders["Accept"] = "*/*"
        }
        if (caseInsensitiveHeaders["Sec-Fetch-Mode"].isNullOrBlank()) {
            caseInsensitiveHeaders["Sec-Fetch-Mode"] = "cors"
        }
        if (caseInsensitiveHeaders["Sec-Fetch-Site"].isNullOrBlank()) {
            caseInsensitiveHeaders["Sec-Fetch-Site"] = "cross-site"
        }
        if (caseInsensitiveHeaders["Sec-Fetch-Dest"].isNullOrBlank()) {
            caseInsensitiveHeaders["Sec-Fetch-Dest"] = "empty"
        }

        // Cookies from the WebView session: часть CDN (vkvideo.cloud, interkh) отдаёт 403
        // на сегменты, если не пришла сессионная cookie, даже при совпадающих заголовках.
        if (caseInsensitiveHeaders["Cookie"].isNullOrBlank()) {
            val cookies = runCatching {
                cookieProvider?.invoke(targetUri?.toString().orEmpty())
            }.getOrNull()
            if (!cookies.isNullOrBlank()) {
                caseInsensitiveHeaders["Cookie"] = cookies
            }
        }

        val finalHeaders = HashMap<String, String>()
        for ((k, v) in caseInsensitiveHeaders) {
            val canonicalKey = when (k.lowercase()) {
                "referer" -> "Referer"
                "user-agent" -> "User-Agent"
                "accept" -> "Accept"
                "range" -> "Range"
                "authorization" -> "Authorization"
                "origin" -> "Origin"
                "accepts-controls" -> "accepts-controls"
                "authorizations" -> "authorizations"
                "borth" -> "borth"
                "x-requested-with" -> "X-Requested-With"
                "sec-fetch-dest" -> "Sec-Fetch-Dest"
                "sec-fetch-mode" -> "Sec-Fetch-Mode"
                "sec-fetch-site" -> "Sec-Fetch-Site"
                "cookie" -> "Cookie"
                else -> k
            }
            finalHeaders[canonicalKey] = v
        }

        factory.setDefaultRequestProperties(finalHeaders)
        val ds = factory.createDataSource()
        for (listener in transferListeners) {
            ds.addTransferListener(listener)
        }
        return ds
    }

    override fun open(dataSpec: DataSpec): Long {
        val originalUriString = dataSpec.uri.toString()
        val rewrittenUriString = AllohaSessionHolder.rewriteUrlIfExpired(originalUriString)
        val effectiveDataSpec = if (rewrittenUriString != originalUriString) {
            dataSpec.buildUpon().setUri(Uri.parse(rewrittenUriString)).build()
        } else {
            dataSpec
        }

        val delegate = createDelegate(effectiveDataSpec.uri)
        activeDelegate = delegate
        return try {
            delegate.open(effectiveDataSpec)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            if (e.responseCode == 403 || e.responseCode == 401) {
                // Stream URLs contain short-lived signatures. Never write them to logcat.
                android.util.Log.w("DynamicHttpDataSource", "Playback request returned ${e.responseCode}. Attempting dynamic token recovery...")

                // Synchronously attempt dynamic token recovery
                val recoveredUriString = synchronized(tokenRecoveryLock) {
                    // Check if another thread already resolved a fresh token while we were waiting
                    val rechecked = AllohaSessionHolder.rewriteUrlIfExpired(originalUriString)
                    if (rechecked != originalUriString && rechecked != effectiveDataSpec.uri.toString()) {
                        rechecked
                    } else {
                        val iframeUrl = iframeUrlProvider()
                        if (!iframeUrl.isNullOrBlank() && tokenRefresher != null) {
                            val freshStreamUrl = tokenRefresher.invoke(iframeUrl)
                            if (!freshStreamUrl.isNullOrBlank()) {
                                // registerStreamUrlUpdate expects the complete old URL and derives
                                // its directory itself. Passing the directory here stripped one
                                // path component twice, producing invalid refreshed playlist URLs.
                                AllohaSessionHolder.registerStreamUrlUpdate(
                                    effectiveDataSpec.uri.toString(),
                                    freshStreamUrl
                                )
                                AllohaSessionHolder.rewriteUrlIfExpired(originalUriString)
                            } else null
                        } else null
                    }
                }

                if (!recoveredUriString.isNullOrBlank() && recoveredUriString != effectiveDataSpec.uri.toString()) {
                    android.util.Log.d("DynamicHttpDataSource", "Dynamic token recovery returned a replacement URL; retrying playback request.")
                    val freshSpec = effectiveDataSpec.buildUpon().setUri(Uri.parse(recoveredUriString)).build()
                    val freshDelegate = createDelegate(freshSpec.uri)
                    activeDelegate = freshDelegate
                    return freshDelegate.open(freshSpec)
                }

                // Fallback: If token recovery didn't succeed, retry once with standard browser navigation headers
                android.util.Log.w("DynamicHttpDataSource", "Fallback: Retrying with standard browser navigation headers...")
                val iframeUrl = iframeUrlProvider()
                val referer = try {
                    iframeUrl?.let {
                        val uri = Uri.parse(it)
                        "${uri.scheme ?: "https"}://${uri.authority}/"
                    }
                } catch (_: Exception) { null }

                val fallbackFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(userAgentProvider())
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(30_000)
                val fallbackHeaders = HashMap<String, String>()
                fallbackHeaders["Accept"] = "*/*"
                if (!referer.isNullOrBlank()) {
                    fallbackHeaders["Referer"] = referer
                }
                fallbackFactory.setDefaultRequestProperties(fallbackHeaders)
                val fallbackDelegate = fallbackFactory.createDataSource()
                for (listener in transferListeners) {
                    fallbackDelegate.addTransferListener(listener)
                }
                activeDelegate = fallbackDelegate
                fallbackDelegate.open(effectiveDataSpec)
            } else {
                throw e
            }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return activeDelegate?.read(buffer, offset, length) ?: -1
    }

    override fun getUri(): Uri? = activeDelegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = activeDelegate?.responseHeaders ?: emptyMap()

    override fun getResponseCode(): Int = activeDelegate?.responseCode ?: -1

    override fun close() {
        activeDelegate?.close()
        activeDelegate = null
    }

    override fun setRequestProperty(name: String, value: String) {
        dynamicProperties.set(name, value)
    }

    override fun clearRequestProperty(name: String) {
        dynamicProperties.remove(name)
    }

    override fun clearAllRequestProperties() {
        dynamicProperties.clear()
    }
}

/**
 * Factory producing [DynamicHttpDataSource] instances.
 */
@OptIn(UnstableApi::class)
class DynamicHttpDataSourceFactory(
    private val headerProvider: () -> Map<String, String>,
    private val iframeUrlProvider: () -> String? = { null },
    private val userAgentProvider: () -> String = { "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36" },
    private val tokenRefresher: ((String) -> String?)? = null,
    private val cookieProvider: ((String) -> String?)? = null
) : HttpDataSource.Factory {

    private val defaultProperties = HttpDataSource.RequestProperties()

    override fun setDefaultRequestProperties(defaultRequestProperties: MutableMap<String, String>): HttpDataSource.Factory {
        this.defaultProperties.clearAndSet(defaultRequestProperties)
        return this
    }

    override fun createDataSource(): HttpDataSource {
        return DynamicHttpDataSource(
            headerProvider = headerProvider,
            iframeUrlProvider = iframeUrlProvider,
            userAgentProvider = userAgentProvider,
            defaultProperties = defaultProperties,
            tokenRefresher = tokenRefresher,
            cookieProvider = cookieProvider
        )
    }
}
