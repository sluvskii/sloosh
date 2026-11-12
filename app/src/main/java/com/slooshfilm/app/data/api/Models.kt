package com.slooshfilm.app.data.api

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.net.URLEncoder

private val cartoonGenreIds = setOf("16", "мультфильм", "аниме")
private val cartoonGenreNames = setOf("мультфильм", "аниме", "animation")
private val animeGenreAliases = setOf("anime", "аниме")
private val talkShowGenreIds = setOf("10767", "10763", "10764")
private val talkShowGenreNameParts = listOf(
    "ток-шоу", "talk", "реалити", "reality", "новости", "news", "телешоу",
    "тв-шоу", "тв шоу", "интервью", "interview"
)
private val talkShowTitleParts = listOf(
    "the tonight show", "jimmy kimmel live", "the late late show", "the late show with",
    "late night with", "the graham norton show", "the daily show", "the ellen degeneres show",
    "the kelly clarkson show", "the drew barrymore show", "last week tonight",
    "saturday night live", "good morning america", "entertainment tonight",
    "inside the actors studio", "hot ones", "73 questions", "actors on actors",
    "вечерний ургант", "шоу воли", "вдудь", "осторожно: собчак", "алена, блин",
    "алёна, блин", "что было дальше", "comedy club", "камеди клаб", "голубой огонёк",
    "голубой огонек", "песня года", "пусть говорят", "прямой эфир", "мужское / женское",
    "давай поженимся", "битва экстрасенсов", "пацанки", "выжить в дубае", "звёзды в африке",
    "academy awards", "golden globe awards", "emmy awards", "grammy awards", "премия муз-тв",
    "премия muz-tv"
)

@Immutable
data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: T?
)

@Immutable
data class MediaResponse(
    @SerializedName("page") val page: Int?,
    @SerializedName("results") val results: List<MediaDto>?,
    @SerializedName("items") val items: List<MediaDto>? = null,
    @SerializedName("pages") val pages: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("total_results") val totalResults: Int?
) {
    val allItems: List<MediaDto> get() = items ?: results ?: emptyList()
    val effectiveTotalPages: Int get() = pages ?: totalPages ?: 1
    val effectiveTotalResults: Int get() = total ?: totalResults ?: allItems.size
}

@Immutable
data class MediaDto(
    @SerializedName("id") val originalId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("year") val year: Any?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("ratings") val ratings: RatingsV2Dto? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("genres") val genres: List<GenreDto>? = null,
    @SerializedName("externalIds") val externalIds: ExternalIdsDto? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("countries") val countries: List<String>? = null,
    @SerializedName("logo") val logo: String? = null
) {
    val identifier: String
        get() {
            if (!originalId.isNullOrEmpty()) return originalId
            val titlePart = (title ?: name ?: originalTitle ?: "unknown").trim().lowercase()
            val yearPart = year?.toString() ?: ""
            val posterPart = (poster ?: posterUrl ?: posterPath ?: "").trim().lowercase()
            val typePart = (type ?: "unknown").lowercase()
            return "fallback|$typePart|$titlePart|$yearPart|$posterPart"
        }

    val displayTitle: String get() = title ?: name ?: originalTitle ?: "Без названия"

    val yearString: String get() = when (year) {
        is Double -> year.toInt().toString()
        else -> year?.toString() ?: ""
    }

    fun getDisplayPosterUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        val rawUrl = poster ?: posterUrl ?: posterPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality, quality = quality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        val rawUrl = backdrop ?: backdropPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality, quality = quality, isBackdrop = true) ?: getDisplayPosterUrl(isLowQuality = isLowQuality, quality = quality)
    }

    val displayLogoUrl: String?
        get() = normalizeImageUrl(path = logo, id = originalId, isLowQuality = false, isLogo = true)

    fun getDisplayLogoUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        return normalizeImageUrl(path = logo, id = originalId, isLowQuality = isLowQuality, quality = quality, isLogo = true)
    }

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            if (typeLower in tvTypeAliases) return true
            if (typeLower in movieTypeAliases) return false
            return originalId?.startsWith("tv_", ignoreCase = true) == true
        }

    val isCartoon: Boolean
        get() {
            val genreIds = genres?.mapNotNull { it.id?.lowercase() }.orEmpty()
            val genreNames = genres?.mapNotNull { it.name?.lowercase() }.orEmpty()
            return genreIds.any { it in cartoonGenreIds } || genreNames.any { it in cartoonGenreNames }
        }

    val isAnime: Boolean
        get() {
            val genreIds = genres?.mapNotNull { it.id?.lowercase() }.orEmpty()
            val genreNames = genres?.mapNotNull { it.name?.lowercase() }.orEmpty()
            if (genreIds.any { it in animeGenreAliases } ||
                genreNames.any { it in animeGenreAliases }
            ) return true

            val isJapan = countries.orEmpty().any {
                val country = it.lowercase()
                country.contains("япония") || country.contains("japan") || country == "jp"
            }
            val hasAnimationGenre = genreIds.any { it == "16" || it == "мультфильм" } ||
                genreNames.any { it == "мультфильм" || it == "animation" }
            return isJapan && hasAnimationGenre
        }

    val isTalkShowOrReality: Boolean
        get() {
            val genreIds = genres?.mapNotNull { it.id?.lowercase() }.orEmpty()
            if (genreIds.any { it in talkShowGenreIds }) return true
            val hasTalkGenre = genres?.mapNotNull { it.name?.lowercase() }?.any { name ->
                talkShowGenreNameParts.any { name.contains(it) }
            } == true
            if (hasTalkGenre) return true
            val normalizedTitle = (title ?: name ?: originalTitle).orEmpty().lowercase()
            return talkShowTitleParts.any { normalizedTitle.contains(it) }
        }

    val isMovie: Boolean
        get() = !isTvSeries && !isCartoon

    val id: String get() = originalId ?: identifier
    val cleanId: String get() = id.replace("tv_", "").replace("movie_", "")
    val favoriteIdentity: String get() = "${canonicalMediaType(type, originalId)}:$cleanId"

    val kpId: Int? get() = externalIds?.effectiveKp?.takeIf { it > 0 } ?: if (originalId?.startsWith("kp_") == true) cleanId.toIntOrNull()?.takeIf { it > 0 } else null
    val tmdbId: Int? get() = externalIds?.effectiveTmdb?.takeIf { it > 0 } ?: if (originalId?.startsWith("kp_") != true) cleanId.toIntOrNull()?.takeIf { it > 0 } else null
    val imdbId: String? get() = externalIds?.effectiveImdb
}

