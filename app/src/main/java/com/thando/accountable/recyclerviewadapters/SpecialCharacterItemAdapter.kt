package com.thando.accountable.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.databinding.SpecialCharacterItemBinding
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import com.thando.accountable.recyclerviewadapters.diffutils.SpecialCharacterDiffItemCallback
import kotlinx.coroutines.Job

class SpecialCharacterItemAdapter(
    private val viewLifecycleOwner: LifecycleOwner,
    private val viewModel: TeleprompterViewModel
): ListAdapter<SpecialCharacters, SpecialCharacterItemAdapter.SpecialCharacterItemViewHolder>(SpecialCharacterDiffItemCallback()) {

    init {
        collectFlow(viewLifecycleOwner,viewModel.specialCharactersList){ specialCharactersList ->
            submitList(specialCharactersList)
            updateStates()
            viewModel.emitUpdateContentAdapterSpecialCharacters()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SpecialCharacterItemViewHolder {
        return inflateSpecialCharacterItem(parent)
    }

    override fun onBindViewHolder(holder: SpecialCharacterItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onViewRecycled(holder: SpecialCharacterItemViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    fun notifyAddSpecialCharacter(position: Int){
        notifyItemInserted(position)
        updateStates()
    }

    fun delete(specialCharactersInput: SpecialCharacters){
        val position = viewModel.specialCharactersList.value.indexOf(specialCharactersInput)
        viewModel.specialCharactersList.value.remove(specialCharactersInput)
        notifyItemRemoved(position)
        updateStates()
        viewModel.emitUpdateContentAdapterSpecialCharacters()
    }

    private fun updateStates(){
        val charList = arrayListOf<String>()
        viewModel.specialCharactersList.value.forEach { specialCharacter -> charList.add(specialCharacter.character.value) }
        charList.forEachIndexed { index, searchString ->
            if (searchString.isEmpty())  viewModel.specialCharactersList.value[index].setDuplicateIndexes(listOf(),index)
            else{
                viewModel.specialCharactersList.value[index].setDuplicateIndexes(
                    charList.mapIndexed { i, string -> i to string }
                    .filter { it.second == searchString }
                    .map { it.first },
                    index
                )
            }
        }
    }

    private fun inflateSpecialCharacterItem(parent: ViewGroup): SpecialCharacterItemViewHolder {
        val binding = SpecialCharacterItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SpecialCharacterItemViewHolder(binding)
    }

    inner class SpecialCharacterItemViewHolder(private val binding: SpecialCharacterItemBinding): RecyclerView.ViewHolder(binding.root){

        private val jobs = arrayListOf<Job>()

        fun bind(item:SpecialCharacters){
            binding.task = item
            binding.lifecycleOwner = viewLifecycleOwner
            binding.viewLifecycleOwner = viewLifecycleOwner

            jobs.add(collectFlow(viewLifecycleOwner,item.deleteClicked){ deleteClicked->
                if (deleteClicked){
                    viewModel.deleteSpecialCharacter(item)
                    delete(item)
                }
            })
            jobs.add(collectFlow(viewLifecycleOwner,item.character){
                updateStates()
                if (item.canUpdateList()) viewModel.emitUpdateContentAdapterSpecialCharacters()
            })
            jobs.add(collectFlow(viewLifecycleOwner,item.editingAfterChar){
                if (item.canUpdateList()) viewModel.emitUpdateContentAdapterSpecialCharacters()
            })
            jobs.add(collectFlow(viewLifecycleOwner,item.backgroundColour){ colour ->
                binding.background.setCardBackgroundColor(colour)
            })
            jobs.add(collectFlow(viewLifecycleOwner,item.errorMessageButtonVisibility){ visibility ->
                binding.errorMessageTextView.visibility = visibility
            })
            jobs.add(collectFlow(viewLifecycleOwner,item.duplicateErrorMessage){ message ->
                binding.errorMessageTextView.text = message
            })
        }

        fun unbind(){
            jobs.forEach { it.cancel() }
            jobs.clear()
        }
    }
}