# Sloosh - Техническая диаграмма и интеграция API

## 🔌 Интеграция с HDRezka API

### Структура запроса к HDRezka

```
Тип запроса: HTTP GET/POST
Базовые URL (зеркала):
  - https://rezka.ag
  - https://hdrezka.website
  - https://rezka.fi
  - https://rezka-ua.org
  - https://hdrzk.org (Official)
  - https://stepnet.video (Official)

Примеры endpoints:
  1. GET /?filter=new&genre=1          # Новые фильмы
  2. GET /?filter=popular&genre=1      # Популярные фильмы
  3. GET /?filter=watching             # Сейчас смотрят
  4. GET /?s=<query>                   # Поиск
  5. GET /page/<id>/                   # Деталь фильма
```

### Парсинг HTML структуры

```html
<!-- Пример структуры карточки фильма в HTML -->
<div class="film-item">
    <div class="film-poster">
        <img src="poster_url" alt="title">
    </div>
    <div class="film-info">
        <h3 class="film-title">Название фильма</h3>
        <div class="film-rating">8.5</div>
        <div class="film-year">2023</div>
    </div>
</div>
```

**Парсинг с помощью Jsoup:**
```kotlin
val doc: Document = Jsoup.connect(url).get()
val filmItems = doc.select("div.film-item")

filmItems.forEach { element ->
    val title = element.selectFirst("h3.film-title")?.text()
    val imageUrl = element.selectFirst("img")?.attr("src")
    val rating = element.selectFirst("div.film-rating")?.text()
    val year = element.selectFirst("div.film-year")?.text()
    
    // Создать Movie объект
    val movie = Movie(
        url = element.attr("href"),
        title = title ?: "",
        imageUrl = imageUrl ?: "",
        isSeries = false,
        rating = rating,
        year = year
    )
}
```

---

## 🔄 Последовательные диаграммы

### 1. Загрузка списка фильмов

```
User                   UI Layer              Repository           HdRezkaApi         HDRezka
 │                        │                      │                    │               │
 │─ Тап на Films ────────>│                      │                    │               │
 │                        │                      │                    │               │
 │                        │─ loadFilms() ───────>│                    │               │
 │                        │                      │                    │               │
 │                        │                      │─ getWorkingMirror()>               │
 │                        │                      │<─ return mirror ───│               │
 │                        │                      │                    │               │
 │                        │                      │─ getPage(url, 1) ─>               │
 │                        │                      │                    │───HTTP GET──>│
 │                        │                      │                    │<─HTML page ──│
 │                        │                      │                    │               │
 │                        │                      │<─ List<Movie> ─────│               │
 │                        │                      │                    │               │
 │                        │<─ List<Movie> ───────│                    │               │
 │                        │                      │                    │               │
 │<─ Show movies list ────│                      │                    │               │
 │                        │                      │                    │               │
```

### 2. Просмотр деталей фильма

```
User              Fragment             ViewModel          Repository        Api         HDRezka
 │                   │                    │                   │              │            │
 │─ Клик на фильм ──>│                    │                   │              │            │
 │                   │                    │                   │              │            │
 │                   │─ loadMovieDetails()>                  │              │            │
 │                   │                    │                   │              │            │
 │                   │                    │─ getMovieDetails()>              │            │
 │                   │                    │                   │              │            │
 │                   │                    │                   │─ getMovieDetails──>      │
 │                   │                    │                   │              │           │
 │                   │                    │                   │<─ MovieDetails──────────│
 │                   │                    │                   │              │           │
 │                   │                    │                   │─ getTranslations──>    │
 │                   │                    │                   │<─ List<Translator>──────│
 │                   │                    │                   │              │           │
 │                   │                    │<─ MovieDetails ────│              │           │
 │                   │                    │                    │              │           │
 │                   │<─ Details & state ─│                    │              │           │
 │                   │                    │                    │              │           │
 │<─ Show details ───│                    │                    │              │           │
 │                   │                    │                    │              │           │
```

### 3. Авторизация пользователя

```
User          LoginFragment          ViewModel          Repository          Api          HDRezka
 │                  │                    │                   │               │              │
 │─ Вход данные ──> │                    │                   │               │              │
 │                  │                    │                   │               │              │
 │                  │─ login() ──────────>                   │               │              │
 │                  │                    │                   │               │              │
 │                  │                    │─ login() ─────────>               │              │
 │                  │                    │                   │               │              │
 │                  │                    │                   │─ login() ─────────────────>│
 │                  │                    │                   │               │              │
 │                  │                    │                   │<─ Cookies & Response ─────│
 │                  │                    │                   │               │              │
 │                  │                    │                   │─ saveCookies()            │
 │                  │                    │                   │   to LocalStorage          │
 │                  │                    │                   │               │              │
 │                  │                    │<─ isLoggedIn=true ─│               │              │
 │                  │                    │                   │               │              │
 │                  │<─ Login Success ───│                   │               │              │
 │                  │                    │                   │               │              │
 │<─ Navigate ─────>│                    │                   │               │              │
 │   to Main        │                    │                   │               │              │
```

