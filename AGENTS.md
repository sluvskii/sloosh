# Agent Onboarding & Architecture Guide

## Workspace Overview

This workspace contains the flagship native Android client for **sloosh**:

- **Package**: `com.slooshfilm.app`
- **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Android 12+ AGSL Shaders & RenderEffect. Единственный движок стекла — вендоренный glasmio (`io.github.neoruaa.glasmio.glass` + `glass.render`, дословный порт 0.1.3); собственного движка бэкдропа в проекте больше нет.
- **Design Language**: Apple TV+ meets authentic iOS Liquid Glass adapted for Android.

---

## Agent Conduct, Honesty & Engineering Rules

1. **Strict Honesty & Zero Hallucinations**:
   - **Never lie, deceive, or mislead the user.** Never claim that a glass refraction, shader, or UI effect is working properly if it is faked, broken, or substituted by a cheap workaround.
   - If something cannot be done (e.g. due to Android RenderEffect platform limitations, AGSL shader constraints, or Compose architectural barriers), **state it honestly, clearly, and directly**. Never attempt to hide limitations behind broken hacks.
2. **Strict Adherence to User Instructions (Zero Unprompted Inventions)**:
   - **Do strictly what the user asked.** Do not invent unauthorized "improvements", spontaneous color stops, arbitrary offsets, unrequested borders, or custom redesigns.
   - **Inspect the code — never assume or guess.** Always check actual source code, exact layout bounds, parent paddings, and math instead of making assumptions.
   - If an idea, doubt, or alternative solution arises, **ask the user first** before implementing it.
3. **Product Name**: `sloosh` (strictly lowercase).
4. **UI Copy Language**: Russian strictly.
5. **Provider Confidentiality**: Never introduce any user-facing mention of internal sources, providers, or scrapers (`Alloha`, `Collaps`, `NeoMovies`, `TMDB`, `Kinopoisk`, etc.).
6. **Streaming Sources**: Exclusively `Alloha` (`api.alloha.tv`).
7. **Terminal Constraints**: **DO NOT** execute `./gradlew` commands in terminal.
8. **Zero UltraThinMaterial**: Do not use flat generic semi-transparent surfaces; every glass surface and button must come from the vendored glasmio engine (`GlassSurface` / `navigationGlassBackground` / `Glasmio*` controls), never from a hand-rolled `background(color.copy(alpha = …))`.
9. **Active Slider Droplet Scale Rule**: The active tab dynamic droplet pill during dragging / active state strictly inflates to `pressedScale = 60f / 44f` (60dp over 44dp dynamic expansion) and the active tab icon scales to `1.2f`. **NEVER reduce this scale or make the slider smaller.**
10. **Strict Prohibition of Android / Google Material Icons (SF Symbols Only)**:
   - **Total Ban on Google Material Icons**: Do not use `androidx.compose.material.icons` (`Icons.Rounded.*`, `Icons.Default.*`, `Icons.Filled.*`, etc.) or generic Android drawables.
   - **Strictly SF Symbols**: All icons across the entire application must strictly be authentic Apple SF Symbols, matching iOS `sloosh` 1:1 in geometry, stroke weight, and corner radii. Primary rendering: the official variable SF Pro symbol font (`res/font/sf_pro_symbols.ttf`) drawn through the vendored glasmio-icons renderer (`com.slooshfilm.app.ui.sficons`, `SfIcon("имя.символа")` — exact Apple names, Apache-2.0, см. `docs/THIRD_PARTY_glasmio.md`). **All existing call sites are migrated: any new code MUST render icons via `SfIcon(systemName = "<apple.name>", size = ...)` — never `Icon(painterResource(...))` with `ic_sf_*` drawables.** The drawables remain only as the automatic missing-glyph fallback (`SlooshIconFallback` map) and for non-symbol artwork (studio/brand logos `ic_studio_*`, flags). Never introduce any other icon set.

---

## Liquid Glass Design System — единый движок glasmio

Всё стекло и все кнопки приложения рисует **вендоренный glasmio** (`io.github.neoruaa.glasmio.*`),
тот же движок, что у нижнего навбара. Собственный самописный движок
(`com.slooshfilm.app.ui.backdrop/**`, `ui/components/LiquidGlass*.kt`, `Ios27ControlLens.kt`,
`InteractiveHighlight.kt`, `DampedDragAnimation.kt`) **удалён полностью** — его больше нет и
возвращать его нельзя. Любая новая поверхность/кнопка/меню/шит строится на glasmio.

Слои движка:

