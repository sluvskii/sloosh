package com.slooshfilm.app.data.alloha

import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

/**
 * Intelligent HLS Master Playlist Rewriter for Alloha / VKVideo streams.
 *
 * Implements 100% architectural parity with iOS `PlaybackHlsRewriter.swift`:
 * 1. Rewrites generic audio tracks (e.g. `audio0`, `index-a1.m3u8`..`index-a7.m3u8`) with authentic
 *    voiceover / studio names (e.g. "HDRezka Studio", "LostFilm", "Red Head Sound", "Eng.Original").
 * 2. Marks the user's chosen voiceover with `DEFAULT=YES, AUTOSELECT=YES` and all others with `DEFAULT=NO, AUTOSELECT=NO`
 *    so ExoPlayer immediately starts playing the chosen Russian translation without a single glitch or delay.
 * 3. Enforces correct `LANGUAGE="ru"` on Russian tracks and `LANGUAGE="en"` on English tracks.
 * 4. Normalizes relative URIs into absolute URLs against `baseUrl` so ExoPlayer can resolve them directly.
 * 5. Normalizes `#EXT-X-VERSION` to 4+ as required by RFC 8216 when `#EXT-X-MEDIA` is present.
 * 6. Strips unsupported codecs (AV1) and invalid HDR transfer attributes on non-HEVC streams.
 * 7. When a target quality is requested (e.g. "1080p", "720p"), filters `#EXT-X-STREAM-INF` to that resolution tier.
 */
object AllohaHlsRewriter {

    fun normalizeResolutionLabel(width: Int, height: Int): String {
        return when {
            width >= 3800 || height >= 2000 -> "4K"
            width >= 2500 || height >= 1400 -> "2K"
            width >= 1900 || height >= 800 -> "1080p"
            width >= 1200 || height >= 500 -> "720p"
            width >= 800 || height >= 400 -> "480p"
            width >= 600 || height >= 250 -> "360p"
            height > 0 -> "${height}p"
            else -> "HD"
        }
    }

    fun qualityHeight(quality: String): Int {
        val clean = quality.lowercase(Locale.ROOT).trim()
        return when {
            clean.contains("4k") || clean.contains("2160") -> 2160
            clean.contains("2k") || clean.contains("1440") -> 1440
            clean.contains("1080") -> 1080
            clean.contains("720") -> 720
            clean.contains("480") -> 480
            clean.contains("360") -> 360
            else -> clean.replace("p", "").toIntOrNull() ?: 0
        }
    }

    private data class ParsedVariant(
        val streamInfLine: String,
        val uriLine: String,
        val resolutionLabel: String,
        val bandwidth: Double,
        val height: Int,
        val width: Int
    )

