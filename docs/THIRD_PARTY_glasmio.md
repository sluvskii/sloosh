# Third-party notices — vendored glasmio (icons + glass engine)

Вендорено из [Glasmio](https://github.com/NEORUAA/Glasmio), снимок
`daa1b06` (v0.1.3, origin/main от 2026-11-12).

## 1. glasmio-icons → `com.slooshfilm.app.ui.sficons`

- Файлы: `SfSymbolCatalog.kt`, `SfSymbol.kt`, `GlasmioIcons.kt`
  (снимок `700a611`, v0.1.1; пакет переименован в `com.slooshfilm.app.ui.sficons`).
- Локальные изменения: Material-fallback удалён; фолбэк — drawables `ic_sf_*`.

## 2. glasmio-glass (render-движок) → `io.github.neoruaa.glasmio.glass`

Ползунок навбара рисуется ДОСЛОВНЫМ `nativeControlLens` (NativeControlLens.kt,
TabBarIndicator) — тот самый AGSL-конвейер iOS 27 NativeControlLens: тело покоя
через UIKit-матрицы NativeTabIdleLight/Dark, портальная деформация, френелевское
кольцо, контактная тень, спектральная дисперсия.

- Скопированы вербатим: `glass/render/**` (39 файлов, кроме AdaptiveGlass.kt)
  и корневые `NativeControlLens*.kt`, `NativeGlassCompletion.kt`,
  `NativeGlassMipPyramid.kt`, `NavigationGlassFilter/Material/DropShadow/
  ContactRefraction/ContactField/NativeUnion/UnionShape/Geometry/InnerShadows.kt`,
  экстракт `LiquidContactShape.kt` (data class из IosToolbarActionGroup.kt),
  `SubsetBoundary.kt` (hasBuiltInGlassLayerBlockIndependentSource — ветки
  CrossWindow/SharedGlassBackdrop опущены: классы вне подмножества).
- Патчи (помечены VENDOR-PATCH или описаны здесь): в NavigationContactRefraction.kt
  и NavigationDropShadow.kt удалены мёртвые wildcard-импорты `core.*`/`icons.*`
  (символы не использовались).
- Точка входа в приложении: `LiquidBottomTabs.kt` Layer 3 — `drawPlainBackdrop`
  + `nativeControlLens(style = TabBarIndicator, …, liftedContent = …)` через
  мост `GlasmioBackdropBridge.kt` (адаптер Backdrop-интерфейсов наших движков).
- Доп. зависимость: `io.github.kyant0:capsule:2.1.3` (continuous-геометрия).
- glasmio.core и glasmio.icons НЕ вендорились: у приложения своя тема и свои иконки (п. 1).

## 3. glasmio-ui (частично) → `io.github.neoruaa.glasmio.ui`

- `GlassBottomBar.kt` — вербатим из апстрима, кроме трёх групп правок:
  1. `GlasmioTabItem.badgeCount` + обёртка `BadgedBox` вокруг глифа (непрочитанные «Чаты»);
  2. `pressedScale = 60f / 44f` вместо апстримовских `78f / 56f` (метрики бара sloosh, правило 9);
  3. **VENDOR-PATCH(sloosh)** — режим «только иконки»: параметр `showsLabels` у
     `GlasmioNavigationBar` и `showLabel` у `FullTabContent` / `MorphingSelectedNavigationItem` /
     `NavigationTabContent`. Подпись при `false` не компонуется вовсе (иначе её отступы 1dp + 4dp
     смещают глиф выше центра капсулы), `hasNativeShortTabs` считает внутреннюю ширину элемента
     только по глифу (UIKit-распределение коротких табов), а имя таба уходит в `contentDescription`
     для TalkBack. Хост включает его из настройки `tab_bar_shows_labels` (1:1 с iOS
     `tabBarShowsLabels` / `.labelStyle(.iconOnly)`).
- `GlassButtons.kt` — вербатим (`GlasmioIconButton` и др.).
- `NativeControlLensGeometry.kt` — вербатим-экстракт `Modifier.nativeControlLensGeometry`
  из апстримовского `GlassControls.kt` (сам `GlassControls.kt` не вендорился).
- `GlasmioNavigationBarWithTrailingItem.kt` — **файл sloosh, не апстрим**: обёртка, собирающая
  вендоренный `GlasmioNavigationBar`, свой `FloatingNavigationControl` (отцеплённая круглая кнопка
  поиска) и `BottomNavigationContact`; параметр `showsLabels` пробрасывается транзитом.

## Лицензии

- Glasmio: Apache-2.0 (Copyright 2026 NEORUAA), текст — в исходниках файлов.
- glasmio-glass/render — AndroidLiquidGlass 2.0.1 (Kyant0), Apache-2.0
  (см. NOTICE.md внутри render/).
- Шрифт `app/src/main/res/font/sf_pro_symbols.ttf` — официальный переменный
  SF Pro из дистрибутива Apple (developer.apple.com/fonts, SF-Pro.dmg от
  2026-10-29); распространяется по лицензии Apple, не покрыт лицензиями
  Glasmio или этого проекта.

