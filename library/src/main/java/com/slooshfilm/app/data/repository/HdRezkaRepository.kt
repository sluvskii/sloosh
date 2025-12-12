package com.slooshfilm.app.data.repository

import com.slooshfilm.app.data.LocalStorage
import com.slooshfilm.app.data.hdrezka.HdRezkaApi
import com.slooshfilm.app.data.hdrezka.Notification
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.model.MovieDetails
import com.slooshfilm.app.data.models.Bookmark
import com.slooshfilm.app.data.models.BookmarksPage
import com.slooshfilm.app.data.parser.ConfigProvider
import com.slooshfilm.app.data.parser.UniversalParser
import com.slooshfilm.app.data.hdrezka.StreamInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ContentType(val value: String) {
    FILMS("films"),
    SERIES("series")
}

enum class ContentCategory(val value: String, val ru: String) {
    NEW("new", "Новинки"),
    BEST("best", "Популярные"),
    WATCHING_NOW("watching-now", "Сейчас смотрят")
}

class HdRezkaRepository(private val localStorage: LocalStorage) {

    private val universalParser = UniversalParser()
    private val configProvider = ConfigProvider()
    private val api = HdRezkaApi(localStorage, universalParser, configProvider)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private var bookmarksPageCache: BookmarksPage? = null
    private val membershipCache = mutableMapOf<String, Pair<Long, List<Pair<Bookmark, Boolean>>>>()
    private val watchLaterCache = mutableMapOf<Int, List<Movie>>()
    // Cache episodes per postId+translatorId to avoid refetching on repeated dialog opens
    private val episodesCache = mutableMapOf<String, Map<Int, List<Episode>>>()

    init {
        scope.launch {
            _isLoggedIn.value = api.isLoggedIn()
            if (_isLoggedIn.value) {
                loadCaches()
            }
        }
    }

    private suspend fun loadCaches() {
        bookmarksPageCache = api.getBookmarksPage()
    }

    private fun invalidateCaches() {
        bookmarksPageCache = null
        watchLaterCache.clear()
        membershipCache.clear()
        scope.launch {
            if(isLoggedIn.value) loadCaches()
        }
    }

    private fun parseFilmType(movie: Movie): String {
        if (movie.isSeries) {
            return if (movie.title.contains("мульт", ignoreCase = true)) "cartoon-series" 
                   else if (movie.title.contains("аниме", ignoreCase = true)) "anime-series"
                   else "series"
        }
        return if (movie.title.contains("мульт", ignoreCase = true)) "cartoon" 
               else if (movie.title.contains("аниме", ignoreCase = true)) "anime"
               else "film"
    }

    suspend fun getContent(contentType: ContentType, category: ContentCategory, page: Int): Pair<List<Movie>, Int> {
        val baseUrl = api.getWorkingMirror() ?: return Pair(emptyList(), 1)

        var pageUrl = when (category) {
            ContentCategory.WATCHING_NOW -> "/?filter=watching"
            ContentCategory.BEST -> "/?filter=popular"
            ContentCategory.NEW -> "/?filter=new"
        }

        if (contentType == ContentType.FILMS) {
            pageUrl = if (pageUrl.contains('?')) "$pageUrl&genre=1" else "$pageUrl?genre=1"
        }

        val (allContent, totalPages) = api.getPage(pageUrl, page)

        allContent.forEach { it.type = parseFilmType(it) }

        val filtered = when (contentType) {
            ContentType.FILMS -> allContent.filter { !it.isSeries }
            ContentType.SERIES -> allContent.filter { it.isSeries }
        }

        return Pair(filtered, totalPages)
    }
    
    // ... (rest of the file remains the same)
    suspend fun getBookmarksPage(): BookmarksPage {
        if (!_isLoggedIn.value) return BookmarksPage(emptyList(), emptyList())
        if (bookmarksPageCache == null) {
            bookmarksPageCache = api.getBookmarksPage()
        }
        return bookmarksPageCache ?: BookmarksPage(emptyList(), emptyList())
    }

    suspend fun getFilmsForBookmark(bookmark: Bookmark, sort: String? = "added", genre: String? = null): List<Movie> {
        // build url: /favorites/{id}/page/1/?filter={sort}[&genre={genre}]
        val base = bookmark.link.trimEnd('/')
        var url = "$base/page/1/"
        url += if (!sort.isNullOrBlank()) "?filter=$sort" else "?filter=added"
        if (!genre.isNullOrBlank()) {
            url += "&genre=$genre"
        }
        val films = api.getFilmsFromBookmarkPage(url)
        films.forEach { it.type = parseFilmType(it) }
        return films
    }

    suspend fun renameBookmarkCategory(catId: String, newName: String): Boolean {
        val result = api.renameBookmarkCategory(catId, newName)
        if (result) invalidateCaches()
        return result
    }

    suspend fun deleteBookmarkCategory(catId: String): Boolean {
        val result = api.deleteBookmarkCategory(catId)
        if (result) invalidateCaches()
        return result
    }

