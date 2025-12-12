package com.slooshfilm.app.data.models

import androidx.room.Embedded
import androidx.room.Relation
import com.slooshfilm.app.data.model.Movie

data class WatchHistoryWithDetails(
    @Embedded
    val watchHistory: WatchHistoryItem,

    @Relation(
        parentColumn = "movieId",
        entityColumn = "url"
    )
    val movie: Movie? // Movie can be null if it's not in the DB for some reason
)
