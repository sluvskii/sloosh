package com.slooshfilm.app.data

import com.slooshfilm.app.data.hdrezka.Comment
import com.slooshfilm.app.data.model.Movie
import kotlinx.coroutines.flow.Flow

// Assuming Series and Cartoon are type aliases for Movie
typealias Series = Movie
typealias Cartoon = Movie

interface HdRezkaApi {

    fun getMovies(): Flow<List<Movie>>

    fun getMovie(id: String): Flow<Movie>

    fun getSeries(): Flow<List<Series>>

    fun getSeries(id: String): Flow<Series>

    fun getCartoons(): Flow<List<Cartoon>>

    fun getCartoon(id: String): Flow<Cartoon>

    fun getComments(id: String): Flow<List<Comment>>

}
