# Third-party notices — vendored glasmio (иконки, стеклянный движок, контролы)

Вендорено из [Glasmio](https://github.com/NEORUAA/Glasmio), снимок
`daa1b06` (v0.1.3, origin/main от 2026-11-12).

Весь стеклянный слой приложения (навбар, кнопки, поверхности, меню, шиты, диалоги,
переключатели, ползунки, сегмент-контролы) — это glasmio. Собственный движок
`com.slooshfilm.app.ui.backdrop/**` и компоненты `ui/components/LiquidGlass*.kt`
удалены из проекта.

## 1. glasmio-icons → `com.slooshfilm.app.ui.sficons`

- Файлы: `SfSymbolCatalog.kt`, `SfSymbol.kt`, `GlasmioIcons.kt`
  (снимок `700a611`, v0.1.1; пакет переименован в `com.slooshfilm.app.ui.sficons`).
- Локальные изменения: Material-fallback удалён; фолбэк — drawables `ic_sf_*`.

## 2. glasmio-glass (движок) → `io.github.neoruaa.glasmio.glass` + `glass.render`

- `glass/render/**` — 39 файлов вербатим (кроме `AdaptiveGlass.kt`).
- Корень `glass/` — 37 файлов: `nativeControlLens` (NativeControlLens.kt), материалы навбара
  (`Navigation*`), `GlassSurface.kt`, `ProgressiveBackdropBlur.kt`, `BottomNavigationContact.kt`,
  `DampedDragAnimation.kt`, `InteractiveHighlight.kt`, `PressHighlightRenderer.kt`,
  `LiquidMenu*`, `IosToolbarActionGroup.kt`, `GlassMorphRenderer.kt`,
  экстракт `LiquidContactShape.kt` (data class из `IosToolbarActionGroup.kt`),
  `SubsetBoundary.kt` (`hasBuiltInGlassLayerBlockIndependentSource`; ветки
  CrossWindow/SharedGlassBackdrop опущены — классы вне подмножества).
- Патчи:
  - **VENDOR-PATCH** в `NavigationContactRefraction.kt` и `NavigationDropShadow.kt` — удалены
    мёртвые wildcard-импорты `core.*` / `icons.*`;
  - приватный класс `Backdrop` в `render/backdrops/` переименован в `BackdropDecorator`
    (иначе конфликт имён с публичным `Backdrop` внутри того же пакета).
- Доп. зависимость: `io.github.kyant0:capsule:2.1.3` (continuous-геометрия).
- `io.github.neoruaa.glasmio.icons` в проект не переносился — иконки даёт п. 1.
- Точки входа в приложении: `ui/components/SlooshBottomBar.kt` (навбар, эталон),
  `ui/components/SlooshGlass.kt` (`Modifier.slooshGlassSurface` → `navigationGlassBackground`),
  `ui/components/SlooshGlassButton.kt` (`GlassSurface`). Мост-адаптер `GlasmioBackdropBridge.kt`
  больше не нужен: собственного `Backdrop` в проекте нет, тип один.

## 3. glasmio-core → `io.github.neoruaa.glasmio.core`

4 файла вербатим: `GlasmioTheme.kt`, `GlasmioTypography.kt`, `GlassTokens.kt`
(`GlassColors`, `GlassDimensions`, `GlassMetrics`, `GlassAppearance`, `LocalGlassColors`, …),
`SwipeBackExclusion.kt`. Нужны, потому что `GlassSurface`/`navigationGlassBackground`
и все вендоренные контролы читают `LocalGlassColors` / `GlassDimensions`.

## 4. glasmio-ui → `io.github.neoruaa.glasmio.ui`

23 файла. Вербатим, кроме перечисленных правок:

- `GlassBottomBar.kt` — три группы правок:
  1. `GlasmioTabItem.badgeCount` + обёртка `BadgedBox` вокруг глифа (непрочитанные «Чаты»);
  2. `pressedScale = 60f / 44f` вместо апстримовских `78f / 56f` (метрики бара sloosh, правило 9);
  3. **VENDOR-PATCH(sloosh)** — режим «только иконки»: параметр `showsLabels` у
     `GlasmioNavigationBar` и `showLabel` у `FullTabContent` / `MorphingSelectedNavigationItem` /
     `NavigationTabContent`. Подпись при `false` не компонуется вовсе (иначе её отступы 1dp + 4dp
     смещают глиф выше центра капсулы), `hasNativeShortTabs` считает внутреннюю ширину элемента
     только по глифу (UIKit-распределение коротких табов), а имя таба уходит в `contentDescription`
     для TalkBack. Хост включает его из настройки `tab_bar_shows_labels`
     (1:1 с iOS `tabBarShowsLabels` / `.labelStyle(.iconOnly)`).
- `IosMenus.kt` / `IosLists.kt` — **VENDOR-PATCH(sloosh)**: ссылки на
  `com.slooshfilm.app.ui.sficons.LocalSfToolbarSymbols` сделаны полными именами (импорт
  остаётся только в `GlassSurface.kt`), чтобы не тянуть локал в чужие пакеты.
- `GlasmioNavigationBarWithTrailingItem.kt` — **файл sloosh, не апстрим**: обёртка, собирающая
  вендоренный `GlasmioNavigationBar`, свой `FloatingNavigationControl` (отцеплённая круглая кнопка
  поиска) и `BottomNavigationContact`; `showsLabels` пробрасывается транзитом.
- Контролы, на которых живут настройки и медиа-UI: `GlasmioSwitch` / `GlasmioSlider` /
  `GlasmioSegmentedControl` (`GlassControls.kt`), `GlasmioAdaptiveSlider`,
  `GlasmioGroupedList` / `GlasmioSectionTitle` / `GlasmioGroupedTableFooter` (`IosLists.kt`),
  `GlasmioMenu` / `GlasmioMenuItem` (`IosMenus.kt`), `GlasmioAlertDialog` (`IosAlerts.kt`),
  шиты (`IosSheet*`, `IosOverlays.kt`), `GlasmioIconButton` (`GlassButtons.kt`).
- `MaterialGlasmioIconResolver` (апстрим) не используется: требует `material-icons`, которые
  проекту запрещены.

## Лицензии

- Glasmio: Apache-2.0 (Copyright 2026 NEORUAA), текст — в исходниках файлов.
- glasmio-glass/render — AndroidLiquidGlass 2.0.1 (Kyant0), Apache-2.0
  (см. NOTICE.md внутри render/).
- Шрифт `app/src/main/res/font/sf_pro_symbols.ttf` — официальный переменный
  SF Pro из дистрибутива Apple (developer.apple.com/fonts, SF-Pro.dmg от
  2026-10-29); распространяется по лицензии Apple, не покрыт лицензиями
  Glasmio или этого проекта.
