package com.slooshfilm.app.data.repository

import com.slooshfilm.app.data.api.MediaDetailsDto
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.api.MediaResponse
import com.slooshfilm.app.data.api.MovieCollectionDto
import com.slooshfilm.app.data.api.MoviesApi
import com.slooshfilm.app.data.api.RelatedStudioResponseDto
import com.slooshfilm.app.data.api.TvSeasonDto
import com.slooshfilm.app.data.api.StudioBrand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MoviesRepository {
    private val api = MoviesApi.service

    private val categoryCache = ConcurrentHashMap<String, List<MediaDto>>()
    private val detailsCache = ConcurrentHashMap<String, MediaDetailsDto>()

    suspend fun getPopularMovies(page: Int = 1, forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getPopularMovies(page).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getTopMovies(page: Int = 1, forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "top_movies_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getTopMovies(page).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getTopTv(page: Int = 1, forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "top_tv_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getTopTv(page).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getPopularTv(page: Int = 1, forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_tv_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getPopularTv(page).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getCartoons(page: Int = 1, forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "cartoons_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getCartoons(page).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getAnime(page: Int = 1, forceRefresh: Boolean = false, order: String = "popular"): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "anime_${order}_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getAnime(page, order).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getTrending(page: Int = 1, window: String = "week", forceRefresh: Boolean = false): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_${window}_$page"
        if (!forceRefresh && categoryCache.containsKey(cacheKey)) {
            return@withContext categoryCache[cacheKey] ?: emptyList()
        }
        val items = api.getTrending(page, window).data?.allItems ?: emptyList()
        if (page == 1 && items.isNotEmpty()) {
            categoryCache[cacheKey] = items
        }
        items
    }

    suspend fun getMediaDetails(id: String, isTv: Boolean, forceRefresh: Boolean = false): MediaDetailsDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("tv_", "").replace("movie_", "")
        val cacheKey = "${if (isTv) "tv" else "movie"}:$cleanId"
        if (!forceRefresh && detailsCache.containsKey(cacheKey)) {
            return@withContext detailsCache[cacheKey]
        }
        val response = if (isTv) {
            api.getTvDetails(cleanId)
        } else {
            api.getMovieDetails(cleanId)
        }
        val details = response.data
        if (details != null) {
            detailsCache[cacheKey] = details
        }
        details
    }

    suspend fun searchMovies(query: String, page: Int = 1): MediaResponse? = withContext(Dispatchers.IO) {
        api.searchMovies(query, page).data
    }

    suspend fun getGenreCatalog(genre: String, type: String?, page: Int = 1): MediaResponse? = withContext(Dispatchers.IO) {
        api.discoverMovies(
            type = type?.takeIf { it.isNotBlank() && it != "all" },
            genres = genre.trim().lowercase(java.util.Locale.ROOT),
            page = page
        ).data
    }

    suspend fun getStudioCatalog(studioId: String, page: Int = 1): MediaResponse? = withContext(Dispatchers.IO) {
        val response = try {
            api.getCollection(studioId, page).data
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        val rawItems = response?.allItems.orEmpty()
        if (rawItems.isNotEmpty()) {
            val qualityItems = rawItems.filter(::isQualityStudioItem)
            return@withContext if (qualityItems.isEmpty()) response else response?.copy(items = qualityItems, results = null)
        }

        val brand = StudioBrand.find(studioId) ?: return@withContext response
        val fallback = try {
            api.searchMovies(brand.name, page).data
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return@withContext null
        val qualityItems = fallback.allItems.filter(::isQualityStudioItem)
        fallback.copy(items = qualityItems, results = null)
    }

    private fun isQualityStudioItem(item: MediaDto): Boolean {
        val rawPoster = (item.posterUrl ?: item.posterPath ?: item.poster).orEmpty().trim()
        val displayPoster = item.getDisplayPosterUrl().orEmpty().trim()
        if (rawPoster.isEmpty() || rawPoster.contains("no-poster", ignoreCase = true) ||
            displayPoster.isEmpty() || displayPoster.contains("no-poster", ignoreCase = true)
        ) return false

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val year = item.yearString.toIntOrNull() ?: return false
        if (year < 1930 || year > currentYear + 1) return false

        val rating = item.rating?.takeIf { it > 0.0 }
            ?: item.ratings?.kp?.takeIf { it > 0.0 }
            ?: item.ratings?.imdb?.takeIf { it > 0.0 }
            ?: item.ratings?.tmdb?.takeIf { it > 0.0 }
            ?: 0.0
        if (rating > 0.0 && rating < 5.0) return false
        if (rating <= 0.0 && year < currentYear - 1) return false

        val junkGenres = listOf("короткометражк", "short", "документальн", "documentary", "новост", "news", "ток-шоу", "talk-show", "церемони", "ceremony", "концерт", "concert", "музык", "music")
        if (item.genres.orEmpty().any { genre ->
                val value = "${genre.name.orEmpty()} ${genre.id.orEmpty()}".lowercase()
                junkGenres.any { keyword -> value.contains(keyword) }
            }
        ) return false

        val title = "${item.displayTitle} ${item.originalTitle.orEmpty()}".lowercase()
        val junkTitles = listOf("короткометражк", "one-shot", "ван-шот", "короткий метр", "фильм о фильме", "making of", "behind the scenes", "за кадром", "клип", "music video", "реагируют", "reaction", "реакци", "coca-cola", "реклам", "commercial", "holiday special", "спешл", "специальный выпуск", "bloopers", "неудачные дубли", "смешные дубли", "тизер", "трейлер", "trailer", "teaser", "промо", "promo", "интервью", "interview", "бонусы", "bonus")
        return junkTitles.none { keyword -> title.contains(keyword) }
    }

    private val collectionCache = java.util.concurrent.ConcurrentHashMap<String, MovieCollectionDto>()
    suspend fun getMovieCollection(id: String, forceRefresh: Boolean = false): MovieCollectionDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("movie_", "").replace("tv_", "").replace("kp_", "")
        if (!forceRefresh) collectionCache[cleanId]?.let { return@withContext it }
        try {
            val res = api.getMovieCollection(cleanId).data
            if (res != null) collectionCache[cleanId] = res
            res
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private val relatedCache = java.util.concurrent.ConcurrentHashMap<String, RelatedStudioResponseDto>()
    suspend fun getRelatedByStudio(type: String, id: String, forceRefresh: Boolean = false): RelatedStudioResponseDto? = withContext(Dispatchers.IO) {
        val cleanType = if (type.contains("tv", ignoreCase = true) || type.contains("serial", ignoreCase = true)) "tv" else "movie"
        val cleanId = id.replace("movie_", "").replace("tv_", "").replace("kp_", "")
        val cacheKey = "$cleanType:$cleanId"
        if (!forceRefresh) relatedCache[cacheKey]?.let { return@withContext it }
        try {
            val res = api.getRelatedByStudio(cleanType, cleanId).data
            if (res != null) relatedCache[cacheKey] = res
            res
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private val seasonCache = java.util.concurrent.ConcurrentHashMap<String, TvSeasonDto>()
    suspend fun getSeason(id: String, seasonNumber: Int, forceRefresh: Boolean = false): TvSeasonDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("tv_", "").replace("movie_", "").replace("kp_", "")
        val cacheKey = "$cleanId:$seasonNumber"
        if (!forceRefresh) seasonCache[cacheKey]?.let { return@withContext it }
        try {
            val res = api.getSeason(cleanId, seasonNumber).data
            if (res != null) seasonCache[cacheKey] = res
            res
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: MoviesRepository? = null

        fun getInstance(): MoviesRepository = instance ?: synchronized(this) {
            instance ?: MoviesRepository().also { instance = it }
        }

        fun getInstance(context: android.content.Context): MoviesRepository = getInstance()
    }
}
