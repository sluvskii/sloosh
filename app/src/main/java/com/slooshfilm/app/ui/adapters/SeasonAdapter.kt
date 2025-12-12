package com.slooshfilm.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.Episode
import com.slooshfilm.app.data.model.EpisodeStatus
import com.slooshfilm.app.databinding.ItemSeasonHeaderBinding
import com.slooshfilm.app.databinding.ItemEpisodeBinding

data class SeasonWithEpisodes(
    val seasonNumber: Int,
    val episodes: List<Episode>,
    var isExpanded: Boolean = false
)

sealed class ScheduleItem {
    data class SeasonHeader(val season: SeasonWithEpisodes) : ScheduleItem()
    data class EpisodeItem(val episode: Episode) : ScheduleItem()
}

class SeasonAdapter(
    private val seasons: List<SeasonWithEpisodes>,
    private var filteredStatus: EpisodeStatus? = null,
    private val onEpisodeClick: (Episode) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<ScheduleItem> = mutableListOf()

    init {
        rebuildItems()
    }

    private fun rebuildItems() {
        items.clear()
        for (season in seasons) {
            items.add(ScheduleItem.SeasonHeader(season))
            if (season.isExpanded) {
                val episodesToShow = if (filteredStatus == null) {
                    season.episodes
                } else {
                    season.episodes.filter { it.status == filteredStatus }
                }
                items.addAll(episodesToShow.map { ScheduleItem.EpisodeItem(it) })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SEASON_HEADER -> {
                val binding = ItemSeasonHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SeasonViewHolder(binding)
            }
            VIEW_TYPE_EPISODE -> {
                val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EpisodeViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SeasonViewHolder -> {
                val seasonHeader = items[position] as ScheduleItem.SeasonHeader
                holder.bind(seasonHeader.season)
            }
            is EpisodeViewHolder -> {
                val episodeItem = items[position] as ScheduleItem.EpisodeItem
                holder.bind(episodeItem.episode)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ScheduleItem.SeasonHeader -> VIEW_TYPE_SEASON_HEADER
            is ScheduleItem.EpisodeItem -> VIEW_TYPE_EPISODE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateStatusFilter(status: EpisodeStatus?) {
        filteredStatus = status
        rebuildItems()
        notifyDataSetChanged()
    }

    private inner class SeasonViewHolder(private val binding: ItemSeasonHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(season: SeasonWithEpisodes) {
            binding.seasonTitle.text = "Сезон ${season.seasonNumber}"

            val displayCount = if (filteredStatus == null) {
                season.episodes.size
            } else {
                season.episodes.count { it.status == filteredStatus }
            }
            binding.episodeCount.text = "$displayCount эпизодов"

            updateExpandIcon(season.isExpanded)

            binding.root.setOnClickListener {
                season.isExpanded = !season.isExpanded
                animateExpandIcon(binding, season.isExpanded)
                rebuildItems()
                notifyDataSetChanged()
            }
        }

        private fun updateExpandIcon(isExpanded: Boolean) {
            val rotation = if (isExpanded) 180f else 0f
            binding.expandIcon.rotation = rotation
        }

        private fun animateExpandIcon(binding: ItemSeasonHeaderBinding, isExpanded: Boolean) {
            val fromRotation = if (isExpanded) 0f else 180f
            val toRotation = if (isExpanded) 180f else 0f

            RotateAnimation(fromRotation, toRotation, binding.expandIcon.width / 2f, binding.expandIcon.height / 2f).apply {
                duration = 300
                fillAfter = true
                binding.expandIcon.startAnimation(this)
            }
        }
    }

    private inner class EpisodeViewHolder(private val binding: ItemEpisodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Episode) {
            // Show episode number
            binding.episodeNumber.text = "${episode.season}x${String.format("%02d", episode.number)}"
            
            // Show the complete episode title which contains all info:
            // number, title, date, status - exactly as it appears on the website
            binding.episodeTitle.text = episode.title ?: "Без названия"

            binding.root.setOnClickListener {
                onEpisodeClick(episode)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_SEASON_HEADER = 0
        private const val VIEW_TYPE_EPISODE = 1
    }
}
