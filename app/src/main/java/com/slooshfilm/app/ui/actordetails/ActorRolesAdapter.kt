package com.slooshfilm.app.ui.actordetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.data.model.ActorRole
import com.slooshfilm.app.data.model.Movie
import com.slooshfilm.app.ui.adapters.MovieAdapter

class ActorRolesAdapter(
    private val roles: List<ActorRole>,
    private val onMovieClick: (Movie) -> Unit,
    private val onViewAllClick: (ActorRole) -> Unit
) : RecyclerView.Adapter<ActorRolesAdapter.RoleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_actor_role, parent, false)
        return RoleViewHolder(view, onMovieClick, onViewAllClick)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        roles.getOrNull(position)?.let { holder.bind(it) }
    }

    override fun getItemCount(): Int = roles.size

    class RoleViewHolder(
        itemView: View,
        private val onMovieClick: (Movie) -> Unit,
        private val onViewAllClick: (ActorRole) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val roleTitle: TextView = itemView.findViewById(R.id.role_title)
        private val roleInfo: TextView = itemView.findViewById(R.id.role_info)
        private val filmsRecyclerView: RecyclerView = itemView.findViewById(R.id.films_recycler_view)
        private val viewAllButton: TextView = itemView.findViewById(R.id.view_all_button)
        private val gradientLeft: View = itemView.findViewById(R.id.gradient_left)
        private val gradientRight: View = itemView.findViewById(R.id.gradient_right)

        fun bind(role: ActorRole) {
            roleTitle.text = role.role
            roleInfo.text = role.info ?: ""
            roleInfo.visibility = if (role.info.isNullOrEmpty()) View.GONE else View.VISIBLE

            filmsRecyclerView.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            filmsRecyclerView.setHasFixedSize(true)
            filmsRecyclerView.itemAnimator = null

            val adapter = MovieAdapter(3) { movieUrl ->
                role.films.find { it.url == movieUrl }?.let { movie ->
                    onMovieClick(movie)
                }
            }
            adapter.submitList(role.films)

            filmsRecyclerView.adapter = adapter

            viewAllButton.setOnClickListener { onViewAllClick(role) }

            filmsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                    val itemCount = adapter.itemCount

                    gradientLeft.visibility = if (firstVisible > 0) View.VISIBLE else View.GONE
                    gradientRight.visibility = if (lastVisible < itemCount - 1) View.VISIBLE else View.GONE
                }
            })
        }
    }
}