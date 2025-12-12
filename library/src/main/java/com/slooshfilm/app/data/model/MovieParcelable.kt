package com.slooshfilm.app.data.model

import android.os.Parcel
import android.os.Parcelable

data class MovieParcelable(
    val url: String,
    val title: String,
    val imageUrl: String,
    val isSeries: Boolean,
    val rating: String?,
    val year: String?,
    val filmName: String?,
    val filmYear: String?,
    val postId: String?,
    val type: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeString(imageUrl)
        parcel.writeByte(if (isSeries) 1 else 0)
        parcel.writeString(rating)
        parcel.writeString(year)
        parcel.writeString(filmName)
        parcel.writeString(filmYear)
        parcel.writeString(postId)
        parcel.writeString(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MovieParcelable> {
        override fun createFromParcel(parcel: Parcel): MovieParcelable {
            return MovieParcelable(parcel)
        }

        override fun newArray(size: Int): Array<MovieParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
