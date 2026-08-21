package com.photostudio.pro.ui.templates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.photostudio.pro.databinding.ItemTemplateBinding

data class Template(
    val name: String,
    val size: String,
    val ratio: String,
    val width: Int,
    val height: Int,
    val isCollage: Boolean = false
)

class TemplateAdapter(
    private val templates: List<Template>,
    private val onClick: (Template) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateVH>() {

    inner class TemplateVH(val binding: ItemTemplateBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateVH {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TemplateVH(binding)
    }

    override fun onBindViewHolder(holder: TemplateVH, position: Int) {
        val template = templates[position]
        holder.binding.tvTemplateName.text = template.name
        holder.binding.tvTemplateSize.text = "${template.size} • ${template.ratio}"
        holder.binding.root.setOnClickListener { onClick(template) }
    }

    override fun getItemCount() = templates.size
}