| Слой | Пакет | Что даёт |
|---|---|---|
| Захват и эффекты | `io.github.neoruaa.glasmio.glass.render` | `Backdrop`, `layerBackdrop`, `rememberLayerBackdrop`, `rememberCanvasBackdrop`, `drawBackdrop`, `drawPlainBackdrop`, `effects.*`, `highlight.*`, `shadow.*` |
| Материал | `io.github.neoruaa.glasmio.glass` | `GlassSurface`, `navigationGlassBackground`, `nativeControlLens`, `progressiveBackdropBlur`, `LocalGlassBackdrop` |
| Токены | `io.github.neoruaa.glasmio.core` | `GlassColors` (в т.ч. `LocalGlassColors`), `GlassAppearance`, `GlassDimensions`, `GlasmioTheme` |
| Контролы | `io.github.neoruaa.glasmio.ui` | `GlasmioSwitch`, `GlasmioSlider`/`GlasmioAdaptiveSlider`, `GlasmioSegmentedControl`, `GlasmioGroupedList`/`GlasmioListRow`, `GlasmioMenu`/`GlasmioMenuItem`, `GlasmioAlertDialog`, `GlasmioBottomSheet`, `GlasmioIconButton`, `GlasmioButton` |

### 1. Пресеты sloosh → материал glasmio

Вызывающий код продолжает говорить пресетами, но это **только словарь**:
`SlooshGlassTokens.Bar / Slider / Button / ButtonActive / Surface / CompactPill / Menu / Dialog`
(тип `GlassPreset`, объявлен в `ui/components/SlooshGlass.kt`). Пресеты больше ничего не рисуют
сами — они отображаются на материал glasmio в одной точке (`Modifier.slooshGlassSurface`).

### 2. Обёртки sloosh (имена сохранены ради мест вызова)

Все они лежат в `ui/components/` и внутри вызывают glasmio:

1. **`Modifier.slooshGlassSurface(...)` / `Modifier.slooshGlassInteractive(...)`** (`SlooshGlass.kt`) —
   стеклянная поверхность glasmio (`navigationGlassBackground`) поверх бэкдропа.
2. **`SlooshFloatingButton`** (`SlooshGlassButton.kt`) — плавающая кнопка на `GlassSurface`
   (материал навбара); `SlooshDualButton` / `SlooshMultiButton` — капсулы из двух/нескольких действий.
3. **`SlooshToggle`** → `GlasmioSwitch`; **`SlooshSlider`** → `GlasmioAdaptiveSlider`
   (`SlooshToggle.kt` / `SlooshSlider.kt`); `SettingsSegmentedRow` →
   `GlasmioSegmentedControl` напрямую.
4. **`SlooshGlassDialog` / `SlooshAlertDialog` / `SlooshGlassPlayerDialog`** (`SlooshGlassDialog.kt`) —
   карточка на `GlassSurface` (34dp непрерывный радиус) + скрим; **`SlooshGlassMenuOverlay`** /
   **`SettingsMenuTrigger`** → `GlasmioMenu` + `GlasmioMenuItem`; состояние — `SlooshMenuState`
   (`LocalSlooshMenuHostState`).
5. **`SlooshGroupedList`** (`SlooshGroupedList.kt`) → `GlasmioGroupedList`.
6. **`Modifier.plusBlendMode()`** — iOS `.blendMode(.plusLighter)` через `saveLayer` + `BlendMode.Plus`.

### 3. Настройки стекла идут в движок

`SlooshGlassSettingsProvider(...)` (`NavGraph.kt`, а также `AuthSheet` — диалог живёт в отдельном
окне) переводит настройки sloosh в `GlassAppearance` движка:

- «Режим без стекла» → `reduceTransparency = true`;
- «Размытие стекла» (0..1) → при значении ≤ 0.08 `liquidGlassEnabled = false`, иначе материал
  остаётся оптическим;
- `LocalProgressiveBlur` управляет только прогрессивным блюром шапок
  (`progressiveBackdropBlur`, 4dp пик, хвост `ProgressiveBlurTokens.Extension` = 30dp).

---

### 3. Screen Architectures & Layout Patterns

#### Details Screen (`DetailsScreen.kt`)
- **Dominant Ambient Color**: Dynamic background color `effectiveBg` matching iOS `UIImage.averageColor` 1:1. Extracted using `rememberAdaptiveAmbientColor` (cached via `LruCache`, downsampled to 16x16 with arithmetic RGB mean, blended 35% with black `lerp(Color.Black, ambientColor, 0.35f)`).
- **Poster Fade**: Backdrop poster fades smoothly into `effectiveBg` using an authentic bottom gradient.
- **Top Dominant Gradient Bar**: Height `160.dp` with soft 2-stop fade (`listOf(effectiveBg, effectiveBg.copy(alpha = 0f))`). Crossfades in via `graphicsLayer { alpha = topGradientAlpha }` when scrolled past discrete threshold (`370.dp`). Rendered inside `layerBackdrop(detailsBackdrop)` so that top floating glass buttons refract the gradient seamlessly. Matches `HomeScreen.kt` gradient feathering 1:1.
- **Top Floating Navigation Bar**:
  - Leading: Back button (`44.dp` `SlooshFloatingButton`, icon `24.dp`).
  - Center: Compact Title / Logo (`graphicsLayer { alpha = topLogoAlpha }`), smoothly appearing when scrolled past `370.dp` while content logo fades out.
