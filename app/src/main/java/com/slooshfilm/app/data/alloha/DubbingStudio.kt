package com.slooshfilm.app.data.alloha

import androidx.annotation.DrawableRes
import com.slooshfilm.app.R
import java.util.regex.Pattern

/**
 * Популярные студии озвучки и дубляжа.
 * 1:1 портировано из iOS-клиента (DubbingStudio.swift).
 * Обеспечивает автоматическое распознавание брендовых студий из названий потоков.
 */
enum class DubbingStudio(
    val id: String,
    val title: String,
    @DrawableRes val iconResId: Int?,
    val searchKeywords: List<String>
) {
    RED_HEAD_SOUND(
        id = "rhs",
        title = "Red Head Sound",
        iconResId = R.drawable.ic_studio_rhs,
        searchKeywords = listOf("red head sound", "redheadsound", "ред хед саунд", "редхед", "rhs", "рхс")
    ),
    HD_REZKA(
        id = "hdrezka",
        title = "HDRezka Studio",
        iconResId = R.drawable.ic_studio_hdrezka,
        searchKeywords = listOf("hdrezka", "hd-rezka", "rezka", "хдрезка", "хд резка", "резка")
    ),
    FLARROW_FILMS(
        id = "flarrowfilms",
        title = "Flarrow Films",
        iconResId = R.drawable.ic_studio_flarrowfilms,
        searchKeywords = listOf("flarrow films", "flarrowfilms", "flarrow", "флэрроу филмс", "флэрроу", "флероу")
    ),
    TV_SHOWS(
        id = "tvshows",
        title = "TVShows",
        iconResId = R.drawable.ic_studio_tvshows,
        searchKeywords = listOf("tvshows", "tv shows", "tv-shows", "твшоус", "тв шоу", "твшоу")
    ),
    ALEX_FILM(
        id = "alexfilm",
        title = "AlexFilm",
        iconResId = R.drawable.ic_studio_alexfilm,
        searchKeywords = listOf("alexfilm", "alex film", "alex-film", "алексфильм", "алекс фильм", "alex")
    ),
    RU_DUB(
        id = "rudub",
        title = "RuDub",
        iconResId = null,
        searchKeywords = listOf("rudub", "ru dub", "ru-dub", "рудаб", "ру даб")
    ),
    COLD_FILM(
        id = "coldfilm",
        title = "ColdFilm",
        iconResId = R.drawable.ic_studio_coldfilm,
        searchKeywords = listOf("coldfilm", "cold film", "cold-film", "колдфильм", "колд фильм", "cold")
    ),
    LOST_FILM(
        id = "lostfilm",
        title = "LostFilm",
        iconResId = R.drawable.ic_studio_lostfilm,
        searchKeywords = listOf("lostfilm", "lost film", "lost-film", "лостфильм", "лост фильм", "lost")
    ),
    KUBIK_V_KUBE(
        id = "kubik",
        title = "Кубик в кубе",
        iconResId = R.drawable.ic_studio_kubik,
        searchKeywords = listOf("кубик в кубе", "кубик в кубе (18+)", "кубики", "kubik v kube", "kubik-v-kube", "kubik")
    ),
    DARON_MONEY(
        id = "dms",
        title = "Dragon Money Studio",
        iconResId = R.drawable.ic_studio_dms,
        searchKeywords = listOf("daron money studio", "daron money", "dragon money studio", "dragon money", "daron", "dragon", "dms", "дарон мани", "драгон мани", "дарон", "драгон")
    ),
    JASKIER(
        id = "jaskier",
        title = "Jaskier",
        iconResId = R.drawable.ic_studio_jaskier,
        searchKeywords = listOf("jaskier", "яскъ", "яск", "яскьер", "яскиер", "жаскьер")
    ),
    NEW_STUDIO(
        id = "newstudio",
        title = "NewStudio",
        iconResId = R.drawable.ic_studio_newstudio,
        searchKeywords = listOf("newstudio", "new studio", "new-studio", "ньюстудио", "нью студио", "нью-студио", "newstudio tv")
    ),
    LE_PRODUCTION(
        id = "leproduction",
        title = "LE-Production",
        iconResId = R.drawable.ic_studio_le,
        searchKeywords = listOf("le-production", "le - production", "le production", "leproduction", "l.e. production", "l.e.production", "ле-продакшн", "ле продакшн", "лепродакшн", "ле продакшен", "ле-продакшен", "le-prod", "le prod")
    );

    companion object {
        /**
         * Интеллектуальное определение студии дубляжа по сырому или очищенному названию перевода.
         */
        fun detect(name: String): DubbingStudio? {
            val lower = name.lowercase().trim()
            if (lower.isEmpty()) return null

            for (studio in entries) {
                for (keyword in studio.searchKeywords) {
                    if (keyword.length <= 4) {
                        try {
                            val pattern = Pattern.compile("(?i)(^|[\\s(\\[_-])${Pattern.quote(keyword)}([\\s)\\]_.,:!?-]|$)")
                            if (pattern.matcher(lower).find()) {
                                return studio
                            }
                        } catch (_: Exception) {
                            if (lower.contains(keyword)) return studio
                        }
                    } else if (lower.contains(keyword)) {
                        return studio
                    }
                }
            }
            return null
        }

        /**
         * Очищает и красиво форматирует название озвучки — 1:1 с iOS `cleanTranslationName`.
         * Сохраняет слово «Дубляж» и студию: «Дубляж HDrezka Studio» ≠ «HDrezka Studio».
         */
        fun cleanTranslationName(rawName: String): String = cleanTranslationDisplayName(rawName)

        /**
         * Уникальное название для чипа — 1:1 с iOS `displayTranslationName`.
         * Суффикс «#N» добавляется только если очищенные названия действительно совпали.
         */
        fun displayTranslationName(
            rawName: String,
            indexInAll: Int,
            allRawNames: List<String>
        ): String = uniqueTranslationDisplayName(rawName, indexInAll, allRawNames)

        /**
         * Очищает эмодзи в начале названия, сохраняя само название целиком без обрезки (1:1 iOS DubbingStudio.cleanChipTitle).
         */
        fun cleanChipTitle(displayName: String): String = LanguageFlag.stripLeadingEmoji(displayName)
    }
}