private val tvTypeAliases = setOf("tv", "tv_series", "series", "mini_series", "serial", "show")
private val movieTypeAliases = setOf("movie", "film", "cinema")

fun canonicalMediaType(type: String?, id: String? = null): String {
    val normalizedType = type?.trim()?.lowercase()
    return when {
        normalizedType != null && normalizedType in tvTypeAliases -> "tv"
        normalizedType != null && normalizedType in movieTypeAliases -> "movie"
        id?.startsWith("tv_", ignoreCase = true) == true -> "tv"
        else -> "movie"
    }
}

private val countryCodeToRussian = mapOf(
    "US" to "США", "USA" to "США", "GB" to "Великобритания", "UK" to "Великобритания",
    "RU" to "Россия", "SU" to "СССР", "FR" to "Франция", "DE" to "Германия",
    "IT" to "Италия", "ES" to "Испания", "JP" to "Япония", "KR" to "Южная Корея",
    "CN" to "Китай", "HK" to "Гонконг", "TW" to "Тайвань", "CA" to "Канада",
    "AU" to "Австралия", "IN" to "Индия", "TR" to "Турция", "SE" to "Швеция",
    "NO" to "Норвегия", "DK" to "Дания", "FI" to "Финляндия", "NL" to "Нидерланды",
    "BE" to "Бельгия", "PL" to "Польша", "CZ" to "Чехия", "AT" to "Австрия",
    "CH" to "Швейцария", "IE" to "Ирландия", "NZ" to "Новая Зеландия", "ZA" to "ЮАР",
    "IL" to "Израиль", "UA" to "Украина", "BY" to "Беларусь", "KZ" to "Казахстан",
    "TH" to "Таиланд", "ID" to "Индонезия", "IS" to "Исландия", "GR" to "Греция",
    "PT" to "Португалия", "MX" to "Мексика", "BR" to "Бразилия", "AR" to "Аргентина",
    "AE" to "ОАЭ", "EG" to "Египет", "GE" to "Грузия", "AM" to "Армения"
)

