package com.slooshfilm.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R

enum class SearchListItemType {
    HISTORY,
    SUGGESTION
}

data class SearchListItem(val text: String, val type: SearchListItemType)

class SearchListAdapter(
    private val onQueryClick: (String) -> Unit,
    private val onDeleteClick: ((String) -> Unit)? = null
) : ListAdapter<SearchListItem, SearchListAdapter.SearchListViewHolder>(SearchDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_search_history, parent, false)
        return SearchListViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val textView: TextView = itemView.findViewById(R.id.query_text)
        private val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)

        fun bind(item: SearchListItem) {
            textView.text = item.text
            itemView.setOnClickListener { onQueryClick(item.text) }

            when (item.type) {
                SearchListItemType.HISTORY -> {
                    icon.setImageResource(R.drawable.ic_history)
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener { onDeleteClick?.invoke(item.text) }
                }
                SearchListItemType.SUGGESTION -> {
                    icon.setImageResource(R.drawable.ic_arrow_right_up)
                    deleteButton.visibility = View.GONE
                }
            }
        }
    }

    class SearchDiffCallback : DiffUtil.ItemCallback<SearchListItem>() {
        override fun areItemsTheSame(oldItem: SearchListItem, newItem: SearchListItem): Boolean {
            return oldItem.text == newItem.text && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SearchListItem, newItem: SearchListItem): Boolean {
            return oldItem == newItem
        }
    }
}
