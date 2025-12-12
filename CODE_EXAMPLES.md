# Sloosh - Примеры кода и Best Practices

## 📖 Примеры использования основных компонентов

### 1. Получение контента с Repository

```kotlin
// В ViewModel
class MoviesViewModel(
    private val repository: HdRezkaRepository
) : ViewModel() {
    
    private val _moviesState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val moviesState = _moviesState.asStateFlow()
    
    fun loadFilms(page: Int = 1) {
        viewModelScope.launch {
            try {
                _moviesState.value = UiState.Loading
                val (movies, totalPages) = repository.getContent(
                    contentType = ContentType.FILMS,
                    category = ContentCategory.NEW,
                    page = page
                )
                _moviesState.value = UiState.Success(movies)
            } catch (e: Exception) {
                _moviesState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// В Fragment
class FilmsFragment : Fragment() {
    
    private val viewModel: MoviesViewModel by activityViewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.moviesState.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading()
                        is UiState.Success -> showMovies(state.data)
                        is UiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }
}
```

---

### 2. Поиск фильмов

```kotlin
// В Repository
suspend fun searchMovies(query: String, page: Int = 1): Pair<List<Movie>, Int> {
    val baseUrl = api.getWorkingMirror() ?: return Pair(emptyList(), 1)
    val searchUrl = "/?s=$query"
    
    val (results, totalPages) = api.getPage(searchUrl, page)
    
    results.forEach { 
        it.type = parseFilmType(it)
    }
    
    return Pair(results, totalPages)
}

// Использование
viewModel.search(query = "Матрица").collect { result ->
    // Обработка результатов
}
```

---

### 3. Работа с деталями фильма

```kotlin
// В Repository
suspend fun getMovieDetails(url: String): MovieDetails? {
    val baseUrl = api.getWorkingMirror() ?: return null
    return api.getMovieDetails(baseUrl + url)
}

// В ViewModel
fun loadMovieDetails(url: String) {
    viewModelScope.launch {
        try {
            val details = repository.getMovieDetails(url)
            if (details != null) {
                _movieDetailsState.value = UiState.Success(details)
                
                // Автоматически загружаем переводы
                loadTranslations(details.postId)
            }
        } catch (e: Exception) {
            _movieDetailsState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }
}

private fun loadTranslations(postId: String) {
    viewModelScope.launch {
        try {
            val translations = repository.getTranslations(postId)
            _translationsState.value = translations ?: emptyList()
        } catch (e: Exception) {
            Log.e("MovieDetails", "Failed to load translations", e)
        }
    }
}
```

---

### 4. Управление закладками

```kotlin
// Добавление в закладки
suspend fun addToBookmarks(movie: Movie): Boolean {
    return try {
        api.addToBookmarks(movie.postId ?: return false)
        invalidateCaches()  // Перезагружаем кэш закладок
        true
    } catch (e: Exception) {
        Log.e("Bookmarks", "Failed to add bookmark", e)
        false
    }
}

// Получение закладок
suspend fun getBookmarks(): BookmarksPage? {
    return bookmarksPageCache ?: run {
        val page = api.getBookmarksPage()
        bookmarksPageCache = page
        page
    }
}

// В ViewModel с обработкой состояния
fun toggleBookmark(movie: Movie) {
    viewModelScope.launch {
        val success = if (isBookmarked(movie)) {
            repository.removeFromBookmarks(movie.url)
        } else {
            repository.addToBookmarks(movie)
        }
        
        if (success) {
            // Обновляем локальное состояние
            _bookmarkedMovies.update { list ->
                if (isBookmarked(movie)) {
                    list.filter { it.url != movie.url }
                } else {
                    list + movie
                }
            }
        }
    }
}
```

---

### 5. Воспроизведение видео (Media3/ExoPlayer)