private val englishCountryNameToRussian = mapOf(
    "united states of america" to "США", "united states" to "США", "usa" to "США",
    "united kingdom" to "Великобритания", "great britain" to "Великобритания", "uk" to "Великобритания",
    "england" to "Великобритания", "russia" to "Россия", "russian federation" to "Россия",
    "soviet union" to "СССР", "ussr" to "СССР", "france" to "Франция", "germany" to "Германия",
    "italy" to "Италия", "spain" to "Испания", "japan" to "Япония", "south korea" to "Южная Корея",
    "korea, republic of" to "Южная Корея", "republic of korea" to "Южная Корея", "korea" to "Южная Корея",
    "china" to "Китай", "hong kong" to "Гонконг", "taiwan" to "Тайвань", "canada" to "Канада",
    "australia" to "Австралия", "india" to "Индия", "turkey" to "Турция", "türkiye" to "Турция",
    "sweden" to "Швеция", "norway" to "Норвегия", "denmark" to "Дания", "finland" to "Финляндия",
    "netherlands" to "Нидерланды", "belgium" to "Бельгия", "poland" to "Польша",
    "czech republic" to "Чехия", "czechia" to "Чехия", "austria" to "Австрия", "switzerland" to "Швейцария",
    "ireland" to "Ирландия", "new zealand" to "Новая Зеландия", "south africa" to "ЮАР",
    "israel" to "Израиль", "ukraine" to "Украина", "belarus" to "Беларусь", "kazakhstan" to "Казахстан",
    "thailand" to "Таиланд", "indonesia" to "Индонезия", "philippines" to "Филиппины",
    "iceland" to "Исландия", "greece" to "Греция", "portugal" to "Португалия",
    "united arab emirates" to "ОАЭ", "uae" to "ОАЭ", "egypt" to "Египет", "georgia" to "Грузия",
    "armenia" to "Армения", "mexico" to "Мексика", "brazil" to "Бразилия", "argentina" to "Аргентина"
)

fun formatCountryName(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""

    val upper = trimmed.uppercase(Locale.ROOT)
    countryCodeToRussian[upper]?.let { return it }
    englishCountryNameToRussian[trimmed.lowercase(Locale.ROOT)]?.let { return it }

    if (upper.length == 2) {
        val localized = Locale("", upper).getDisplayCountry(Locale("ru", "RU"))
        if (localized.isNotEmpty()) return localized
    }
    return trimmed
}

@Immutable
data class MediaDetailsDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("countries") val countries: List<String>?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("ratings") val ratings: RatingsV2Dto?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("backdrop") val backdrop: String?,
    @SerializedName("backdrops") val backdrops: List<String>?,
    @SerializedName("cast") val cast: List<CastMemberDto>?,
    @SerializedName("crew") val crew: List<CrewMemberDto>?,
    @SerializedName("trailers") val trailers: List<TrailerDto>?,
    @SerializedName("seasons") val seasons: List<TvSeasonDto>?,
    @SerializedName("collection") val collection: MovieCollectionDto?,
    @SerializedName("similar") val similar: List<MediaDto>?,
    @SerializedName("studios") val studios: List<StudioDto>?,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("budget") val budget: Long? = null,
    @SerializedName("revenue") val revenue: Long? = null,
    @SerializedName("ageRating") val ageRating: String? = null,
    @SerializedName("productionCompanies") val productionCompanies: List<StudioDto>? = null,
    @SerializedName("networks") val networks: List<StudioDto>? = null,
    @SerializedName("ids") val ids: IdsDto? = null,
    @SerializedName("externalIds") val externalIds: ExternalIdsDto? = null
) {
    val kpId: Int? get() = (ids?.kp ?: externalIds?.effectiveKp)?.takeIf { it > 0 } ?: if (id?.startsWith("kp_") == true) cleanId.toIntOrNull()?.takeIf { it > 0 } else null
    val tmdbId: Int? get() = (externalIds?.effectiveTmdb ?: ids?.tmdb ?: if (id?.startsWith("kp_") != true) cleanId.toIntOrNull() else null)?.takeIf { it > 0 }
    val imdbId: String? get() = externalIds?.effectiveImdb ?: ids?.imdb
    fun getDisplayPosterUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        return normalizeImageUrl(path = poster, id = id, isLowQuality = isLowQuality, quality = quality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        val path = backdrop ?: backdrops?.firstOrNull()
        return normalizeImageUrl(path = path, id = id, isLowQuality = isLowQuality, quality = quality, isBackdrop = true) ?: getDisplayPosterUrl(isLowQuality = isLowQuality, quality = quality)
    }

    val displayBackdropUrls: List<String>
        get() {
            val list = mutableListOf<String>()
            backdrops?.forEach { b ->
                val norm = normalizeImageUrl(path = b, id = id, isLowQuality = false, isBackdrop = true)
                if (!norm.isNullOrBlank() && !list.contains(norm)) {
                    list.add(norm)
                }
            }
            if (list.isEmpty()) {
                val single = getDisplayBackdropUrl(isLowQuality = false)
                if (!single.isNullOrBlank()) list.add(single)
            }
            return list
        }

    val displayLogoUrl: String?
        get() = normalizeImageUrl(path = logo, id = id, isLowQuality = false, isLogo = true)

    fun getDisplayLogoUrl(isLowQuality: Boolean = false, quality: String? = null): String? {
        return normalizeImageUrl(path = logo, id = id, isLowQuality = isLowQuality, quality = quality, isLogo = true)
    }

    val formattedBudget: String?
        get() = formatMoney(budget)

    val formattedRevenue: String?
        get() = formatMoney(revenue)

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            return typeLower in tvTypeAliases ||
                id?.startsWith("tv_", ignoreCase = true) == true ||
                (seasons != null && seasons.isNotEmpty())
        }

    val displayRating: Double get() = ratings?.kp ?: ratings?.imdb ?: ratings?.tmdb ?: 0.0
    val cleanId: String get() = (id ?: "").replace("tv_", "").replace("movie_", "")

    val primaryTrailerKey: String?
        get() {
            return trailers?.firstNotNullOfOrNull { trailer ->
                val url = trailer.url ?: ""
                when {
                    url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                    url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
                    url.contains("embed/") -> url.substringAfter("embed/").substringBefore("?")
                    !trailer.id.isNullOrBlank() && (trailer.site?.contains("youtube", ignoreCase = true) == true || trailer.site == null) -> trailer.id
                    else -> null
                }
            }
        }

    fun toMediaDto(): MediaDto = MediaDto(
        originalId = id,
        title = title,
        originalTitle = originalTitle,
        year = year,
        rating = displayRating,
        poster = poster,
        description = description,
        type = type,
        backdrop = backdrop,
        logo = logo
    )

    companion object {
        fun formatMoney(amount: Long?): String? {
            if (amount == null || amount <= 0) return null
            return when {
                amount >= 1_000_000_000 -> String.format(java.util.Locale.US, "$%.1f млрд", amount / 1_000_000_000.0).replace(".0", "")
                amount >= 1_000_000 -> String.format(java.util.Locale.US, "$%.1f млн", amount / 1_000_000.0).replace(".0", "")
                amount >= 1_000 -> String.format(java.util.Locale.US, "$%.0f тыс.", amount / 1_000.0)
                else -> "$$amount"
            }
        }
    }
}