// MARK: - Названия озвучек (порт iOS MediaHelpers.cleanTranslationName)

private data class TranslationLanguage(val defaultName: String, val patterns: List<String>)

private val TRANSLATION_LANGUAGES = listOf(
    TranslationLanguage("Русский", listOf("russian", "русский", "рус", "ru")),
    TranslationLanguage(
        "Оригинал",
        listOf("original", "оригинальный", "оригинал", "english", "английский", "англ", "eng", "usa", "en")
    ),
    TranslationLanguage("Украинский", listOf("ukrainian", "украинский", "укр", "ukr", "uk")),
    TranslationLanguage("Японский", listOf("japanese", "японский", "япон", "jap", "ja")),
    TranslationLanguage("Корейский", listOf("korean", "корейский", "корей", "kor", "ko")),
    TranslationLanguage("Китайский", listOf("chinese", "китайский", "китай", "chi", "zh")),
    TranslationLanguage("Казахский", listOf("kazakh", "казахский", "казах", "kaz", "kz")),
    TranslationLanguage("Грузинский", listOf("georgian", "грузинский", "грузин", "geo", "ka")),
    TranslationLanguage("Французский", listOf("french", "французский", "француз", "fra", "fr")),
    TranslationLanguage("Немецкий", listOf("german", "немецкий", "немец", "ger", "de")),
    TranslationLanguage("Испанский", listOf("spanish", "испанский", "испан", "spa", "es")),
    TranslationLanguage("Итальянский", listOf("italian", "итальянский", "итальян", "ita", "it")),
    TranslationLanguage("Турецкий", listOf("turkish", "турецкий", "турец", "tur", "tr"))
)

