package com.slooshfilm.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.slooshfilm.app.data.models.WatchHistoryItem
import com.slooshfilm.app.data.models.WatchHistoryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchHistoryItem)

    @Query("SELECT * FROM watch_history WHERE movieId = :movieId")
    suspend fun getByMovieId(movieId: String): WatchHistoryItem?

    @Transaction
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getWatchHistoryWithDetails(): Flow<List<WatchHistoryWithDetails>>
}
