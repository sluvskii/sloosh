package com.slooshfilm.tv.ui.presenters

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.slooshfilm.app.data.model.MovieDetails

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        itemData: Any
    ) {
        val movieDetails = itemData as MovieDetails

        viewHolder.title.text = movieDetails.title
        viewHolder.subtitle.text = ""
        viewHolder.body.text = movieDetails.description
    }
}