private val SCENE_TAG_REGEX = Regex(
    "(?i)[a-z0-9._-]{3,}(?:2160p|1080p|720p|480p|internal|web-dl|web-dlrip|bdrip|bluray|hdr10|hdr|dv|hevc|x264|x265|spacehd\\d*)[a-z0-9._-]*"
)
private val AUDIO_CODEC_REGEX = Regex(
    "(?i)\\b(?:AC3|E-AC3|EAC3|DDP|DD|DTS-HD|DTS|TrueHD|AAC|FLAC|MP3|PCM|LPCM)\\s*(?:5[.]?1|7[.]?1|2[.]?0|51)?(?:\\s*@\\s*\\d+\\s*(?:kbps|kbit|кбит/с|кб/с)?)?"
)
private val BITRATE_AT_REGEX = Regex("(?i)@\\s*\\d+\\s*(?:kbps|kbit|кбит/с|кб/с)?")
private val BITRATE_WORD_REGEX = Regex("(?i)\\b\\d+\\s*(?:kbps|kbit|кбит/с|кб/с)\\b")
private val RELEASE_TAG_REGEX = Regex("(?i)\\b(?:Blu-ray(?:\\s*CEE)?|BDRip|WEB-DL|HDTV|Line)\\b")
private val CHANNELS_REGEX = Regex("(?i)\\b(?:5[.]?1|7[.]?1|2[.]?0)\\b")
private val HDREZKA_SHORT_REGEX = Regex("(?i)\\bHDrezka\\s+St(?:\\.|\\b)")
private val DUB_PREFIX_REGEX = Regex(
    "(?i)^(?:профессиональный\\s+|проф[.]\\s+|полный\\s+)?(?:дублированный|дубляж)[:\\s-]+(.+)$"
)
private val VOICE_PREFIX_REGEX = Regex(
    "(?i)^(?:профессиональный\\s+|проф[.]\\s+|любительский\\s+|люб[.]\\s+)?(?:многоголосый|двухголосый|одноголосый|закадровый)[:\\s-]+(.+)$"
)

// Кэш обязателен: displayTranslationName сравнивает названия попарно, а очистка
// компилирует десятки регулярных выражений. Без кэша это десятки миллисекунд на кадр.
private val CLEANED_TRANSLATION_NAMES = java.util.concurrent.ConcurrentHashMap<String, String>()

private fun cleanTranslationDisplayName(rawName: String): String =
    CLEANED_TRANSLATION_NAMES.getOrPut(rawName) { cleanTranslationDisplayNameUncached(rawName) }

