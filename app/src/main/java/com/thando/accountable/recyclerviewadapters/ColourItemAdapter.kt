package com.thando.accountable.recyclerviewadapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.databinding.ColourItemBinding
import com.thando.accountable.recyclerviewadapters.diffutils.ColourDiffItemCallback
import com.thando.accountable.ui.cards.Colour

class ColourItemAdapter(
    private val context: Context,
    private val onItemClick: (Int) -> Unit
): ListAdapter<Colour, ColourItemAdapter.ColourItemViewHolder>(ColourDiffItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourItemViewHolder {
        return ColourItemViewHolder.inflateFrom(parent)
    }

    override fun onBindViewHolder(holder: ColourItemViewHolder, position: Int) {
        holder.bind(getItem(position), context, onItemClick)
    }

    override fun onViewRecycled(holder: ColourItemViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    class ColourItemViewHolder(private val binding: ColourItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object{
            fun inflateFrom(parent: ViewGroup): ColourItemViewHolder {
                val binding = ColourItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ColourItemViewHolder(binding)
            }
        }

        fun bind(item: Colour, context: Context, onItemClick: (Int) -> Unit) {
            binding.task = item
            binding.task?.setDrawable(context)
            binding.root.setOnClickListener { onItemClick(item.colour) }
        }

        fun unbind() {
            binding.task?.removeDrawable()
        }
    }
}