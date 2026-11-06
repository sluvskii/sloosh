package com.slooshfilm.app.data.alloha

import androidx.annotation.DrawableRes
import com.slooshfilm.app.R

/**
 * Круглые флаги стран и служебные значки для озвучек и субтитров.
 * 1:1 портировано из iOS LanguageFlag.swift.
 */
enum class LanguageFlag(
    val id: String,
    @DrawableRes val iconResId: Int
) {
    RU("ru", R.drawable.ic_flag_ru),
    UA("ua", R.drawable.ic_flag_ua),
    US("us", R.drawable.ic_flag_us),
    KZ("kz", R.drawable.ic_flag_kz),
    BY("by", R.drawable.ic_flag_by),
    GB("gb", R.drawable.ic_flag_gb),
    JP("jp", R.drawable.ic_flag_jp),
    KR("kr", R.drawable.ic_flag_kr),
    CN("cn", R.drawable.ic_flag_cn),
    FR("fr", R.drawable.ic_flag_fr),
    DE("de", R.drawable.ic_flag_de),
    TR("tr", R.drawable.ic_flag_tr),
    GE("ge", R.drawable.ic_flag_ge),
    ES("es", R.drawable.ic_flag_es),
    IT("it", R.drawable.ic_flag_it),
    SUBTITLES("subtitles", R.drawable.ic_flag_subtitles);

    companion object {
        /**
         * Определяет флаг языка по тексту перевода или эмодзи (1:1 iOS LanguageFlag.detect).
         */
        fun detect(text: String): LanguageFlag {
            val lower = text.lowercase()

            // 1. Субтитры
            if (lower.contains("субтитр") || lower.contains("subtitle") || text.contains("💬")) {
                return SUBTITLES
            }

            // 2. По эмодзи флагов
            if (text.contains("🇷🇺")) return RU
            if (text.contains("🇺🇦")) return UA
            if (text.contains("🇺🇸")) return US
            if (text.contains("🇰🇿")) return KZ
            if (text.contains("🇧🇾")) return BY
            if (text.contains("🇬🇧")) return GB
            if (text.contains("🇯🇵")) return JP
            if (text.contains("🇰🇷")) return KR
            if (text.contains("🇨🇳")) return CN
            if (text.contains("🇫🇷")) return FR
            if (text.contains("🇩🇪")) return DE
            if (text.contains("🇹🇷")) return TR
            if (text.contains("🇬🇪")) return GE
            if (text.contains("🇪🇸")) return ES
            if (text.contains("🇮🇹")) return IT

            // 3. По ключевым словам
            if (lower.contains("украин") || lower.contains("ukrain") || lower.contains("укр")) {
                return UA
            }
            if (lower.contains("казах") || lower.contains("kazakh") || lower.contains("каз")) {
                return KZ
            }
            if (lower.contains("белорус") || lower.contains("belarus") || lower.contains("бел")) {
                return BY
            }
            if (lower.contains("оригинал") || lower.contains("original") || lower.contains("english") ||
                lower.contains("английск") || lower.contains("англ") || lower.contains("usa")
            ) {
                return US
            }
            if (lower.contains("япон") || lower.contains("japan") || lower.contains("аниме")) {
                return JP
            }
            if (lower.contains("корей") || lower.contains("korean")) {
                return KR
            }
            if (lower.contains("китай") || lower.contains("chinese")) {
                return CN
            }
            if (lower.contains("француз") || lower.contains("french")) {
                return FR
            }
            if (lower.contains("немец") || lower.contains("german")) {
                return DE
            }
            if (lower.contains("турец") || lower.contains("turkish")) {
                return TR
            }
            if (lower.contains("грузин") || lower.contains("georgian")) {
                return GE
            }
            if (lower.contains("испан") || lower.contains("spanish")) {
                return ES
            }
            if (lower.contains("итальян") || lower.contains("italian")) {
                return IT
            }

            // По умолчанию для отечественных переводов и озвучек
            return RU
        }

        private val LEADING_EMOJIS = listOf(
            "🇷🇺", "🇺🇸", "🇺🇦", "🇬🇧", "🇰🇿", "🇧🇾", "🇯🇵", "🇰🇷", "🇨🇳", "🇫🇷", "🇩🇪", "🇹🇷", "🇬🇪", "🇪🇸", "🇮🇹", "💬"
        )

        /**
         * Аккуратно удаляет эмодзи флага в начале строки, не трогая само название (1:1 iOS LanguageFlag.stripLeadingEmoji).
         */
        fun stripLeadingEmoji(text: String): String {
            var s = text.trim()
            for (emoji in LEADING_EMOJIS) {
                if (s.startsWith(emoji)) {
                    s = s.removePrefix(emoji).trim()
                    break
                }
            }
            return s
        }
    }
}