    suspend fun createBookmarkCategory(name: String): Boolean {
        val result = api.createBookmarkCategory(name)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    suspend fun search(query: String): List<Movie> {
        val films = api.search(query)
        films.forEach { it.type = parseFilmType(it) }
        return films
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return api.getSearchSuggestions(query)
    }

    suspend fun getMovieDetails(url: String): MovieDetails? {
        return api.getMovieDetails(url)
    }

    suspend fun getEpisodes(postId: String, translatorId: String, refererUrl: String? = null): Map<Int, List<Episode>> {
        val key = "$postId|$translatorId"
        val cached = episodesCache[key]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        val fetched = api.getEpisodes(postId, translatorId, refererUrl)
        if (fetched.isNotEmpty()) {
            episodesCache[key] = fetched
        }
        return fetched
    }

    suspend fun getStreamsForPost(postId: String, translatorId: String, season: String? = null, episode: String? = null): StreamInfo {
        return api.getStreamsForPost(postId, translatorId, season, episode)
    }

    suspend fun getNotifications(): List<Notification> {
        return api.getNotifications()
    }

    suspend fun login(login: String, password: String): Boolean {
        val loggedIn = api.login(login, password)
        _isLoggedIn.value = loggedIn
        if (loggedIn) {
            // Save username to preferences so the UI can display real login/email
            try {
                localStorage.putString("user_name", login)
            } catch (_: Exception) {}
            loadCaches()
        }
        return loggedIn
    }

    /** Returns username saved in preferences (if any). */
    fun getSavedUserName(): String? {
        return try {
            localStorage.getString("user_name", null)
        } catch (_: Exception) {
            null
        }
    }

    fun logout() {
        api.logout()
        _isLoggedIn.value = false
        bookmarksPageCache = null
        watchLaterCache.clear()
        try {
            localStorage.remove("user_name")
            localStorage.remove("user_avatar")
        } catch (_: Exception) {}
    }

    suspend fun getWatchLater(page: Int = 1): Pair<List<Movie>, Int> {
        if (!_isLoggedIn.value) return Pair(emptyList(), 0)
        if (watchLaterCache.containsKey(page)) {
            return Pair(watchLaterCache.getValue(page), watchLaterCache.size) // Не совсем точно, но для примера
        }
        val (movies, totalPages) = api.getWatchLater(page)
        if (movies.isNotEmpty()) {
            movies.forEach { it.type = parseFilmType(it) }
            watchLaterCache[page] = movies
        }
        return Pair(movies, totalPages)
    }

    suspend fun addBookmark(postId: String): Boolean {
        val result = api.addBookmark(postId)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    // Add a bookmark to a specific category (catId is the category id on rezka)
    suspend fun addBookmark(postId: String, catId: String): Boolean {
        val result = api.addBookmarkToCategory(postId, catId)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    suspend fun removeBookmark(postId: String): Boolean {
        val result = api.removeBookmark(postId)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    /** Remove a bookmark from a specific category. */
    suspend fun removeBookmark(postId: String, catId: String): Boolean {
        val result = api.removeBookmarkFromCategory(postId, catId)
        if (result) invalidateCaches()
        return result
    }

    /**
     * Returns list of pairs (Bookmark, containsPost) for the current user's bookmark categories.
     * This loads each category page and checks whether the given postId exists in that category.
     */
    suspend fun getBookmarkMembership(postId: String): List<Pair<Bookmark, Boolean>> {
        if (!_isLoggedIn.value) return emptyList()
        val page = getBookmarksPage()
        val cats = page.categories

        // Return cached membership if fresh (60s)
        val cached = membershipCache[postId]
        if (cached != null && System.currentTimeMillis() - cached.first < 60_000) {
            return cached.second
        }

        // Parallelize page checks for membership for faster results but limit concurrency to avoid IO overload
        val result = kotlinx.coroutines.coroutineScope {
            val semaphore = kotlinx.coroutines.sync.Semaphore(4)
            val deferred = cats.map { cat ->
                async(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        try {
                            val films = api.getFilmsFromBookmarkPage(cat.link)
                            val contains = films.any { it.postId == postId }
                            cat to contains
                        } catch (e: Exception) {
                            cat to false
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }
            deferred.map { it.await() }
        }

        // Save to cache
        membershipCache[postId] = System.currentTimeMillis() to result
        return result
    }

    /** Fast cached check for bookmark presence using local bookmarks page cache (non-blocking). */
    fun isBookmarkedCached(postId: String): Boolean {
        return bookmarksPageCache?.films?.any { it.postId == postId } ?: false
    }

    suspend fun addWatchLater(postId: String): Boolean {
        val result = api.addWatchLater(postId)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    suspend fun removeWatchLater(postId: String): Boolean {
        val result = api.removeWatchLater(postId)
        if (result) {
            invalidateCaches()
        }
        return result
    }

    suspend fun isBookmarked(postId: String): Boolean {
        if (!_isLoggedIn.value) return false
        // Check membership across all categories to be accurate (some mirrors return films only for the first category)
        return try {
            val membership = getBookmarkMembership(postId)
            membership.any { it.second }
        } catch (e: Exception) {
            // Fallback to cached page if membership check fails
            if (bookmarksPageCache == null) {
                loadCaches()
            }
            bookmarksPageCache?.films?.any { it.postId == postId } ?: false
        }
    }

    suspend fun isInWatchLater(postId: String): Boolean {
        if (!_isLoggedIn.value) return false
        
        // Эта проверка теперь неэффективна, т.к. кеш неполный
        // Для точной проверки нужно либо загружать все страницы, либо положиться на API
        // Пока что будем считать, что если фильм есть в кэше, то он в "досмотреть"
        return watchLaterCache.values.flatten().any { it.postId == postId }
    }

    suspend fun getComments(postId: String, page: Int = 1): Pair<List<com.slooshfilm.app.data.hdrezka.Comment>, Int> {
        return api.getComments(postId, page)
    }

    suspend fun getCommentsCount(postId: String, movieUrl: String? = null): Int {
        return api.getCommentsCount(postId, movieUrl)
    }

    suspend fun getActorDetails(link: String): com.slooshfilm.app.data.model.ActorDetails? {
        return api.getActorDetails(link)
    }
    
    suspend fun updateTime(postId: String, translatorId: String, season: String?, episode: String?, timeInSeconds: Long) {
        api.updateTime(postId, translatorId, season, episode, timeInSeconds)
    }
}