package com.slooshfilm.app.ui.adapters

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.MoviePart

import com.slooshfilm.app.ui.utils.applyScaleAnimation

class FranchiseAdapter(
    private var parts: List<MoviePart>,
    private var currentMovieTitle: String,
    private val onPartClick: (String) -> Unit
) : RecyclerView.Adapter<FranchiseAdapter.FranchiseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FranchiseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_franchise_part, parent, false)
        return FranchiseViewHolder(view, onPartClick)
    }

    override fun onBindViewHolder(holder: FranchiseViewHolder, position: Int) {
        parts.getOrNull(position)?.let { holder.bind(it, it.name == currentMovieTitle, position) }
    }

    override fun getItemCount(): Int = parts.size

    fun updateParts(newParts: List<MoviePart>) {
        this.parts = newParts
        notifyDataSetChanged()
    }

    class FranchiseViewHolder(
        itemView: View,
        private val onPartClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val partNumber: TextView = itemView.findViewById(R.id.part_number)
        private val partName: TextView = itemView.findViewById(R.id.part_name)
        private val partInfo: TextView = itemView.findViewById(R.id.part_info)
        private val partRating: TextView = itemView.findViewById(R.id.part_rating)

        fun bind(part: MoviePart, isActive: Boolean, position: Int) {
            partNumber.text = (position + 1).toString()
            partName.text = part.name

            partInfo.text = part.year
            partInfo.isVisible = !part.year.isNullOrEmpty()

            val ratingValue = part.rating?.takeIf { it.isNotEmpty() && it != "0" && it != "0.0" }?.toFloatOrNull()
            if (ratingValue != null) {
                partRating.text = ratingValue.toString()
                partRating.isVisible = true

                val ratingColor = when {
                    ratingValue >= 8 -> R.color.rating_high
                    ratingValue >= 7 -> R.color.rating_medium_high
                    ratingValue >= 6 -> R.color.rating_medium
                    ratingValue >= 5 -> R.color.rating_medium_low
                    else -> R.color.rating_low
                }
                partRating.setTextColor(ContextCompat.getColor(itemView.context, ratingColor))
            } else {
                partRating.isVisible = false
            }

            val context = itemView.context
            val color = if (isActive) {
                com.google.android.material.color.MaterialColors.getColor(context, MaterialR.attr.colorPrimary, 0)
            } else {
                com.google.android.material.color.MaterialColors.getColor(context, MaterialR.attr.colorOnSurface, 0)
            }
            partName.setTextColor(color)
            partNumber.setTextColor(color) // Also color the number

            itemView.applyScaleAnimation()
            itemView.setOnClickListener {
                onPartClick(part.link)
            }
        }
    }
}
