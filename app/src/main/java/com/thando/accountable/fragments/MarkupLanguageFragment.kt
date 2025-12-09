package com.thando.accountable.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.databinding.FragmentMarkupLanguageBinding
import com.thando.accountable.fragments.viewmodels.MarkupLanguageViewModel
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter
import kotlinx.coroutines.currentCoroutineContext

class MarkupLanguageFragment : Fragment() {

    private var _binding: FragmentMarkupLanguageBinding? = null
    private val binding get() = _binding!!
    private val viewModel : MarkupLanguageViewModel by viewModels<MarkupLanguageViewModel> { MarkupLanguageViewModel.Factory }
    private var updateOnPause = true
    private lateinit var cardAdapter : MarkupLanguageCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMarkupLanguageBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        cardAdapter = MarkupLanguageCardAdapter(requireContext())
        binding.spansList.adapter = cardAdapter

        binding.appBarLayout.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (viewModel.scrollRange.value == -1) {
                viewModel.scrollRange.value = barLayout?.totalScrollRange!!
            }
            viewModel.addition.value = if (viewModel.scrollRange.value+verticalOffset>=0)
                viewModel.scrollRange.value+verticalOffset else 0
            if (viewModel.addition.value == 0) {
                viewModel.isShow.value = true
            } else{
                if (viewModel.isShow.value) {
                    viewModel.isShow.value = false
                }
            }
        }

        val mainActivity = (requireActivity() as MainActivity)
        //mainActivity.setSupportActionBar(binding.markupLanguageToolbar)

        collectFlow(this,viewModel.script) { script ->
            if (script!=null) {
                viewModel.loadMarkupLanguage()
            }
        }

        collectFlow(this,viewModel.markupLanguagesList){ markupLanguagesList ->
            if (markupLanguagesList.isEmpty()) return@collectFlow
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, markupLanguagesList)
            binding.markupLanguageSpinner.adapter = adapter

            var initializing = 0
            binding.markupLanguageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?, position: Int, id: Long
                ) {
                    if (initializing == 0){
                        initializing++
                        if (viewModel.script.value?.scriptMarkupLanguage == null){
                            binding.markupLanguageSpinner.setSelection(markupLanguagesList.size-1)
                            viewModel.setSelectedIndex(markupLanguagesList.size-1,cardAdapter.spansNotSimilar())
                            if (markupLanguagesList.size-1==0) initializing++
                        }
                        else{
                            for ((index,language) in markupLanguagesList.withIndex()){
                                if (language.name.value == viewModel.script.value?.scriptMarkupLanguage){
                                    binding.markupLanguageSpinner.setSelection(index)
                                    viewModel.setSelectedIndex(index,cardAdapter.spansNotSimilar())
                                    if (index==0) initializing++
                                    break
                                }
                            }
                        }
                    }
                    else {
                        if (initializing == 1){
                            initializing++
                        }
                        else if (initializing>1){
                            viewModel.setSelectedIndex(position)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }

            setScrollPosition(viewModel.getScrollPosition())
        }

        collectFlow(this,viewModel.selectedIndex){ index->
            viewModel.loadMarkupLanguage(index,cardAdapter.spansNotSimilar())
        }

        collectFlow(this,viewModel.markupLanguage){ markupLanguage ->
            if (markupLanguage!=null && viewModel.markupLanguagesList.value.isNotEmpty()){
                viewModel.setMarkupLanguageFunctions(markupLanguage, requireContext())
                cardAdapter.setMarkupLanguage(markupLanguage)
                collectFlow(currentCoroutineContext(),markupLanguage.opening){
                    viewModel.setOpeningClosingExample(
                        requireContext(), cardAdapter
                    )
                }
                collectFlow(currentCoroutineContext(),markupLanguage.closing){
                    viewModel.setOpeningClosingExample(
                        requireContext(), cardAdapter
                    )
                }
            }
        }

        collectFlow(this,viewModel.showNameNotUniqueSnackBar){ name ->
            Snackbar.make(
                binding.markupLanguageSpinner,
                requireContext().getString(R.string.name_is_not_unique, name),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        collectFlow(this,viewModel.notifySpinnerDataChanged){
            (binding.markupLanguageSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }

        collectFlow(this,viewModel.navigateToScript){ save ->
            updateOnPause = false
            viewModel.closeMarkupLanguageFragment(save,cardAdapter.spansNotSimilar(),requireContext())
        }

        return binding.root
    }

    override fun onResume() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    viewModel.navigateToScript(true)
                }
            }
        )
        super.onResume()
    }

    override fun onPause() {
        viewModel.setScrollPosition(getScrollPosition())
        super.onPause()
    }

    private fun setScrollPosition(position: Int) {
        val layoutManager = binding.spansList.layoutManager as LinearLayoutManager
        binding.spansList.post {
            layoutManager.scrollToPosition(position)
        }
    }

    private fun getScrollPosition(): Int{
        val layoutManager = binding.spansList.layoutManager as LinearLayoutManager
        return layoutManager.findLastCompletelyVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}