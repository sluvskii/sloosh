package com.slooshfilm.app.data.alloha

import java.util.Locale

/**
 * Intelligent voiceover / translation matching engine for Alloha streaming streams.
 * Matches iOS `allohaTranslationNamesMatch`, `findMatchingAudioVariant`, and ranking algorithm for 100% parity.
 */
object AllohaTranslationMatcher {

    private val noiseWords = setOf(
        "studio", "студия", "дубляж", "дублированный", "дублирование", "полное",
        "многоголосый", "двухголосый", "одноголосый", "авторский", "закадровый", "озвучка",
        "профессиональный", "проф", "любительский", "люб", "production", "films", "film",
        "team", "voice", "line", "перевод", "голос", "звук", "чистый", "версия", "театральная",
        "расширенная", "режиссерская", "режиссёрская"
    )

    private val knownStudios = listOf(
        "red head sound", "rhs", "flarrow", "lostfilm", "tvshows", "newstudio", "newcomers",
        "alexfilm", "кубик", "hdrezka", "rezka", "baibako", "jaskier", "vsi", "iron voice",
        "кураж бамбей", "лостфильм", "ньюстудио"
    )

    private val exclusiveStudios = listOf(
        "red head sound", "rhs", "flarrow", "lostfilm", "tvshows", "newstudio", "newcomers",
        "alexfilm", "кубик", "baibako", "jaskier", "vsi", "iron voice", "кураж бамбей"
    )

