package com.thando.accountable.recyclerviewadapters.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.thando.accountable.database.tables.SpecialCharacters

class SpecialCharacterDiffItemCallback: DiffUtil.ItemCallback<SpecialCharacters>() {
    override fun areItemsTheSame(
        oldItem: SpecialCharacters,
        newItem: SpecialCharacters
    ): Boolean {
        return oldItem.teleprompterSettingsId == newItem.teleprompterSettingsId &&
                oldItem.character.text.toString() == newItem.character.text.toString() &&
                oldItem.oldCharacter == newItem.oldCharacter
    }

    override fun areContentsTheSame(
        oldItem: SpecialCharacters,
        newItem: SpecialCharacters
    ): Boolean {
        return oldItem.editingAfterChar.text.toString() == newItem.editingAfterChar.text.toString()
    }
}