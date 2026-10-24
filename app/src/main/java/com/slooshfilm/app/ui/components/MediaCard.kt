package com.slooshfilm.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.theme.*

val LocalCardStyle = compositionLocalOf { LocalStorage.CARD_STYLE_CLASSIC }
val LocalCardDensity = compositionLocalOf { LocalStorage.CARD_DENSITY_REGULAR }
val LocalPosterQuality = compositionLocalOf { "Высокое" }
val LocalShowOriginalTitle = compositionLocalOf { true }
val LocalTabBarShowsLabels = compositionLocalOf { false }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    media: MediaDto,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    width: Int? = null,
    cardStyle: String = LocalCardStyle.current
) {
    val cardModifier = if (width != null) {
        modifier.width(width.dp)
    } else {
        modifier.fillMaxWidth()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = cardModifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Poster Box (2:3 aspect ratio) with iOS Continuous G2 Squircle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedRectangle(12.dp))
                .background(SurfaceDark)
        ) {
            val posterQuality = LocalPosterQuality.current
            val isLowQuality = posterQuality == "Низкое"
            val posterUrl = media.getDisplayPosterUrl(isLowQuality = isLowQuality)
            var isError by remember(posterUrl) { mutableStateOf(false) }

            if (!isError && !posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
                    onError = { isError = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = media.displayTitle.take(1),
                        style = SlooshTypography.headlineMedium,
                        color = TextMutedDark
                    )
                }
            }

            // Rating badge (top-start)
            val rating = media.rating ?: media.ratings?.kp ?: media.ratings?.imdb
            if (rating != null && rating > 0.0) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 6.dp)
                        .clip(RoundedRectangle(7.5.dp))
                        .background(ratingColor(rating))
                        .padding(horizontal = 5.5.dp, vertical = 2.2.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", rating),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 14.sp
                    )
                }
            }
            // Overlay Info (when cardStyle == "overlay")
            if (cardStyle == LocalStorage.CARD_STYLE_OVERLAY) {
                val subtitle = buildString {
                    if (media.yearString.isNotBlank()) append(media.yearString)
                    val primaryGenre = media.genres?.firstOrNull()?.name
                    if (!primaryGenre.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(primaryGenre.replaceFirstChar { it.uppercase() })
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = media.displayTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (cardStyle != LocalStorage.CARD_STYLE_OVERLAY) {
            Spacer(modifier = Modifier.height(5.dp))

            val showOriginal = LocalShowOriginalTitle.current
            val origTitle = media.originalTitle
            val displayTitleText = if (showOriginal && !origTitle.isNullOrBlank() && !origTitle.equals(media.displayTitle, ignoreCase = true)) {
                "${media.displayTitle} / $origTitle"
            } else {
                media.displayTitle
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                // Title
                Text(
                    text = displayTitleText,
                    style = SlooshTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Year / Genre subtitle
                val subtitle = buildString {
                    if (media.yearString.isNotBlank()) append(media.yearString)
                    val primaryGenre = media.genres?.firstOrNull()?.name
                    if (!primaryGenre.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(primaryGenre.replaceFirstChar { it.uppercase() })
                    }
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = SlooshTypography.labelMedium,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
