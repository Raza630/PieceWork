package com.example.workman.adaptes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.CustomStarRatingBar
import com.example.workman.R
import com.example.workman.dataClass.Category
import com.example.workman.dataClass.Workers
import com.example.workman.databinding.ItemCategoryBinding

class WorkCategoryWiseAdapter(private var categories: List<Category>) :
    RecyclerView.Adapter<WorkCategoryWiseAdapter.CategoryViewHolder>() {

    private val selectedCategories = mutableSetOf<Category>() // Track selected categories
    private var onSelectionChangeListener: ((Set<Category>) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        with(holder.binding) {
            categoryName.text = category.name
            categoryImage.setImageResource(category.imageResId)

            // Highlight selected categories by changing the background or state.
            root.isSelected = selectedCategories.contains(category)

            // Toggle selection on item click.
            root.setOnClickListener {
                toggleCategorySelection(category)
                onSelectionChangeListener?.invoke(selectedCategories)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateList(newCategories: List<Category>) {
        categories = newCategories
        selectedCategories.clear() // Optionally clear selection when list updates.
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(selectedCategories)
    }

    fun setOnSelectionChangeListener(listener: (Set<Category>) -> Unit) {
        onSelectionChangeListener = listener
    }

    private fun toggleCategorySelection(category: Category) {
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category)
        } else {
            selectedCategories.add(category)
        }
        // Notify that this item has changed.
        notifyItemChanged(categories.indexOf(category))
    }

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
}