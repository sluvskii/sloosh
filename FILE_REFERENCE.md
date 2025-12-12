# 🔗 Sloosh - Справочник исходных файлов

## 📂 Основные файлы проекта

### 🏠 Корневые файлы

| Файл | Описание |
|------|---------|
| `build.gradle.kts` | Root build конфигурация |
| `settings.gradle.kts` | Конфигурация модулей (app, library, tv) |
| `gradle.properties` | Gradle свойства |
| `gradle/libs.versions.toml` | **Версии всех зависимостей** |
| `local.properties` | Локальные настройки (создать с SDK path) |
| `gradlew.bat` | Gradle Wrapper для Windows |

---

## 📱 Приложение (app модуль)

### 🔧 Build конфигурация

**Файл:** `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")                    // Android plugin
    id("org.jetbrains.kotlin.android")               // Kotlin
    id("org.jetbrains.kotlin.plugin.compose")        // Compose
    id("com.google.devtools.ksp")                    // KSP for Room
    id("kotlin-parcelize")                           // Parcelable
    id("org.jetbrains.kotlin.plugin.serialization")  // Serialization
}

android {
    namespace = "com.slooshfilm.app"
    compileSdk = 34
    minSdk = 33
    targetSdk = 34
    // ... зависимости
}
```

---

### 📦 Основной пакет: `com.slooshfilm.app`

#### 🌍 Слой данных (`data/`)

| Файл | Класс | Описание |
|------|-------|---------|
| `data/AppDatabase.kt` | `AppDatabase` | Room Database конфигурация |
| `data/LocalStorage.kt` | `LocalStorage` | SharedPreferences обертка |
| `data/AuthManager.kt` | `AuthManager` | Управление аутентификацией |
| `data/MovieDao.kt` | `MovieDao` | Room DAO для фильмов |
| `data/WatchHistoryDao.kt` | `WatchHistoryDao` | DAO для истории просмотров |
| `data/Result.kt` | `Result<T>` | Результат операции (Success/Error) |
| `data/FilmsListModel.kt` | `FilmsListModel` | Модель для списка фильмов |

#### 📊 Модели данных (`data/model/`)

| Файл | Класс | Описание |
|------|-------|---------|
| `data/model/Movie.kt` | `Movie` | **Основная модель фильма/сериала** |
| `data/model/MovieDetails.kt` | `MovieDetails` | Детали о фильме |
| `data/model/Episode.kt` | `Episode` | Эпизод сериала |
| `data/model/Translator.kt` | `Translator` | Информация о переводчике |
| `data/model/MovieParcelable.kt` | `MovieParcelable` | Parcelable версия Movie |

#### 📚 Дополнительные модели (`data/models/`)

| Файл | Класс | Описание |
|------|-------|---------|
| `data/models/Bookmark.kt` | `Bookmark` | Закладка пользователя |
| `data/models/BookmarksPage.kt` | `BookmarksPage` | Страница с закладками |

#### 🌐 Сетевой API (`data/hdrezka/`)

| Файл | Класс | Ключевые методы |
|------|-------|-----------------|
| `data/hdrezka/HdRezkaApi.kt` | `HdRezkaApi` | `getWorkingMirror()`, `getPage()`, `getMovieDetails()`, `getTranslations()`, `getEpisodes()`, `login()` |
| `data/hdrezka/UserData.kt` | `UserData` | Singleton с данными пользователя |
| `data/hdrezka/UserModel.kt` | `UserModel` | Методы для работы с профилем |

#### 🔧 Парсер (`data/parser/`)

| Файл | Класс | Описание |
|------|-------|---------|
| `data/parser/UniversalParser.kt` | `UniversalParser` | Парсинг HTML с Jsoup |
| `data/parser/ConfigProvider.kt` | `ConfigProvider` | Провайдер конфигурации |

#### 📦 Репозиторий (`data/repository/`)

| Файл | Класс | Ключевые методы |
|------|-------|-----------------|
| `data/repository/HdRezkaRepository.kt` | `HdRezkaRepository` | `getContent()`, `getMovieDetails()`, `searchMovies()`, `getBookmarks()`, `addToBookmarks()` |
| `data/repository/HdRezkaRepository.kt` | `ContentType` enum | `FILMS`, `SERIES` |
| `data/repository/HdRezkaRepository.kt` | `ContentCategory` enum | `NEW`, `BEST`, `WATCHING_NOW` |
| `data/repository/WatchHistoryRepository.kt` | `WatchHistoryRepository` | Управление историей просмотров |