- **Action Buttons (iOS 1:1, DetailsHeroActionMetrics)**: Hero row with 22dp spacing: Download button (`52.dp` circular glass `SlooshFloatingButton`, unified `26.dp` `ic_sf_arrow_down_to_line` icon), Primary Play disc (`76.dp` white 85% disc with `56.dp` glyph-box cut-out triangle (чернила ~36dp, пропорция iOS ~47% диска) and built-in optical centering), and Favorite button (`52.dp` circular glass `SlooshFloatingButton`, unified `26.dp` heart icon).

#### Home Screen (`HomeScreen.kt`)
- **Top Header**: Category text tabs with soft 2-stop vertical gradient (`BackgroundDark` to transparent) with `statusBarsPadding()`.
- **Infinite 3-Column Grid**: Responsive lazy vertical grid with shimmer placeholders.

---

### 4. GPU & Compose Performance Standards (60/120 FPS)

1. **Кэширование кромок и геометрии делает glasmio.** Прежние `GlassBorderCache` /
   `Modifier.glassBorder` / `iosLens` жили в удалённом самописном движке. Вендоренный glasmio
   кэширует контуры, `Outline`, записи `GraphicsLayer` и матрицы линзы сам
   (`NativeGlassMipPyramid`, `NavigationInnerShadows`, `NativeControlLensConsumerPool`),
   а при прозрачности слоя (`graphicsLayer { alpha = … }`) запись не повторяется — альфу
   обновляет только GPU.
2. **Zero-Cost Resting State**: у навбара линза на покое не запускает рантайм-шейдеры; не
   добавляй собственных шейдерных проходов в места, где стекло статично.
3. **Ambient Color Extraction Efficiency**:
   - Memory-cached in `LruCache<String, Color>`.
   - Downsamples image requests to 16x16 thumbnails matching iOS `UIImage.averageColor`, computing arithmetic RGB mean via bulk `Bitmap.getPixels` in `< 0.1ms` on IO dispatcher without UI thread frame drops.

---

### 5. Bottom Navigation (`SlooshBottomBar.kt` + vendored glasmio-ui)

The navbar is the vendored glasmio bar (iOS 27 Liquid Glass), **not** a hand-rolled 3-layer composite:

- **Main platter**: `GlasmioNavigationBar` with the four `Screen.bottomBarTabs`
  (Главная, Чаты, Продолжить, Профиль) on one `BottomNavigationContact` sheet; height
  `NavigationBarSurfaceHeight` = 62dp in every orientation.
- **Trailing control**: the detached circular Поиск button (`FloatingNavigationControl`,
  `detachTrailingItem = true`; 62dp touch surface, `CompactBottomControlSize` = 48dp glass).
- **Selection**: the morphing `nativeControlLens(TabBarIndicator)` droplet driven by
  `DampedDragAnimation`; the platter inflates with `pressedScale = 60f / 44f` and the selected
  glyph scales to `1.2f`. Never reduce these (rule 9).
- **Search mode** (`currentRoute == Screen.Search.route`): the platter is replaced by
  back-to-previous-tab + glass search field + close button inside an `AnimatedContent`.
- **Unread badge**: `GlasmioTabItem.badgeCount` → Material `BadgedBox` around the Чаты glyph
  (sloosh addition to the vendored file).

**Tab glyph rule (iOS 1:1).** One symbol per tab, *identical* in the resting and the selected state.
`private fun Screen.tabSymbol()` in `SlooshBottomBar.kt` is the single source of truth:
`house.fill`, `bubble.left.and.bubble.right.fill`, `clock.arrow.circlepath`, `person.fill`,
`magnifyingglass` — exactly `iOS-sloosh` `Sources/UI/Home/ContentView.swift` (Чаты has no iOS
counterpart). Selection is conveyed by tint and the droplet only. **Never swap an outline glyph for a
filled one on selection**: iOS never does it, and the outline pair reads as a foreign icon set.