---

## 🗂️ Схема базы данных

```sql
-- Room Database Schema

-- Таблица фильмов/сериалов
CREATE TABLE movies (
    url TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    image_url TEXT,
    is_series BOOLEAN DEFAULT 0,
    rating TEXT,
    year TEXT,
    film_name TEXT,
    film_year TEXT,
    postId TEXT,
    type TEXT
);

-- Таблица истории просмотров
CREATE TABLE watch_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    movie_url TEXT NOT NULL UNIQUE,
    watch_date INTEGER NOT NULL,
    position_ms INTEGER DEFAULT 0,
    watched BOOLEAN DEFAULT 0,
    FOREIGN KEY(movie_url) REFERENCES movies(url)
);

-- Таблица закладок
CREATE TABLE bookmarks (
    url TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    image_url TEXT,
    is_series BOOLEAN DEFAULT 0,
    saved_date INTEGER NOT NULL,
    rating TEXT,
    year TEXT
);

-- Индексы
CREATE INDEX idx_movie_type ON movies(type);
CREATE INDEX idx_watch_date ON watch_history(watch_date DESC);
CREATE INDEX idx_saved_date ON bookmarks(saved_date DESC);
```

---

## 📊 Диаграмма состояний ViewModel

```
                    ┌─────────────┐
                    │   Loading   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
         ┌──────────│   Success   │◄──────────┐
         │          └──────┬──────┘           │
         │                 │                  │
         │                 │ Error            │ Retry
         │                 │                  │
         │          ┌──────▼──────┐           │
         │          │    Error    │───────────┘
         │          └─────────────┘
         │
         └─ RefreshCache or Reload

// StateFlow emits these states in sequence:
_moviesState.value = UiState.Loading
  ↓ (after network call)
_moviesState.value = UiState.Success(data)  OR  UiState.Error(message)
  ↓ (on refresh)
_moviesState.value = UiState.Loading
```

---

## 🎬 Диаграмма плеера

```
PlayerViewModel
    ├── videoUrl: String
    ├── currentPosition: Long
    ├── isPlaying: Boolean
    ├── duration: Long
    ├── playbackSpeed: Float
    └── availableQualities: List<Quality>

ExoPlayer Events
    ├── onPlaybackStateChanged()
    │   ├── STATE_IDLE
    │   ├── STATE_BUFFERING
    │   ├── STATE_READY
    │   └── STATE_ENDED
    │
    └── onPositionDiscontinuity()
        ├── Update UI progress bar
        └── Save position to ViewModel

WatchHistory
    ├── Save position every 5 seconds
    ├── Mark as watched on STATE_ENDED
    └── Resume from saved position on re-open
```

---

## 🔐 Диаграмма управления Cookies

```
┌──────────────────┐
│  HTTP Response   │
│  with Cookies    │
└────────┬─────────┘
         │
         ▼
    ┌────────────────────────┐
    │  OkHttpClient          │
    │  CookieJar Interface   │
    │  saveFromResponse()    │
    └────────┬───────────────┘
             │
             ▼
    ┌──────────────────────────┐
    │  Cookie List             │
    │  Filter:                 │
    │  - Remove duplicates     │
    │  - Check expiration      │
    │  - Validate              │
    └────────┬─────────────────┘
             │
             ▼
    ┌──────────────────────────┐
    │  LocalStorage            │
    │  (SharedPreferences)     │
    └──────────────────────────┘

Next HTTP Request:
    ├── Load cookies from LocalStorage
    ├── Filter by expiration
    ├── Add to request headers
    └── Send to HDRezka
```

---

## 🚀 Диаграмма инициализации приложения

```
Application.onCreate()
    │
    ├─→ Global Crash Handler
    │   └─ Write crashes to file
    │
    ├─→ Create LocalStorage
    │   └─ Load SharedPreferences
    │
    ├─→ Create HdRezkaRepository
    │   ├─ UniversalParser
    │   ├─ ConfigProvider
    │   └─ HdRezkaApi
    │       └─ Create OkHttpClient
    │           └─ Setup CookieJar
    │
    ├─→ Create Room Database
    │   ├─ Build AppDatabase
    │   └─ Initialize DAOs
    │
    ├─→ Create WatchHistoryRepository
    │   └─ Connect to Database
    │
    ├─→ Create AppViewModelFactory
    │
    ├─→ Initialize UserData singleton
    │
    └─→ Check Login Status
        ├─ Load cookies from LocalStorage
        ├─ Verify if still valid
        └─ Fetch user profile if needed
```