```kotlin
// В PlayerFragment
class PlayerFragment : Fragment() {
    
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private var exoPlayer: ExoPlayer? = null
    
    private fun initializePlayer(videoUrl: String) {
        val player = ExoPlayer.Builder(requireContext()).build()
        
        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        
        // Восстанавливаем позицию просмотра
        val savedPosition = viewModel.getSavedPosition(movieUrl)
        if (savedPosition > 0) {
            player.seekTo(savedPosition)
        }
        
        binding.playerView.player = player
        player.prepare()
        player.play()
        
        exoPlayer = player
        
        // Сохраняем прогресс просмотра
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.markAsWatched(movieUrl)
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val position = player.currentPosition
                viewModel.savePosition(movieUrl, position)
            }
        })
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        // Сохраняем текущую позицию
        exoPlayer?.currentPosition?.let {
            viewModel.savePosition(movieUrl, it)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
```

---

### 6. Работа с Jetpack Compose компонентами

```kotlin
// Простой компонент для отображения карточки фильма
@Composable
fun MovieCard(
    movie: Movie,
    onMovieClick: (Movie) -> Unit,
    onBookmarkClick: (Movie) -> Unit,
    isBookmarked: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMovieClick(movie) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Изображение фильма с использованием Coil
            AsyncImage(
                model = movie.imageUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator()
                }
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${movie.year ?: "N/A"} • ${if (movie.isSeries) "Series" else "Film"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    if (!movie.rating.isNullOrEmpty()) {
                        RatingBadge(rating = movie.rating)
                    }
                }
                
                // Кнопка закладки
                Icon(
                    imageVector = if (isBookmarked) {
                        Icons.Filled.Bookmark
                    } else {
                        Icons.Outlined.Bookmark
                    },
                    contentDescription = "Bookmark",
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { onBookmarkClick(movie) },
                    tint = if (isBookmarked) Color.Yellow else Color.Gray
                )
            }
        }
    }
}

// LazyColumn с пагинацией
@Composable
fun MovieList(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onLoadMore: () -> Unit,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = movies,
            key = { it.url }
        ) { movie ->
            MovieCard(
                movie = movie,
                onMovieClick = onMovieClick,
                onBookmarkClick = { /* Bookmark logic */ }
            )
        }
        
        // Load more trigger
        if (!isLoading && movies.isNotEmpty()) {
            item {
                Button(onClick = onLoadMore) {
                    Text("Load More")
                }
            }
        }
        
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
```

---

## 🎯 Best Practices в проекте

### 1. Управление состоянием (State Management)

✅ **Правильно:**
```kotlin
private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
val state = _state.asStateFlow()  // Immutable for observers

viewModelScope.launch {
    try {
        _state.value = UiState.Loading
        val data = repository.getData()
        _state.value = UiState.Success(data)
    } catch (e: Exception) {
        _state.value = UiState.Error(e.message ?: "Unknown")
    }
}
```

❌ **Неправильно:**
```kotlin
// Прямое обновление без обработки ошибок
val data = repository.getData()  // Может вызвать краш
movieList.value = data
```

---

### 2. Корутины и асинхронность

✅ **Правильно:**
```kotlin
fun loadData() {
    viewModelScope.launch {  // Автоматически отменяется с ViewModel
        try {
            val data = repository.getData()  // suspend function
            updateState(data)
        } catch (e: Exception) {
            handleError(e)
        }
    }
}
```

❌ **Неправильно:**
```kotlin
fun loadData() {
    GlobalScope.launch {  // Может привести к утечкам памяти
        repository.getData()
    }
}
```

---

### 3. Обработка ошибок сети

✅ **Правильно:**
```kotlin
suspend fun getWorkingMirror(): String? {
    // Попытка использовать кэш первой
    val cachedMirror = localStorage.getLastWorkingMirror()
    if (cachedMirror != null && isMirrorValid(cachedMirror)) {
        return cachedMirror
    }
    
    // Параллельная проверка всех зеркал
    val mirrors = getAllMirrors()
    val results = mirrors.map { mirror ->
        async {
            try {
                checkMirror(mirror)
            } catch (e: Exception) {
                null  // Ignore failures
            }
        }
    }
    
    val workingMirror = results.awaitAll().firstNotNull()
    localStorage.saveLastWorkingMirror(workingMirror)
    return workingMirror
}
```