    fun detectLanguageTag(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("украин") || lower.contains("ukrain") || lower.contains("укр") || lower.contains("ukr") -> "ukr"
            lower.contains("казах") || lower.contains("kazakh") || lower.contains("каз") || lower.contains("kaz") -> "kaz"
            lower.contains("узбек") || lower.contains("uzbek") || lower.contains("узб") || lower.contains("uzb") -> "uzb"
            lower.contains("оригинал") || lower.contains("original") || lower.contains("english") || lower.contains("английск") || lower.contains("eng") || lower == "en" -> "eng"
            lower.contains("грузин") || lower.contains("georgian") || lower.contains("geo") -> "geo"
            lower.contains("турец") || lower.contains("turkish") || lower.contains("turk") -> "tur"
            lower.contains("япон") || lower.contains("japan") || lower.contains("jap") -> "jap"
            lower.contains("корей") || lower.contains("korean") || lower.contains("kor") -> "kor"
            lower.contains("китай") || lower.contains("chinese") || lower.contains("chi") -> "chi"
            lower.contains("русск") || lower.contains("rus") || lower.contains("дубл") || lower.contains("дуб") || lower.contains("закадр") || lower.contains("многоголос") || lower == "ru" -> "rus"
            else -> null
        }
    }

    fun isOriginalOrEnglishTranslation(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase(Locale.ROOT)
        if (lower.contains("дубл") || lower.contains("дуб") || lower.contains("русск") || lower.contains("полное дублирование")) {
            return false
        }
        if (lower == "en" || lower == "eng" || lower == "оригинал" || lower == "original") {
            return true
        }
        if (lower.contains("оригинал") || lower.contains("original") || lower.contains("english") || lower.contains("английск")) {
            return true
        }
        return detectLanguageTag(name) == "eng"
    }

    fun isRussianTranslation(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        if (isOriginalOrEnglishTranslation(name)) return false
        val tag = detectLanguageTag(name)
        return tag == null || tag == "rus"
    }

    fun translationRank(name: String): Int {
        val lower = name.lowercase(Locale.ROOT)
        val lang = detectLanguageTag(name)

        if (lang != null && lang != "rus" && lang != "eng") {
            return 80
        }
        if (isOriginalOrEnglishTranslation(name)) {
            return 90
        }
        if (lower.contains("дубл") || lower.contains("дуб") || lower.contains("полное дублирование")) {
            return 10
        }
        val topStudios = listOf("пифагор", "невафильм", "red head sound", "rhs", "flarrow")
        if (topStudios.any { lower.contains(it) }) {
            return 20
        }
        val proKeywords = listOf("профессиональн", "проф", "многоголос", "hdrezka", "lostfilm", "кубик в кубе")
        if (proKeywords.any { lower.contains(it) }) {
            return 30
        }
        return 40
    }

    fun isTranslationNoiseWord(word: String): Boolean = noiseWords.contains(word.lowercase(Locale.ROOT))

    fun normalizedAllohaTranslationName(raw: String?): String {
        var value = raw?.trim() ?: return ""
        if (value.isEmpty()) return ""

        value = value
            .replace(Regex("""(?i)\b(?:AC3|E-AC3|EAC3|DDP|DD|DTS-HD|DTS|TrueHD|AAC|FLAC|MP3|PCM|LPCM)\s*(?:5[.]?1|7[.]?1|2[.]?0|51)?(?:\s*@\s*\d+\s*(?:kbps|kbit|кбит/с|кб/с)?)?"""), "")
            .replace(Regex("""(?i)@\s*\d+\s*(?:kbps|kbit|кбит/с|кб/с)?"""), "")
            .replace(Regex("""(?i)\b\d+\s*(?:kbps|kbit|кбит/с|кб/с)\b"""), "")
            .replace(Regex("""(?i)\b(?:Blu-ray(?:\s*CEE)?|BDRip|WEB-DL|HDTV|Line)\b"""), "")
            .replace(Regex("""(?i)\((?:Russian|Ukrainian|Kazakh|English|Uzbek|Turkish|Georgian|Japanese|Korean|Chinese)\)"""), "")
            .replace(Regex("""(?i)\bDUB\b"""), "Дубляж")
            .replace(Regex("""(?i)\bMVO\b"""), "Многоголосый")
            .replace(Regex("""(?i)\bDVO\b"""), "Двухголосый")
            .replace(Regex("""(?i)\bAVO\b"""), "Авторский")
            .replace(Regex("""(?i)\bПМ\b"""), "Проф. многоголосый")
            .replace(Regex("""(?i)\bПД\b"""), "Проф. двухголосый")
            .replace(Regex("""(?i)\bЛМ\b"""), "Люб. многоголосый")
            .replace(Regex("""(?i)\bЛД\b"""), "Люб. двухголосый")
            .replace("[", " ")
            .replace("]", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace("|", " ")
            .trim()

        while (value.startsWith("-") || value.startsWith(",") || value.startsWith("–") || value.startsWith("—")) {
            value = value.drop(1).trim()
        }
        while (value.endsWith("-") || value.endsWith(",") || value.endsWith("–") || value.endsWith("—")) {
            value = value.dropLast(1).trim()
        }

        return value
            .replace(Regex("""(?i)\bHDrezka\s+St(?:\.|\b)"""), "HDrezka Studio")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun cleanTranslationName(name: String?): String {
        val norm = normalizedAllohaTranslationName(name)
        return norm.ifBlank { name?.trim().orEmpty() }
    }

    fun matches(lhs: String?, rhs: String?, exactOnly: Boolean = false): Boolean {
        val lhsRaw = lhs?.trim() ?: return false
        val rhsRaw = rhs?.trim() ?: return false
        if (lhsRaw.isEmpty() || rhsRaw.isEmpty()) return false

        val left = normalizedAllohaTranslationName(lhsRaw).lowercase(Locale.ROOT)
        val right = normalizedAllohaTranslationName(rhsRaw).lowercase(Locale.ROOT)

        if (left == right) return true

        // Strict language tag mismatch check
        val langLeft = detectLanguageTag(lhsRaw)
        val langRight = detectLanguageTag(rhsRaw)

        if (langLeft != null && langLeft != "rus" && langRight != langLeft) return false
        if (langRight != null && langRight != "rus" && langLeft != langRight) return false
        if (langLeft != null && langRight != null && langLeft != langRight) return false

        if (langLeft != null && langRight != null && langLeft == langRight && langLeft != "rus") {
            return true
        }

        if (isOriginalOrEnglishTranslation(left) && isOriginalOrEnglishTranslation(right)) {
            return true
        }

        if (exactOnly) return false

        // Check studio names
        val leftStudios = knownStudios.filter { left.contains(it) }
        val rightStudios = knownStudios.filter { right.contains(it) }

        if (leftStudios.isNotEmpty() && rightStudios.isNotEmpty()) {
            val shared = leftStudios.intersect(rightStudios.toSet())
            if (shared.isEmpty()) return false
            val leftHasDub = left.contains("дубл")
            val rightHasDub = right.contains("дубл")
            if (leftHasDub != rightHasDub) return false
            return true
        }

        val leftHasExclusive = exclusiveStudios.any { left.contains(it) }
        val rightHasExclusive = exclusiveStudios.any { right.contains(it) }
        if (leftHasExclusive != rightHasExclusive) return false

        val leftHasDub = left.contains("дубл")
        val rightHasDub = right.contains("дубл")
        if (leftHasDub && rightHasDub) {
            if (langLeft != null && langRight != null) {
                return langLeft == langRight
            }
            return true
        }

        val isGenericRussian: (String) -> Boolean = { n ->
            val clean = n.lowercase(Locale.ROOT).trim()
            clean == "русский" || clean == "russian" || clean == "rus" || clean == "ru" || clean == "рус" || clean == "русская"
        }
        val isGenericRussianVoiceover: (String) -> Boolean = { n ->
            val clean = n.lowercase(Locale.ROOT)
            clean.contains("дубл") || clean.contains("дуб") || clean.contains("профессиональн") || clean.contains("закадр") || clean.contains("многоголос")
        }

        if ((isGenericRussian(left) && isGenericRussianVoiceover(right)) ||
            (isGenericRussian(right) && isGenericRussianVoiceover(left))
        ) {
            return true
        }

        fun stripNoise(name: String): String {
            var n = name.lowercase(Locale.ROOT)
            for (w in noiseWords) {
                n = n.replace(Regex("(?i)\\b$w\\b"), "")
            }
            return n.replace(Regex("[-–—,]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        val leftCore = stripNoise(left)
        val rightCore = stripNoise(right)

        if (leftCore.isNotEmpty() && leftCore == rightCore) {
            return true
        }

        fun extractDistinctiveWords(s: String): List<String> {
            return s.lowercase(Locale.ROOT)
                .split(Regex("[^a-zA-Zа-яА-Я0-9]"))
                .filter { it.length >= 4 && !isTranslationNoiseWord(it) }
        }

        val rightWords = extractDistinctiveWords(right)
        val leftWords = extractDistinctiveWords(left)

        if (rightWords.isNotEmpty()) {
            val hasShared = rightWords.any { rw ->
                leftWords.any { lw -> lw == rw || lw.contains(rw) || rw.contains(lw) }
            }
            if (hasShared) return true
        }

        if (leftCore.length >= 4 && rightCore.length >= 4) {
            if (leftCore.contains(rightCore) || rightCore.contains(leftCore)) {
                return true
            }
        }

        return false
    }

    /**
     * Finds the best matching audio variant from resolved audioVariants for the target voiceover.
     * Implements 1:1 parity with iOS `findMatchingAudioVariant`.
     */
    fun findMatchingAudioVariant(
        audioVariants: List<Map<String, Any>>,
        targetVoice: String?,
        isDedicatedIframe: Boolean = false
    ): Map<String, Any>? {
        if (audioVariants.isEmpty()) return null
        val target = targetVoice?.trim().orEmpty()

        if (target.isEmpty()) {
            // If target voice is unspecified, prioritize a Russian audio track over English variant 0
            val rus = audioVariants.firstOrNull {
                val url = it["url"] as? String ?: return@firstOrNull false
                url.isNotBlank() && isRussianTranslation(it["title"] as? String)
            }
            if (rus != null) return rus
            return audioVariants.firstOrNull { ((it["url"] as? String) ?: "").isNotBlank() }
        }

        // 1. Exact name match
        audioVariants.firstOrNull { matches(it["title"] as? String, target, exactOnly = true) }?.let { return it }

        // 2. Fuzzy / studio / author match
        audioVariants.firstOrNull { matches(it["title"] as? String, target, exactOnly = false) }?.let { return it }

        // A named studio or translator must never silently fall back to another voice in the
        // same language. Generic choices (for example «Русский» or «Оригинал») can use the
        // language-based fallbacks below; specific choices require a real name match.
        val genericLanguageWords = setOf(
            "русский", "русская", "русское", "рус", "russian", "rus", "ru",
            "английский", "английская", "english", "eng", "en",
            "украинский", "украинская", "ukrainian", "ukr",
            "казахский", "kazakh", "kaz",
            "узбекский", "uzbek", "uzb",
            "турецкий", "turkish", "tur",
            "грузинский", "georgian", "geo",
            "дубляж", "дублированный", "дублирование", "многоголосый", "двухголосый",
            "одноголосый", "профессиональный", "профессиональная", "профессиональное",
            "закадровый", "авторский", "любительский", "любительская", "озвучка",
            "оригинал", "original"
        )
        val targetIdentityWords = target.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Zа-яА-Я0-9]"))
            .filter { it.length >= 3 && !isTranslationNoiseWord(it) && it !in genericLanguageWords }
        val hasSpecificVoiceIdentity = knownStudios.any { target.lowercase(Locale.ROOT).contains(it) } ||
            targetIdentityWords.isNotEmpty()
        if (hasSpecificVoiceIdentity) return null

        // 3. Language tag match (e.g. ukr, kaz, rus, eng)
        val targetLang = detectLanguageTag(target)
        if (targetLang != null) {
            audioVariants.firstOrNull {
                val title = it["title"] as? String ?: return@firstOrNull false
                detectLanguageTag(title) == targetLang
            }?.let { return it }
        }

        // 4. Distinctive words (author surname: «Есарев», «Сербин», «Гаврилов»)
        val targetWords = target.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Zа-яА-Я0-9]"))
            .filter { it.length >= 4 && !isTranslationNoiseWord(it) }
        if (targetWords.isNotEmpty()) {
            for (word in targetWords) {
                audioVariants.firstOrNull {
                    val t = (it["title"] as? String).orEmpty().lowercase(Locale.ROOT)
                    t.contains(word)
                }?.let { return it }
            }
        }

        // 5. Film / series fallback:
        // If target is Russian and variant 0 is English/original, select variant 1 (Russian/dubbed)
        val targetIsRussian = isRussianTranslation(target)
        val targetIsOriginal = isOriginalOrEnglishTranslation(target)

        if (targetIsRussian && audioVariants.size > 1) {
            audioVariants.firstOrNull {
                val t = it["title"] as? String ?: ""
                isRussianTranslation(t)
            }?.let { return it }

            val v0Title = audioVariants[0]["title"] as? String ?: ""
            if (isOriginalOrEnglishTranslation(v0Title)) {
                return audioVariants[1]
            }
        } else if (targetIsOriginal && audioVariants.size > 1) {
            audioVariants.firstOrNull {
                val t = it["title"] as? String ?: ""
                isOriginalOrEnglishTranslation(t)
            }?.let { return it }
        }

        // 6. Dedicated iframe fallback
        if (isDedicatedIframe && audioVariants.size > 1) {
            val isTargetDub = target.lowercase(Locale.ROOT).contains("дубл")
            if (!isTargetDub) {
                val nonDub = audioVariants.firstOrNull {
                    val t = (it["title"] as? String).orEmpty().lowercase(Locale.ROOT)
                    !t.contains("дубл") && !t.contains("dub")
                }
                if (nonDub != null) return nonDub
                return audioVariants[1]
            } else {
                val dubVariant = audioVariants.firstOrNull {
                    val t = it["title"] as? String ?: ""
                    !isOriginalOrEnglishTranslation(t)
                }
                if (dubVariant != null) return dubVariant
            }
        }

        return null
    }

    /**
     * Selects the most optimal translation for the user, prioritizing preferredName,
     * Russian dubbing, top studios, and professional multi-voice.
     */
    fun bestTranslation(translations: List<AllohaTranslation>, preferredName: String? = null): AllohaTranslation? {
        if (translations.isEmpty()) return null

        // 1. Exact match with preferred voice
        if (!preferredName.isNullOrBlank()) {
            translations.firstOrNull { matches(it.name, preferredName, exactOnly = true) }?.let { return it }
        }

        // 1b. Fuzzy match with preferred voice
        if (!preferredName.isNullOrBlank()) {
            translations.firstOrNull { matches(it.name, preferredName, exactOnly = false) }?.let { return it }
        }

        // 2. Russian official dubbing
        translations.firstOrNull {
            val n = it.name.lowercase(Locale.ROOT)
            val tag = detectLanguageTag(it.name)
            (n.contains("дубл") || n.contains("дуб") || n.contains("полное дублирование")) &&
                    (tag == null || tag == "rus") &&
                    !isOriginalOrEnglishTranslation(it.name)
        }?.let { return it }

        // 3. Known top dubbing studios
        val topStudios = listOf("пифагор", "невафильм", "red head sound", "rhs", "flarrow")
        translations.firstOrNull { t ->
            val n = t.name.lowercase(Locale.ROOT)
            topStudios.any { n.contains(it) } && isRussianTranslation(t.name)
        }?.let { return it }

        // 4. Professional multi-voice (HDRezka, LostFilm, Кубик в кубе, etc.)
        val proKeywords = listOf("профессиональн", "проф", "многоголос", "hdrezka", "lostfilm", "кубик в кубе")
        translations.firstOrNull { t ->
            val n = t.name.lowercase(Locale.ROOT)
            proKeywords.any { n.contains(it) } && isRussianTranslation(t.name)
        }?.let { return it }

        // 5. Any Russian translation
        translations.firstOrNull { isRussianTranslation(it.name) }?.let { return it }

        // 6. First available
        return translations.firstOrNull()
    }
}
