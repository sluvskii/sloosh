# Анализ проекта Sloosh - Приложение для просмотра фильмов и сериалов

## 📋 Обзор проекта

**Sloosh** - это Android приложение для потокового просмотра фильмов и сериалов с источника данных HDRezka.

**Основные технологии:**
- **Язык:** Kotlin 2.0.21
- **Android SDK:** Min SDK 33, Target SDK 34
- **Архитектура:** MVVM с Repository pattern
- **UI:** Jetpack Compose + Fragment-based Layout
- **Хранение данных:** Room Database + SharedPreferences
- **Сетевые запросы:** OkHttp3 + Jsoup (для парсинга HTML)
- **Плеер:** Media3 (ExoPlayer)

---

## 📁 Структура проекта

### Корневая структура
```
sloosh/
├── app/                    # Основной модуль приложения
├── library/                # Общая библиотека (реиспользуемые компоненты)
├── tv/                     # Модуль для TV приложения
├── gradle/                 # Gradle конфигурация
├── icons/                  # Иконки приложения
├── references/             # Справочные примеры и ссылки
└── build.gradle.kts        # Конфигурация сборки
```

### Структура app модуля

#### `/app/src/main/java/com/slooshfilm/app/`

**Основные пакеты:**

1. **`data/`** - Слой работы с данными
   ```
   data/
   ├── hdrezka/
   │   ├── HdRezkaApi.kt          # API для взаимодействия с HDRezka
   │   ├── UserData.kt            # Данные пользователя
   │   └── UserModel.kt           # Модель пользователя
   ├── model/                     # Основные модели данных
   │   ├── Movie.kt               # Модель фильма/сериала
   │   ├── MovieDetails.kt        # Детали о фильме
   │   ├── Episode.kt             # Эпизод сериала
   │   └── Translator.kt          # Переводы фильмов
   ├── models/                    # Дополнительные модели
   │   ├── Bookmark.kt            # Закладки
   │   └── BookmarksPage.kt       # Страница с закладками
   ├── parser/
   │   ├── UniversalParser.kt     # Парсер HTML контента
   │   └── ConfigProvider.kt      # Провайдер конфигурации
   ├── repository/
   │   ├── HdRezkaRepository.kt   # Основной репозиторий
   │   └── WatchHistoryRepository.kt # История просмотров
   ├── AppDatabase.kt             # Room Database конфигурация
   ├── LocalStorage.kt            # SharedPreferences обертка
   └── AuthManager.kt             # Управление аутентификацией
   ```

2. **`ui/`** - Слой пользовательского интерфейса
   ```
   ui/
   ├── home/                      # Главная страница
   │   ├── HomeFragment.kt        # Фрагмент главной
   │   ├── HomeViewModel.kt       # ViewModel для главной
   │   ├── FilmsFragment.kt       # Фрагмент фильмов
   │   ├── SeriesFragment.kt      # Фрагмент сериалов
   │   └── CategoryAdapter.kt     # Адаптер категорий
   ├── moviedetails/              # Детали фильма
   ├── player/                    # Видеоплеер
   ├── search/                    # Поиск
   ├── bookmarks/                 # Закладки
   ├── profile/                   # Профиль пользователя
   ├── login/                     # Экран логина
   ├── register/                  # Регистрация
   ├── settings/                  # Настройки
   ├── components/                # Переиспользуемые компоненты Compose
   ├── theme/                     # Тема приложения
   ├── adapters/                  # RecyclerView адаптеры
   └── MoviesViewModel.kt         # Основной ViewModel
   ```

3. **Основные классы приложения**
   - `SlooshApplication.kt` - Application класс
   - `MainActivity.kt` - Главная Activity
   - `SplashActivity.kt` - Splash экран

---

## 🔑 Ключевые компоненты

### 1. HdRezkaApi (Сетевой слой)
**Файл:** `data/hdrezka/HdRezkaApi.kt`

**Функционал:**
- Управление зеркалами HDRezka (fallback механизм)
- Кеширование рабочих зеркал на 6 часов
- Парсинг HTML контента с помощью Jsoup
- Управление cookies (авторизация)
- Обработка SSL ошибок
- Асинхронные запросы через Kotlin Coroutines

**Ключевые методы:**
- `getWorkingMirror()` - получить рабочее зеркало
- `getPage(url, page)` - получить список фильмов/сериалов
- `getMovieDetails(url)` - получить детали фильма
- `getTranslations(postId)` - получить доступные переводы
- `getEpisodes(postId, translatorId)` - получить эпизоды сериала

