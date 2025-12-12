
package com.slooshfilm.app.data.parser

import kotlinx.serialization.Serializable

@Serializable
data class ParsingConfig(
    val movieDetailsSelectors: MovieDetailsSelectors
)

@Serializable
data class MovieDetailsSelectors(
    val titleSelector: String,
    val descriptionSelector: String,
    val posterUrlSelector: String,
    val infoTableSelector: String,
    val imdbRatingSelector: String,
    val imdbVotesSelector: String,
    val kpRatingSelector: String,
    val kpVotesSelector: String,
    val yearSelector: String,
    val countrySelector: String,
    val directorSelector: String,
    val genreSelector: String,
    val actorsSelector: String,
    val durationSelector: String,
    val releaseDateSelector: String,
    val ageLimitSelector: String,
    val postIdSelector: String,
    val isSerialSelector: String,
    val trailerDataIdSelector: String,
    val translatorsSelector: String
)
