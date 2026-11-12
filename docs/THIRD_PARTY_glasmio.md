# Third-party notices — vendored glasmio-icons

Вендорено из [Glasmio](https://github.com/NEORUAA/Glasmio), модуль `glasmio-icons`,
снимок `700a611` (v0.1.1), дата снимка 2026-11-04.

- Файлы: `app/src/main/java/com/slooshfilm/app/ui/sficons/SfSymbolCatalog.kt`,
  `SfSymbol.kt`, `GlasmioIcons.kt` (пакет переименован в `com.slooshfilm.app.ui.sficons`).
- Лицензия оригинала: Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0).
  Copyright 2026 NEORUAA.
- Локальные изменения: удалён Material-fallback (правило проекта — запрет Google
  Material Icons); фолбэк — векторные drawables проекта `ic_sf_*` через
  `SlooshIconFallback.kt`; `Typeface.create(Typeface, Int, Boolean)` защищён
  гардом для API 26/27.

Каталог имён SF Symbols (name → codepoint) сгенерирован оригинальным автором
скриптом из официального SF Symbols CLI; сам каталог — данные имён и адресов.

Шрифт `app/src/main/res/font/sf_pro_symbols.ttf` — официальный переменный SF Pro
из дистрибутива Apple (developer.apple.com/fonts, SF-Pro.dmg от 2026-10-29).
Шрифтовые файлы Apple распространяются по собственной лицензии Apple и не
покрываются лицензией ни Glasmio, ни этого проекта.

Отдельные растровые ассеты Glasmio (PNG глифов прослушивания) НЕ вендорились.
