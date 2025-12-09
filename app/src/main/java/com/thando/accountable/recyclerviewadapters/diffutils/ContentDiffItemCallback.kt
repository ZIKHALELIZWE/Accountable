package com.thando.accountable.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.database.tables.Content

class ContentDiffItemCallback: DiffUtil.ItemCallback<Content>() {
    override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
        // A lot of the content Ids are null, the types can also be the same
        return (
                oldItem.id == newItem.id &&
                oldItem.type == newItem.type &&
                oldItem.position == newItem.position &&
                oldItem.content == newItem.content &&
                oldItem.content.value == newItem.content.value
        )
    }

    override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
        return oldItem == newItem
    }
}