# Анализ Проектов в Папке referencias

## Обзор

Папка `referencias` содержит 2 разных реализации приложения HDRezka:

1. **HDrezka-app-master** - Native Android (Kotlin)
2. **lumen-master 2** - React Native / Expo (TypeScript)

---

## 1. HDrezka-app (Native Android - Kotlin)

### Архитектура

**Структура проекта:**
```
app/src/main/java/com/falcofemoralis/hdrezkaapp/
├── models/          # Парсинг и бизнес-логика
├── objects/         # Data classes
├── controllers/     # Контроллеры
├── presenters/      # Presenters (MVP)
├── views/           # UI (Fragments, Activities)
├── clients/         # WebView clients для плеера
├── broadcasts/      # BroadcastReceivers
├── constants/       # Константы
├── utils/           # Utility функции
└── interfaces/      # Интерфейсы
```

**Паттерн архитектуры:** MVP (Model-View-Presenter)
- Отличается от вашего MVVM подхода
- Более старый паттерн, но хорошо структурирован

### Ключевые Компоненты Парсинга

#### 1. FilmModel.kt (831 строка)
Один из самых объемных файлов, отвечает за:

**Получение основных данных фильма:**
```kotlin
fun getMainData(film: Film): Film
fun getMainDataById(film: Film)      // По ID
fun getMainDataByLink(film: Film)    // По ссылке
```

**Селекторы CSS (Jsoup):**
```kotlin
const val FILM_TITLE = "div.b-post__title h1"
const val FILM_POSTER = "div.b-sidecover a"
const val FILM_TABLE_INFO = "table.b-post__info tbody tr"
const val FILM_IMDB_RATING = "span.imdb span"
const val FILM_KP_RATING = "span.kp span"
```

**Парсинг таблицы информации:**
```kotlin
private fun parseTable(document: Document, film: Film) {
    // Извлекает из таблицы:
    // - Рейтинги (IMDB, KP, WA)
    // - Дата выхода и год
    // - Страны (множественные)
    // - И другие данные
}
```

**Парсинг актеров и режиссеров:**
```kotlin
val personsElements: Elements = document.select("div.persons-list-holder")
for (el in personsElements) {
    val els: Elements = el.select("span.item")
    
    if (el.select("span.inline h2").text() == "В ролях актеры") {
        // Парсинг актеров с data-id и data-pid
    } else {
        // Парсинг режиссеров
    }
}
```

**Парсинг расписания серий:**
```kotlin
val seasonsElements: Elements = document.select("div.b-post__schedule div.b-post__schedule_block")
for (block in seasonsElements) {
    // Название сезона
    // Список серий с датами
    // Статусы выхода
}
```

**Функции-помощники:**
```kotlin
fun getTypeByName(name: String): String
fun getConstTypeByName(name: String): FilmType
```

**Получение дополнительных данных:**
```kotlin
fun getAdditionalData(film: Film): Film {
    // Оригинальное название
    // Описание
    // Количество голосов по рейтингам
    // Длительность
    // HR рейтинг
    // Полноразмерный постер
    // Актеры и режиссеры
    // Расписание серий
}
```

#### 2. Другие Модели

**BaseModel** - базовый класс для парсинга:
- Работает с Jsoup для получения HTML
- Управляет куками
- Обработка исключений

**CategoriesModel** - парсинг категорий/жанров
**NewestFilmsModel** - парсинг новейших фильмов
**FilmsListModel** - парсинг списков фильмов
**WatchLaterModel** - управление списком "Смотреть позже"
**UserModel** - профиль пользователя
**BookmarksModel** - закладки/избранное

### Отсутствующие Компоненты в вашем проекте

❌ **Поддержка расписания серий (Schedule)**
- В HDrezka-app это целая система парсинга дат выхода серий
- У вас нет этого функционала

❌ **Детальный парсинг таблицы информации**
- HDrezka-app парсит все ячейки таблицы структурированно
- Извлекает страны, даты, рейтинги

❌ **Парсинг режиссеров отдельно**
- У вас режиссеры в одном методе с описанием
- У них отдельный парсинг с обработкой данных

❌ **HR Рейтинг** (HumanRate или локальный рейтинг)
- Дополнительный рейтинг от сообщества
- `film.ratingHR`, `film.votesHR`, `film.isHRratingActive`

---

## 2. Lumen (React Native - TypeScript)

### Архитектура

**Структура проекта:**
```
src/
├── api/              # API слой
│   ├── RezkaApi/     # Реализация парсинга
│   │   ├── filmApi.ts
│   │   ├── searchApi.ts
│   │   ├── commentsApi.ts
│   │   ├── playerApi.ts
│   │   ├── configApi.ts
│   │   └── utils.ts
│   └── index.ts
├── component/        # React компоненты
├── context/          # Context API
├── type/             # TypeScript интерфейсы
├── util/             # Утилиты
├── service/          # Сервисы
├── store/            # State management
└── navigation/       # Навигация
```