@Immutable
data class RatingsV2Dto(
    @SerializedName("kp") val kp: Double?,
    @SerializedName("imdb") val imdb: Double?,
    @SerializedName("tmdb") val tmdb: Double? = null
)

@Immutable
data class GenreDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

@Immutable
data class ExternalIdsDto(
    @SerializedName("kp") val kp: Int? = null,
    @SerializedName("tmdb") val tmdb: Int? = null,
    @SerializedName("imdb") val imdb: String? = null,
    @SerializedName("kpId") val kpId: Int? = null,
    @SerializedName("tmdbId") val tmdbId: Int? = null,
    @SerializedName("imdbId") val imdbId: String? = null
) {
    val effectiveKp: Int? get() = kp ?: kpId
    val effectiveTmdb: Int? get() = tmdb ?: tmdbId
    val effectiveImdb: String? get() = imdb ?: imdbId
}

data class IdsDto(
    @SerializedName("kp") val kp: Int? = null,
    @SerializedName("imdb") val imdb: String? = null,
    @SerializedName("tmdb") val tmdb: Int? = null
)

data class CastMemberDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("photo") val photo: String?
) {
    fun getDisplayPhotoUrl(): String? = normalizeImageUrl(path = photo, id = id, isLowQuality = true)
}

data class CrewMemberDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("photo") val photo: String?
) {
    fun getDisplayPhotoUrl(): String? = normalizeImageUrl(path = photo, id = id, isLowQuality = true)
}

data class TrailerDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("site") val site: String?
) {
    val youtubeKey: String?
        get() {
            val u = url ?: ""
            return when {
                u.contains("v=") -> u.substringAfter("v=").substringBefore("&")
                u.contains("youtu.be/") -> u.substringAfter("youtu.be/").substringBefore("?")
                u.contains("embed/") -> u.substringAfter("embed/").substringBefore("?")
                !id.isNullOrBlank() && (site?.contains("youtube", ignoreCase = true) == true || site == null) -> id
                else -> null
            }
        }

    val thumbnailUrl: String?
        get() = youtubeKey?.let { "https://img.youtube.com/vi/$it/mqdefault.jpg" }
}

