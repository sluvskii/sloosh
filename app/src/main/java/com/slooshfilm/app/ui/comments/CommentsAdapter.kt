package com.slooshfilm.app.ui.comments

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.slooshfilm.app.data.hdrezka.Comment
import com.slooshfilm.app.databinding.ItemCommentBinding

class CommentsAdapter(initialComments: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var commentStates: MutableList<CommentItemState> = initialComments.map { CommentItemState(it) }.toMutableList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newComments: List<Comment>) {
        commentStates = newComments.map { CommentItemState(it) }.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding) { position ->
            if (position != RecyclerView.NO_POSITION) {
                commentStates[position].isSpoilerRevealed = true
                notifyItemChanged(position)
            }
        }
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(commentStates[position])
    }

    override fun getItemCount(): Int = commentStates.size

    class CommentViewHolder(
        private val binding: ItemCommentBinding,
        private val onSpoilerClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val initialPaddingStart = binding.root.paddingStart

        init {
            binding.spoilerOverlay.setOnClickListener {
                onSpoilerClick(adapterPosition)
            }
        }

        fun bind(state: CommentItemState) {
            val comment = state.comment
            binding.authorName.text = comment.author
            binding.commentDate.text = comment.date
            binding.commentText.text = comment.text

            val indentSize = (24 * binding.root.resources.displayMetrics.density).toInt()
            val leftPadding = initialPaddingStart + (indentSize * comment.indent)

            binding.root.updatePadding(left = leftPadding)

            if (comment.isSpoiler && !state.isSpoilerRevealed) {
                binding.commentText.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                val radius = binding.commentText.textSize / 3
                val filter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
                binding.commentText.paint.maskFilter = filter
                binding.spoilerOverlay.visibility = View.VISIBLE
            } else {
                binding.commentText.paint.maskFilter = null
                binding.spoilerOverlay.visibility = View.GONE
            }

            if (comment.likes > 0) {
                binding.likesContainer.visibility = View.VISIBLE
                binding.likesCount.text = comment.likes.toString()
            } else {
                binding.likesContainer.visibility = View.GONE
            }

            Glide.with(binding.authorAvatar.context)
                .load(comment.avatar)
                .transform(CircleCrop())
                .into(binding.authorAvatar)
        }
    }
}