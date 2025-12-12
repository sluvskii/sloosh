package com.slooshfilm.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.slooshfilm.app.R
import com.slooshfilm.app.data.repository.ContentCategory

class CategoryAdapter(
    private val categories: List<ContentCategory>,
    private val currentCategory: ContentCategory,
    private val onCategoryClick: (ContentCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view, onCategoryClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val isSelected = category == currentCategory
        holder.bind(category, isSelected)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(
        itemView: View,
        private val onCategoryClick: (ContentCategory) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.category_name)

        fun bind(category: ContentCategory, isSelected: Boolean) {
            categoryName.text = category.ru
            
            val colorRes = if (isSelected) R.color.brand_yellow_green else R.color.text_primary
            categoryName.setTextColor(ContextCompat.getColor(itemView.context, colorRes))

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}