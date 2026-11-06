package com.slooshfilm.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.size.Precision
import coil.size.Size
import com.kyant.shapes.RoundedRectangle
import com.slooshfilm.app.data.api.MediaDto
import com.slooshfilm.app.data.storage.LocalStorage
import com.slooshfilm.app.ui.theme.*

val LocalCardStyle = compositionLocalOf { LocalStorage.CARD_STYLE_CLASSIC }
val LocalCardDensity = compositionLocalOf { LocalStorage.CARD_DENSITY_REGULAR }
val LocalPosterQuality = compositionLocalOf { "Высокое" }
val LocalShowOriginalTitle = compositionLocalOf { true }
val LocalTabBarShowsLabels = compositionLocalOf { false }

private val PosterCardShape = RoundedCornerShape(12.dp)
private val RatingBadgeShape = RoundedCornerShape(7.5.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    media: MediaDto,
    onClick: (MediaDto) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: ((MediaDto) -> Unit)? = null,
    width: Int? = null,
    cardStyle: String = LocalCardStyle.current
) {
    val cardModifier = if (width != null) {
        modifier.width(width.dp)
    } else {
        modifier.fillMaxWidth()
    }

    val clickModifier = if (onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick(media) },
            onLongClick = { onLongClick(media) }
        )
    } else {
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClick = { onClick(media) }
        )
    }

    // Pre-computed subtitle string avoiding duplicate allocations across styles
    val subtitle = remember(media.yearString, media.genres) {
        val year = media.yearString
        val genre = media.genres?.firstOrNull()?.name
        val genreCapitalized = if (!genre.isNullOrBlank()) {
            genre.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
        } else null

        when {
            year.isNotBlank() && !genreCapitalized.isNullOrBlank() -> "$year, $genreCapitalized"
            year.isNotBlank() -> year
            !genreCapitalized.isNullOrBlank() -> genreCapitalized
            else -> ""
        }
    }

    Column(
        modifier = cardModifier.then(clickModifier)
    ) {
        // Poster Box (2:3 aspect ratio) with high-efficiency hardware-accelerated rounded rect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(PosterCardShape)
                .background(SurfaceDark)
        ) {
            val posterQuality = LocalPosterQuality.current
            val isLowQuality = posterQuality == "Низкое"
            val posterUrl = remember(media, posterQuality) {
                media.getDisplayPosterUrl(isLowQuality = isLowQuality, quality = posterQuality)
            }

            val context = LocalContext.current
            val imageRequest = remember(posterUrl) {
                if (posterUrl.isNullOrBlank()) null else {
                    ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(false)
                        .build()
                }
            }

            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = media.displayTitle,
                    contentScale = ContentScale.Crop,
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

            // Pre-computed rating string avoiding String.format overhead on scroll
            val rating = media.rating ?: media.ratings?.kp ?: media.ratings?.imdb
            val formattedRating = remember(rating) {
                if (rating != null && rating > 0.0) {
                    val rounded = (rating * 10).toInt()
                    "${rounded / 10}.${rounded % 10}"
                } else null
            }

            // Rating badge (top-start)
            if (formattedRating != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 6.dp)
                        .clip(RatingBadgeShape)
                        .background(ratingColor(rating ?: 0.0))
                        .padding(horizontal = 5.5.dp, vertical = 2.2.dp)
                ) {
                    Text(
                        text = formattedRating,
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
            val displayTitleText = remember(media.displayTitle, media.originalTitle, showOriginal) {
                val origTitle = media.originalTitle
                if (showOriginal && !origTitle.isNullOrBlank() && !origTitle.equals(media.displayTitle, ignoreCase = true)) {
                    "${media.displayTitle} / $origTitle"
                } else {
                    media.displayTitle
                }
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

                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
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