#### 🌐 Сетевые компоненты (`data/network/`)

Дополнительные сетевые классы (если есть)

---

### 🎨 Слой UI (`ui/`)

#### 🏠 Главная страница (`ui/home/`)

| Файл | Класс | Описание |
|------|-------|---------|
| `ui/home/HomeFragment.kt` | `HomeFragment` | **Главный экран с новинками** |
| `ui/home/HomeViewModel.kt` | `HomeViewModel` | ViewModel для главной |
| `ui/home/FilmsFragment.kt` | `FilmsFragment` | Экран фильмов |
| `ui/home/SeriesFragment.kt` | `SeriesViewModel` | Экран сериалов |
| `ui/home/SeriesViewModel.kt` | `SeriesViewModel` | ViewModel для сериалов |
| `ui/home/CategoryAdapter.kt` | `CategoryAdapter` | Адаптер категорий |
| `ui/home/ViewPagerAdapter.kt` | `ViewPagerAdapter` | Адаптер для ViewPager |
| `ui/home/GridSpacingItemDecoration.kt` | `GridSpacingItemDecoration` | Декорация сетки |
| `ui/home/UiModel.kt` | `UiModel` | Модели для UI |

#### 🎬 Детали фильма (`ui/moviedetails/`)

| Файл | Описание |
|------|---------|
| `ui/moviedetails/` | Экран с детальной информацией о фильме |
| | - Описание, рейтинг, год |
| | - Список переводов |
| | - Эпизоды для сериалов |

#### ▶️ Плеер (`ui/player/`)

| Файл | Описание |
|------|---------|
| `ui/player/` | Видеоплеер с Media3/ExoPlayer |
| | - Управление воспроизведением |
| | - Сохранение позиции |
| | - История просмотров |

#### 🔍 Поиск (`ui/search/`)

| Файл | Описание |
|------|---------|
| `ui/search/` | Экран поиска фильмов |

#### 📚 Закладки (`ui/bookmarks/`)

| Файл | Описание |
|------|---------|
| `ui/bookmarks/` | Сохраненные фильмы и сериалы |

#### 👤 Профиль (`ui/profile/`)

| Файл | Описание |
|------|---------|
| `ui/profile/` | Профиль пользователя |
| | - Данные аккаунта |
| | - Статистика просмотров |

#### 🔐 Аутентификация (`ui/login/` и `ui/register/`)

| Файл | Описание |
|------|---------|
| `ui/login/` | Экран входа в аккаунт |
| `ui/register/` | Регистрация нового аккаунта |

#### ⚙️ Настройки (`ui/settings/`)

| Файл | Описание |
|------|---------|
| `ui/settings/` | Параметры приложения |

#### 🎨 Компоненты (`ui/components/`)

| Файл | Описание |
|------|---------|
| `ui/components/` | Переиспользуемые Compose компоненты |

#### 🎭 Тема (`ui/theme/`)

| Файл | Описание |
|------|---------|
| `ui/theme/` | Material Design 3 тема |

#### 🎪 Прочие UI компоненты

| Директория | Содержит |
|-----------|----------|
| `ui/adapters/` | RecyclerView адаптеры |
| `ui/widget/` | App widgets |
| `ui/liquid/` | Liquid анимация |
| `ui/comments/` | Комментарии |
| `ui/filmography/` | Фильмография актеров |
| `ui/kinoteka/` | Кинотека |
| `ui/library/` | Библиотека |
| `ui/updates/` | Обновления |
| `ui/watchlist/` | Список для просмотра |
| `ui/actordetails/` | Детали актера |

#### 🏗️ Архитектурные компоненты UI

| Файл | Класс | Описание |
|------|-------|---------|
| `ui/MoviesViewModel.kt` | `MoviesViewModel` | Главный ViewModel для фильмов |
| `ui/AppViewModelFactory.kt` | `AppViewModelFactory` | Factory для ViewModels |
| `ui/ViewModelFactory.kt` | `ViewModelFactory` | Дополнительный Factory |
| `ui/UserPresenter.kt` | `UserPresenter` | Presenter для пользователя |
| `ui/SingleLiveEvent.kt` | `SingleLiveEvent<T>` | Single-shot LiveData |

