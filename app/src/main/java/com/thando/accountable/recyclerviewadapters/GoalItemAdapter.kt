package com.thando.accountable.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.databinding.GoalItemBinding
import com.thando.accountable.recyclerviewadapters.diffutils.GoalDiffItemCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class GoalItemAdapter(
    private val setOnLongClick: Boolean,
    private val viewModelLifecycleOwner: LifecycleOwner,
    private val childFragmentManager: FragmentManager,
    private val clickListener: (goalId:Long) -> Unit,
    private val onEditClickListener: (goalId:Long) -> Unit,
    private val onDeleteClickListener: (goalId:Long?) -> Unit
): ListAdapter<Goal, GoalItemAdapter.GoalItemViewHolder>(GoalDiffItemCallback()) {

    private var goalList = mutableListOf<Goal>()
    init {
        submitList(goalList)
    }

    fun setGoalList(newGoalList: List<Goal>){
        val previousSize = goalList.size
        goalList.clear()
        val iterator = newGoalList.listIterator()
        while (iterator.hasNext()) {
            goalList.add(iterator.next())
        }
        val newSize = goalList.size
        if (newSize < previousSize){
            notifyItemRangeChanged(0,newSize)
            notifyItemRangeRemoved(newSize,previousSize-newSize)
        }
        else if ( newSize == previousSize) notifyItemRangeChanged(0,newSize)
        else{
            notifyItemRangeChanged(0,previousSize)
            notifyItemRangeInserted(previousSize,newSize-previousSize)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalItemViewHolder {
        return inflateGoalViewHolder(parent)
    }

    override fun onBindViewHolder(holder: GoalItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onViewRecycled(holder: GoalItemViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = goalList.size

    private fun inflateGoalViewHolder(parent: ViewGroup): GoalItemViewHolder {
        val binding = GoalItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalItemViewHolder(binding)
    }

    inner class GoalItemViewHolder(private val binding: GoalItemBinding): RecyclerView.ViewHolder(binding.root){
        private var contentPreviewAsync: Job? = null

        fun bind(item: Goal){
            binding.task = item
            binding.lifecycleOwner = viewModelLifecycleOwner

            contentPreviewAsync = CoroutineScope(Dispatchers.IO).launch {
                async {

                }.await()
            }

            binding.root.setOnClickListener{ item.id?.let { clickListener(it) }}
            if (setOnLongClick) {
                /*binding.root.setOnLongClickListener {
                    val longClickDialog = FolderAndScriptLongClickDialog()
                    longClickDialog.setDisplayView(this@GoalItemAdapter, item)
                    longClickDialog.setOnEditActionListener {
                        item.id?.let { onEditClickListener(it) }
                        longClickDialog.dismiss()
                    }
                    longClickDialog.setOnDeleteActionListener {
                        onDeleteClickListener(item.id)
                        val position = goalList.indexOf(item)
                        goalList.remove(item)
                        notifyItemRemoved(position)
                        longClickDialog.dismiss()
                    }
                    // Showing the menu
                    longClickDialog.show(childFragmentManager, "Goal Long Click Dialog")
                    true
                }*/
            }
        }

        fun unbind(){
            contentPreviewAsync?.cancel()
        }
    }
}