---

### 4. Управление ресурсами в UI

✅ **Правильно:**
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // Lifecycle-aware collection
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                updateUI(state)  // Автоматически отменяется
            }
        }
    }
}
```

---

### 5. Кеширование данных

✅ **Правильно:**
```kotlin
// Multi-level caching
suspend fun getContent(...): Pair<List<Movie>, Int> {
    // 1. Check in-memory cache
    memoryCache[cacheKey]?.let { return it }
    
    // 2. Check Room database
    val dbMovies = database.movieDao().getMovies()
    if (dbMovies.isNotEmpty()) {
        memoryCache[cacheKey] = dbMovies to totalPages
        return Pair(dbMovies, totalPages)
    }
    
    // 3. Fetch from network
    val (movies, pages) = api.getPage(url, page)
    
    // 4. Save to Room
    database.movieDao().insertAll(movies)
    
    // 5. Save to memory cache
    memoryCache[cacheKey] = Pair(movies, pages)
    
    return Pair(movies, pages)
}
```

---

### 6. Обработка cookie jar

✅ **Правильно:**
```kotlin
val cookieJar = object : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Обновляем существующие cookies, удаляя старые
        val currentCookies = localStorage.getAllCookies().toMutableList()
        currentCookies.removeAll { oldCookie ->
            cookies.any { newCookie -> oldCookie.name == newCookie.name }
        }
        currentCookies.addAll(cookies)
        localStorage.saveAllCookies(currentCookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // Фильтруем истекшие cookies
        return localStorage.getAllCookies()
            .filter { it.expiresAt > System.currentTimeMillis() }
    }
}
```

---

### 7. Структура UI слоя с Compose

✅ **Правильно:**
```kotlin
// Разделение логики и представления
@Composable
fun MovieDetailsScreen(
    viewModel: MovieDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Success -> MovieDetailsContent(state.data)
        is UiState.Error -> ErrorScreen(state.message)
    }
}

@Composable
fun MovieDetailsContent(movie: MovieDetails) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Только отображение, без логики
    }
}
```

---

## 🧪 Тестирование

### Unit тестирование Repository

```kotlin
class HdRezkaRepositoryTest {
    
    private lateinit var repository: HdRezkaRepository
    private val mockLocalStorage = mockk<LocalStorage>()
    
    @Before
    fun setup() {
        repository = HdRezkaRepository(mockLocalStorage)
    }
    
    @Test
    fun `getContent returns filtered films only`() = runTest {
        // Arrange
        val mockContent = listOf(
            Movie(url = "film1", title = "Film", isSeries = false),
            Movie(url = "series1", title = "Series", isSeries = true)
        )
        
        // Act
        val (films, _) = repository.getContent(ContentType.FILMS, ContentCategory.NEW, 1)
        
        // Assert
        assertEquals(1, films.size)
        assertFalse(films[0].isSeries)
    }
    
    @Test
    fun `searchMovies returns correct results`() = runTest {
        val results = repository.searchMovies("Матрица", 1)
        assertTrue(results.first.isNotEmpty())
    }
}
```

---

## 📚 Полезные ссылки в кодовой базе

| Компонент | Файл | Назначение |
|-----------|------|-----------|
| API | `data/hdrezka/HdRezkaApi.kt` | Сетевое взаимодействие |
| Репозиторий | `data/repository/HdRezkaRepository.kt` | Бизнес-логика |
| Парсер | `data/parser/UniversalParser.kt` | HTML парсинг |
| ViewModel | `ui/MoviesViewModel.kt` | Управление состоянием |
| Database | `data/AppDatabase.kt` | Room конфигурация |
| Storage | `data/LocalStorage.kt` | SharedPreferences обертка |