**Паттерн архитектуры:** CLEAN Architecture с Context API
- Похожа на вашу, но использует Context вместо StateFlow
- Разделение на слои более явное

### Ключевые Компоненты Парсинга

#### 1. filmApi.ts (610 строк)

**Получение списка фильмов:**
```typescript
async getFilms(
  page: number,
  path = '',
  variables?: Variables,
  params?: ApiParams
): Promise<FilmListInterface>
```

**Получение фильма по ссылке:**
```typescript
async getFilm(link: string): Promise<FilmInterface | null> {
    const root = await configApi.fetchPage(link);
    
    const id = root.querySelector('#user-favorites-holder')?.attributes['data-post_id'] ?? '';
    const title = root.querySelector('.b-post__title h1')?.rawText ?? '';
    const poster = root.querySelector('.b-sidecover img')?.attributes.src ?? '';
    const largePoster = root.querySelector('.b-sidecover a')?.attributes.href ?? '';
    // ... и так далее
}
```

**Использование утилит парсинга:**
```typescript
import {
  parseFilmCard,
  parseActorCard,
  parseSeasons,
  parseStreams,
  parseSubtitles,
  // и другие утилиты
} from './utils';
```

#### 2. commentsApi.ts

**Получение комментариев:**
```typescript
getComments: async (filmId: string, page: number) => {
    const json = await configApi.getRequest('/ajax/get_comments', {
        t: String(Date.now()),
        news_id: filmId,
        cstart: String(page),
        type: '0',
        comment_id: '0',
        skin: 'hdrezka',
    });

    const result = safeJsonParse<CommentsResult>(json);
    const { comments: commentsHtml, navigation: navigationHtml } = result;
    
    // Парсинг комментариев
    const comments = commentsRoot.querySelectorAll('.comments-tree-item')
      .map((comment) => {
        const id = comment.attributes['data-id'];
        const avatar = comment.querySelector('.ava img')?.attributes.src ?? '';
        const username = comment.querySelector('.name')?.rawText ?? '';
        const date = comment.querySelector('.date')?.rawText ?? '';
        const indent = comment.attributes['data-indent'];  // Для ветвления комментариев!
        const likes = comment.querySelector('.b-comment__like_it')?.attributes['data-likes_num'];
        // ...
      });
}
```

**Обработка форматирования текста:**
```typescript
const text = textElements?.childNodes.reduce<CommentTextInterface[]>((acc, el) => {
    let type = CommentTextType.REGULAR;
    
    switch (el.rawTagName) {
        case 'b': type = CommentTextType.BOLD; break;
        case 'i': type = CommentTextType.INCLINED; break;
        case 'u': type = CommentTextType.UNDERLINE; break;
        case 's': type = CommentTextType.CROSSED; break;
        case 'br': type = CommentTextType.BREAK; break;
        // И поддержка спойлеров!
    }
    
    acc.push({ type, text: decodeHtml(el.rawText) });
    return acc;
}, []);
```

#### 3. searchApi.ts

**Поиск с подсказками:**
```typescript
searchSuggestions: async (query) => {
    const res = await configApi.postRequest('/engine/ajax/search.php', {
        q: query,
    });
    
    const root = configApi.parseContent(res);
    const suggestionsItems = root.querySelectorAll('li');
    
    // Удаление дубликатов через Set
    const arr = suggestionsItems.map((item) => item.querySelector('.enty')?.rawText || '');
    const mArray = new Set(arr);
    const uniqueArray = [...mArray];
    
    return uniqueArray;
}
```

**Обычный поиск:**
```typescript
search: async (query, page) => {
    const root = await configApi.fetchPage('/search/', {
        do: 'search',
        subaction: 'search',
        q: query,
        page: String(page),
    });
    
    return parseFilmsListRoot(root);  // Переиспользование парсера
}
```

### Отличия в подходе

✅ **Более модульный парсинг**
- Вынесены отдельные функции `parseFilmCard`, `parseActorCard` и т.д.
- Легче переиспользовать код

✅ **Типобезопасность**
- Все интерфейсы жестко типизированы
- Утечек типов не будет

✅ **Лучше организованы утилиты**
- `decodeHtml()` для HTML сущностей
- `safeJsonParse()` для безопасного парсинга JSON
- Обработка ошибок встроена

✅ **Поддержка ветвления комментариев**
- Атрибут `data-indent` для вложенности комментариев

---

## Сравнительная Таблица

| Функция | HDrezka-app (Kotlin) | Lumen (TypeScript) | Sloosh |
|---------|---------------------|-------------------|--------|
| Получение фильма | ✅ | ✅ | ✅ |
| Получение актеров | ✅ | ✅ | ✅ |
| Получение режиссеров | ✅ (отдельно) | ✅ | ❌ |
| Расписание серий | ✅ | ✅ | ❌ |
| HR Рейтинг | ✅ | ✅ | ❌ |
| Комментарии | ❌ (пусто) | ✅ | ❌ |
| Поиск с подсказками | ❌ | ✅ | ❌ |
| Форматированный текст комментариев | ❌ | ✅ | ❌ |
| Ветвление комментариев | ❌ | ✅ | ❌ |
| Получение потоков (streams) | ✅ | ✅ | ✅ |
| Получение голосов | ✅ | ✅ | ✅ |

