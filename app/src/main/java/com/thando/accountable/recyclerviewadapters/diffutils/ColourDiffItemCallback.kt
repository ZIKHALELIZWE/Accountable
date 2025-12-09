package com.thando.accountable.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.ui.cards.Colour

class ColourDiffItemCallback : DiffUtil.ItemCallback<Colour>() {
    override fun areItemsTheSame(oldItem: Colour, newItem: Colour): Boolean {
        return (oldItem.colour == newItem.colour)
    }

    override fun areContentsTheSame(oldItem: Colour, newItem: Colour): Boolean {
        return oldItem == newItem
    }
}