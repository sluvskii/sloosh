package com.slooshfilm.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey
    val movieId: String,
    val position: Long,
    val duration: Long,
    val watchedAt: Long
)
