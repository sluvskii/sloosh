package com.slooshfilm.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.databinding.ItemFilmGridBinding

class FilmAdapter(
    private var films: List<Movie>,
    private val onFilmClick: (Movie) -> Unit
) : RecyclerView.Adapter<FilmAdapter.FilmViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmViewHolder {
        val binding = ItemFilmGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilmViewHolder, position: Int) {
        holder.bind(films[position])
    }

    override fun getItemCount(): Int = films.size

    fun updateData(newFilms: List<Movie>) {
        films = newFilms
        notifyDataSetChanged()
    }

    inner class FilmViewHolder(private val binding: ItemFilmGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(film: Movie) {
            binding.filmPoster.load(film.imageUrl) {
                crossfade(true)
            }
            binding.filmTitle.text = film.filmName ?: film.title
            binding.filmYear.text = film.filmYear ?: film.year
            binding.root.setOnClickListener { onFilmClick(film) }
        }
    }
}