package com.photostudio.pro.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.photostudio.pro.databinding.ItemStickerBinding

class StickerAdapter(
    private val stickers: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerVH>() {

    inner class StickerVH(val binding: ItemStickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerVH {
        val binding = ItemStickerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StickerVH(binding)
    }

    override fun onBindViewHolder(holder: StickerVH, position: Int) {
        val sticker = stickers[position]
        holder.binding.tvStickerEmoji.text = sticker
        holder.binding.root.setOnClickListener { onClick(sticker) }
    }

    override fun getItemCount() = stickers.size
}
