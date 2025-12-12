# Архитектура Sloosh Application

## 🏗️ Общая архитектура (MVVM + Repository Pattern)

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER (UI)                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   Fragments  │  │   Compose    │  │   Activities │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │                                     │
│                  ┌────────▼────────┐                            │
│                  │   ViewModels    │                            │
│                  │ - HomeViewModel │                            │
│                  │ - SeriesViewModel                            │
│                  │ - MoviesViewModel                            │
│                  └────────┬────────┘                            │
└───────────────────────────┼─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            HdRezkaRepository                             │  │
│  │  - Content Management (Films, Series)                   │  │
│  │  - Search & Filtering                                   │  │
│  │  - Bookmarks Management                                 │  │
│  │  - Caching Logic                                        │  │
│  │  - Authentication Status                                │  │
│  └──────────┬───────────────────────────────────────────┬──┘  │
│             │                                           │      │
│  ┌──────────▼───────┐                        ┌─────────▼───┐   │
│  │ WatchHistory     │                        │AuthManager  │   │
│  │Repository        │                        │             │   │
│  └─────────────────┘                         └─────────────┘   │
│                                                                 │
└─────────────────────────┬─────────────────────────────────────┘
                          │
┌─────────────────────────▼─────────────────────────────────────┐
│                      DATA LAYER                               │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │  HdRezkaApi     │  │    Room DB   │  │  LocalStorage    │ │
│  │                 │  │              │  │ (SharedPrefs)    │ │
│  │ - Mirror mgmt   │  │ - Movies     │  │ - Cookies        │ │
│  │ - HTTP Requests │  │ - History    │  │ - Preferences    │ │
│  │ - HTML Parsing  │  │ - Bookmarks  │  │ - Cache Keys     │ │
│  │ - Cookie jar    │  │              │  │                  │ │
│  └────────┬────────┘  └──────────────┘  └──────────────────┘ │
│           │                                                   │
│  ┌────────▼────────────────────────────────┐                 │
│  │     UniversalParser                     │                 │
│  │  Parses HTML using Jsoup                │                 │
│  └─────────────────────────────────────────┘                 │
│                                                               │
└───────────────────────────┬───────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│                   EXTERNAL APIs                               │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│     HDRezka Mirrors (Multiple endpoints)                     │
│     - https://rezka.ag                                       │
│     - https://hdrezka.website                                │
│     - https://rezka.fi                                       │
│     - https://rezka-ua.org                                   │
│     - https://hdrzk.org (Official)                           │
│     - https://stepnet.video (Official)                       │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

---

## 🔄 Поток данных для загрузки фильмов

```
User navigates to Films/Series
         │
         ▼
┌────────────────────┐
│ HomeFragment.kt    │
│ FilmsFragment.kt   │
└────────┬───────────┘
         │
         ▼
┌────────────────────────────┐
│ HomeViewModel              │
│ getContent()               │
└────────┬───────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ HdRezkaRepository              │
│ getContent(type, category, pg) │
└────────┬───────────────────────┘
         │
         ├─────────────────────────────────────┐
         │                                     │
         ▼                                     ▼
    ┌─────────────┐              ┌──────────────────┐
    │ Check Cache │              │ Get Working      │
    │ (Room DB)   │              │ Mirror from      │
    │             │              │ LocalStorage     │
    └─────────────┘              └────────┬─────────┘
         │                               │
         │ Cache Miss                    │
         ▼                               ▼
    ┌──────────────────────────────────────────────┐
    │ HdRezkaApi.getPage(url, pageNum)             │
    │ 1. Build request with cookies from storage   │
    │ 2. Send HTTP request via OkHttpClient        │
    │ 3. Parse HTML response with Jsoup            │
    └────────┬─────────────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────────────┐
    │ UniversalParser                              │
    │ Extract movie data from HTML                 │
    │ - Title, Image, URL, Rating, Year           │
    └────────┬─────────────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────────────┐
    │ Save to Cache (Room Database)                │
    │ Save to LocalStorage                         │
    └────────┬─────────────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────────────┐
    │ Return List<Movie> to ViewModel              │
    └────────┬─────────────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────────────┐
    │ Update State in ViewModel                    │
    │ _moviesState.value = List<Movie>             │
    └────────┬─────────────────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────────────────┐
    │ Fragment observes LiveData/StateFlow         │
    │ UI gets recomposed                           │
    │ Display movies in RecyclerView/Compose       │
    └──────────────────────────────────────────────┘
```

---

## 🎯 Ключевые классы и их взаимодействие

### 1. HdRezkaApi

```kotlin
class HdRezkaApi(
    localStorage: LocalStorage,
    universalParser: UniversalParser,
    configProvider: ConfigProvider
)
```

**Основные функции:**
- Управление зеркалами (failover)
- HTTP клиент с поддержкой cookies
- Парсинг контента

**Ключевые методы:**
```kotlin
suspend fun getWorkingMirror(): String?
suspend fun getPage(url: String, pageNum: Int): Pair<List<Movie>, Int>
suspend fun getMovieDetails(url: String): MovieDetails?
suspend fun getTranslations(postId: String): List<Translator>?
suspend fun getEpisodes(postId: String, translatorId: Int): Map<Int, List<Episode>>?
suspend fun searchMovies(query: String, page: Int): Pair<List<Movie>, Int>
suspend fun login(username: String, password: String): Boolean
suspend fun isLoggedIn(): Boolean
```

---

### 2. HdRezkaRepository

