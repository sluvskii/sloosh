package com.slooshfilm.app.data.models.auth

import com.google.gson.annotations.SerializedName
import com.slooshfilm.app.data.api.GenreDto

data class FavoriteDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("mediaId") val mediaId: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("genres") val genres: List<GenreDto>? = null
)

data class PlaybackProgressRecord(
    @SerializedName("mediaId") val mediaId: String,
    @SerializedName("kpId") val kpId: Int = 0,
    @SerializedName("tmdbId") val tmdbId: Int? = null,
    @SerializedName("season") val season: Int? = null,
    @SerializedName("episode") val episode: Int? = null,
    @SerializedName("voiceover") val voiceover: String? = null,
    @SerializedName("positionSec") val positionSec: Double = 0.0,
    @SerializedName("durationSec") val durationSec: Double = 0.0,
    @SerializedName("watched") val watched: Boolean = false,
    @SerializedName("updatedAtMs") val updatedAtMs: Long = System.currentTimeMillis()
)

data class PlaybackMediaMetadata(
    @SerializedName("kpId") val kpId: Int = 0,
    @SerializedName("tmdbId") val tmdbId: Int? = null,
    @SerializedName("detailsId") val detailsId: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("backdropUrl") val backdropUrl: String? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("mediaKey") val mediaKey: String? = null
)
