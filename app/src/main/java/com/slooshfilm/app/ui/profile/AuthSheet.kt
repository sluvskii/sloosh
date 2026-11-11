package com.slooshfilm.app.ui.profile

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.kyant.shapes.Capsule
import com.slooshfilm.app.R
import com.slooshfilm.app.data.repository.AuthRepository
import com.slooshfilm.app.data.repository.NativeGoogleAuthManager
import com.slooshfilm.app.ui.backdrop.Backdrop
import com.slooshfilm.app.ui.backdrop.backdrops.rememberCanvasBackdrop
import com.slooshfilm.app.ui.components.LiquidGlassFloatingButton
import com.slooshfilm.app.ui.components.LocalGlassBlurMultiplier
import com.slooshfilm.app.ui.components.LocalGlassEnabled
import com.slooshfilm.app.ui.components.LocalProgressiveBlur
import com.slooshfilm.app.ui.components.plusBlendMode
import com.slooshfilm.app.ui.components.rememberIsShortViewport
import com.slooshfilm.app.ui.theme.BackgroundDark
import com.slooshfilm.app.ui.theme.BackgroundLight
import com.slooshfilm.app.ui.theme.LocalSlooshColors
import com.slooshfilm.app.ui.theme.SlooshAccent
import com.slooshfilm.app.ui.theme.TextPrimary
import com.slooshfilm.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Flagship Cinematic Auth View for sloosh.
 * Inspired by ChatGPT & Claude: dynamic typewriter cinema quotes,
 * minimal aesthetic, 100% native Google Sign-In, zero clutter.
 */
