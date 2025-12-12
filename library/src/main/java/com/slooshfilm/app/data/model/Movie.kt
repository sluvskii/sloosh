package com.slooshfilm.app.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey
    @SerializedName("url")
    val url: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("is_series")
    val isSeries: Boolean,
    @SerializedName("rating")
    val rating: String?,
    @SerializedName("year")
    val year: String?,
    @SerializedName("film_name")
    val filmName: String? = null,
    @SerializedName("film_year")
    val filmYear: String? = null,
    val postId: String? = null,
    var type: String? = null
) {
    @Ignore
    fun toParcelable(): MovieParcelable {
        return MovieParcelable(
            url = url,
            title = title,
            imageUrl = imageUrl,
            isSeries = isSeries,
            rating = rating,
            year = year,
            filmName = filmName,
            filmYear = filmYear,
            postId = postId,
            type = type
        )
    }
    
    companion object {
        fun fromParcelable(parcelable: MovieParcelable): Movie {
            return Movie(
                url = parcelable.url,
                title = parcelable.title,
                imageUrl = parcelable.imageUrl,
                isSeries = parcelable.isSeries,
                rating = parcelable.rating,
                year = parcelable.year,
                filmName = parcelable.filmName,
                filmYear = parcelable.filmYear,
                postId = parcelable.postId,
                type = parcelable.type
            )
        }
    }
}