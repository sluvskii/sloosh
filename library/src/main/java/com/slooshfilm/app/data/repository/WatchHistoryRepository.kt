package com.slooshfilm.app.data.repository

import com.slooshfilm.app.data.MovieDao
import com.slooshfilm.app.data.WatchHistoryDao
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.data.models.WatchHistoryItem
import com.slooshfilm.app.data.models.WatchHistoryWithDetails
import kotlinx.coroutines.flow.Flow

class WatchHistoryRepository(private val watchHistoryDao: WatchHistoryDao, private val movieDao: MovieDao) {

    suspend fun getWatchHistory(movieId: String): WatchHistoryItem? {
        return watchHistoryDao.getByMovieId(movieId)
    }

    suspend fun saveWatchHistory(movieId: String, position: Long, duration: Long) {
        val item = WatchHistoryItem(
            movieId = movieId,
            position = position,
            duration = duration,
            watchedAt = System.currentTimeMillis()
        )
        watchHistoryDao.insert(item)
    }

    fun getWatchHistoryWithDetails(): Flow<List<WatchHistoryWithDetails>> {
        return watchHistoryDao.getWatchHistoryWithDetails()
    }

    suspend fun insertMovie(movie: Movie) {
        movieDao.insertMovie(movie)
    }
}