@Composable
fun AuthSheet(
    onDismissRequest: (() -> Unit)? = null,
    canDismiss: Boolean = (onDismissRequest != null),
    backdrop: Backdrop? = null
) {
    val isGlassEnabled = LocalGlassEnabled.current
    val isProgressiveBlurEnabled = LocalProgressiveBlur.current
    val glassBlurMultiplier = LocalGlassBlurMultiplier.current
    val slooshColors = LocalSlooshColors.current

    if (canDismiss && onDismissRequest != null) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogView = LocalView.current
            SideEffect {
                var parent = dialogView.parent
                while (parent != null && parent !is DialogWindowProvider) {
                    parent = parent.parent
                }
                (parent as? DialogWindowProvider)?.window?.let { window ->
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
            }
            CompositionLocalProvider(
                LocalGlassEnabled provides isGlassEnabled,
                LocalProgressiveBlur provides isProgressiveBlurEnabled,
                LocalGlassBlurMultiplier provides glassBlurMultiplier,
                LocalSlooshColors provides slooshColors
            ) {
                AuthContent(
                    onDismissRequest = onDismissRequest,
                    backdrop = backdrop
                )
            }
        }
    } else {
        AuthContent(
            onDismissRequest = null,
            backdrop = backdrop
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthContent(
    onDismissRequest: (() -> Unit)?,
    backdrop: Backdrop?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val authRepo = remember { AuthRepository.getInstance(context) }

    val versionName = remember(context) {
        runCatching {
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pi.versionName ?: "2.0.0"
        }.getOrDefault("2.0.0")
    }

    val isLoading by authRepo.isLoading.collectAsState()
    val lastError by authRepo.lastError.collectAsState()

    val isLightTheme = !LocalSlooshColors.current.isDark
    val screenBg = if (isLightTheme) BackgroundLight else BackgroundDark
    val screenBgState = rememberUpdatedState(screenBg)

    val sheetBackdrop = rememberCanvasBackdrop {
        drawRect(screenBgState.value)
    }

    DisposableEffect(Unit) {
        onDispose {
            authRepo.clearError()
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            scope.launch {
                try {
                    val success = authRepo.signInWithGoogleNative(data, context)
                    if (success) {
                        Toast.makeText(context, "Вход через Google выполнен", Toast.LENGTH_SHORT).show()
                        onDismissRequest?.invoke()
                    } else {
                        val err = authRepo.lastError.value
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка входа: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Телефон в горизонтальной ориентации: вертикального места (~320-430dp) не хватает на
    // логотип, hero-текст и кнопки входа. Вход в sloosh обязательный (NavGraph не монтирует
    // основной UI без авторизации), поэтому без прокрутки кнопка «Войти через Google» уезжает
    // за край экрана и приложение становится непригодным до поворота обратно в портрет.
    // В портрете и на планшете isShortViewport == false — раскладка остаётся прежней.
    val isShortViewport = rememberIsShortViewport()
    val authScrollState = rememberScrollState()

    // Relatable cinema thoughts & marketing slogans (ChatGPT style, zero cringe)
    val phrases = remember {
        listOf(
            "Не только смотри, но и sloosh-ай",
            "Смотри глазами. sloosh-ай сердцем",
            "Твой личный кинозал в кармане",
            "Один клик — и ты уже в кино",
            "Кинотеатр, который всегда с тобой",
            "Залипнуть на все выходные? Легко",
            "Ещё одну серию и точно спать",
            "Тот самый фильм из сохранённых",
            "Кино, которое давно откладывали",
            "Сериал, который затянет с первой серии",
            "Что вышло в хорошем качестве?"
        )
    }

    var currentPhrase by remember { mutableStateOf("") }
    var revealedWordCount by remember { mutableIntStateOf(0) }
    var isDissolving by remember { mutableStateOf(false) }

    // Generative word-by-word blur focus loop
    LaunchedEffect(phrases) {
        var playlist = phrases.shuffled()
        var playlistIndex = 0

        while (true) {
            val target = playlist[playlistIndex]
            currentPhrase = target
            val words = target.split(" ").filter { it.isNotBlank() }
            revealedWordCount = 0
            isDissolving = false

            // Smooth fade-in of empty container before typing begins
            delay(280)

            // Sequentially materialize each word from blur into sharp focus
            for (i in 1..words.size) {
                revealedWordCount = i
                val currentWord = words[i - 1]
                val wordDelay = when {
                    currentWord.length <= 2 -> 110L
                    currentWord.endsWith(".") || currentWord.endsWith("?") -> 240L
                    currentWord.endsWith(",") || currentWord == "—" -> 190L
                    else -> 160L
                }
                delay(wordDelay)
            }

            // Reading phase (held in crisp focus)
            delay(2800)

            // Dissolve phase (entire phrase smoothly blurs away into 0 alpha)
            isDissolving = true
            delay(380) // 380ms guarantees 320ms dissolve animation has fully reached 0 alpha

            // Advance to next random phrase without immediate repeat
            playlistIndex++
            if (playlistIndex >= playlist.size) {
                val lastItem = playlist.lastOrNull()
                var nextShuffled = phrases.shuffled()
                if (nextShuffled.size > 1 && nextShuffled.first() == lastItem) {
                    val swapIdx = (1 until nextShuffled.size).random()
                    val mutable = nextShuffled.toMutableList()
                    val temp = mutable[0]
                    mutable[0] = mutable[swapIdx]
                    mutable[swapIdx] = temp
                    nextShuffled = mutable
                }
                playlist = nextShuffled
                playlistIndex = 0
            }

            // Reset words count while STILL completely dissolved (alpha = 0f)
            revealedWordCount = 0
            delay(40) // Allow Compose to mount reset while invisible
        }
    }

    val dissolveProgress by animateFloatAsState(
        targetValue = if (isDissolving) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "heroDissolveProgress"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Atmospheric cinematic background: breathing aurora orbs + 35mm film grain texture
        CinematicAtmosphereBackground(isLightTheme = isLightTheme)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxSize()
                    // Прокрутка включается только в коротком вьюпорте: в портроте цепочка
                    // модификаторов остаётся байт-в-байт прежней.
                    .then(if (isShortViewport) Modifier.verticalScroll(authScrollState) else Modifier)
                    .padding(horizontal = 28.dp)
                    .padding(
                        top = statusBarPadding + 42.dp,
                        bottom = navBarBottom + 20.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // MARK: - Top Logo (Claude style)
            Image(
                painter = painterResource(R.drawable.logo_sloosh),
                contentDescription = "sloosh",
                colorFilter = ColorFilter.tint(SlooshAccent),
                modifier = Modifier
                    .height(24.dp)
                    .widthIn(max = 95.dp)
            )

            // В коротком вьюпорте weight(1f) внутри scroll-контейнера схлопнулся бы в 0,
            // поэтому центрирование заменяется фиксированным отступом.
            if (isShortViewport) {
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // MARK: - Center Hero (Word-by-Word Blur Focus Reveal)
            val brandAccent = if (isLightTheme) SlooshAccent else SlooshAccent.copy(alpha = 0.88f)
            val textPrimaryColor = if (isLightTheme) TextPrimary else TextPrimary.copy(alpha = 0.78f)
            val density = LocalDensity.current
            val textOffsetYPx = remember(density) { with(density) { (-8).dp.toPx() } }
            val parentBlur = (dissolveProgress * 7f).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp)
                    .graphicsLayer {
                        alpha = 1f - dissolveProgress
                        translationY = dissolveProgress * textOffsetYPx
                        clip = false
                    }
                    .plusBlendMode()
                    .then(
                        if (parentBlur > 0.5.dp) {
                            Modifier.blur(
                                radius = parentBlur,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                val words = remember(currentPhrase) {
                    if (currentPhrase.isNotBlank()) currentPhrase.split(" ").filter { it.isNotBlank() } else emptyList()
                }

                key(currentPhrase) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                    ) {
                        words.forEachIndexed { index, word ->
                            BlurRevealWord(
                                word = word,
                                isRevealed = revealedWordCount > index,
                                brandAccent = brandAccent,
                                textColor = textPrimaryColor
                            )
                        }
                    }
                }
            }

            // Error notice (if any)
            AnimatedVisibility(
                visible = !lastError.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    val errBg = Color(0xFFFF453A).copy(alpha = if (isLightTheme) 0.08f else 0.14f)
                    val errBorder = Color(0xFFFF453A).copy(alpha = if (isLightTheme) 0.25f else 0.35f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(errBg)
                            .border(1.dp, errBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sf_exclamationmark_triangle_fill),
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = lastError.orEmpty(),
                            color = Color(0xFFFF453A),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isShortViewport) {
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // MARK: - Google Sign-In Button (Claude style bold clean pill)
            val buttonContainerColor = if (isLightTheme) Color.White else Color.White
            LiquidGlassFloatingButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    authRepo.clearError()
                    try {
                        val intent = NativeGoogleAuthManager.getInstance(context).getSignInIntent()
                        googleLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ошибка запуска Google: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = Capsule(),
                backdrop = sheetBackdrop,
                tint = buttonContainerColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Авторизация...",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Продолжить через Google",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value proposition: Cloud profile sync
            Text(
                text = "История просмотров и избранное сохранятся в вашем профиле",
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = 0.70f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Discrete app version
            Text(
                text = "Версия $versionName",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.38f),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
}

/**
 * Renders a single word with an authentic optical blur-focus reveal.
 * - Zero edge clipping: uses BlurredEdgeTreatment.Unbounded and clip = false so blur fades organically into space.
 * - Zero layout passes during animation: translationY is executed strictly on the GPU display list in graphicsLayer.
 * - Zero GPU leak: Drops instantly to pure native static Text as soon as progress reaches 1.0f.
 */
@Composable
private fun BlurRevealWord(
    word: String,
    isRevealed: Boolean,
    brandAccent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val offsetYPx = remember(density) { with(density) { 4.dp.toPx() } }

    val progress by animateFloatAsState(
        targetValue = if (isRevealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 260,
            easing = FastOutSlowInEasing
        ),
        label = "blurWordReveal"
    )

    val isSloosh = remember(word) { word.contains("sloosh", ignoreCase = true) }

    val annotated = remember(word, brandAccent, isSloosh) {
        if (isSloosh) {
            buildAnnotatedString {
                val slooshRegex = Regex("(?i)sloosh")
                val match = slooshRegex.find(word)
                if (match != null) {
                    append(word.substring(0, match.range.first))
                    withStyle(SpanStyle(color = brandAccent)) {
                        append(match.value)
                    }
                    append(word.substring(match.range.last + 1))
                } else {
                    append(word)
                }
            }
        } else {
            AnnotatedString(word)
        }
    }

    val blurAmount = ((1f - progress) * 6f).dp

    // Single stable Text node with persistent graphicsLayer — eliminates node-switching blinks & gamma pops
    Text(
        text = annotated,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        lineHeight = 36.sp,
        modifier = modifier
            .graphicsLayer {
                this.alpha = progress
                this.scaleX = 1f + (1f - progress) * 0.05f
                this.scaleY = 1f + (1f - progress) * 0.05f
                this.translationY = (1f - progress) * offsetYPx
                this.clip = false
            }
            .then(
                if (progress in 0.01f..0.98f && blurAmount > 0.5.dp) {
                    Modifier.blur(
                        radius = blurAmount,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Flagship Cinematic Background:
 * 1. Deep OLED Obsidian Base (BackgroundDark / BackgroundLight).
 * 2. Slow, breathing ambient aurora orbs (Brand lime, cinema emerald, twilight violet) drifting in harmonic orbits.
 * 3. Authentic 35mm cinema film grain texture (micro-noise, tiled via GPU ImageShader at 120 FPS).
 * 4. Cinematic edge vignette focusing optical depth onto center typography.
 */
@Composable
private fun CinematicAtmosphereBackground(
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val brandAccent = SlooshAccent
    val screenBg = if (isLightTheme) BackgroundLight else BackgroundDark

    val infiniteTransition = rememberInfiniteTransition(label = "auroraBreath")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraPhase"
    )

    // Ultra-fine 128x128 35mm film grain texture generated once and cached in memory (64KB RAM)
    val noiseBitmap = remember(isLightTheme) {
        val size = 128
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val random = java.util.Random(1337)
        for (i in pixels.indices) {
            if (isLightTheme) {
                val alpha = random.nextInt(10) // subtle dark speckles (0..4%)
                pixels[i] = android.graphics.Color.argb(alpha, 0, 0, 0)
            } else {
                val brightness = 210 + random.nextInt(46)
                val alpha = random.nextInt(18) // subtle silver-halide film grain (0..7%)
                pixels[i] = android.graphics.Color.argb(alpha, brightness, brightness, brightness)
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.asImageBitmap()
    }

    val grainBrush = remember(noiseBitmap) {
        ShaderBrush(
            ImageShader(
                image = noiseBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val twoPi = (2.0 * Math.PI).toFloat()
        val angle = (phase * twoPi).toDouble()

        // 1. Base OLED dark / clean light canvas
        drawRect(color = screenBg, size = size)

        // 2. Orb 1: Grand sloosh lime light — sweeps the entire height and width
        val c1X = (w * (0.50 + 0.38 * kotlin.math.sin(angle))).toFloat()
        val c1Y = (h * (0.50 + 0.35 * kotlin.math.cos(angle))).toFloat()
        val r1 = w * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brandAccent.copy(alpha = if (isLightTheme) 0.12f else 0.22f),
                    brandAccent.copy(alpha = if (isLightTheme) 0.04f else 0.07f),
                    Color.Transparent
                ),
                center = Offset(c1X, c1Y),
                radius = r1
            ),
            radius = r1,
            center = Offset(c1X, c1Y)
        )

        // 3. Orb 2: Counter-orbiting lime wave traversing diagonal corners in infinity loop (2*angle)
        val c2X = (w * (0.50 + 0.35 * kotlin.math.cos(angle + Math.PI / 4.0))).toFloat()
        val c2Y = (h * (0.50 + 0.38 * kotlin.math.sin(2.0 * angle))).toFloat()
        val r2 = w * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brandAccent.copy(alpha = if (isLightTheme) 0.08f else 0.14f),
                    Color.Transparent
                ),
                center = Offset(c2X, c2Y),
                radius = r2
            ),
            radius = r2,
            center = Offset(c2X, c2Y)
        )

        // 4. Orb 3: Counter-balance lime aura (opposite pole, 180° shift for continuous ambient fill)
        val c3X = (w * (0.50 - 0.36 * kotlin.math.sin(angle))).toFloat()
        val c3Y = (h * (0.50 - 0.34 * kotlin.math.cos(angle))).toFloat()
        val r3 = w * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    brandAccent.copy(alpha = if (isLightTheme) 0.06f else 0.11f),
                    Color.Transparent
                ),
                center = Offset(c3X, c3Y),
                radius = r3
            ),
            radius = r3,
            center = Offset(c3X, c3Y)
        )

        // 5. Film grain texture overlay (35mm tactile micro-grain)
        drawRect(
            brush = grainBrush,
            size = size
        )

        // 6. Subtle cinema vignette (focuses eye into center)
        val vignetteAlpha = if (isLightTheme) 0.06f else 0.28f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = vignetteAlpha)
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.95f
            ),
            size = size
        )
    }
}
