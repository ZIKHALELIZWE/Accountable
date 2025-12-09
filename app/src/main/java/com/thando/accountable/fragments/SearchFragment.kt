package com.thando.accountable.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.IntentActivity
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.databinding.FragmentSearchBinding
import com.thando.accountable.databinding.ScriptSearchItemBinding
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import kotlinx.coroutines.Job

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding
    private val viewModel: SearchViewModel by viewModels { SearchViewModel.Factory }
    private lateinit var scriptSearchAdapter: ScriptSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding?.viewModel = viewModel
        binding?.lifecycleOwner = viewLifecycleOwner

        scriptSearchAdapter = ScriptSearchAdapter()
        binding?.searchRecyclerView?.adapter = scriptSearchAdapter

        collectFlow(viewLifecycleOwner,viewModel.searchString){
            search()
        }

        collectFlow(viewLifecycleOwner,viewModel.matchCaseCheck){
            search()
        }

        collectFlow(viewLifecycleOwner,viewModel.wordCheck){
            search()
        }

        collectFlow(this,viewModel.openScript){ scriptId ->
            viewModel.loadAndOpenScript(scriptId,activity)
        }

        return binding?.root
    }

    private fun search(){
        viewModel.search {
            val scrollPosition = viewModel.getScrollPosition()
            if (scrollPosition!=0){
                setScrollPosition(scrollPosition)
                viewModel.setScrollPosition(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.intentString==null) {
            val mainActivity = (requireActivity() as MainActivity)
            mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        viewModel.navigateToFoldersAndScripts()
                    }
                }
            )
        }
        else{
            val intentActivity = (requireActivity() as IntentActivity)
            intentActivity.dialogFragment.dialog?.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    viewModel.navigateToFoldersAndScripts()
                    true
                }
                else false
            }
        }
    }

    override fun onPause() {
        viewModel.setScrollPosition(getScrollPosition())
        super.onPause()
    }

    private fun setScrollPosition(position: Int) {
        binding?.searchRecyclerView?.layoutManager?.let {
            val layoutManager = it as LinearLayoutManager
            binding?.searchRecyclerView?.post {
                layoutManager.scrollToPosition(position)
            }
        }
    }

    private fun getScrollPosition(): Int{
        val layoutManager = binding?.searchRecyclerView?.layoutManager as LinearLayoutManager
        return layoutManager.findLastCompletelyVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ScriptSearchDiffItemCallback: DiffUtil.ItemCallback<SearchViewModel.ScriptSearch>() {
        override fun areItemsTheSame(oldItem: SearchViewModel.ScriptSearch, newItem: SearchViewModel.ScriptSearch
        ): Boolean {
            return (oldItem.folderId == newItem.folderId && oldItem.script.scriptId == newItem.script.scriptId)
        }

        override fun areContentsTheSame(oldItem: SearchViewModel.ScriptSearch, newItem: SearchViewModel.ScriptSearch
        ): Boolean { return oldItem == newItem }
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class ScriptSearchAdapter
        :ListAdapter<SearchViewModel.ScriptSearch, ScriptSearchAdapter.ScriptSearchViewHolder>(
        ScriptSearchDiffItemCallback()
    )
    {

        init {
            submitList(viewModel.scriptsList)
            collectFlow(viewLifecycleOwner,viewModel.notifyListCleared){
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptSearchViewHolder {
            return inflateScriptSearchItem(parent)
        }

        override fun onBindViewHolder(holder: ScriptSearchViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: ScriptSearchViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        override fun getItemCount() = viewModel.scriptsList.size

        private fun inflateScriptSearchItem(parent: ViewGroup): ScriptSearchViewHolder {
            val binding = ScriptSearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ScriptSearchViewHolder(binding)
        }

        inner class ScriptSearchViewHolder(private val binding:ScriptSearchItemBinding): RecyclerView.ViewHolder(binding.root){

            private var contentPreviewAsync: Job? = null

            fun bind(item: SearchViewModel.ScriptSearch) {
                binding.scriptSearch = item
                binding.script = item.script
                binding.lifecycleOwner = viewLifecycleOwner

                item.script.scriptId?.let { scriptId ->
                    binding.contentPreview = viewModel.getScriptContentPreview(scriptId)
                    contentPreviewAsync = binding.contentPreview?.init{
                        contentPreviewAsync = null
                    }
                }
            }

            fun unbind(){
                contentPreviewAsync?.cancel()
            }
        }
    }
}