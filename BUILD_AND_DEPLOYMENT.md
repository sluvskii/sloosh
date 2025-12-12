# Sloosh - Сборка, тестирование и развертывание

## 🔨 Сборка проекта

### Требования

- **JDK:** 11+
- **Android SDK:** 34 (Target SDK)
- **Gradle:** 8.13.0
- **Kotlin:** 2.0.21
- **Android Studio:** Arctic Fox или новее (рекомендуется)

### Локальная сборка

#### 1. Настройка окружения

```bash
# Создайте local.properties в корне проекта
cd w:\sloosh

# Добавьте путь к Android SDK
echo sdk.dir=C:\\Users\\<YourUsername>\\AppData\\Local\\Android\\sdk >> local.properties
```

#### 2. Сборка через Gradle Wrapper

```bash
# Windows PowerShell
# Debug APK
.\gradlew.bat assembleDebug

# Release APK (потребуется подпись)
.\gradlew.bat assembleRelease

# Сборка с очисткой
.\gradlew.bat clean build

# Установка на устройство
.\gradlew.bat installDebug
```

#### 3. Запуск через Android Studio

1. Откройте проект в Android Studio
2. Выберите модуль `:app` в селекторе Run Configuration
3. Нажмите `Shift + F10` (Run) или `Ctrl + F11` (Debug)

---

## 📦 Структура Build Gradle

### app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")           // Kotlin Symbol Processing
    id("kotlin-parcelize")                  // Parcelable support
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.slooshfilm.app"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.slooshfilm.app"
        minSdk = 33              // Android 13+
        targetSdk = 34           // Android 14
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    // Все зависимости указаны в файле
}
```

---

## 🧪 Тестирование

### Типы тестов в проекте

#### 1. Unit тесты (Локальные тесты)

**Расположение:** `/app/src/test/java/`

Используются для тестирования:
- Repository логики
- ViewModel логики
- Утилит и хелперов
- Парсеров

```bash
# Запуск всех unit тестов
.\gradlew.bat test

# Запуск конкретного теста
.\gradlew.bat test --tests com.slooshfilm.app.data.repository.*

# С отчетом покрытия
.\gradlew.bat test --info
```

#### 2. Integration тесты (AndroidTest)

**Расположение:** `/app/src/androidTest/java/`

Используются для тестирования:
- Взаимодействия с Room Database
- UI компонентов
- Fragment навигации

```bash
# Запуск на подключенном устройстве/эмуляторе
.\gradlew.bat connectedAndroidTest

# Запуск конкретного инструментального теста
.\gradlew.bat connectedAndroidTest --tests com.slooshfilm.app.ui.*
```

---

## 🔐 Подпись APK для Release

### 1. Создание keystore файла

```bash
# Использование keytool из JDK
keytool -genkey -v -keystore sloosh-release.jks `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -alias sloosh-release

# При запросе введите:
# - Пароль keystore
# - Пароль ключа
# - Данные владельца (имя, организация и т.д.)
```

### 2. Сборка подписанного APK

```bash
# Через Gradle (требуется конфигурация в build.gradle.kts)
.\gradlew.bat assembleRelease

# Или используйте Android Studio:
# Build → Generate Signed Bundle / APK
```

### 3. Проверка подписи APK

```bash
# Проверить сертификат в APK
jarsigner -verify -verbose sloosh-release.apk

# Или использовать apksigner
apksigner verify -v sloosh-release.apk
```

---

## 🚀 Развертывание на устройстве

### Опция 1: Через Android Studio

1. Подключите устройство с включенной разработкой по USB
2. Run → Run 'app'
3. Выберите устройство из списка

### Опция 2: Через команду gradle

```bash
# Debug версия
.\gradlew.bat installDebug

# Запуск приложения
adb shell am start -n com.slooshfilm.app/.MainActivity
```

### Опция 3: Ручная установка APK

```bash
# Скопируйте APK на устройство
adb push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/

# Установите
adb shell pm install /data/local/tmp/app-debug.apk
```

---

## 📊 Gradle Tasks

### Основные команды

```bash
# Список доступных задач
.\gradlew.bat tasks

# Очистка
.\gradlew.bat clean

# Сборка всех вариантов
.\gradlew.bat build

# Только компиляция
.\gradlew.bat compileDebugKotlin
.\gradlew.bat compileReleaseKotlin

# Проверка зависимостей
.\gradlew.bat dependencies
.\gradlew.bat dependencyInsight --dependency androidx.compose.ui:ui

# Форматирование кода
.\gradlew.bat ktlintFormat

# Анализ кода
.\gradlew.bat lint
```

### Custom Tasks

