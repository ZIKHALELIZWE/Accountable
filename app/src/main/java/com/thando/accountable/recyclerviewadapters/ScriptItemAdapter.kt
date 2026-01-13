package com.thando.accountable.recyclerviewadapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.database.tables.Script
import com.thando.accountable.databinding.ScriptItemBinding
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.recyclerviewadapters.diffutils.ScriptDiffItemCallback
import kotlinx.coroutines.Job

class ScriptItemAdapter(
    private val setOnLongClick: Boolean,
    private val viewModelLifecycleOwner: LifecycleOwner,
    private val childFragmentManager: FragmentManager,
    private val clickListener: (scriptId: Long) -> Unit,
    private val onDeleteClickListener: (scriptId: Long?) -> Unit,
    private val viewModel: BooksViewModel
): ListAdapter<Script, ScriptItemAdapter.ScriptItemViewHolder>(ScriptDiffItemCallback()) {
    private var scriptList = mutableListOf<Script>()
    init {
        submitList(scriptList)
    }

    fun setScriptList(newScriptList: List<Script>){
        val previousSize = scriptList.size
        scriptList.clear()
        newScriptList.forEach { scriptList.add(it) }
        val newSize = scriptList.size
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScriptItemViewHolder {
        return inflateScriptItem(parent)
    }

    override fun onBindViewHolder(holder: ScriptItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ScriptItemViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    override fun getItemCount() = scriptList.size

    private fun inflateScriptItem(parent: ViewGroup): ScriptItemViewHolder {
        val binding = ScriptItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScriptItemViewHolder(binding)
    }

    inner class ScriptItemViewHolder(private val binding: ScriptItemBinding): RecyclerView.ViewHolder(binding.root) {

        private var contentPreviewAsync: Job? = null
        //private var longClickDialog: FolderAndScriptLongClickDialog? = null

        fun bind(item: Script) {
            binding.task = item
            binding.lifecycleOwner = viewModelLifecycleOwner
            item.scriptId?.let { scriptId ->
                binding.contentPreview = viewModel.getScriptContentPreview(scriptId)
                contentPreviewAsync = binding.contentPreview?.init{
                    contentPreviewAsync = null
                }
            }
            binding.root.setOnClickListener{ item.scriptId?.let { it1 -> clickListener(it1) } }
            if (setOnLongClick) {
                /*binding.root.setOnLongClickListener {
                    longClickDialog = FolderAndScriptLongClickDialog()
                    longClickDialog!!.setDisplayView(this@ScriptItemAdapter, item)
                    longClickDialog!!.setOnDeleteActionListener {
                        onDeleteClickListener(item.scriptId)
                        val position = scriptList.indexOf(item)
                        scriptList.remove(item)
                        notifyItemRemoved(position)
                        longClickDialog?.dismiss()
                    }
                    // Showing the menu
                    longClickDialog!!.show(childFragmentManager, "Script Long Click Dialog")
                    true
                }*/
            }
        }

        fun unbind(){
            contentPreviewAsync?.cancel()
        }
    }
}