private fun cleanTranslationDisplayNameUncached(rawName: String): String {
    var name = rawName.trim()
    if (name.isEmpty()) return "По умолчанию"

    // 1. Сцен-теги, аудиокодеки, битрейты и каналы.
    name = SCENE_TAG_REGEX.replace(name, "").trim()
    name = AUDIO_CODEC_REGEX.replace(name, "")
    name = BITRATE_AT_REGEX.replace(name, "")
    name = BITRATE_WORD_REGEX.replace(name, "")
    name = RELEASE_TAG_REGEX.replace(name, "")
    name = CHANNELS_REGEX.replace(name, "")
    name = HDREZKA_SHORT_REGEX.replace(name, "HDrezka Studio")

    // 2. Субтитры помечаем, но не ломаем название.
    if (name.lowercase().contains("субтитр") || name.lowercase().contains("subtitle")) {
        return "💬 $name"
    }

    // 3. Динамическое извлечение языка: (Russian), [Ukrainian] или префикс «English: …».
    var detectedDefaultName = "Русский"
    var remainder = name
    var languageMatched = false
    for (language in TRANSLATION_LANGUAGES) {
        if (languageMatched) break
        for (pattern in language.patterns) {
            val parenMatch = Regex("(?i)[(\\[]\\s*$pattern\\s*[)\\]]").find(remainder)
            if (parenMatch != null) {
                detectedDefaultName = language.defaultName
                remainder = remainder.replaceRange(parenMatch.range, " ")
                languageMatched = true
                break
            }
            val prefixMatch = Regex("(?i)^$pattern[:\\s-]+").find(remainder)
            if (prefixMatch != null) {
                detectedDefaultName = language.defaultName
                remainder = remainder.replaceRange(prefixMatch.range, " ")
                languageMatched = true
                break
            }
            if (remainder.trim().lowercase() == pattern) {
                detectedDefaultName = language.defaultName
                remainder = ""
                languageMatched = true
                break
            }
        }
    }

    // 4. Скобки, разделители, лишние пробелы.
    remainder = remainder
        .replace("(", " ")
        .replace(")", " ")
        .replace("[", " ")
        .replace("]", " ")
        .replace("|", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    remainder = Regex("\\s*[–—-]\\s*").replace(remainder, " - ").trim()

    // «Дубляж - Студия» -> «Дубляж Студия» (слово «Дубляж» сохраняем!),
    // «Многоголосый - Студия» -> «Студия».
    if (remainder.contains(" - ")) {
        val dashIndex = remainder.indexOf(" - ")
        val prefix = remainder.substring(0, dashIndex).trim()
        val suffix = remainder.substring(dashIndex + 3).trim()
        val lowerPrefix = prefix.lowercase()
        if (lowerPrefix.contains("дубл")) {
            remainder = if (suffix.isEmpty()) "Дубляж" else "Дубляж $suffix"
        } else if (lowerPrefix.contains("многоголос") || lowerPrefix.contains("двухголос") ||
            lowerPrefix.contains("закадр") || lowerPrefix.contains("проф") || lowerPrefix.contains("люб")
        ) {
            if (suffix.isNotEmpty()) remainder = suffix
        }
    } else {
        var matchedDub = false
        DUB_PREFIX_REGEX.find(remainder)?.let { match ->
            val studioPart = match.groupValues[1].trim()
            if (studioPart.isNotEmpty()) {
                remainder = "Дубляж $studioPart"
                matchedDub = true
            }
        }
        if (!matchedDub) {
            VOICE_PREFIX_REGEX.find(remainder)?.let { match ->
                val studioPart = match.groupValues[1].trim()
                if (studioPart.isNotEmpty()) remainder = studioPart
            }
        }
    }

    if (remainder.lowercase() == "дублированный") remainder = "Дубляж"

    while (remainder.startsWith("-") || remainder.startsWith(",") ||
        remainder.startsWith("–") || remainder.startsWith("—")
    ) {
        remainder = remainder.drop(1).trim()
    }
    while (remainder.endsWith("-") || remainder.endsWith(",") ||
        remainder.endsWith("–") || remainder.endsWith("—")
    ) {
        remainder = remainder.dropLast(1).trim()
    }

    if (remainder.isEmpty() || remainder.toIntOrNull() != null) {
        return detectedDefaultName
    }
    return remainder
}

private fun uniqueTranslationDisplayName(
    rawName: String,
    indexInAll: Int,
    allRawNames: List<String>
): String {
    val cleaned = cleanTranslationDisplayName(rawName)
    val totalDuplicates = allRawNames.count { cleanTranslationDisplayName(it) == cleaned }
    if (totalDuplicates <= 1) return cleaned

    val maxIndex = indexInAll.coerceIn(0, allRawNames.lastIndex.coerceAtLeast(0))
    var occurrence = 0
    for (i in 0..maxIndex) {
        if (cleanTranslationDisplayName(allRawNames[i]) == cleaned) occurrence++
    }
    return "$cleaned #$occurrence"
}
