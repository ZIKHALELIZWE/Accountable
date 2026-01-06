package com.thando.accountable.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.database.tables.Folder

class FolderDiffItemCallback: DiffUtil.ItemCallback<Folder>() {
    override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
        return (oldItem.folderId == newItem.folderId)
    }

    override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
        return true//oldItem == newItem
    }
}