# Расширенное логирование для отладки парсинга эпизодов

## Проблема
Эпизоды показывают только "1x01 Серия 1" вместо реальных названий и дат.

Это значит, что **`ep.text()` возвращает пустую строку**.

## Решение - усиленное логирование

Я добавил подробное логирование на каждом этапе парсинга. Теперь вам нужно:

### Шаг 1: Собрать приложение

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
cd W:\sloosh
.\gradlew.bat clean :app:assembleDebug
```

### Шаг 2: Запустить на эмуляторе или телефоне

Откройте приложение, перейдите на сериал с эпизодами.

### Шаг 3: Собрать логи в Android Studio

1. Откройте **Logcat** (View → Tool Windows → Logcat)
2. Фильтр: `HdRezkaApi`
3. Экспортируйте логи (правой кнопкой → Export)

### Шаг 4: Посмотрите на ключевые сообщения

**Главные вопросы, которые помогут логи ответить:**

1. **Сколько сезонов найдено?**
   ```
   D/HdRezkaApi: Found X seasons in HTML
   ```
   - Если 0 → селекторы `.b-simple_season__item` не находят элементы
   - Если > 0 → элементы есть

2. **Сколько эпизодов в сезоне?**
   ```
   D/HdRezkaApi: Season 1: found X episode elements
   ```
   - Если 0 → селектор `#simple-episodes-list-$seasonId li.b-simple_episode__item` не работает
   - Если > 0 → элементы есть

3. **Какой текст получаем из элемента?**
   ```
   D/HdRezkaApi: Episode 1 x 1: title='...'
   ```
   - Если пусто → элемент существует, но `ep.text()` пуст
   - Если текст есть → будет показано в UI

4. **HTML структура**
   ```
   D/HdRezkaApi: HTML sample: ...
   ```
   - Здесь видна реальная структура HTML, которую возвращает сервер

## Вероятные проблемы и решения

### Проблема 1: Сезонов вообще не найдено (Found 0 seasons)
**Причина:** Селектор `.b-simple_season__item` неверный для этого сайта
**Решение:** Нужно посмотреть HTML sample в логе и найти правильный селектор

### Проблема 2: Сезоны найдены, но эпизодов нет в каждом
**Причина:** Селектор для элементов эпизодов неверный
**Решение:** Нужно посмотреть HTML sample и понять структуру

### Проблема 3: Эпизоды найдены, но title пуст
**Причина:** `ep.text()` возвращает пустую строку, но текст есть в дочерних элементах
**Решение:** Код уже пытается извлечь из дочерних элементов (span, a, div)
- Если это не помогает → нужно посмотреть HTML и выбрать правильные селекторы

### Проблема 4: HTML sample пуст или содержит только `<body></body>`
**Причина:** API возвращает пустой ответ
**Решение:** Проблема с зеркалом или сервером вернул ошибку

## Дополнительное логирование

Я добавил логирование:

1. **Первых 500 символов ответа API**
   ```
   D/HdRezkaApi: getEpisodes response (first 500 chars): ...
   ```

2. **JSON структуры**
   ```
   D/HdRezkaApi: JSON response: success=true, keys=[...]
   ```

3. **HTML фрагментов из JSON**
   ```
   D/HdRezkaApi: Seasons HTML (first 300): ...
   D/HdRezkaApi: Episodes HTML (first 300): ...
   ```

4. **Структуры HTML документа**
   ```
   D/HdRezkaApi: HTML structure: div, span, ...
   ```

5. **Каждого найденного эпизода**
   ```
   D/HdRezkaApi: Episode 1 x 1: title='...'
   D/HdRezkaApi: Episode 1 x 2: title='...'
   ...
   ```

## Процедура отладки

### Если ничего не работает:

1. Собирайте логи как описано выше
2. Посмотрите логи и найдите:
   - Количество сезонов
   - Количество эпизодов
   - HTML sample (что там есть)
3. Отправьте основные логи (первые 100 строк HdRezkaApi)
4. По ним я смогу точно определить, что нужно изменить в селекторах

## Ожидаемые хорошие логи

Если всё работает, вы должны увидеть:

```
D/HdRezkaApi: getEpisodes response (first 500 chars): {"success":true,"seasons":"<div class='...
D/HdRezkaApi: JSON response: success=true, keys=[success, seasons, episodes, ...]
D/HdRezkaApi: Extracted from JSON: seasonsHtml length=X, episodesHtml length=Y
D/HdRezkaApi: Found 3 seasons in HTML
D/HdRezkaApi: Season 1: found 10 episode elements
D/HdRezkaApi: Episode 1 x 1: title='1 - Пилот 15 сентября 2012'
D/HdRezkaApi: Episode 1 x 2: title='2 - Куча молока 22 сентября 2012'
...
D/HdRezkaApi: Successfully parsed 3 seasons with 30 total episodes
```

---

**Следующий шаг:** Соберите приложение, запустите, посмотрите логи и поделитесь ими.
