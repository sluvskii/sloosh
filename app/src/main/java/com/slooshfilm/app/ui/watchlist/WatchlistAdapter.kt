package com.slooshfilm.app.ui.watchlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.databinding.ListItemMovieBinding

import com.slooshfilm.app.ui.utils.applyScaleAnimation

class WatchlistAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (String) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ListItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    fun updateMovies(newMovies: List<Movie>) {
        this.movies = newMovies
        notifyDataSetChanged()
    }

    inner class MovieViewHolder(private val binding: ListItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.movieTitle.text = movie.title
            binding.movieYear.text = movie.year
            Glide.with(binding.root.context)
                .load(movie.imageUrl)
                .into(binding.moviePoster)

            if (movie.rating != null) {
                binding.movieRating.text = movie.rating
                binding.ratingContainer.visibility = View.VISIBLE
            } else {
                binding.ratingContainer.visibility = View.GONE
            }

            binding.root.applyScaleAnimation()
            binding.root.setOnClickListener {
                onMovieClick(movie.url)
            }
        }
    }
}
