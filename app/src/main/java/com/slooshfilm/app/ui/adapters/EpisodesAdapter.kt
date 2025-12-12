package com.slooshfilm.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.Episode

class EpisodesAdapter(
    private val onEpisodeClick: (Episode) -> Unit
) : ListAdapter<Episode, EpisodesAdapter.EpisodeViewHolder>(EpisodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view, onEpisodeClick)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EpisodeViewHolder(
        itemView: View,
        private val onEpisodeClick: (Episode) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val episodeNumber: TextView = itemView.findViewById(R.id.episode_number)
        private val episodeTitle: TextView = itemView.findViewById(R.id.episode_title)

        fun bind(episode: Episode) {
            episodeNumber.text = "${episode.season}x${String.format("%02d", episode.number)}"
            val titleText = episode.title?.takeIf { it.isNotBlank() } ?: itemView.context.getString(R.string.episode_default_title, episode.number)
            episodeTitle.text = titleText

            itemView.setOnClickListener { onEpisodeClick(episode) }
        }
    }

    class EpisodeDiffCallback : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem.season == newItem.season && oldItem.number == newItem.number
        }

        override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
            return oldItem == newItem
        }
    }
}

