# Agent Onboarding & Architecture Guide

## Workspace Overview

This workspace contains the flagship native Android client for **sloosh**:

- **Package**: `com.slooshfilm.app`
- **Tech Stack**: Kotlin, Jetpack Compose, Material 3, Android 12+ AGSL Shaders & RenderEffect (via in-tree backdrop engine `com.slooshfilm.app.ui.backdrop` and custom AGSL extensions).
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
8. **Zero UltraThinMaterial**: Do not use flat generic semi-transparent surfaces; always use the centralized `LiquidGlass` system.
9. **Active Slider Droplet Scale Rule**: The active tab dynamic droplet pill during dragging / active state strictly inflates to `pressedScale = 60f / 44f` (60dp over 44dp dynamic expansion) and the active tab icon scales to `1.2f`. **NEVER reduce this scale or make the slider smaller.**
10. **Strict Prohibition of Android / Google Material Icons (SF Symbols Only)**:
   - **Total Ban on Google Material Icons**: Do not use `androidx.compose.material.icons` (`Icons.Rounded.*`, `Icons.Default.*`, `Icons.Filled.*`, etc.) or generic Android drawables.
   - **Strictly SF Symbols**: All icons across the entire application must strictly be authentic Apple SF Symbols, matching iOS `sloosh` 1:1 in geometry, stroke weight, and corner radii. Primary rendering: the official variable SF Pro symbol font (`res/font/sf_pro_symbols.ttf`) drawn through the vendored glasmio-icons renderer (`com.slooshfilm.app.ui.sficons`, `SfIcon("имя.символа")` — exact Apple names, Apache-2.0, см. `docs/THIRD_PARTY_glasmio.md`). **All existing call sites are migrated: any new code MUST render icons via `SfIcon(systemName = "<apple.name>", size = ...)` — never `Icon(painterResource(...))` with `ic_sf_*` drawables.** The drawables remain only as the automatic missing-glyph fallback (`SlooshIconFallback` map) and for non-symbol artwork (studio/brand logos `ic_studio_*`, flags). Never introduce any other icon set.

---

## Liquid Glass Design System (`com.slooshfilm.app.ui.components`)

