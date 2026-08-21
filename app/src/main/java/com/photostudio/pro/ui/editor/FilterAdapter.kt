package com.photostudio.pro.ui.editor

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.photostudio.pro.databinding.ItemFilterBinding

/**
 * يعرض الفلاتر مع معاينات مصغّرة مُولَّدة مسبقاً (تعرض الفلتر الفعلي وليس الصورة
 * الأصلية). المعاينات تُولَّد مرة واحدة خارج المحوّل لتجنّب العمل المكلف داخل
 * RecyclerView.
 */
class FilterAdapter(
    private val filters: List<FilterType>,
    private val thumbnails: List<Bitmap>,
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
        holder.binding.ivFilterPreview.setImageBitmap(thumbnails[position])
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

    fun setSelected(position: Int) {
        val old = selectedPos
        selectedPos = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }
}
