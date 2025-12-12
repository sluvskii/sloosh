package com.slooshfilm.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.ui.utils.applyScaleAnimation

class MovieAdapter(
    private val gridColumns: Int,
    private val onMovieClick: (String) -> Unit
) : ListAdapter<Movie, MovieAdapter.MovieViewHolder>(MovieDiffCallback()) {

    private val VIEW_TYPE_MOVIE = 0
    private val VIEW_TYPE_LOADING = 1

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.url == "LOADING") VIEW_TYPE_LOADING else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        var isHorizontal = false
        try {
            if (parent is RecyclerView) {
                val layoutManager = parent.layoutManager
                if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
                    isHorizontal = layoutManager.orientation == RecyclerView.HORIZONTAL
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        val layoutRes = if (viewType == VIEW_TYPE_LOADING) {
            R.layout.list_item_loading
        } else if (isHorizontal) {
            R.layout.list_item_movie_horizontal
        } else if (gridColumns == 3) {
            R.layout.list_item_movie_grid_3_cols
        } else {
            R.layout.list_item_movie_grid
        }

        val view = layoutInflater.inflate(layoutRes, parent, false)
        return MovieViewHolder(view, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_MOVIE) {
            holder.bind(getItem(position))
        }
    }

    class MovieViewHolder(itemView: View, private val onMovieClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView? = itemView.findViewById(R.id.movie_poster)
        private val title: TextView? = itemView.findViewById(R.id.movie_title)
        private val year: TextView? = itemView.findViewById(R.id.movie_year)
        private val ratingContainer: View? = itemView.findViewById(R.id.rating_container)
        private val rating: TextView? = itemView.findViewById(R.id.movie_rating)

        fun bind(movie: Movie) {
            // Extract and set title
            val titleText = movie.title.substringBefore("(").trim()
            title?.text = titleText

            // Extract and set year
            year?.visibility = if (!movie.year.isNullOrEmpty()) View.VISIBLE else View.GONE
            if (!movie.year.isNullOrEmpty()) {
                if (movie.isSeries) {
                    val yearRegex = "\\((\\d{4})\\s*–\\s*(\\d{4}|\\.\\.\\.)\\)".toRegex()
                    val matchResult = yearRegex.find(movie.title)
                    if (matchResult != null) {
                        val startYear = matchResult.groupValues[1]
                        val endYear = matchResult.groupValues[2]
                        year?.text = "$startYear - $endYear"
                    } else {
                        year?.text = movie.year
                    }
                } else {
                    year?.text = movie.year
                }
            }

            poster?.let {
                Glide.with(itemView.context)
                    .load(movie.imageUrl)
                    .thumbnail(0.25f)
                    .dontAnimate()
                    .placeholder(R.drawable.placeholder_movie)
                    .into(it)
            }

            if (movie.rating != null && ratingContainer != null && rating != null) {
                ratingContainer.visibility = View.VISIBLE
                rating.text = movie.rating
            } else {
                ratingContainer?.visibility = View.GONE
            }

            if (movie.url != "LOADING") { 
                itemView.applyScaleAnimation()
                itemView.setOnClickListener { onMovieClick(movie.url) }
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}
