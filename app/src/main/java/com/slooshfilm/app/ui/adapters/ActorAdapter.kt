package com.slooshfilm.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.Actor

import com.slooshfilm.app.ui.utils.applyScaleAnimation

class ActorAdapter(
    private val onActorClick: (Actor) -> Unit
) : ListAdapter<Actor, ActorAdapter.ActorViewHolder>(ActorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_actor, parent, false)
        return ActorViewHolder(view, onActorClick)
    }

    override fun onBindViewHolder(holder: ActorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActorViewHolder(
        itemView: View,
        private val onActorClick: (Actor) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val actorPhoto: com.google.android.material.imageview.ShapeableImageView = itemView.findViewById(R.id.actor_photo)
        private val actorName: TextView = itemView.findViewById(R.id.actor_name)

        fun bind(actor: Actor) {
            actorName.text = actor.name

            // Загружаем фото актера, если есть
            if (!actor.photo.isNullOrEmpty() && actor.photo != "null") {
                Glide.with(itemView.context)
                    .load(actor.photo)
                    .placeholder(R.drawable.placeholder_movie)
                    .error(R.drawable.placeholder_movie)
                    .circleCrop()
                    .into(actorPhoto)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.placeholder_movie)
                    .circleCrop()
                    .into(actorPhoto)
            }

            itemView.applyScaleAnimation()
            itemView.setOnClickListener {
                onActorClick(actor)
            }
        }
    }

    class ActorDiffCallback : DiffUtil.ItemCallback<Actor>() {
        override fun areItemsTheSame(oldItem: Actor, newItem: Actor): Boolean {
            return oldItem.name == newItem.name && oldItem.link == newItem.link
        }

        override fun areContentsTheSame(oldItem: Actor, newItem: Actor): Boolean {
            return oldItem == newItem
        }
    }
}

