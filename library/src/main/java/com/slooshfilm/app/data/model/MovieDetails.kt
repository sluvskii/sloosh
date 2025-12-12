package com.slooshfilm.app.data.model

data class Translator(
    val name: String,
    val id: String? = null,
    val streams: Map<String, String> = emptyMap() // "1080p" -> "https://...m3u8"
)

enum class EpisodeStatus {
    RELEASED,    // Вышла (зелёная или серая)
    CURRENT,     // Выходит сейчас (красная, пульсирующая)
    UPCOMING     // Ожидается (серая, тусклая)
}

data class Episode(
    val season: Int,
    val number: Int,
    val title: String?,
    val url: String,
    val releaseDate: String? = null,      // "15 января 2024" или "2024-01-15"
    val status: EpisodeStatus = EpisodeStatus.UPCOMING
)

data class MoviePart(
    val name: String,
    val year: String? = null,
    val rating: String? = null,
    val link: String
)

data class MovieDetails(
    val postId: String? = null,
    val title: String,
    val description: String,
    val posterUrl: String,
    val isSerial: Boolean = false,
    // The original page URL used to load these details (useful as Referer for AJAX calls)
    val pageUrl: String? = null,
    val translators: Map<String, Translator> = emptyMap(),
    val selectedTranslatorId: String? = null,
    val trailerDataId: String? = null,
    var trailerUrl: String? = null,
    val seasons: Map<Int, List<Episode>> = emptyMap(),
    val ratingImdb: String? = null,
    val ratingImdbVotes: String? = null,
    val ratingKinopoisk: String? = null,
    val ratingKinopoiskVotes: String? = null,
    val genres: List<String>? = null,
    val year: String? = null,
    val country: String? = null,
    val director: String? = null,
    val directors: List<Actor> = emptyList(),
    val actors: List<String>? = null,
    val actorsWithPhotos: List<Actor> = emptyList(),
    val ageLimit: String? = null,
    val duration: String? = null,
    val releaseDate: String? = null,
    val franchise: List<MoviePart> = emptyList(),
    val defaultStreams: Map<String, String> = emptyMap()
)