    fun rewrite(
        master: String,
        voices: List<String>,
        preferredVoiceName: String? = null,
        targetQuality: String? = null,
        baseUrl: String? = null
    ): String {
        val trimmed = master.trim()
        if (trimmed.isEmpty()) return master
        if (!master.contains("#EXT-X-STREAM-INF")) return master

        val lines = master.replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")

        val output = mutableListOf<String>()
        val filteredLines = filterFailoverDuplicates(lines)

        val streamInfIndex = filteredLines.indexOfFirst { it.startsWith("#EXT-X-STREAM-INF") }
            .takeIf { it >= 0 } ?: filteredLines.size
        val hasMediaTags = filteredLines.any { it.startsWith("#EXT-X-MEDIA") }
        var hasEmittedVersion = false
        var audioLineCounter = 0

        for (i in 0 until streamInfIndex) {
            val line = filteredLines[i]
            if (line.startsWith("#EXT-X-VERSION:")) {
                hasEmittedVersion = true
                val vStr = line.removePrefix("#EXT-X-VERSION:").trim()
                val v = vStr.toIntOrNull() ?: 0
                if (v < 4 && hasMediaTags) {
                    output.add("#EXT-X-VERSION:4")
                    continue
                }
            }
            if (line.startsWith("#EXT-X-MEDIA") && line.contains("TYPE=AUDIO")) {
                output.add(rewriteMediaLine(line, voices, preferredVoiceName, baseUrl, fallbackIndex = audioLineCounter++))
            } else if (line.startsWith("#EXT-X-MEDIA") && line.contains("TYPE=SUBTITLES")) {
                val subUri = extractQuotedAttr(line, "URI")
                if (!subUri.isNullOrBlank() && baseUrl != null) {
                    val absSubUri = resolveUrl(subUri, baseUrl)
                    output.add(addOrReplaceAttribute(line, "URI", absSubUri, quoted = true))
                } else {
                    output.add(line)
                }
            } else {
                output.add(line)
            }
        }

        if (hasMediaTags && !hasEmittedVersion && output.none { it.startsWith("#EXT-X-VERSION:") }) {
            val extm3uIdx = output.indexOfFirst { it.startsWith("#EXTM3U") }
            if (extm3uIdx >= 0) {
                output.add(extm3uIdx + 1, "#EXT-X-VERSION:4")
            } else {
                output.add(0, "#EXT-X-VERSION:4")
            }
        }

        // Parse variant entries: (#EXT-X-STREAM-INF, URI)
        val parsedVariants = mutableListOf<ParsedVariant>()
        var idx = streamInfIndex
        while (idx < filteredLines.size) {
            val line = filteredLines[idx]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                var modifiedLine = normalizeStreamInfVideoRange(line)

                var width = 0
                var height = 0
                val resMatch = Regex("""RESOLUTION=([0-9]+)x([0-9]+)""").find(line)
                if (resMatch != null) {
                    width = resMatch.groupValues[1].toIntOrNull() ?: 0
                    height = resMatch.groupValues[2].toIntOrNull() ?: 0
                }

                var bandwidth = 0.0
                val bwMatch = Regex("""BANDWIDTH=([0-9]+)""").find(line)
                if (bwMatch != null) {
                    bandwidth = bwMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                }

                var uri = ""
                var nextIdx = idx + 1
                while (nextIdx < filteredLines.size) {
                    val nextLine = filteredLines[nextIdx]
                    if (!nextLine.startsWith("#") && nextLine.isNotBlank()) {
                        uri = nextLine.trim()
                        nextIdx += 1
                        break
                    }
                    nextIdx += 1
                }

                if (height == 0) {
                    val lowerUri = uri.lowercase(Locale.ROOT)
                    when {
                        lowerUri.contains("2160") || lowerUri.contains("4k") -> { height = 2160; width = 3840 }
                        lowerUri.contains("1440") || lowerUri.contains("2k") -> { height = 1440; width = 2560 }
                        lowerUri.contains("1080") -> { height = 1080; width = 1920 }
                        lowerUri.contains("720") -> { height = 720; width = 1280 }
                        lowerUri.contains("480") -> { height = 480; width = 854 }
                        lowerUri.contains("360") -> { height = 360; width = 640 }
                        bandwidth >= 3_500_000 -> { height = 1080; width = 1920 }
                        bandwidth >= 1_800_000 -> { height = 720; width = 1280 }
                        bandwidth >= 800_000 -> { height = 480; width = 854 }
                        bandwidth > 0 -> { height = 360; width = 640 }
                    }
                }

                if (!modifiedLine.contains("RESOLUTION=") && width > 0 && height > 0) {
                    modifiedLine = addOrReplaceAttribute(modifiedLine, "RESOLUTION", "${width}x${height}", quoted = false)
                }

                val resLabel = normalizeResolutionLabel(width, height)

                if (uri.isNotBlank()) {
                    val absoluteUri = resolveUrl(uri, baseUrl)
                    parsedVariants.add(
                        ParsedVariant(
                            streamInfLine = modifiedLine,
                            uriLine = absoluteUri,
                            resolutionLabel = resLabel,
                            bandwidth = bandwidth,
                            height = height,
                            width = width
                        )
                    )
                }
                idx = nextIdx
            } else {
                idx += 1
            }
        }

        // Sort parsed variants descending so highest resolution & bitrate is always first
        parsedVariants.sortWith { a, b ->
            val scoreA = a.height * 100_000_000L + a.bandwidth.toLong()
            val scoreB = b.height * 100_000_000L + b.bandwidth.toLong()
            scoreB.compareTo(scoreA)
        }

