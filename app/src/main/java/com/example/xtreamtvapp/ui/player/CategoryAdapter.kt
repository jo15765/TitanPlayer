package com.example.xtreamtvapp.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R
import com.example.xtreamtvapp.data.LiveCategory

class CategoryAdapter(
    private var categories: List<LiveCategory>,
    private var selectedCategoryId: String?,
    private val onCategoryClick: (LiveCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    fun setCategories(newCategories: List<LiveCategory>, newSelectedId: String?) {
        categories = newCategories
        selectedCategoryId = newSelectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position], categories[position].categoryId == selectedCategoryId)
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)

        fun bind(category: LiveCategory, selected: Boolean) {
            categoryName.text = category.categoryName
            itemView.setBackgroundResource(
                if (selected) R.color.channel_selected else android.R.color.transparent
            )
            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }
}