```kotlin
class HdRezkaRepository(
    private val localStorage: LocalStorage
)
```

**Состояние:**
```kotlin
private val _isLoggedIn = MutableStateFlow(false)
private var bookmarksPageCache: BookmarksPage? = null
private val membershipCache = mutableMapOf<String, ...>()
private val watchLaterCache = mutableMapOf<Int, List<Movie>>()
private val episodesCache = mutableMapOf<String, Map<Int, List<Episode>>>()
```

**Основные операции:**
```kotlin
suspend fun getContent(
    contentType: ContentType,
    category: ContentCategory,
    page: Int
): Pair<List<Movie>, Int>

suspend fun getMovieDetails(url: String): MovieDetails?

suspend fun searchMovies(query: String, page: Int): Pair<List<Movie>, Int>

suspend fun getBookmarks(): BookmarksPage?

suspend fun addToWatchLater(movie: Movie): Boolean

suspend fun removeFromWatchLater(movieUrl: String): Boolean

suspend fun addToBookmarks(movie: Movie): Boolean

suspend fun removeFromBookmarks(movieUrl: String): Boolean
```

---

### 3. ViewModels

#### HomeViewModel
```kotlin
class HomeViewModel(
    private val repository: HdRezkaRepository
) : ViewModel()

// State
MutableStateFlow<UiState<List<Movie>>> newMovies
MutableStateFlow<UiState<List<Movie>>> popularMovies
MutableStateFlow<UiState<List<Movie>>> watchingNow
```

#### MoviesViewModel
```kotlin
class MoviesViewModel(
    private val repository: HdRezkaRepository
) : ViewModel()

// State
MutableStateFlow<List<Movie>> movies
MutableStateFlow<Boolean> isLoading
MutableStateFlow<String?> error
```

---

## 📊 Типы содержимого

```kotlin
enum class ContentType(val value: String) {
    FILMS("films"),      // Фильмы
    SERIES("series")     // Сериалы
}

enum class ContentCategory(val value: String, val ru: String) {
    NEW("new", "Новинки"),              // Новые релизы
    BEST("best", "Популярные"),         // Популярные
    WATCHING_NOW("watching-now", "Сейчас смотрят")  // Тренды
}
```

---

## 🔑 Ключевые особенности реализации

### Mirror Management (Управление зеркалами)
```
1. Проверка вынужденного зеркала (forced_mirror в SharedPrefs)
2. Проверка кэшированного зеркала (TTL 6 часов)
3. Параллельная проверка всех зеркал
4. Сохранение первого рабочего в кэш
```

### Cookie Management
```
CookieJar Interface:
- saveFromResponse() → сохранить в LocalStorage
- loadForRequest() → загрузить из LocalStorage
- Фильтрация по сроку действия
```

### Data Caching Strategy
```
Level 1: SharedPreferences (LocalStorage)
  - Cookies
  - Last working mirror
  - User preferences
  
Level 2: Room Database
  - Movies list
  - Watch history
  - Bookmarks
  
Level 3: In-Memory (Kotlin Maps)
  - Bookmarks page
  - Episodes per translator
  - Translations
```

### Error Handling
```
Try-Catch blocks для:
- Mirror discovery failures
- Network request errors
- Parsing errors
- Cookie operations

Fallback strategies:
- Использование альтернативных зеркал
- Возврат кэшированных данных
- Graceful degradation UI
```

---

## 🎬 Пример: Получение деталей фильма

```
User clicks movie card
         │
         ▼
MovieDetailsFragment
         │
         ▼
MovieDetailsViewModel.getDetails(movieUrl)
         │
         ▼
HdRezkaRepository.getMovieDetails(url)
         │
         ├─ Check episodesCache
         │
         └─ HdRezkaApi.getMovieDetails(url)
            │
            ├─ getTranslations(postId)
            │
            └─ getEpisodes(postId, translatorId) for each translator
               │
               └─ Parse HTML for seasons/episodes
                  │
                  ▼
                  Save to episodesCache
                  │
                  ▼
                  Return MovieDetails:
                  - Title, Description, Rating
                  - Poster, Genres
                  - Translations available
                  - Episodes per season
```

---

## 🔐 Процесс аутентификации

```
Login Screen
     │
     ▼
User enters credentials
     │
     ▼
LoginViewModel.login(username, password)
     │
     ▼
HdRezkaRepository.login()
     │
     ▼
HdRezkaApi.login()
     │
     ├─ POST request with credentials
     │
     ├─ Receive response with cookies
     │
     ├─ Save cookies via CookieJar
     │    └─ localStorage.saveAllCookies()
     │
     ├─ Verify login success
     │
     └─ Return success/failure
         │
         ▼
Update _isLoggedIn state
     │
     ▼
Load bookmarks & history
     │
     ▼
Navigate to main screen
```

---

## 🚀 Инициализация приложения

```
SlooshApplication.onCreate()
     │
     ├─ setupGlobalCrashHandler()
     │
     ├─ Create LocalStorage instance
     │    └─ Load SharedPreferences
     │
     ├─ Create HdRezkaRepository
     │    ├─ Create UniversalParser
     │    ├─ Create ConfigProvider  
     │    └─ Create HdRezkaApi
     │
     ├─ Create Room Database
     │    └─ Build AppDatabase
     │
     ├─ Create WatchHistoryRepository
     │
     ├─ Create AppViewModelFactory
     │
     ├─ Initialize UserData singleton
     │
     └─ Check if user is logged in
          ├─ Load cached username/avatar
          └─ Fetch fresh profile if needed
```