data class TvSeasonDto(
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("episodes") val episodes: List<TvEpisodeDetailsDto>? = null,
    @SerializedName("episodeCount") val episodeCount: Int? = null,
    @SerializedName("airDate") val airDate: String? = null,
    @SerializedName("poster") val poster: String? = null
)

data class TvEpisodeDetailsDto(
    @SerializedName("episodeNumber") val episodeNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("still") val still: String?,
    @SerializedName("airDate") val airDate: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("duration") val duration: Int?
) {
    fun getDisplayStillUrl(): String? = normalizeImageUrl(path = still, id = null, isLowQuality = true)
}

data class MovieCollectionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("items") val items: List<MediaDto>? = null,
    @SerializedName("parts") val parts: List<MediaDto>? = null
) {
    val allParts: List<MediaDto> get() = parts ?: items ?: emptyList()
}

data class RelatedStudioResponseDto(
    @SerializedName("items") val items: List<MediaDto>? = null,
    @SerializedName("results") val results: List<MediaDto>? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("totalPages") val totalPages: Int? = null
) {
    val allItems: List<MediaDto> get() = items ?: results ?: emptyList()
}

data class StudioDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("logo") val logo: String?
)

data class PersonDetailDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("originalName") val originalName: String?,
    @SerializedName("photo") val photo: String?,
    @SerializedName("biography") val biography: String?,
    @SerializedName("facts") val facts: List<String>?,
    @SerializedName("movies") val movies: List<MediaDto>?
)

data class CategorySectionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("items") val items: List<MediaDto>?
)

data class StreamConfigDto(
    @SerializedName("tokens") val tokens: List<String>?
)

fun normalizeImageUrl(
    path: String?,
    id: String? = null,
    isLowQuality: Boolean = false,
    quality: String? = null,
    isBackdrop: Boolean = false,
    isLogo: Boolean = false
): String? {
    if (path.isNullOrBlank()) return null
    val base = MoviesApi.activeImagesBaseUrl
    val proxyBase = "$base/api/v1/images/tmdb"

    // Map quality options ("Низкое", "Среднее", "Высокое") to native TMDB sizes supported by sloosh-api proxy
    val size = when {
        isLogo -> {
            when {
                quality == "Низкое" || isLowQuality -> "w300"
                quality == "Среднее" -> "w500"
                else -> "original"
            }
        }
        isBackdrop -> {
            when {
                quality == "Низкое" || isLowQuality -> "w780"
                quality == "Среднее" -> "w1280"
                quality == "Высокое" -> "w1280"
                else -> "w1280"
            }
        }
        else -> {
            when {
                quality == "Низкое" || isLowQuality -> "w185" // 20 KB ultra-fast thumbnail
                quality == "Среднее" -> "w342"               // 60 KB crisp retina card
                quality == "Высокое" -> "w500"               // 119 KB high-res
                else -> "w342"
            }
        }
    }

    if (path.startsWith("http://") || path.startsWith("https://")) {
        // If the URL is already routed through our image proxy (/api/v1/images/tmdb/w500/...)
        // adapt the size segment to match the requested quality setting
        if (path.contains("/api/v1/images/tmdb/")) {
            val tmdbPath = path.substringAfter("/api/v1/images/tmdb/").substringAfter("/")
            return "$proxyBase/$size/$tmdbPath"
        }
        if (path.contains("api-sloosh.vercel.app")) {
            val replaced = path.replace("https://api-sloosh.vercel.app", base)
                .replace("http://api-sloosh.vercel.app", base)
            if (replaced.contains("/api/v1/images/tmdb/")) {
                val tmdbPath = replaced.substringAfter("/api/v1/images/tmdb/").substringAfter("/")
                return "$proxyBase/$size/$tmdbPath"
            }
            return replaced
        }
        if (path.contains("image.tmdb.org/t/p/")) {
            val tmdbPath = path.substringAfter("image.tmdb.org/t/p/").substringAfter("/")
            return "$proxyBase/$size/$tmdbPath"
        }
        if (path.contains("/api/v1/images/logos/")) {
            val targetSegment = if (size == "w300") "small" else "original"
            return path.replace(Regex("/api/v1/images/logos/([^/]+)/(original|small|w\\d+)"), "/api/v1/images/logos/$1/$targetSegment")
        }
        return path
    }
    val cleanPath = path.trimStart('/')
    return "$proxyBase/$size/$cleanPath"
}