### 2. HdRezkaRepository (Бизнес-логика)
**Файл:** `data/repository/HdRezkaRepository.kt`

**Компоненты:**
- Кеширование данных (bookmarks, watch-later, episodes)
- Управление авторизацией пользователя
- Фильтрация контента по типам (фильмы, сериалы, мультфильмы, аниме)
- Работа с закладками и историей просмотров

**Основные методы:**
```kotlin
suspend fun getContent(contentType: ContentType, category: ContentCategory, page: Int): Pair<List<Movie>, Int>
suspend fun getMovieDetails(url: String): MovieDetails?
suspend fun searchMovies(query: String, page: Int): Pair<List<Movie>, Int>
suspend fun getBookmarks(): BookmarksPage?
suspend fun addToWatchLater(movie: Movie): Boolean
```

### 3. MVVM ViewModels
- **MoviesViewModel** - Главный ViewModel для управления состоянием
- **HomeViewModel** - ViewModel для главной страницы
- **SeriesViewModel** - ViewModel для сериалов

### 4. Room Database
**Таблицы:**
- `movies` - Кеш фильмов
- `watch_history` - История просмотров
- `bookmarks` - Закладки пользователя

### 5. Локальное хранилище
- **LocalStorage** - Обертка над SharedPreferences
  - Сохранение cookies
  - Предпочтения пользователя
  - Кеширование последнего рабочего зеркала

---

## 🎬 Основной поток данных

```
User Action (UI)
    ↓
ViewModel (HomeViewModel, MoviesViewModel)
    ↓
Repository (HdRezkaRepository)
    ↓
Network API (HdRezkaApi) + Parser (UniversalParser)
    ↓
HDRezka Website (Parsing HTML)
    ↓
Local Cache (Room Database + SharedPreferences)
    ↓
Display in UI (Fragments, Compose Components)
```

---

## 🔐 Аутентификация

**Процесс:**
1. Пользователь вводит учетные данные на экране логина
2. AuthManager отправляет запрос на HDRezka
3. Cookies сохраняются в LocalStorage
4. При каждом запросе cookies автоматически добавляются в заголовки
5. Доступ к закладкам и истории просмотров возможен только для авторизованных пользователей

---

## 📱 Основные экраны

1. **Home/Feed** - Главная страница с новинками и популярным контентом
2. **Films** - Список фильмов с фильтрацией и поиском
3. **Series** - Список сериалов
4. **Movie Details** - Детали фильма с описанием, переводами, рейтингом
5. **Player** - Видеоплеер для просмотра контента
6. **Search** - Поиск по названию
7. **Bookmarks** - Сохраненные фильмы/сериалы
8. **Profile** - Профиль пользователя
9. **Settings** - Параметры приложения

---

## 🛠️ Инструменты и библиотеки

| Компонент | Библиотека | Версия |
|-----------|-----------|--------|
| UI Framework | Jetpack Compose | 1.9.4 |
| Сетевые запросы | OkHttp3 | 4.11.0 |
| Парсинг HTML | Jsoup | 1.16.1 |
| База данных | Room | 2.7.0 |
| Видеоплеер | Media3 | 1.8.0 |
| Асинхронность | Kotlin Coroutines | 1.7.3 |
| Сериализация | Kotlinx Serialization | 1.6.0 |
| Загрузка изображений | Glide | 4.16.0 |

---

## 🚀 Сборка и запуск

### Требования
- Android SDK 34
- Kotlin 2.0.21
- Gradle 8.13.0

### Команды сборки
```bash
# Gradlew build
./gradlew build

# Запуск тестов
./gradlew test

# Создание APK
./gradlew assembleRelease
```

---

## 🔒 Особенности безопасности

1. **SSL/TLS:** Использование безопасных соединений
2. **Cookie Management:** Автоматическое управление и хранение cookies
3. **User Data:** Сохранение данных пользователя локально
4. **Crash Handling:** Глобальный обработчик исключений с логированием

---

## 📊 Состояние проекта

**Версия:** 1.0
**API уровень:** 33 (Android 13) - 34 (Android 14)
**Язык:** Kotlin с использованием современных фич

**Возможные улучшения:**
- Реализация полнотекстового поиска
- Оптимизация производительности при загрузке больших списков
- Добавление offline режима
- Улучшение обработки ошибок сети
