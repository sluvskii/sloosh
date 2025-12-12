# Улучшения парсера HDRezka API

## 📋 Содержание
1. [Улучшения парсинга потоков](#1-улучшения-парсинга-потоков)
2. [Новые функции](#2-новые-функции)
3. [Улучшения парсинга данных](#3-улучшения-парсинга-данных)
4. [Обработка ошибок и отказоустойчивость](#4-обработка-ошибок-и-отказоустойчивость)
5. [Оптимизация производительности](#5-оптимизация-производительности)
6. [Дополнительные возможности](#6-дополнительные-возможности)

---

## 1. Улучшения парсинга потоков

### 1.1 Улучшенное декодирование потоков

**Проблема:** Текущий метод `decodeStreamUrl` использует простой Base64 декодинг, но референсы показывают более сложный алгоритм.

**Решение:** Добавить более продвинутое декодирование, как в референсах:

```kotlin
// Вместо простого Base64, использовать более сложный алгоритм
private fun decryptStreams(encoded: String): String {
    // Референсы используют специальную функцию decrypt
    // Пока используем текущий метод, но можно улучшить
    var decoded = encoded
    
    // Удаление маркеров //_//
    while (decoded.contains("//_//")) {
        val index = decoded.indexOf("//_//")
        if (index != -1) {
            val endIndex = minOf(index + "//_//".length + 20, decoded.length)
            decoded = decoded.removeRange(index, endIndex)
        } else {
            break
        }
    }
    
    // Base64 декодирование
    return try {
        String(Base64.decode(decoded, Base64.DEFAULT))
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to decode streams", e)
        encoded
    }
}
```

### 1.2 Модификация CDN для потоков

**Проблема:** Нет возможности менять CDN для потоков, что может быть полезно для обхода блокировок.

**Решение:** Добавить функцию модификации CDN (как в `configApi.modifyCDN`):

```kotlin
fun modifyCDN(streams: Map<String, String>, cdn: String? = null): Map<String, String> {
    val selectedCDN = cdn ?: localStorage.getString("selected_cdn", "auto")
    
    if (selectedCDN == "auto") {
        return streams
    }
    
    return streams.mapValues { (_, url) ->
        // Заменяем домен в URL на выбранный CDN
        val cdnUrl = url.replace(Regex("https?://[^/]+"), "https://$selectedCDN")
        cdnUrl
    }
}
```

### 1.3 Парсинг субтитров

**Проблема:** Текущий парсер не извлекает информацию о субтитрах.

**Решение:** Добавить парсинг субтитров из ответа API:

```kotlin
data class Subtitle(
    val name: String,
    val languageCode: String,
    val url: String,
    val isDefault: Boolean = false
)

private fun parseSubtitles(
    subtitle: String?,
    subtitleDef: String,
    subtitleLns: Map<String, String>
): List<Subtitle> {
    if (subtitle.isNullOrEmpty()) return emptyList()
    
    val rawSubtitles = mutableListOf<Pair<String, String>>()
    subtitle.split(",").forEach { str ->
        val language = str.substring(1, str.indexOf(']'))
        val url = str.substring(str.indexOf(']') + 1)
        rawSubtitles.add(language to url)
    }
    
    return rawSubtitles.map { (name, url) ->
        Subtitle(
            name = name,
            languageCode = subtitleLns[name] ?: "",
            url = url,
            isDefault = subtitleLns[name] == subtitleDef
        )
    }
}

// Обновить getStreamsForPost чтобы возвращать субтитры
suspend fun getStreamsForPost(
    postId: String,
    translatorId: String,
    season: String? = null,
    episode: String? = null
): Pair<Map<String, String>, List<Subtitle>> {
    // ... существующий код ...
    
    val subtitles = parseSubtitles(
        json.optString("subtitle", null),
        json.optString("subtitle_def", ""),
        parseSubtitleLns(json.optJSONObject("subtitle_lns"))
    )
    
    return Pair(parseStreams(decodedStreams), subtitles)
}
```

### 1.4 Поддержка Storyboard (превью кадров)

**Новая функция:** Референсы показывают, что API возвращает URL для storyboard:

```kotlin
suspend fun getStreamsForPost(...): StreamInfo {
    // ... парсинг ...
    
    val storyboardUrl = json.optString("thumbnails", null)
    
    return StreamInfo(
        streams = parseStreams(decodedStreams),
        subtitles = subtitles,
        storyboardUrl = storyboardUrl?.let { "$baseUrl$it" }
    )
}

data class StreamInfo(
    val streams: Map<String, String>,
    val subtitles: List<Subtitle> = emptyList(),
    val storyboardUrl: String? = null
)
```

---

## 2. Новые функции

### 2.1 Поиск с подсказками (Autocomplete)

**Проблема:** Нет функции поиска с подсказками.

**Решение:** Добавить поиск подсказок (как в `searchApi.searchSuggestions`):

```kotlin
suspend fun searchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext emptyList()
    
    val formBody = FormBody.Builder()
        .add("q", query)
        .build()
    
    val request = Request.Builder()
        .url("$baseUrl/engine/ajax/search.php")
        .post(formBody)
        .build()
    
    try {
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        
        val suggestions = doc.select("li").mapNotNull { el ->
            el.select(".enty").firstOrNull()?.text()
        }.distinct()
        
        return@withContext suggestions
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to get search suggestions", e)
        return@withContext emptyList()
    }
}
```

### 2.2 Получение комментариев

**Новая функция:** Референсы показывают поддержку комментариев:

```kotlin
data class Comment(
    val id: String,
    val avatar: String,
    val username: String,
    val date: String,
    val text: String,
    val likes: Int,
    val indent: Int, // Уровень вложенности
    val replies: List<Comment> = emptyList()
)

suspend fun getComments(filmId: String, page: Int = 1): Pair<List<Comment>, Int> = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext Pair(emptyList(), 1)
    
    val queryParams = mapOf(
        "t" to System.currentTimeMillis().toString(),
        "news_id" to filmId,
        "cstart" to page.toString(),
        "type" to "0",
        "comment_id" to "0",
        "skin" to "hdrezka"
    )
    
    val url = HttpUrl.parse("$baseUrl/ajax/get_comments")?.newBuilder()?.apply {
        queryParams.forEach { addQueryParameter(it.key, it.value) }
    }?.build() ?: return@withContext Pair(emptyList(), 1)
    
    val request = Request.Builder().url(url).get().build()
    
    try {
        val response = client.newCall(request).execute()
        val jsonStr = response.body?.string() ?: return@withContext Pair(emptyList(), 1)
        val json = JSONObject(jsonStr)
        
        val commentsHtml = json.optString("comments", "")
        val navigationHtml = json.optString("navigation", "")
        
        val commentsDoc = Jsoup.parse(commentsHtml)
        val navDoc = Jsoup.parse(navigationHtml)
        
        val comments = commentsDoc.select(".comments-tree-item").mapNotNull { el ->
            try {
                Comment(
                    id = el.attr("data-id"),
                    avatar = el.select(".ava img").firstOrNull()?.absUrl("src") ?: "",
                    username = el.select(".name").firstOrNull()?.text() ?: "",
                    date = el.select(".date").firstOrNull()?.text()?.replace("оставлен ", "") ?: "",
                    text = el.select(".text").text(),
                    likes = el.select(".b-comment__like_it").firstOrNull()?.attr("data-likes_num")?.toIntOrNull() ?: 0,
                    indent = el.attr("data-indent").toIntOrNull() ?: 0
                )
            } catch (e: Exception) {
                Log.e("HdRezkaApi", "Failed to parse comment", e)
                null
            }
        }
        
        // Парсим общее количество страниц
        val navLinks = navDoc.select(".b-navigation a")
        val totalPages = if (navLinks.size >= 2) {
            navLinks[navLinks.size - 2].text().toIntOrNull() ?: 1
        } else {
            1
        }
        
        return@withContext Pair(comments, totalPages)
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to get comments", e)
        return@withContext Pair(emptyList(), 1)
    }
}
```

### 2.3 Получение уведомлений о новых сериях

**Новая функция:** Референсы показывают поддержку уведомлений:

```kotlin
data class SeriesUpdateNotification(
    val date: String,
    val items: List<UpdateItem>
)

data class UpdateItem(
    val name: String,
    val link: String,
    val info: String // "Сезон X - Серия Y"
)

suspend fun getUserNotifications(): List<SeriesUpdateNotification> = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext emptyList()
    val html = getHtml("$baseUrl/")
    val doc = Jsoup.parse(html, baseUrl)
    
    return@withContext doc.select(".b-seriesupdate__block").mapNotNull { block ->
        try {
            val date = block.select(".b-seriesupdate__block_date").firstOrNull()?.text()
                ?.replace(" развернуть", "")?.trim() ?: return@mapNotNull null
            
            val items = block.select(".tracked").mapNotNull { item ->
                val season = item.select(".season").firstOrNull()?.text() ?: ""
                val episode = item.select(".cell-2").firstOrNull()?.text() ?: ""
                val linkEl = item.select(".b-seriesupdate__block_list_link").firstOrNull()
                
                if (linkEl != null) {
                    UpdateItem(
                        name = linkEl.text(),
                        link = linkEl.absUrl("href"),
                        info = "$season - $episode"
                    )
                } else {
                    null
                }
            }
            
            SeriesUpdateNotification(date, items)
        } catch (e: Exception) {
            Log.e("HdRezkaApi", "Failed to parse notification block", e)
            null
        }
    }
}
```

### 2.4 Получение трейлера

**Новая функция:** Референсы показывают парсинг трейлера:

```kotlin
suspend fun getTrailer(filmId: String): String? = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext null
    
    val formBody = FormBody.Builder()
        .add("id", filmId)
        .build()
    
    val request = Request.Builder()
        .url("$baseUrl/engine/ajax/gettrailervideo.php")
        .post(formBody)
        .build()
    
    try {
        val response = client.newCall(request).execute()
        val jsonStr = response.body?.string() ?: return@withContext null
        val json = JSONObject(jsonStr)
        
        return if (json.optBoolean("success", false)) {
            json.optString("url", null)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to get trailer", e)
        return@withContext null
    }
}

// Или можно парсить из страницы фильма:
// val trailerUrl = doc.selectFirst("a.b-trailer__link")?.attr("href")
```

### 2.5 Сохранение прогресса просмотра

**Новая функция:** Референсы показывают сохранение прогресса:

```kotlin
suspend fun saveWatchProgress(
    postId: String,
    translatorId: String,
    season: String?,
    episode: String?,
    currentTime: Long = 1L
): Boolean = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext false
    
    val formBody = FormBody.Builder()
        .add("post_id", postId)
        .add("translator_id", translatorId)
        .add("season", season ?: "0")
        .add("episode", episode ?: "0")
        .add("current_time", currentTime.toString())
        .build()
    
    val request = Request.Builder()
        .url("$baseUrl/ajax/send_save")
        .post(formBody)
        .build()
    
    return@withContext try {
        val response = client.newCall(request).execute()
        // API может возвращать success=false, но это нормально
        response.isSuccessful
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to save watch progress", e)
        false
    }
}
```

---

## 3. Улучшения парсинга данных

### 3.1 Улучшенный парсинг актеров и режиссеров

**Проблема:** Текущий парсер извлекает только имена, без фото и ссылок.

**Решение:** Улучшить парсинг (как в `parseActorCard`):

```kotlin
data class Actor(
    val name: String,
    val photo: String?,
    val link: String?,
    val job: String? = null, // роль актера
    val isDirector: Boolean = false
)

private fun parseActors(doc: Document, isDirector: Boolean = false): List<Actor> {
    return doc.select(".persons-list-holder").flatMap { holder ->
        holder.select(".person-name-item").mapNotNull { el ->
            try {
                Actor(
                    name = el.select("span").firstOrNull()?.text() ?: "",
                    photo = el.attr("data-photo").takeIf { it != "null" },
                    link = el.select("a").firstOrNull()?.absUrl("href"),
                    job = el.attr("data-job").takeIf { it.isNotBlank() },
                    isDirector = isDirector
                )
            } catch (e: Exception) {
                Log.e("HdRezkaApi", "Failed to parse actor", e)
                null
            }
        }
    }
}

// Обновить getMovieDetails:
val directors = parseActors(
    doc.selectFirst("tr:contains(Режиссер)")?.select("td")?.getOrNull(1)?.let { 
        Jsoup.parse("<div>$it</div>") 
    } ?: Document(""),
    isDirector = true
)

val actors = parseActors(
    doc.selectFirst("tr:contains(В ролях)")?.select("td")?.getOrNull(1)?.let { 
        Jsoup.parse("<div>$it</div>") 
    } ?: Document(""),
    isDirector = false
)
```

### 3.2 Улучшенный парсинг рейтингов

**Проблема:** Текущий парсер не извлекает все доступные рейтинги.

**Решение:** Расширить парсинг рейтингов:

```kotlin
data class RatingInfo(
    val name: String, // "IMDb", "Кинопоиск", "Rezka"
    val rating: Double?,
    val votes: String?,
    val text: String
)

private fun parseRatings(doc: Document): List<RatingInfo> {
    val ratings = mutableListOf<RatingInfo>()
    
    // Парсим рейтинги из таблицы
    val ratingsRow = doc.select("tr:contains(Рейтинги)").firstOrNull()
    ratingsRow?.select("span")?.forEach { span ->
        val ratingEl = span.select("span").firstOrNull()
        val name = span.childNodes().firstOrNull()?.toString()?.trim() ?: ""
        val rating = ratingEl?.text()?.toDoubleOrNull()
        val votes = span.select("i").firstOrNull()?.text()
        
        if (name.isNotBlank() && rating != null) {
            ratings.add(
                RatingInfo(
                    name = name,
                    rating = rating,
                    votes = votes,
                    text = "$name: $rating ${votes?.let { "($it)" } ?: ""}"
                )
            )
        }
    }
    
    // Парсим основной рейтинг Rezka
    val mainRating = doc.select(".b-post__rating").firstOrNull()
    mainRating?.let { el ->
        val num = el.select(".num").firstOrNull()?.text()?.toDoubleOrNull()
        val votes = el.select(".votes").firstOrNull()?.text()
            ?.replace("(", "")?.replace(")", "")?.replaceAll(" ", "")
        
        if (num != null) {
            ratings.add(
                RatingInfo(
                    name = "Rezka",
                    rating = num,
                    votes = votes,
                    text = "$num ($votes)"
                )
            )
        }
    }
    
    return ratings
}
```

### 3.3 Парсинг дополнительной информации о фильме

**Новая функция:** Референсы показывают больше полей:

```kotlin
// Расширить MovieDetails:
data class MovieDetails(
    // ... существующие поля ...
    
    // Новые поля:
    val originalTitle: String? = null,
    val ageLimit: String? = null, // "16+", "18+"
    val duration: String? = null, // "120 мин."
    val releaseDate: String? = null,
    val countries: List<String> = emptyList(),
    val isPendingRelease: Boolean = false,
    val isRestricted: Boolean = false, // Требует авторизации
    val includedIn: List<CollectionInfo> = emptyList(), // Входит в списки
    val fromCollections: List<CollectionInfo> = emptyList(), // Из серии
    val mainRating: RatingInfo? = null, // Основной рейтинг Rezka
    val ratings: List<RatingInfo> = emptyList(), // Все рейтинги
    val directors: List<Actor> = emptyList(),
    val actors: List<Actor> = emptyList(),
    val fullSizePoster: String? = null
)

data class CollectionInfo(
    val name: String,
    val link: String,
    val position: String? = null
)

private fun parseIncludedIn(doc: Document): List<CollectionInfo> {
    return doc.select("tr:contains(Входит в списки)").firstOrNull()
        ?.select("td")?.getOrNull(1)?.select("a")?.mapNotNull { el ->
            CollectionInfo(
                name = el.text(),
                link = el.absUrl("href"),
                position = el.nextSibling()?.toString()?.trim()
            )
        } ?: emptyList()
}
```

### 3.4 Улучшенный парсинг сезонов и эпизодов

**Проблема:** Текущий парсер может не обрабатывать все случаи.

**Решение:** Улучшить парсинг сезонов (как в `parseSeasons`):

```kotlin
data class Season(
    val seasonId: String,
    val name: String,
    val episodes: List<Episode>,
    val isOnlyEpisodes: Boolean = false // Если нет явных сезонов
)

data class Episode(
    val seasonId: Int,
    val episodeId: Int,
    val name: String,
    val link: String = "",
    val isWatched: Boolean = false
)

suspend fun getSeasonsAndEpisodes(
    postId: String,
    translatorId: String
): List<Season> = withContext(Dispatchers.IO) {
    // ... существующий код getEpisodes ...
    
    val seasons = mutableListOf<Season>()
    
    // Парсим явные сезоны
    val seasonItems = doc.select(".b-simple_season__item")
    if (seasonItems.isNotEmpty()) {
        seasonItems.forEach { seasonEl ->
            val seasonId = seasonEl.attr("data-tab_id")
            val seasonName = seasonEl.text()
            
            val episodes = doc.select("#simple-episodes-list-$seasonId .b-simple_episode__item")
                .mapNotNull { epEl ->
                    val epId = epEl.attr("data-episode_id").toIntOrNull() ?: return@mapNotNull null
                    val name = epEl.text().trim()
                    val isWatched = epEl.hasClass("active")
                    
                    Episode(
                        seasonId = seasonId.toIntOrNull() ?: 1,
                        episodeId = epId,
                        name = name,
                        isWatched = isWatched
                    )
                }
            
            seasons.add(Season(seasonId, seasonName, episodes))
        }
    } else {
        // Если нет явных сезонов, обрабатываем все эпизоды как один сезон
        val episodeItems = doc.select(".b-simple_episode__item")
        if (episodeItems.isNotEmpty()) {
            val episodes = episodeItems.mapNotNull { epEl ->
                val seasonId = epEl.attr("data-season_id").toIntOrNull() ?: 1
                val epId = epEl.attr("data-episode_id").toIntOrNull() ?: return@mapNotNull null
                val name = epEl.text().trim()
                val isWatched = epEl.hasClass("active")
                
                Episode(seasonId, epId, name, isWatched = isWatched)
            }
            
            seasons.add(Season("1", "", episodes, isOnlyEpisodes = true))
        }
    }
    
    return@withContext seasons
}
```

---

## 4. Обработка ошибок и отказоустойчивость

### 4.1 Retry механизм для запросов

**Проблема:** Нет автоматического повторения запросов при ошибках.

**Решение:** Добавить retry логику:

```kotlin
suspend fun <T> executeWithRetry(
    maxRetries: Int = 3,
    retryDelay: Long = 1000L,
    block: suspend () -> T
): T {
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                Log.w("HdRezkaApi", "Attempt ${attempt + 1} failed, retrying...", e)
                delay(retryDelay * (attempt + 1)) // Exponential backoff
            }
        }
    }
    
    throw lastException ?: Exception("Failed after $maxRetries attempts")
}

// Использование:
suspend fun getMovieDetails(pageUrl: String): MovieDetails? = withContext(Dispatchers.IO) {
    executeWithRetry {
        // ... существующий код ...
    }
}
```

### 4.2 Улучшенная обработка ошибок API

**Проблема:** Не все ошибки API обрабатываются правильно.

**Решение:** Расширить обработку ошибок:

```kotlin
sealed class ApiError : Exception() {
    data class NetworkError(val message: String) : ApiError()
    data class ParseError(val message: String) : ApiError()
    data class AuthRequiredError(val message: String) : ApiError()
    data class NotFoundError(val message: String) : ApiError()
    data class RateLimitError(val message: String) : ApiError()
}

private fun handleApiResponse(json: JSONObject): Result<JSONObject> {
    return when {
        json.optBoolean("success", false) -> Result.success(json)
        json.optString("message", "").contains("авториз", ignoreCase = true) -> 
            Result.failure(ApiError.AuthRequiredError(json.optString("message")))
        json.optInt("code", 0) == 404 -> 
            Result.failure(ApiError.NotFoundError(json.optString("message")))
        json.optInt("code", 0) == 429 -> 
            Result.failure(ApiError.RateLimitError(json.optString("message")))
        else -> 
            Result.failure(ApiError.ParseError(json.optString("message", "Unknown error")))
    }
}
```

---

## 5. Оптимизация производительности

### 5.1 Кэширование декодированных потоков

**Проблема:** Потоки декодируются каждый раз заново.

**Решение:** Кэшировать декодированные потоки:

```kotlin
private val streamsCache = mutableMapOf<String, Pair<Long, Map<String, String>>>()
private val cacheExpiry = 60_000L // 1 минута

suspend fun getStreamsForPost(...): Map<String, String> {
    val cacheKey = "$postId-$translatorId-$season-$episode"
    val cached = streamsCache[cacheKey]
    
    if (cached != null && System.currentTimeMillis() - cached.first < cacheExpiry) {
        return cached.second
    }
    
    val streams = // ... получение потоков ...
    
    streamsCache[cacheKey] = System.currentTimeMillis() to streams
    return streams
}
```

### 5.2 Batch запросы для нескольких фильмов

**Новая функция:** Референсы показывают возможность быстрой загрузки данных:

```kotlin
suspend fun getMultipleMovieDetails(urls: List<String>): Map<String, MovieDetails?> = coroutineScope {
    urls.map { url ->
        async {
            url to getMovieDetails(url)
        }
    }.awaitAll().toMap()
}
```

---

## 6. Дополнительные возможности

### 6.1 Получение профиля пользователя

**Новая функция:** Референсы показывают парсинг профиля:

```kotlin
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String,
    val premiumDays: Int? = null
)

suspend fun getUserProfile(userId: String? = null): UserProfile? = withContext(Dispatchers.IO) {
    val baseUrl = getWorkingMirror() ?: return@withContext null
    val userIdToUse = userId ?: localStorage.getString("user_id", null) 
        ?: return@withContext null
    
    val html = getHtml("$baseUrl/user/$userIdToUse")
    val doc = Jsoup.parse(html, baseUrl)
    
    try {
        UserProfile(
            id = userIdToUse,
            name = doc.select("head title").firstOrNull()?.text() ?: "",
            email = doc.select("#email").firstOrNull()?.attr("value") ?: "",
            avatar = doc.select(".b-userprofile__avatar_holder img")
                .firstOrNull()?.absUrl("src") ?: "",
            premiumDays = doc.select(".b-tophead-premuser").firstOrNull()?.text()
                ?.replace("Осталось", "")
                ?.replace("днейПродлить", "")
                ?.trim()?.toIntOrNull()
        )
    } catch (e: Exception) {
        Log.e("HdRezkaApi", "Failed to parse user profile", e)
        null
    }
}
```

### 6.2 Получение статических URL

**Новая функция:** Референсы показывают функцию для статических ресурсов:

```kotlin
fun getStaticUrl(path: String): String {
    return when {
        path.startsWith("http") -> path
        path.startsWith("/") -> "https://statichdrezka.ac$path"
        else -> "https://statichdrezka.ac/$path"
    }
}
```

### 6.3 Фильтрация по жанрам и категориям

**Новая функция:** Референсы показывают возможность фильтрации:

```kotlin
suspend fun getContentByGenre(
    genre: String,
    contentType: ContentType,
    page: Int = 1
): Pair<List<Movie>, Int> {
    val pageUrl = when (contentType) {
        ContentType.FILMS -> "/films/?genre=$genre"
        ContentType.SERIES -> "/series/?genre=$genre"
    }
    return getPage(pageUrl, page)
}
```

---

## 📊 Приоритеты внедрения

### Высокий приоритет:
1. ✅ Улучшенное декодирование потоков
2. ✅ Парсинг субтитров
3. ✅ Поиск с подсказками
4. ✅ Улучшенный парсинг актеров и режиссеров
5. ✅ Retry механизм

### Средний приоритет:
6. ✅ Комментарии
7. ✅ Уведомления о новых сериях
8. ✅ Сохранение прогресса просмотра
9. ✅ Улучшенный парсинг рейтингов
10. ✅ Получение трейлера

### Низкий приоритет:
11. ✅ Модификация CDN
12. ✅ Storyboard поддержка
13. ✅ Batch запросы
14. ✅ Кэширование потоков

---

## 🚀 Быстрые улучшения

1. **Поиск с подсказками** (30 минут)
2. **Парсинг субтитров** (1 час)
3. **Улучшенный парсинг актеров** (1 час)
4. **Retry механизм** (1 час)
5. **Сохранение прогресса** (30 минут)

---

## 📝 Заключение

Основные улучшения для парсера:

1. **Декодирование потоков** - более надежное декодирование
2. **Новые функции** - комментарии, уведомления, трейлеры
3. **Улучшенный парсинг** - актеры, рейтинги, дополнительная информация
4. **Отказоустойчивость** - retry, улучшенная обработка ошибок
5. **Оптимизация** - кэширование, batch запросы

Рекомендую начать с высокоприоритетных улучшений, которые дадут наибольший эффект.

