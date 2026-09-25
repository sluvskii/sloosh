package com.slooshfilm.app.data.api

import com.google.gson.annotations.SerializedName
import java.net.URLEncoder

data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: T?
)

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
    @SerializedName("backdrop_path") val backdropPath: String? = null
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

    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        val rawUrl = poster ?: posterUrl ?: posterPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false): String? {
        val rawUrl = backdrop ?: backdropPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality) ?: getDisplayPosterUrl(isLowQuality)
    }

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            return typeLower in listOf("tv", "series", "serial", "show")
        }

    val isCartoon: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            if (typeLower in listOf("cartoon", "animated", "anime")) return true
            return genres?.any { g ->
                val name = (g.name ?: g.id ?: "").lowercase()
                name.contains("мульт") || name.contains("аним")
            } == true
        }

    val isMovie: Boolean
        get() = !isTvSeries && !isCartoon

    val id: String get() = originalId ?: identifier
    val cleanId: String get() = id.replace("tv_", "").replace("movie_", "")
}

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
    @SerializedName("networks") val networks: List<StudioDto>? = null
) {
    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        return normalizeImageUrl(path = poster, id = id, isLowQuality = isLowQuality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false): String? {
        val path = backdrop ?: backdrops?.firstOrNull()
        return normalizeImageUrl(path = path, id = id, isLowQuality = isLowQuality) ?: getDisplayPosterUrl(isLowQuality)
    }

    val displayBackdropUrls: List<String>
        get() {
            val list = mutableListOf<String>()
            backdrops?.forEach { b ->
                val norm = normalizeImageUrl(path = b, id = id, isLowQuality = false)
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
        get() = normalizeImageUrl(path = logo, id = id, isLowQuality = false)

    val formattedBudget: String?
        get() = formatMoney(budget)

    val formattedRevenue: String?
        get() = formatMoney(revenue)

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            return typeLower in listOf("tv", "series", "serial", "show") || (seasons != null && seasons.isNotEmpty())
        }

    val displayRating: Double get() = rating ?: ratings?.kp ?: ratings?.imdb ?: 0.0
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
        backdrop = backdrop
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

data class RatingsV2Dto(
    @SerializedName("kp") val kp: Double?,
    @SerializedName("imdb") val imdb: Double?
)

data class GenreDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class ExternalIdsDto(
    @SerializedName("kpId") val kpId: Int?,
    @SerializedName("tmdbId") val tmdbId: Int?,
    @SerializedName("imdbId") val imdbId: String?
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
)

data class TrailerDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("site") val site: String?
)

data class TvSeasonDto(
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("episodes") val episodes: List<TvEpisodeDetailsDto>?
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
    @SerializedName("items") val items: List<MediaDto>?
)

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

fun normalizeImageUrl(path: String?, id: String? = null, isLowQuality: Boolean = false): String? {
    if (path.isNullOrBlank()) return null
    val base = MoviesApi.activeImagesBaseUrl
    val proxyBase = "$base/api/v1/images/tmdb"
    if (path.startsWith("http://") || path.startsWith("https://")) {
        if (path.contains("api-sloosh.vercel.app")) {
            return path.replace("https://api-sloosh.vercel.app", base)
                .replace("http://api-sloosh.vercel.app", base)
        }
        if (path.contains("image.tmdb.org/t/p/")) {
            val size = if (isLowQuality) "w500" else "w1280"
            val tmdbPath = path.substringAfter("image.tmdb.org/t/p/").substringAfter("/")
            return "$proxyBase/$size/$tmdbPath"
        }
        return path
    }
    val cleanPath = path.trimStart('/')
    val size = if (isLowQuality) "w500" else "w1280"
    return "$proxyBase/$size/$cleanPath"
}