**Tab caption rule.** Captions are a user setting, not a layout constant:
`LocalStorage.getTabBarShowsLabels()` (pref `tab_bar_shows_labels`, default `false` = icons only, the
iOS default) → `LocalTabBarShowsLabels` (provided in `NavGraph.kt`, refreshed by the
`OnSharedPreferenceChangeListener`) → `GlasmioNavigationBarWithTrailingItem(showsLabels = …)`.
User-facing toggle: Настройки → Интерфейс → «Названия вкладок». When captions are off the vendored bar
does not compose the `Text` at all (composing it with zero alpha would leave its 1dp + 4dp paddings and
push the glyph above the capsule center), switches to UIKit's short-item allocation
(`hasNativeShortTabs`), and moves the tab name into `contentDescription` so TalkBack still announces it.

Сегмент-контрол Настроек (`SettingsSegmentedRow`, `settings/Common.kt`) рисуется вендоренным
`GlasmioSegmentedControl` напрямую — собственной обёртки у него нет.

---

> [!IMPORTANT]
> Правило одно для всего приложения: **навбар — это эталон**. Любая стеклянная поверхность,
> кнопка, меню, шит или диалог вне навбара обязаны собираться из того же glasmio-материала
> (`GlassSurface` / `navigationGlassBackground`) и того же бэкдропа
> (`layerBackdrop` / `rememberCanvasBackdrop`). Своих шейдеров, своих линз и своих «стеклянных»
> `Box` с `background(color.copy(alpha = …))` в проекте быть не должно.

### 6. Adaptive Layout: Landscape & Tablets (`AdaptiveLayout.kt`)

`com.slooshfilm.app.ui.components.AdaptiveLayout.kt` is the **single source of truth** for orientation and
screen-size adaptation. Never re-derive `LocalConfiguration.current.orientation` / `screenWidthDp >= 600`
inline in new code, and **never hardcode `54.dp` / `24.dp` for the bottom bar** — consume the helpers:

| Helper | Purpose |
| --- | --- |
| `rememberSlooshDeviceClass()` | `Compact` < 600dp, `Medium` 600..839dp, `Expanded` >= 840dp (Material 3 breakpoints). |
| `rememberIsLandscape()` / `rememberIsTablet()` | Orientation and the historical `screenWidthDp >= 600` tablet check. |
| `rememberIsShortViewport()` | Phone in landscape (height < 600dp): full-screen forms must scroll. |
| `rememberAdaptiveBarMetrics()` | Bottom bar `barHeight`, `buttonSize`, `horizontalPadding`, `maxBarWidth`, `compactTabsWidth`. |
| `rememberAdaptiveSheetHeight(portraitFraction)` | Modal sheet height: 0.62 portrait (iOS 1:1) / 0.92 landscape. |

**Bottom bar contract.** Portrait phone is untouched: 54dp bar, full width, 24dp side padding
(`maxBarWidth == null`). Landscape and tablets get the compact split layout: compact tabs capsule
on the **left** (`barHeight = 44dp`, `compactTabsWidth = 280dp`), circular search button (44dp)
on the **right**, side padding `16.dp + cutoutPadding` (aligning 1:1 with top bar buttons across screens).
Left thumb controls tabs, right thumb controls search, and the center is clear.
This matches what the content screens already reserve — every screen uses
`bottomTotalPadding = navBarBottomPadding + 48.dp` in landscape, i.e. exactly `44dp bar + 4dp top padding`.
`pillHeight` follows as `barHeight - 8.dp` (46dp -> 36dp), and the droplet keeps inflating with the same
`pressedScale = 60f / 44f` / `pressedScaleY = 68f / 44f` ratios (rule 9 is never touched: the ratio is
applied to the pill, so the droplet scales proportionally by itself).

**What the glasmio navbar actually consumes** (see §5): only `horizontalPadding` and `compactTabsWidth`.
Its glass surface is a fixed `NavigationBarSurfaceHeight` = 62dp in both orientations and the detached
search control is 62dp as well, so `barHeight`, `buttonSize` and `maxBarWidth` currently have no consumer
— keep them in sync with the reserved-space math of content screens, but do not expect them to resize the bar.

**Short viewport contract.** Any full-screen form (mandatory `AuthSheet`, TV `PairConfirmScreen`) must stay
reachable in landscape: add `.verticalScroll(...)` gated on `rememberIsShortViewport()` and replace
`Spacer(Modifier.weight(1f))` centering with fixed spacers in that mode, because `weight` collapses to 0
inside a scrolling column. Portrait and tablet layouts must keep the weighted centering.

**Rotation is reactive, not recreating.** `MainActivity` declares
`android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"`, so the
activity is **not** recreated on rotation. Everything adaptive must read `LocalConfiguration.current`
during composition (directly or through these helpers) — values captured once in `remember { }` without the
configuration as a key will silently freeze at the orientation the screen was opened in.
