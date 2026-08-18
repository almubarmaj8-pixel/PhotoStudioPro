package com.photostudio.pro.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.photostudio.pro.R
import com.photostudio.pro.databinding.ItemFilterBinding

class FilterAdapter(
    private val filters: List<FilterType>,
    private val sourceBitmap: Bitmap,
    private val onClick: (FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterVH>() {

    private var selectedPos = 0

    inner class FilterVH(val binding: ItemFilterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterVH {
        val binding = ItemFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterVH(binding)
    }

    override fun onBindViewHolder(holder: FilterVH, position: Int) {
        val filter = filters[position]
        holder.binding.tvFilterName.setText(filter.displayNameRes)

        val preview = Bitmap.createScaledBitmap(sourceBitmap, 72, 72, true)
        holder.binding.ivFilterPreview.setImageBitmap(preview)

        holder.binding.cardFilter.strokeWidth = if (position == selectedPos) 3 else 0

        holder.binding.root.setOnClickListener {
            val oldPos = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPos)
            onClick(filter)
        }
    }

    override fun getItemCount() = filters.size
}
