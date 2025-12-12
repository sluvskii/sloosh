package com.slooshfilm.tv.ui.presenters

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.slooshfilm.app.domain.MovieItem
import com.slooshfilm.tv.R

class CardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(313, 176)
            cardType = BaseCardView.CARD_TYPE_INFO_UNDER_WITH_EXTRA
            infoVisibility = BaseCardView.CARD_REGION_VISIBLE_ALWAYS
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as MovieItem
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = movie.name
        cardView.contentText = movie.type

        Glide.with(viewHolder.view.context)
            .load(movie.poster)
            .centerCrop()
            .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }
}