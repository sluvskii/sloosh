package com.slooshfilm.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R

class SearchHistoryAdapter(
    private var history: List<String>,
    private val onQueryClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder>() {

    fun updateHistory(newHistory: List<String>) {
        this.history = newHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_search_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(history[position], onQueryClick, onDeleteClick)
    }

    override fun getItemCount() = history.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val queryText: TextView = itemView.findViewById(R.id.query_text)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(query: String, onQueryClick: (String) -> Unit, onDeleteClick: (String) -> Unit) {
            queryText.text = query
            itemView.setOnClickListener { onQueryClick(query) }
            deleteButton.setOnClickListener { onDeleteClick(query) }
        }
    }
}
