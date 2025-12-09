package com.thando.accountable.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.ui.cards.MarkupLanguageCard

class MarkupLanguageCardDiffItemCallback: DiffUtil.ItemCallback<MarkupLanguageCard>() {
    override fun areItemsTheSame(oldItem: MarkupLanguageCard, newItem: MarkupLanguageCard): Boolean {
        return (oldItem.markupLanguageName == newItem.markupLanguageName && oldItem.tag.spanName == newItem.tag.spanName)
    }

    override fun areContentsTheSame(oldItem: MarkupLanguageCard, newItem: MarkupLanguageCard): Boolean {
        return oldItem == newItem
    }
}