---

### 🚀 Главное приложение

| Файл | Класс | Описание |
|------|-------|---------|
| `SlooshApplication.kt` | `SlooshApplication` | **Application класс с инициализацией** |
| `SlooshApp.kt` | `SlooshApp` | Дополнительный конфиг |
| `MainActivity.kt` | `MainActivity` | **Главная Activity** |
| `SplashActivity.kt` | `SplashActivity` | Splash экран |
| `MyView.kt` | `MyView` | Пользовательский View |

---

### 🧭 Навигация

| Файл | Описание |
|------|---------|
| `navigation/` | Fragment navigation компоненты |

---

## 📚 Библиотека (library модуль)

| Файл | Описание |
|------|---------|
| `library/build.gradle` | Конфигурация библиотеки |
| `library/src/main/` | Исходный код библиотеки |

---

## 📺 TV приложение (tv модуль)

| Файл | Описание |
|------|---------|
| `tv/build.gradle.kts` | TV версия приложения |
| `tv/src/main/` | Исходный код для ТВ |

---

## 📋 Конфигурационные файлы

| Файл | Назначение |
|------|-----------|
| `gradle/libs.versions.toml` | **Версии зависимостей** (OkHttp, Room, Compose, Media3 и т.д.) |
| `gradle.properties` | Gradle настройки |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper конфигурация |
| `.gitignore` | Файлы для исключения из Git |

---

## 📄 Ресурсы (src/main/res/)

```
res/
├── drawable/              # Изображения и drawable ресурсы
├── layout/               # XML макеты для Fragment
├── menu/                 # Меню ресурсы
├── values/               # Strings, colors, styles, dimens
│   ├── strings.xml      # Строки приложения
│   ├── colors.xml       # Палитра цветов
│   ├── styles.xml       # Стили компонентов
│   └── dimens.xml       # Размеры элементов
├── mipmap/              # Иконки приложения
└── xml/                 # XML конфигурации
```

---

## 🔗 Связь между ключевыми файлами

```
MainActivity.kt
    │
    └─→ NavHostFragment
        │
        ├─→ HomeFragment ←─ HomeViewModel ←─ HdRezkaRepository
        │                                           │
        │                                           ├─→ HdRezkaApi
        │                                           └─→ Room Database
        │
        ├─→ FilmsFragment ←─ MoviesViewModel ←─ HdRezkaRepository
        │
        ├─→ SeriesFragment ←─ SeriesViewModel ←─ HdRezkaRepository
        │
        ├─→ MovieDetailsFragment ←─ MovieDetailsViewModel
        │
        ├─→ PlayerFragment ←─ PlayerViewModel
        │
        ├─→ SearchFragment ←─ SearchViewModel
        │
        ├─→ BookmarksFragment ←─ BookmarksViewModel
        │
        ├─→ ProfileFragment ←─ ProfileViewModel
        │
        ├─→ LoginFragment ←─ LoginViewModel
        │
        └─→ SettingsFragment
```

---

## 🎯 Где находятся ключевые функции

### Получение списка фильмов
1. **UI:** `ui/home/FilmsFragment.kt`
2. **ViewModel:** `ui/MoviesViewModel.kt` → `loadFilms()`
3. **Repository:** `data/repository/HdRezkaRepository.kt` → `getContent()`
4. **API:** `data/hdrezka/HdRezkaApi.kt` → `getPage()`

### Просмотр деталей
1. **UI:** `ui/moviedetails/MovieDetailsFragment.kt`
2. **ViewModel:** `ui/moviedetails/MovieDetailsViewModel.kt`
3. **Repository:** `data/repository/HdRezkaRepository.kt` → `getMovieDetails()`
4. **API:** `data/hdrezka/HdRezkaApi.kt` → `getMovieDetails()`

### Воспроизведение видео
1. **UI:** `ui/player/PlayerFragment.kt`
2. **ViewModel:** `ui/player/PlayerViewModel.kt`
3. **Player:** Media3 ExoPlayer
4. **History:** `data/repository/WatchHistoryRepository.kt`

