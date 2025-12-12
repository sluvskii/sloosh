package com.slooshfilm.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R

class SearchSuggestionsAdapter(
    private var suggestions: List<String>,
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestionsAdapter.SuggestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_search_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    fun updateSuggestions(newSuggestions: List<String>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    inner class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.suggestion_text)

        fun bind(suggestion: String) {
            textView.text = suggestion
            itemView.setOnClickListener { onSuggestionClick(suggestion) }
        }
    }
}
