package com.thando.accountable.recyclerviewadapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.databinding.MarkupLanguageCardBinding
import com.thando.accountable.recyclerviewadapters.diffutils.MarkupLanguageCardDiffItemCallback
import com.thando.accountable.ui.cards.MarkupLanguageCard


class MarkupLanguageCardAdapter(
    private val context: Context
): ListAdapter<MarkupLanguageCard, MarkupLanguageCardAdapter.MarkupLanguageCardViewHolder>(MarkupLanguageCardDiffItemCallback()) {

    companion object {
        const val EXAMPLE_SPAN = "bi"
        const val EXAMPLE_TEXT = "This is example text."
        const val EXAMPLE_PARAGRAPH = "This\t is the first paragraph.\n\nThis\t is the second paragraph.\n\nThis\t is the third paragraph."
    }

    private var cardsList: MutableList<MarkupLanguageCard> = mutableListOf()

    init {
        submitList(cardsList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkupLanguageCardViewHolder {
        return inflateCard(parent)
    }

    override fun onBindViewHolder(holder: MarkupLanguageCardViewHolder, position: Int) {
        //holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: MarkupLanguageCardViewHolder) {
        //holder.unbind()
        super.onViewRecycled(holder)
    }

    private fun inflateCard(parent: ViewGroup): MarkupLanguageCardViewHolder{
        val binding = MarkupLanguageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarkupLanguageCardViewHolder(binding, parent.findViewTreeLifecycleOwner()!!)
    }

    inner class MarkupLanguageCardViewHolder(
        private val binding: MarkupLanguageCardBinding,
        private val lifecycleOwner: LifecycleOwner
    ): RecyclerView.ViewHolder(binding.root) {

    }
}