### Авторизация
1. **UI:** `ui/login/LoginFragment.kt`
2. **ViewModel:** `ui/login/LoginViewModel.kt`
3. **Repository:** `data/repository/HdRezkaRepository.kt` → `login()`
4. **API:** `data/hdrezka/HdRezkaApi.kt` → `login()`
5. **Storage:** `data/LocalStorage.kt` → сохранение cookies

### Закладки
1. **UI:** `ui/bookmarks/BookmarksFragment.kt`
2. **ViewModel:** `ui/bookmarks/BookmarksViewModel.kt`
3. **Repository:** `data/repository/HdRezkaRepository.kt` → `getBookmarks()`, `addToBookmarks()`
4. **API:** `data/hdrezka/HdRezkaApi.kt` → API методы
5. **Database:** Room базирует закладки

---

## 🔧 Как найти нужный файл

### Нужен файл для ...

**... получение фильмов?**
- API: `data/hdrezka/HdRezkaApi.kt` → `getPage()`
- Repository: `data/repository/HdRezkaRepository.kt` → `getContent()`

**... отображение фильмов?**
- Fragment: `ui/home/FilmsFragment.kt`
- ViewModel: `ui/MoviesViewModel.kt`
- Adapter: `ui/adapters/MovieAdapter.kt`

**... работа с плеером?**
- Fragment: `ui/player/PlayerFragment.kt`
- ViewModel: `ui/player/PlayerViewModel.kt`

**... управление авторизацией?**
- API: `data/hdrezka/HdRezkaApi.kt` → `login()`, `isLoggedIn()`
- Manager: `data/AuthManager.kt`
- Storage: `data/LocalStorage.kt`

**... работа с базой данных?**
- Database: `data/AppDatabase.kt`
- DAO: `data/MovieDao.kt`, `data/WatchHistoryDao.kt`

**... парсинг HTML?**
- Parser: `data/parser/UniversalParser.kt`

**... управление состоянием?**
- ViewModel: `ui/MoviesViewModel.kt` или конкретный в `ui/*/`

---

## 📊 Диаграмма импортов

```
MainActivity
    ├─ NavHostFragment
    │   └─ Fragment Navigation
    │
    ├─ SlooshApplication
    │   ├─ HdRezkaRepository
    │   │   ├─ HdRezkaApi
    │   │   │   ├─ OkHttpClient
    │   │   │   ├─ Jsoup (HTML Parser)
    │   │   │   └─ LocalStorage (Cookies)
    │   │   ├─ UniversalParser
    │   │   └─ ConfigProvider
    │   │
    │   ├─ Room Database
    │   │   ├─ MovieDao
    │   │   ├─ WatchHistoryDao
    │   │   └─ BookmarkDao
    │   │
    │   ├─ LocalStorage
    │   │   └─ SharedPreferences
    │   │
    │   └─ AppViewModelFactory
    │
    ├─ ViewModels
    │   ├─ HomeViewModel
    │   ├─ MoviesViewModel
    │   ├─ MovieDetailsViewModel
    │   └─ PlayerViewModel
    │
    └─ UI Components
        ├─ Jetpack Compose
        ├─ Fragments
        └─ RecyclerView Adapters
```

---

## 🎓 Рекомендуемый порядок изучения файлов

1. **Start:** `SlooshApplication.kt` (инициализация)
2. **Then:** `MainActivity.kt` (точка входа)
3. **Then:** `ui/home/HomeFragment.kt` (главный экран)
4. **Then:** `ui/MoviesViewModel.kt` (управление состоянием)
5. **Then:** `data/repository/HdRezkaRepository.kt` (бизнес-логика)
6. **Then:** `data/hdrezka/HdRezkaApi.kt` (сетевой слой)
7. **Then:** `data/parser/UniversalParser.kt` (парсинг)
8. **Then:** `data/LocalStorage.kt` (локальное хранилище)
9. **Then:** `data/AppDatabase.kt` (база данных)

---

## 📞 Помощь

Если не можешь найти файл:
1. Используй **Ctrl+Shift+F** для поиска по тексту
2. Используй **Ctrl+P** для быстрого открытия файла
3. Посмотри структуру в Project Tree слева
4. Поищи в соответствующей документации

---