The custom Liquid Glass engine is fully unified and centralized in [`LiquidGlass.kt`](file:///w:/android-sloosh/app/src/main/java/com/slooshfilm/app/ui/components/LiquidGlass.kt). 
All glass surfaces, buttons, and navigation elements consume shared tokens and modifiers. **Never duplicate 50+ lines of raw shader or backdrop code across screens.**

### 1. Centralized Design Tokens (`LiquidGlassTokens` & `LiquidGlassStyle`)

All optics, refractions, glints, and borders are configured through `@Immutable data class LiquidGlassStyle` in `LiquidGlassTokens`:

- **`LiquidGlassTokens.Bar`**:
  - Primary bottom navigation bar (54dp capsule).
  - Optics: `blurRadius = 10.dp`, `blurOpacity = 0.80f`, `lens(12.dp, 22.dp)`.
  - Glint: Concentrated top highlight `alpha = 0.68f` (`ConcentratedHighlightStyle(angle = 90f)`).
  - Border: Overlay hairline `0.5.dp` (`0.30f` light / `0.55f` dark).
- **`LiquidGlassTokens.Slider`**:
  - Active tab dynamic droplet pill (46dp capsule).
  - Optics: `4.5.dp` height, `8.dp` amount, chromatic aberration `0.16f` (code truth; the old 0.22 was a doc error).
  - Glint: Soft glint `0.50f` during drag.
  - **Rest body and lifted state — AUTHENTIC glasmio**: the pill is rendered by the vendored glasmio-glass engine (`io.github.neoruaa.glasmio.glass`, verbatim 0.1.3) via `drawPlainBackdrop` + `nativeControlLens(style = TabBarIndicator, …, liftedContent = …)` — the real iOS 27 NativeControlLens pipeline (NativeTabIdle matrices at rest, portal warp, Fresnel ring, contact shadow, spectral dispersion). Do NOT reimplement or approximate it with our `iosLens`/custom shaders — the bridge is `GlasmioBackdropBridge.kt` (sloosh Backdrop → glasmio Backdrop). Lift kinematics: `DampedDragAnimation(liftSpringStiffness = 631.65468f, liftHangTimeMs = 220)`; `pressedScale = 60f / 44f` (rule 9) is never replaced by glasmio's `78/56`.
- **`LiquidGlassTokens.Button` / `ButtonActive`**:
  - Floating action buttons (Search button in navbar, media controls, close/back/favorite buttons).
  - Optics: `14.dp` height, `28.dp` amount, inner shadow radius `8.dp`, drop shadow.
  - Interactive kinematics: Elastic `tanh` drag stretch, angular touch scaling, and `InteractiveHighlight` touch ripple.
  - `ButtonActive`: Elevated glint (`0.85f`) and deeper shadows for active/selected buttons.
- **`LiquidGlassTokens.Surface` & `CompactPill`**:
  - Content cards, modal sheets, and search input bars.

> [!TIP]
> **Single Source of Truth**: Modifying parameters in `LiquidGlassTokens` or shaders in `LiquidGlass.kt` automatically propagates across the entire application.

---

### 2. Unified Declarative Modifiers

Always use the standardized modifiers rather than low-level `drawBackdrop` calls:

1. **`Modifier.liquidGlassSurface(...)`**:
   Applies glass optics, vibrant shaders, and overlay borders to static or content surfaces:
   ```kotlin
   Modifier.liquidGlassSurface(
       style = LiquidGlassTokens.Surface,
       shape = RoundedRectangle(16.dp),
       containerColor = tint,
       backdrop = backdrop
   )
   ```
2. **`Modifier.liquidGlassInteractive(...)`**:
   For buttons and interactive pills. Encapsulates full Kyant liquid kinematics (elastic touch stretch with `tanh` damping) + glass optics + `InteractiveHighlight` glow + overlay border:
   ```kotlin
   Modifier.liquidGlassInteractive(
       interactiveHighlight = searchInteractiveHighlight,
       style = if (isSearchActive) LiquidGlassTokens.ButtonActive else LiquidGlassTokens.Button,
       shape = CircleShape,
       containerColor = containerColor,
       backdrop = activeBackdrop
   )
   ```
3. **Convenience Components**:
   - `LiquidGlassFloatingButton`: Pre-built circular floating glass button with touch physics and liquid optics (`44.dp` or `50.dp`).
   - `LiquidGlassDualButton`: Pre-built two-action pill capsule.
   - `Modifier.liquidGlass(...)` & `Modifier.slooshSurface(...)`: Drop-in modifiers for cards and search inputs.

---

### 3. Screen Architectures & Layout Patterns

#### Details Screen (`DetailsScreen.kt`)
- **Dominant Ambient Color**: Dynamic background color `effectiveBg` matching iOS `UIImage.averageColor` 1:1. Extracted using `rememberAdaptiveAmbientColor` (cached via `LruCache`, downsampled to 16x16 with arithmetic RGB mean, blended 35% with black `lerp(Color.Black, ambientColor, 0.35f)`).
- **Poster Fade**: Backdrop poster fades smoothly into `effectiveBg` using an authentic bottom gradient.
- **Top Dominant Gradient Bar**: Height `160.dp` with soft 2-stop fade (`listOf(effectiveBg, effectiveBg.copy(alpha = 0f))`). Crossfades in via `graphicsLayer { alpha = topGradientAlpha }` when scrolled past discrete threshold (`370.dp`). Rendered inside `layerBackdrop(detailsBackdrop)` so that top floating glass buttons refract the gradient seamlessly. Matches `HomeScreen.kt` gradient feathering 1:1.
- **Top Floating Navigation Bar**:
  - Leading: Back button (`44.dp` `LiquidGlassFloatingButton`, icon `24.dp`).
  - Center: Compact Title / Logo (`graphicsLayer { alpha = topLogoAlpha }`), smoothly appearing when scrolled past `370.dp` while content logo fades out.
- **Action Buttons (iOS 1:1, DetailsHeroActionMetrics)**: Hero row with 22dp spacing: Download button (`52.dp` circular glass `LiquidGlassFloatingButton`, unified `26.dp` `ic_sf_arrow_down_to_line` icon), Primary Play disc (`76.dp` white 85% disc with `56.dp` glyph-box cut-out triangle (чернила ~36dp, пропорция iOS ~47% диска) and built-in optical centering), and Favorite button (`52.dp` circular glass `LiquidGlassFloatingButton`, unified `26.dp` heart icon).

#### Home Screen (`HomeScreen.kt`)
- **Top Header**: Category text tabs with soft 2-stop vertical gradient (`BackgroundDark` to transparent) with `statusBarsPadding()`.
- **Infinite 3-Column Grid**: Responsive lazy vertical grid with shimmer placeholders.

---

### 4. GPU & Compose Performance Standards (60/120 FPS)

1. **`GlassBorderCache` in `Modifier.glassBorder`**:
   - Contour geometries (`Outline`), paths (`Path`), and `GraphicsLayer` recordings are cached against `size` and `layoutDirection`.
   - During dragging or alpha animations, `graphicsLayer.record(...)` and outline generation are **bypassed entirely**. The GPU directly updates layer alpha.
   - Supports lambda alpha `alpha = { progress * ... }` to avoid recomposing modifier chains on 120Hz displays.
2. **Zero-Cost Resting State**:
   - When `alpha <= 0f`, `glassBorder` immediately returns, skipping `drawLayer`.
   - In `LiquidBottomTabs` Layer 3, `iosLens` is guarded with `if (progress > 0f)`. At rest, runtime shaders are skipped.
3. **Ambient Color Extraction Efficiency**:
   - Memory-cached in `LruCache<String, Color>`.
   - Downsamples image requests to 16x16 thumbnails matching iOS `UIImage.averageColor`, computing arithmetic RGB mean via bulk `Bitmap.getPixels` in `< 0.1ms` on IO dispatcher without UI thread frame drops.

---

### 5. Bottom Navigation 3-Layer Architecture (`LiquidBottomTabs.kt`)

The bottom bar employs a 3-layer optical composite:
- **Layer 1 (Navbar Base)**: Height `barHeight` (54dp portrait / 44dp landscape, see §6), `LiquidGlassTokens.Bar`. Renders the main glass bar and inactive tab icons.
- **Layer 2 (Off-screen Capture)**: Height `pillHeight` = `barHeight - 8.dp` (46dp / 36dp), `.alpha(0f).layerBackdrop(tabsBackdrop)`.
  - Captures active tab icons with `LocalLiquidBottomTabTint` (`SlooshAccent` lime).
  - Uses `lens(12.dp, 22.dp)` so that Layer 3 refracts the wallpaper underneath without turning flat gray.
  - Highlight and border scale strictly with `progress > 0f` to keep resting state pristine.
- **Layer 3 (Dynamic Lens Pill)**: Height `pillHeight` (46dp / 36dp), width `pillWidthDp`.
  - Samples `rememberCombinedBackdrop(backdrop, tabsBackdrop)`.
  - Features `iosLens` (5/10dp with 0.40 dispersion) only while dragging.
  - Dynamically inflates to `pressedScale = 60f / 44f` during active drag and scales the active tab icon to `1.2f`.
  - In resting state, produces a smooth, unbordered frosted glass tab.

---

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

**Short viewport contract.** Any full-screen form (mandatory `AuthSheet`, TV `PairConfirmScreen`) must stay
reachable in landscape: add `.verticalScroll(...)` gated on `rememberIsShortViewport()` and replace
`Spacer(Modifier.weight(1f))` centering with fixed spacers in that mode, because `weight` collapses to 0
inside a scrolling column. Portrait and tablet layouts must keep the weighted centering.

**Rotation is reactive, not recreating.** `MainActivity` declares
`android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|smallestScreenSize"`, so the
activity is **not** recreated on rotation. Everything adaptive must read `LocalConfiguration.current`
during composition (directly or through these helpers) — values captured once in `remember { }` without the
configuration as a key will silently freeze at the orientation the screen was opened in.