---

## Что можно Добавить в Sloosh

### 1️⃣ ВЫСОКИЙ ПРИОРИТЕТ

**Расписание серий** (Schedule)
- Парсинг дат выхода серий по сезонам
- Статусы выхода (вышла, выходит, ждёт)
- Отображение в UI как горизонтальный список или таблица

```kotlin
data class Schedule(
    val episode: Int,
    val date: String,
    val status: String  // "ждёт", "вышла", "выходит"
)

data class Season(
    val name: String,
    val episodes: List<Schedule>
)
```

**HR Рейтинг** (локальный рейтинг от сообщества)
- Добавить в `Movie` модель: `ratingHR`, `votesHR`, `isHRratingActive`
- Отображать на экране деталей фильма

**Режиссеры отдельно от актеров**
- Парсить режиссеров как отдельный список
- Добавить секцию "Режиссеры" в UI

### 2️⃣ СРЕДНИЙ ПРИОРИТЕТ

**Поиск с автодополнением (подсказки)**
- API: `/engine/ajax/search.php`
- Показывать в реальном времени при вводе
- Удалять дубликаты через `Set`

**Комментарии**
```kotlin
data class Comment(
    val id: String,
    val avatar: String,
    val username: String,
    val date: String,
    val indent: Int,           // Вложенность
    val likes: Int,
    val text: List<CommentText> // С форматированием!
)

enum class CommentTextType {
    REGULAR, BOLD, ITALIC, UNDERLINE, CROSSED, BREAK, SPOILER
}
```

- Парсинг HTML форматирования: `<b>`, `<i>`, `<u>`, `<s>`, спойлеры
- Поддержка вложенных комментариев (по `data-indent`)

### 3️⃣ НИЗКИЙ ПРИОРИТЕТ

**Лучшая организация парсинга**
- Вынести парсинг в отдельные функции (как в Lumen)
- `parseFilm()`, `parseActor()`, `parseSeason()` и т.д.
- Повысить переиспользуемость кода

**Улучшенная обработка ошибок**
- Добавить логирование парсинга
- Graceful fallbacks если элемент не найден

---

## Рекомендуемый План Внедрения

### Этап 1: Быстрые победы (1-2 часа)
1. Добавить HR Рейтинг
2. Добавить режиссеров отдельно
3. Обновить UI в `MovieDetailsFragment`

### Этап 2: Основной функционал (2-3 часа)
1. Парсинг расписания серий
2. Отображение расписания в UI
3. Парсинг комментариев (без форматирования)

### Этап 3: Полировка (1-2 часа)
1. Поиск с подсказками
2. Форматирование комментариев
3. Вложенные комментарии

---

## Селекторы CSS для Парсинга

Исходя из анализа обоих проектов:

```css
/* Основная информация */
div.b-post__title h1               /* Название фильма */
div.b-sidecover img               /* Постер */
div.b-sidecover a                 /* Ссылка на полный постер */

/* Таблица информации */
table.b-post__info tbody tr        /* Строки таблицы */
span.imdb span                     /* IMDB рейтинг */
span.kp span                       /* Kinopoisk рейтинг */

/* Актеры и режиссеры */
div.persons-list-holder            /* Контейнер персон */
span.inline h2                     /* "В ролях актеры" или режиссеры */
span.person-name-item              /* Элемент актера */
[data-id][data-pid]                /* ID и PID актера */

/* HR Рейтинг */
div.b-post__rating                 /* Контейнер HR рейтинга */
span.num                           /* Значение рейтинга */
span.votes span                    /* Количество голосов */

/* Расписание */
div.b-post__schedule               /* Контейнер расписания */
div.b-post__schedule_block         /* Блок сезона */
table tbody tr                     /* Серия */

/* Комментарии */
.comments-tree-item                /* Элемент комментария */
.ava img                           /* Аватар */
.name                              /* Имя */
.date                              /* Дата */
[data-indent]                      /* Вложенность */
.b-comment__like_it                /* Лайки */
.text div                          /* Текст с форматированием */
```

---

## Заключение

Оба проекта в `referencias` имеют более **полный парсинг** и **лучше организованы**:

- **HDrezka-app** - хороший пример для изучения работы с Jsoup и MVP архитектуры
- **Lumen** - отличный пример модульного парсинга и типизации

**Ваш проект (Sloosh)** имеет хорошую базу, но есть **пробелы в парсинге**, особенно:
1. Расписание серий
2. Комментарии
3. Дополнительные рейтинги
4. Поиск с подсказками

Рекомендуется добавить эти функции пошагово, начиная с расписания серий и HR рейтинга.