```bash
# Полная пересборка (полезно при проблемах)
.\gradlew.bat clean assembleDebug

# Сборка и запуск тестов
.\gradlew.bat build test

# Генерация отчета покрытия тестами
.\gradlew.bat testDebugUnitTest --tests "*" --info
```

---

## 🐛 Отладка

### Включение логирования

```bash
# Логирование Gradle
.\gradlew.bat build --info

# С полным debug выводом
.\gradlew.bat build --debug

# Только ошибки
.\gradlew.bat build --warn
```

### Инструменты отладки

#### Logcat (Просмотр логов)

```bash
# В Android Studio: View → Tool Windows → Logcat

# Через adb:
adb logcat

# Фильтрация по тегу
adb logcat | grep HdRezkaApi

# Очистка логов
adb logcat -c

# Сохранение в файл
adb logcat > logs.txt
```

#### Android Profiler

1. Android Studio → Run → Profiler
2. Или иконка на toolbar "Profiler"
3. Можно отслеживать:
   - CPU использование
   - Memory
   - Network трафик
   - Battery

#### Layout Inspector

1. Tools → Layout Inspector
2. Инспектирование структуры View иерархии
3. Полезно для отладки UI проблем

---

## 🔍 Анализ производительности

### Компилирование и сборка

```bash
# Время компиляции
.\gradlew.bat build --profile

# Отчет будет в build/reports/profile/

# Анализ зависимостей
.\gradlew.bat :app:dependencies
```

### Runtime анализ

#### Network Profiler
- Отслеживание HTTP запросов
- Проверка размера payload
- Анализ задержек

#### Memory Profiler
- Поиск утечек памяти
- Анализ использования Heap
- Профилирование GC

#### CPU Profiler
- Анализ горячих функций
- Trace вызовов методов

---

## 📝 CI/CD интеграция

### GitHub Actions пример

```yaml
name: Android Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run Unit Tests
      run: ./gradlew test
    
    - name: Build Debug APK
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/
```

---

## 🔧 Решение распространенных проблем

### Проблема 1: "Cannot resolve symbol"

```bash
# Решение 1: Переиндексировать проект
File → Invalidate Caches → Invalidate and Restart

# Решение 2: Пересборка
.\gradlew.bat clean build

# Решение 3: Перегенерация Room entities
.\gradlew.bat kspDebugKotlin
```

### Проблема 2: Ошибка сборки Room

```kotlin
// Error: [ksp] No suitable constructor found for entity

// Решение: Убедитесь что @Entity имеет правильный конструктор
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey val url: String,
    val title: String,
    // ... остальные поля
    // Все не-primary ключи должны иметь значения по умолчанию
)
```

### Проблема 3: Versioning conflict для зависимостей

```bash
# Просмотр конфликтов
.\gradlew.bat dependencies --configuration compileClasspath

# Решение: Явно указать версию в gradle/libs.versions.toml
[versions]
okhttp = "4.11.0"

[libraries]
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
```

### Проблема 4: SSL Certificate Verification Error

```kotlin
// В HdRezkaApi создается unsafe HTTP client для работы с зеркалами
// Это необходимо, так как некоторые зеркала используют самоподписанные сертификаты

// Для production рекомендуется:
// 1. Использовать только официальные зеркала (hdrzk.org, stepnet.video)
// 2. Добавить их сертификаты в network_security_config.xml
```

---

## 📦 Версионирование и Release

### Version management

```gradle
// В build.gradle.kts
defaultConfig {
    versionCode = 1        // Увеличивается с каждым release
    versionName = "1.0"    // Версия для пользователей
}
```

### Процесс релиза

1. **Обновление версии**
   ```gradle
   versionCode = 2        // Увеличить
   versionName = "1.1"    // Обновить версию
   ```

2. **Создание тагов в Git**
   ```bash
   git tag -a v1.1 -m "Release version 1.1"
   git push origin v1.1
   ```

3. **Сборка Release APK**
   ```bash
   ./gradlew.bat clean assembleRelease
   ```

4. **Подпись APK**
   - Используйте keystore из local.properties
   - Проверьте подпись перед публикацией

5. **Тестирование перед публикацией**
   ```bash
   ./gradlew.bat connectedAndroidTest
   ```

---

## 🎯 Performance Tips

### Оптимизация времени сборки

1. **Включите Gradle Daemon**
   ```gradle
   org.gradle.daemon=true
   ```

2. **Параллельная компиляция**
   ```gradle
   org.gradle.parallel=true
   org.gradle.workers.max=8
   ```

3. **Отключите ненужные Lint проверки**
   ```gradle
   lintOptions {
       disable 'UnusedResources', 'MissingTranslation'
   }
   ```

4. **Используйте KSP вместо KAPT**
   - KSP (~2x быстрее)
   - Уже используется в проекте для Room

---

