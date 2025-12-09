package com.thando.accountable.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import com.thando.accountable.ui.management.states.toolbar.ToolbarState
import com.thando.accountable.ui.screens.MenuItemData
import com.thando.accountable.ui.screens.ScriptFragmentCatalog
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScriptFragment : Fragment() {

    private lateinit var contentAdapter: ContentItemAdapter

    val viewModel : ScriptViewModel by viewModels { ScriptViewModel.Factory }
    private val galleryLauncherMultiple = registerForActivityResult(ActivityResultContracts.GetMultipleContents()){ list -> contentAdapter.multipleContentsCallback(list) }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { galleryUri ->
        try{
            if (galleryUri!=null){
                when (viewModel.chooseContent.value) {
                    AppResources.ContentType.IMAGE -> {
                        viewModel.contentRetrieved()
                        viewModel.setTopImage(galleryUri)
                    }
                    AppResources.ContentType.DOCUMENT -> {
                        viewModel.contentRetrieved()
                        contentAdapter.appendFile(galleryUri)
                    }
                    else -> {
                        viewModel.contentRetrieved()
                    }
                }
            }
        }catch(e:Exception){
            e.printStackTrace()
        }
        viewModel.contentRetrieved()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.viewModel.toolbarVisible.value = false

        return ComposeView(requireContext()).apply {
            WindowCompat.setDecorFitsSystemWindows(mainActivity.window, false)
            setContent {
                mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            viewModel.onBackPressed()
                        }
                    }
                )
                AccountableTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        ScriptFragmentView(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }

        /*contentAdapter = viewModel.getContentAdapter(
            requireContext(),
            viewLifecycleOwner,
            childFragmentManager,
            galleryLauncherMultiple
        ) {
            val layoutManager = binding.scriptRecyclerView.layoutManager as LinearLayoutManager
            binding.scriptRecyclerView.post {
                layoutManager.scrollToPosition((binding.scriptRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
            }
        }
        binding.scriptRecyclerView.adapter = contentAdapter

        var isShow = true
        var scrollRange = -1
        binding.appBarLayout.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (scrollRange + verticalOffset == 0) {
                binding.scriptCollapsingToolbar.title = requireContext().getString(R.string.script)
                isShow = true
                binding.scriptHeadingCardView.alpha= 0.0f
            } else{
                if (isShow) {
                    binding.scriptCollapsingToolbar.title = " " //careful there should a space between double quote otherwise it wont work
                    isShow = false
                }

                // todo app always crashes because of it updating when back is pressed
                val alpha = 1.0f - abs(verticalOffset / binding.appBarLayout.getTotalScrollRange()
                    .toFloat())
                binding.scriptHeadingCardView.alpha = alpha
                binding.toolbarScriptTimeContainer.alpha = alpha
                binding.toolbarScriptDateContainer.alpha = alpha
            }
        }

        val mainActivity = (requireActivity() as MainActivity)
        //mainActivity.setSupportActionBar(binding.scriptToolbar)

        val menuHost: MenuHost = mainActivity
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(inputMenu: Menu, menuInflater: MenuInflater) {
                menu = inputMenu
                menuInflater.inflate( R.menu.script_menu, menu)

                collectFlow(this@ScriptFragment,viewModel.addTimeStampFunction) { function ->
                    if(function!=null) showOption(menu,R.id.script_menu_add_time_stamp)
                    else hideOption(menu,R.id.script_menu_add_time_stamp)
                }
                collectFlow(this@ScriptFragment,viewModel.menuAddTimeStampTitle) { menuAddTimeStampTitle ->
                    val addTimeStampItem = menu?.findItem(R.id.script_menu_add_time_stamp)
                    addTimeStampItem?.title = menuAddTimeStampTitle
                }
                collectFlow(this@ScriptFragment,viewModel.menuEditTitle) { menuEditTitle ->
                    val editTitleItem = menu?.findItem(R.id.script_menu_edit_save_script)
                    editTitleItem?.title = menuEditTitle
                }
                collectFlow(this@ScriptFragment,viewModel.menuImageTitle) { menuImageTitle ->
                    val imageTitleItem = menu?.findItem(R.id.script_menu_image_item)
                    imageTitleItem?.title = menuImageTitle
                }
                collectFlow(this@ScriptFragment,viewModel.markupLanguage) { markupLanguage ->
                    val markupLanguageTitleItem = menu?.findItem(R.id.script_menu_markup_language)
                    if (markupLanguage==null || markupLanguage.name.value.isEmpty()) {
                        markupLanguageTitleItem?.title = requireContext().getString(R.string.choose_markup_language)
                    }
                    else markupLanguageTitleItem?.title = requireContext().getString(R.string.change_markup_language) + " " + markupLanguage.name.value
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.script_menu_add_time_stamp -> {
                        viewModel.addTimeStamp(requireContext())
                    }
                    R.id.script_menu_open_teleprompter -> {
                        viewModel.openTeleprompter()
                    }
                    R.id.script_menu_image_item -> {
                        viewModel.chooseTopImage()
                    }
                    R.id.script_menu_edit_save_script -> {
                        viewModel.editOrSaveScript()
                    }
                    R.id.script_menu_add_document -> {
                        viewModel.loadText()
                    }
                    R.id.script_menu_markup_language -> {
                        viewModel.chooseMarkupLanguage()
                    }
                    R.id.script_menu_print_to_text_file -> {
                        viewModel.printToTextFile()
                    }
                    R.id.script_menu_share_script -> {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val intent = Intent()

                                if (viewModel.script.value?.scriptTitle?.value != null
                                    && !viewModel.script.value?.scriptTitle?.value.isNullOrEmpty()
                                ) {
                                    intent.putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        viewModel.script.value!!.scriptTitle.value
                                    )
                                }

                                val (hasContent, imageUris, sharedText) = viewModel.getShareContent(requireContext())
                                if (imageUris.isNotEmpty()) {
                                    intent.apply {
                                        action = Intent.ACTION_SEND_MULTIPLE
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                                        type = "image/jpg"
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                } else if (sharedText.isNotEmpty()) {
                                    intent.apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, sharedText)
                                        type = "text/plain"
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    if (hasContent) startActivity(
                                        Intent.createChooser(intent, null)
                                    )
                                    else Toast.makeText(
                                        context,
                                        "Nothing To Share",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        }
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        collectFlow(this,viewModel.isEditingScript){ isEditingScript ->
            if (isEditingScript){
                disableOption(menu, R.id.script_menu_open_teleprompter)
                viewModel.editScript(requireContext())
            }
            else{
                enableOption(menu, R.id.script_menu_open_teleprompter)
                viewModel.previewScript(requireContext())
            }
        }

        collectFlow(this,viewModel.script){ script ->
            if (script!=null){
                viewModel.setAddTimeStampTitle(
                    if (script.scriptDateTime.getFullDateStateFlow(requireContext()).value
                        ==
                        AppResources.CalendarResource(Calendar.getInstance()).getFullDateStateFlow(requireContext()).value
                    ) {
                        getString(R.string.add_time_stamp)
                    } else {
                        getString(R.string.add_date_stamp)
                    }
                )
                menu?.findItem(R.id.script_menu_image_item)?.setEnabled(true)
                menu?.findItem(R.id.script_menu_edit_save_script)?.setEnabled(true)
                menu?.findItem(R.id.script_menu_add_document)?.setEnabled(true)
                menu?.findItem(R.id.script_menu_markup_language)?.setEnabled(true)
            }
            else{
                menu?.findItem(R.id.script_menu_image_item)?.setEnabled(false)
                menu?.findItem(R.id.script_menu_edit_save_script)?.setEnabled(false)
                menu?.findItem(R.id.script_menu_add_document)?.setEnabled(false)
                menu?.findItem(R.id.script_menu_markup_language)?.setEnabled(false)
            }
        }

        collectFlow(this,viewModel.chooseMarkupLanguage){ navigate ->
            if (navigate) {
                viewModel.saveScriptAndOpenMarkupLanguage()
            }
        }

        collectFlow(this,viewModel.chooseContent) { contentType ->
            if (contentType != null) {
                if (contentType == AppResources.ContentType.IMAGE){
                    galleryLauncher.launch(AppResources.ContentTypeAccessor[contentType])
                }
                else if (contentType == AppResources.ContentType.DOCUMENT){
                    viewModel.setScriptLoadedToFalse()
                    galleryLauncher.launch(AppResources.ContentTypeAccessor[contentType])
                }
            }
        }

        collectFlow(this,viewModel.printEntryToTextFile) { pressed ->
            if (pressed){
                viewModel.printEntry(contentAdapter)
            }
        }

        collectFlow(this,viewModel.navigateToTeleprompter){ navigate ->
            if (navigate){
                viewModel.saveScriptAndOpenTeleprompter()
            }
        }

        return binding.root*/
    }

    @Composable
    fun ScriptFragmentView(modifier: Modifier = Modifier) {
        val script by viewModel.script.collectAsStateWithLifecycle()
        val scriptContent by viewModel.scriptContentList.collectAsStateWithLifecycle()
        val isEditingScript by viewModel.isEditingScript.collectAsStateWithLifecycle()
        var menuAddTimeStampTitle by remember { viewModel.menuAddTimeStampTitle }

        script?.let { script ->
            val scriptUri by script.getUri(LocalContext.current).collectAsStateWithLifecycle()

            val cal = AppResources.CalendarResource(Calendar.getInstance())
            val date = cal.getFullDateStateFlow(LocalContext.current).value
            menuAddTimeStampTitle =
                if (script.scriptDateTime.getFullDateStateFlow(LocalContext.current).value
                    ==
                    date
                ) {
                    stringResource(R.string.add_time_stamp)
                } else {
                    stringResource(R.string.add_date_stamp)
                }

            ScriptFragmentCatalog(
                script = script,
                isEditingScript,
                scriptContentList = scriptContent,
                modifier = modifier,
                collapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
                navigationIcon = { modifier ->
                    IconButton(
                        modifier = modifier,
                        onClick = { viewModel.onBackPressed() })
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.scripts_navigate_back_button)
                        )
                    }
                },
                shareIcon = { modifier ->
                    IconButton(
                        modifier = modifier,
                        onClick = {
                            lifecycleScope.launch {
                                val result = viewModel.shareScript(requireContext())
                                val hasContent = result.first
                                val intent = result.second
                                withContext(Dispatchers.Main) {
                                    if (hasContent) startActivity(
                                        Intent.createChooser(intent, null)
                                    )
                                    else Toast.makeText(
                                        context,
                                        "Nothing To Share",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            }
                        })
                    {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                },
                teleprompterIcon = { modifier ->
                    IconButton(
                        modifier = modifier,
                        onClick = { viewModel.openTeleprompter() })
                    {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.open_teleprompter)
                        )
                    }
                },
                menuList = listOf(
                    MenuItemData(
                        text = if (isEditingScript) stringResource(R.string.preview_script) else stringResource(R.string.edit_script),
                        onClick = { viewModel.editOrSaveScript() }
                    ),
                    MenuItemData(
                        text = scriptUri?.let { stringResource(R.string.remove_image)}?:stringResource(R.string.choose_image),
                        onClick = { viewModel.chooseTopImage {
                            AppResources.ContentTypeAccessor[AppResources.ContentType.IMAGE]?.let {
                                galleryLauncher.launch(it)
                            }
                        } }
                    ),
                    MenuItemData(
                        text = menuAddTimeStampTitle,
                        onClick = { viewModel.addTimeStamp(requireContext()) }
                    ),
                    MenuItemData(
                        text = stringResource(R.string.choose_markup_language),
                        onClick = { viewModel.chooseMarkupLanguage() }
                    ),
                    MenuItemData(
                        text = stringResource(R.string.load_document),
                        onClick = { viewModel.loadText() }
                    ),
                    MenuItemData(
                        text = stringResource(R.string.print_to_text_file),
                        onClick = { viewModel.printToTextFile() }
                    )
                )
            )
        }
    }

    /*private fun getScrollPosition(): Int{
        val layoutManager = binding.scriptRecyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findLastCompletelyVisibleItemPosition()
    }*/

}