        // Deduplicate variants so each resolution tier appears at most once (highest bitrate kept)
        val uniqueVariants = mutableListOf<ParsedVariant>()
        val seenResolutions = mutableSetOf<String>()
        for (v in parsedVariants) {
            val key = v.resolutionLabel.lowercase(Locale.ROOT)
            if (seenResolutions.add(key)) {
                uniqueVariants.add(v)
            }
        }

        var finalVariants: List<ParsedVariant> = uniqueVariants
        if (!targetQuality.isNullOrBlank() && targetQuality != "Авто" && !targetQuality.equals("auto", ignoreCase = true)) {
            val targetHeight = qualityHeight(targetQuality)
            val matches = uniqueVariants.filter {
                it.resolutionLabel.equals(targetQuality, ignoreCase = true) ||
                    (targetHeight > 0 && it.height == targetHeight)
            }
            if (matches.isNotEmpty()) {
                finalVariants = matches
            } else {
                val maxHeight = uniqueVariants.map { it.height }.maxOrNull() ?: 0
                if (maxHeight > 0) {
                    finalVariants = uniqueVariants.filter { it.height == maxHeight }
                }
            }
        }

        if (finalVariants.isNotEmpty()) {
            for (v in finalVariants) {
                output.add(v.streamInfLine)
                output.add(v.uriLine)
            }
        } else {
            // Fallback: output unmodified variants with absolute URLs
            var i = streamInfIndex
            while (i < filteredLines.size) {
                val line = filteredLines[i]
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    output.add(normalizeStreamInfVideoRange(line))
                } else if (!line.startsWith("#") && line.isNotBlank()) {
                    output.add(resolveUrl(line.trim(), baseUrl))
                } else {
                    output.add(line)
                }
                i += 1
            }
        }

        return output.joinToString("\n")
    }

    private fun isDedicatedStream(voices: List<String>): Boolean {
        if (voices.size != 2) return false
        val v0 = voices[0]
        val v1 = voices[1]
        val hasOriginal = AllohaTranslationMatcher.isOriginalOrEnglishTranslation(v0) ||
            AllohaTranslationMatcher.isOriginalOrEnglishTranslation(v1)
        val hasRussian = AllohaTranslationMatcher.isRussianTranslation(v0) ||
            AllohaTranslationMatcher.isRussianTranslation(v1)
        return hasOriginal && hasRussian
    }

    private fun rewriteMediaLine(
        line: String,
        voices: List<String>,
        preferredVoiceName: String?,
        baseUrl: String?,
        fallbackIndex: Int
    ): String {
        if (!line.startsWith("#EXT-X-MEDIA") || !line.contains("TYPE=AUDIO")) return line
        if (voices.isEmpty()) return line

        val rawName = extractQuotedAttr(line, "NAME")
        val uri = extractQuotedAttr(line, "URI")
        val language = extractQuotedAttr(line, "LANGUAGE")

        val rawMeta = "${rawName.orEmpty()} ${language.orEmpty()} ${uri.orEmpty()}".lowercase(Locale.ROOT)
        val isExplicitEnglish = rawMeta.contains("eng") || rawMeta.contains("orig") ||
            language?.lowercase(Locale.ROOT)?.startsWith("en") == true
        val isExplicitRussian = rawMeta.contains("rus") || rawMeta.contains("рус") ||
            language?.lowercase(Locale.ROOT)?.startsWith("ru") == true

        val voiceName: String
        val normalizedLang: String

        if (voices.size == 2 && isDedicatedStream(voices)) {
            // Dedicated iframe stream (e.g. [Russian Dub, "Оригинал"])
            // For foreign movies/series from VKVideo balancer:
            // Track 0 (fallbackIndex 0 / index-a1) is Original (English)
            // Track 1 (fallbackIndex 1 / index-a2) is Dubbed (Russian)
            val isDubTrack = if (isExplicitRussian) {
                true
            } else if (isExplicitEnglish) {
                false
            } else {
                // If no explicit language tags in HLS metadata:
                // index 0 / index-a1 is English Original, index 1 / index-a2 is Russian Dub
                fallbackIndex > 0 || uri?.contains("a2", ignoreCase = true) == true
            }

            if (isDubTrack) {
                voiceName = voices.firstOrNull { AllohaTranslationMatcher.isRussianTranslation(it) }
                    ?: preferredVoiceName
                    ?: voices[0]
                normalizedLang = "ru"
            } else {
                voiceName = voices.firstOrNull { AllohaTranslationMatcher.isOriginalOrEnglishTranslation(it) }
                    ?: "Оригинал"
                normalizedLang = "en"
            }
        } else {
            val index = extractAudioIndex(uri, isUri = true)
                ?: extractAudioIndex(rawName)
                ?: extractAudioIndex(language)
                ?: fallbackIndex.takeIf { it in voices.indices }

            if (index == null || index !in voices.indices) {
                // If we cannot match by index but the rawName already contains a real name, keep it
                return if (baseUrl != null && !uri.isNullOrBlank()) {
                    val absUri = resolveUrl(uri, baseUrl)
                    addOrReplaceAttribute(line, "URI", absUri, quoted = true)
                } else line
            }

            // Only rewrite if rawName is missing or generic (e.g. "rus0", "audio0", "track1")
            if (rawName != null && !isGenericTrackName(rawName) && rawName.length > 4) {
                return if (baseUrl != null && !uri.isNullOrBlank()) {
                    val absUri = resolveUrl(uri, baseUrl)
                    addOrReplaceAttribute(line, "URI", absUri, quoted = true)
                } else line
            }

            voiceName = voices[index]
            normalizedLang = if (AllohaTranslationMatcher.isOriginalOrEnglishTranslation(voiceName)) "en" else "ru"
        }

        var output = addOrReplaceAttribute(line, "NAME", voiceName, quoted = true)
        output = addOrReplaceAttribute(output, "LANGUAGE", normalizedLang, quoted = true)

        if (!preferredVoiceName.isNullOrBlank()) {
            val isMatch = AllohaTranslationMatcher.matches(voiceName, preferredVoiceName, exactOnly = false)
            output = addOrReplaceAttribute(output, "DEFAULT", if (isMatch) "YES" else "NO", quoted = false)
            output = addOrReplaceAttribute(output, "AUTOSELECT", if (isMatch) "YES" else "NO", quoted = false)
        }

        if (baseUrl != null && !uri.isNullOrBlank()) {
            val absUri = resolveUrl(uri, baseUrl)
            output = addOrReplaceAttribute(output, "URI", absUri, quoted = true)
        }

        return output
    }

    private fun isGenericTrackName(name: String): Boolean {
        val lower = name.trim().lowercase(Locale.ROOT)
        val patterns = listOf(
            Regex("""^[a-z]{2,4}\d*$"""),
            Regex("""^audio[_-]?\d*$"""),
            Regex("""^track[_-]?\d*$"""),
            Regex("""^stream[_-]?\d*$""")
        )
        return patterns.any { it.matches(lower) }
    }

    private fun extractAudioIndex(raw: String?, isUri: Boolean = false): Int? {
        if (raw.isNullOrBlank()) return null
        if (isUri) {
            val match = Regex("""index-a(\d+)""", RegexOption.IGNORE_CASE).find(raw)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null && num >= 1) return num - 1
            }
        }
        val patterns = listOf(
            Regex("""(?:^|[^a-z0-9])(?:[a-z]{2,4}|audio[_-]?|track[_-]?)(\d+)(?:$|[^a-z0-9])""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(raw)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null) return num
            }
        }
        return null
    }

    private fun addOrReplaceAttribute(line: String, key: String, value: String, quoted: Boolean = true): String {
        val pattern = if (quoted) {
            Regex("""\b${Regex.escape(key)}="([^"]*)"""", RegexOption.IGNORE_CASE)
        } else {
            Regex("""\b${Regex.escape(key)}=([^,\s]+)""", RegexOption.IGNORE_CASE)
        }
        val replacement = if (quoted) {
            """$key="${escapeAttr(value)}""""
        } else {
            """$key=$value"""
        }

        if (pattern.containsMatchIn(line)) {
            return pattern.replace(line, replacement)
        } else {
            val colonIdx = line.indexOf(':')
            return if (colonIdx >= 0) {
                val prefix = line.substring(0, colonIdx + 1)
                val rest = line.substring(colonIdx + 1)
                "$prefix$replacement,$rest"
            } else {
                "$line,$replacement"
            }
        }
    }

    private fun extractQuotedAttr(line: String, key: String): String? {
        val regex = Regex("""\b${Regex.escape(key)}="([^"]+)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun filterFailoverDuplicates(lines: List<String>): List<String> {
        val filtered = mutableListOf<String>()
        val seenVariantKeys = mutableSetOf<String>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-MEDIA")) {
                val groupId = extractQuotedAttr(line, "GROUP-ID")
                if (groupId?.lowercase(Locale.ROOT)?.startsWith("failover-") == true) {
                    i += 1
                    continue
                }
                filtered.add(line)
                i += 1
                continue
            }

            if (line.startsWith("#EXT-X-STREAM-INF")) {
                if (isUnsupportedCodec(line)) {
                    i += 2
                    continue
                }
                val audioGroup = extractQuotedAttr(line, "AUDIO")
                if (audioGroup?.lowercase(Locale.ROOT)?.startsWith("failover-") == true) {
                    i += 2
                    continue
                }
                val resolution = Regex("""RESOLUTION=([^,\s]+)""").find(line)?.groupValues?.get(1)
                val bandwidth = Regex("""BANDWIDTH=([^,\s]+)""").find(line)?.groupValues?.get(1)
                val codecs = extractQuotedAttr(line, "CODECS")
                val key = listOfNotNull(resolution, bandwidth, codecs, audioGroup).joinToString("|")

                if (key.isNotEmpty() && !seenVariantKeys.add(key)) {
                    i += 2
                    continue
                }
                filtered.add(line)
                if (i + 1 < lines.size) {
                    filtered.add(lines[i + 1])
                }
                i += 2
                continue
            }

            filtered.add(line)
            i += 1
        }
        return filtered
    }

    private fun isUnsupportedCodec(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return lower.contains("av01.") || lower.contains("av1")
    }

    private fun normalizeStreamInfVideoRange(line: String): String {
        val lower = line.lowercase(Locale.ROOT)
        val isHdrCapable = lower.contains("hvc1") || lower.contains("hev1") ||
            lower.contains("dvh1") || lower.contains("dvhe")
        if (isHdrCapable) return line
        return line.replace(Regex(""",?\s*VIDEO-RANGE=[^,\s]+""", RegexOption.IGNORE_CASE), "")
    }

    private fun resolveUrl(uri: String, baseUrl: String?): String {
        if (uri.startsWith("http://") || uri.startsWith("https://")) return uri
        if (baseUrl.isNullOrBlank()) return uri
        return try {
            val base = URI(baseUrl)
            val resolved = base.resolve(uri)
            if (!uri.contains("?") && !base.rawQuery.isNullOrBlank() && resolved.rawQuery.isNullOrBlank()) {
                "${resolved.toString()}?${base.rawQuery}"
            } else {
                resolved.toString()
            }
        } catch (_: Exception) {
            val queryIdx = baseUrl.indexOf('?')
            val baseWithoutQuery = if (queryIdx >= 0) baseUrl.substring(0, queryIdx) else baseUrl
            val query = if (queryIdx >= 0) baseUrl.substring(queryIdx) else ""
            val lastSlash = baseWithoutQuery.lastIndexOf('/')
            val resolvedPath = if (lastSlash >= 0) {
                baseWithoutQuery.substring(0, lastSlash + 1) + uri
            } else uri
            if (!uri.contains("?") && query.isNotEmpty()) {
                "$resolvedPath$query"
            } else {
                resolvedPath
            }
        }
    }

    private fun escapeAttr(s: String): String {
        return s.replace("\\", "").replace("\"", "'")
    }
}
