package com.slooshfilm.app.data.alloha

/**
 * Static holder so PlayerScreen / DetailsScreen / WatchSelectorSheet can access cached Alloha results,
 * resolved audio/quality variants, and stream configuration.
 */
object AllohaSessionHolder {
    /** Cached API result passed directly from DetailsScreen */
    @Volatile
    var cachedApiResult: AllohaApiResult? = null

    /** Direct iframe URL pre-selected in WatchSelectorSheet */
    @Volatile
    var activeTargetIframeUrl: String? = null

    /** Media ID for which the cachedApiResult was fetched */
    @Volatile
    var activeMediaId: String? = null

    /** Logo URL for the active media */
    @Volatile
    var activeLogoUrl: String? = null

    /** Active HTTP headers captured by AllohaRuntimeResolver (e.g. accepts-controls, referer, origin) */
    @Volatile
    var activeHeaders: Map<String, String> = emptyMap()

    /** Active User-Agent used by AllohaRuntimeResolver */
    @Volatile
    var activeUserAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    /** Parsed audio variants (translations/voices) with respective quality variants from hlsSource */
    var audioVariants: List<Map<String, Any>> = emptyList()

    /** Parsed general quality variants */
    var qualityVariants: List<Map<String, Any>> = emptyList()

    /** Translation names for the current episode / film */
    var translationNames: List<String> = emptyList()

    /** Authentic voice names mapped 1:1 to HLS master audio variants for rewriting */
    @Volatile
    var availableVoiceNames: List<String> = emptyList()

    /** Currently preferred voice / voiceover name for HLS master playlist rewriting */
    @Volatile
    var currentVoice: String? = null

    /** Iframe URLs for each translation (parallel to [translationNames]). */
    var translationUrls: List<String> = emptyList()

    /** Currently active translation name. */
    @Volatile
    var currentTranslation: String = ""

    /** Available quality levels (e.g. "1080p" -> URL, "720p" -> URL). */
    var qualityMap: Map<String, String> = emptyMap()

    /** Currently selected quality key (e.g. "1080p", "Авто"). */
    @Volatile
    var currentQuality: String = "Авто"

    /** True when quality was set by auto-selection (not manually by user). */
    @Volatile
    var isAutoQuality: Boolean = true

    /** Latest refreshed master.m3u8 CDN URL with fresh signature tokens */
    @Volatile
    var latestMasterUrl: String? = null

    /** Skip ranges from bnsi skipTime (start/end in seconds). */
    @Volatile
    var skipRanges: List<LongRange> = emptyList()

    /** Episode list for in-player switching: iframe URLs (parallel to [episodeNames]). */
    var episodeIframeUrls: List<String> = emptyList()
    var episodeNames: List<String> = emptyList()

    /** Per-episode voiceover map: episodeIndex -> (translationName -> iframeUrl) */
    var episodeVoiceoverUrls: List<Map<String, String>> = emptyList()

    @Volatile
    var currentEpisodeIndex: Int = 0

    fun setTranslations(names: List<String>, urls: List<String>, current: String) {
        translationNames = names
        translationUrls = urls
        currentTranslation = current
    }

    fun clear() {
        cachedApiResult = null
        activeTargetIframeUrl = null
        activeMediaId = null
        activeLogoUrl = null
        activeHeaders = emptyMap()
        audioVariants = emptyList()
        qualityVariants = emptyList()
        translationNames = emptyList()
        availableVoiceNames = emptyList()
        currentVoice = null
        translationUrls = emptyList()
        currentTranslation = ""
        qualityMap = emptyMap()
        currentQuality = "Авто"
        isAutoQuality = true
        latestMasterUrl = null
        skipRanges = emptyList()
        episodeIframeUrls = emptyList()
        episodeNames = emptyList()
        episodeVoiceoverUrls = emptyList()
        currentEpisodeIndex = 0
        urlBaseReplacements.clear()
    }

    data class BaseReplacement(val newBase: String, val newQuery: String?)

    private val urlBaseReplacements = java.util.concurrent.ConcurrentHashMap<String, BaseReplacement>()

    /**
     * Registers a transition from [oldUrl] to [newUrl] so that any segments or sub-playlists
     * referring to [oldUrl]'s path base are automatically rewritten to [newUrl]'s path base.
     */
    fun registerStreamUrlUpdate(oldUrl: String?, newUrl: String) {
        latestMasterUrl = newUrl
        if (oldUrl.isNullOrBlank() || oldUrl == newUrl) return

        val oldClean = oldUrl.substringBefore('?')
        val newClean = newUrl.substringBefore('?')

        val oldBase = oldClean.substringBeforeLast('/')
        val newBase = newClean.substringBeforeLast('/')

        val oldQuery = if (oldUrl.contains('?')) oldUrl.substringAfter('?') else null
        val newQuery = if (newUrl.contains('?')) newUrl.substringAfter('?') else null

        // Signed stream URLs may refresh only their query token while keeping the same path.
        // Keep a replacement in that case too, otherwise HLS sub-requests keep the expired token.
        if (oldBase.isNotBlank() && newBase.isNotBlank() && (oldBase != newBase || oldQuery != newQuery)) {
            val rep = BaseReplacement(newBase, newQuery)
            urlBaseReplacements[oldBase] = rep
            // Update transitive chain (if A -> B and now B -> C, update A -> C)
            for ((k, v) in urlBaseReplacements) {
                if (v.newBase == oldBase) {
                    urlBaseReplacements[k] = rep
                }
            }
        }
    }

    /**
     * Rewrites [originalUrl] to use the latest token/base path if a replacement has been registered.
     */
    fun rewriteUrlIfExpired(originalUrl: String): String {
        for ((oldBase, rep) in urlBaseReplacements) {
            if (originalUrl.startsWith(oldBase)) {
                val suffixWithQuery = originalUrl.substring(oldBase.length)
                val pathPart = suffixWithQuery.substringBefore('?')
                val oldQuery = if (suffixWithQuery.contains('?')) suffixWithQuery.substringAfter('?') else null
                val effectiveQuery = rep.newQuery ?: oldQuery
                return if (!effectiveQuery.isNullOrBlank()) {
                    "${rep.newBase}$pathPart?$effectiveQuery"
                } else {
                    "${rep.newBase}$pathPart"
                }
            }
        }
        return originalUrl
    }
}
