package com.thando.accountable.recyclerviewadapters.diffutils;

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.database.tables.Script;

class ScriptDiffItemCallback: DiffUtil.ItemCallback<Script>() {
    override fun areItemsTheSame(oldItem: Script, newItem:Script): Boolean {
        return (oldItem.scriptId == newItem.scriptId)
    }

    override fun areContentsTheSame(oldItem: Script, newItem: Script): Boolean {
        return oldItem == newItem
    }
}