---

## 🎯 Диаграмма фильтрации контента

```
Raw Movie List from API
    │
    ├─→ Parse film type:
    │   ├─ Check if "мультф" or "аниме" in title
    │   ├─ Check if series (isSeries flag)
    │   └─ Return type: film|series|cartoon|anime|cartoon-series|anime-series
    │
    └─→ Filter by ContentType request:
        ├─ ContentType.FILMS:
        │   └─ Filter: !isSeries
        │
        └─ ContentType.SERIES:
            └─ Filter: isSeries

Result: Filtered List<Movie>
```

---

## 📱 Диаграмма навигации

```
SplashActivity
    │
    └─→ MainActivity
        │
        └─→ NavHostFragment
            └─→ Navigation Graph
                │
                ├─ HomeFragment
                │   ├─ FilmsFragment
                │   │   └─ → MovieDetailsFragment
                │   │       └─ → PlayerFragment
                │   └─ SeriesFragment
                │       └─ → MovieDetailsFragment
                │
                ├─ SearchFragment
                │   └─ → MovieDetailsFragment
                │
                ├─ BookmarksFragment
                │   └─ → MovieDetailsFragment
                │
                ├─ ProfileFragment
                │
                ├─ LoginFragment
                │
                └─ SettingsFragment
```

---

## 🔄 Механизм retry для Mirror Discovery

```
try mirror 1
    ↓
timeout or error?
    ├─ YES ──→ try mirror 2
    └─ NO  ──→ SUCCESS ✓
              │
              └─→ Save to cache
                 (TTL: 6 hours)

If all mirrors fail:
    ├─ Load from cache
    └─ Show offline error
```

---

## 📊 Статистика API запросов

```
Типичная сессия пользователя:

1. getWorkingMirror()        - 1 параллельный запрос ко всем зеркалам
2. getPage()                 - 1 запрос на получение списка
3. getMovieDetails()         - N запросов (по клику на фильм)
4. getTranslations()         - N запросов (при открытии деталей)
5. getEpisodes()             - N*M запросов (для каждого переводчика)
6. searchMovies()            - Per search query

Кеширование уменьшает количество запросов на 70-80%
```

---

## 🛡️ Обработка ошибок - Flow chart

```
Network Request
    │
    ├─→ Timeout?
    │   └─ Try next mirror
    │       └─ All failed?
    │           └─ Check cache
    │               └─ Cache empty?
    │                   └─ Show error dialog
    │
    ├─→ Authentication error (401)?
    │   └─ Clear cookies
    │   └─ Redirect to login
    │
    ├─→ Not found (404)?
    │   └─ Show "Not available"
    │
    ├─→ Server error (5xx)?
    │   └─ Try next mirror
    │
    └─→ Other error?
        └─ Log error
        └─ Show generic error message
```

---

## 💾 Жизненный цикл кэша

```
┌──────────────────────────────────────────────────┐
│           Cache Entry Creation                   │
└────────────────┬─────────────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │ Check TTL      │
        │ (if applicable)│
        └────────┬───────┘
                 │
        ┌────────▼───────────────────┐
        │ Valid and not expired?      │
        └────────┬──────────┬─────────┘
                 │ YES      │ NO
                 │          │
                 ▼          ▼
            ┌─────────┐  ┌──────────┐
            │ Use it  │  │ Invalidate
            └─────────┘  │ Fetch new
                         └──────────┘
                              │
                              ▼
                         ┌────────────┐
                         │ Update TTL │
                         └────────────┘
```

---

## 🎓 Пример полного цикла: Поиск фильма

```
1. User enters "Матрица" in SearchFragment
   │
2. viewModel.search("Матрица")
   │
3. repository.searchMovies("Матрица", page=1)
   │
4. api.getWorkingMirror()
   ├─ Load from cache? (exists and valid)
   └─ Return: "https://rezka.ag"
   │
5. api.getPage("https://rezka.ag/?s=Матрица", 1)
   │
6. client.newCall(request).execute()
   ├─ Add cookies from localStorage
   ├─ Send HTTP GET request
   └─ Receive HTML response
   │
7. universalParser.parse(html)
   ├─ Select movie items
   ├─ Extract data (title, image, rating, etc)
   ├─ Create Movie objects
   └─ Return: List<Movie>
   │
8. repository adds to cache
   ├─ Save to in-memory cache
   └─ Save to Room database
   │
9. ViewModel updates StateFlow
   ├─ _searchResults.value = UiState.Success(movies)
   │
10. UI Fragment observes and collects
    ├─ repeatOnLifecycle(STARTED)
    └─ viewModel.searchResults.collect { state → updateUI(state) }
    │
11. RecyclerView/LazyColumn shows results
    └─ User can click to view details
```

