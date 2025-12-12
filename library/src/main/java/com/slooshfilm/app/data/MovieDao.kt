package com.slooshfilm.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.slooshfilm.app.data.model.Movie

@Dao
interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie)

    @Query("SELECT * FROM movies WHERE url = :url")
    suspend fun getMovie(url: String): Movie?

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<Movie>

    @Query("DELETE FROM movies WHERE url = :url")
    suspend fun deleteMovie(url: String)
}
