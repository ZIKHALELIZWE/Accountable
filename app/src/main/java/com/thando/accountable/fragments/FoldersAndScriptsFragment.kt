package com.thando.accountable.fragments

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentCompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thando.accountable.AnimalListPreviewParameterProvider
import com.thando.accountable.IntentActivity
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Script
import com.thando.accountable.fragments.viewmodels.FoldersAndScriptsViewModel
import com.thando.accountable.ui.management.states.toolbar.ToolbarState
import com.thando.accountable.ui.screens.Animal
import com.thando.accountable.ui.screens.Catalog
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.remove
import kotlin.random.Random

class FoldersAndScriptsFragment : Fragment() {

    val viewModel : FoldersAndScriptsViewModel by viewModels<FoldersAndScriptsViewModel> { FoldersAndScriptsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.viewModel.toolbarVisible.value = false

        return ComposeView(requireContext()).apply {
            WindowCompat.setDecorFitsSystemWindows(mainActivity.window, false)
            setContent {
                val coroutineScope = rememberCoroutineScope()
                if (viewModel.intentString==null) {
                    val mainActivity = (requireActivity() as MainActivity)
                    mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                lifecycleScope.launch {
                                    withContext(coroutineScope.coroutineContext) {
                                        if (!mainActivity.viewModel.toggleDrawer(false) && !viewModel.onBackPressed()
                                        ) {
                                            isEnabled = false
                                            mainActivity.onBackPressedDispatcher.onBackPressed()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                else{
                    val intentActivity = (requireActivity() as IntentActivity)
                    intentActivity.dialogFragment.dialog?.setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            viewModel.onBackPressed()
                        }
                        else false
                    }
                }

                AccountableTheme {
                    Scaffold(
                        floatingActionButton = if (viewModel.intentString == null) {@Composable{
                            FloatingActionButton(
                                onClick = { viewModel.addFolderScript() },
                                modifier = Modifier.padding(16.dp),
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_folder)
                                )
                            }
                        }} else {@Composable{}},
                        floatingActionButtonPosition = FabPosition.End,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        FoldersAndScriptsFragmentView(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
        /*
        // Inflate the layout for this fragment
        _binding = FragmentFoldersAndScriptsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        folderAdapter = viewModel.getFolderAdapter(viewLifecycleOwner,childFragmentManager)
        scriptAdapter = viewModel.getScriptAdapter(viewLifecycleOwner,childFragmentManager)
        goalAdapter = viewModel.getGoalAdapter(viewLifecycleOwner,childFragmentManager)

        binding.folderAdapter = folderAdapter
        binding.scriptAdapter = scriptAdapter
        binding.goalAdapter = goalAdapter

        var scrollRange = -1
        binding.appBarLayout.addOnOffsetChangedListener { barLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = barLayout?.totalScrollRange!!
            }
            if (binding.switchFolderScriptButton.isInvisible || scrollRange + verticalOffset == 0) {
                showOption(menu,R.id.menu_switch_folder_script)
            } else {
                hideOption(menu,R.id.menu_switch_folder_script)
            }
        }

        val mainActivity = if (viewModel.intentString == null) (requireActivity() as MainActivity)
        else (requireActivity() as IntentActivity)
        if (viewModel.intentString == null) {
            (mainActivity as MainActivity).enableDrawerLayout()
            mainActivity.setToolbar(binding.toolbar)
        }
        else mainActivity.setSupportActionBar(binding.toolbar)

        val menuHost: MenuHost = mainActivity
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(inputMenu: Menu, menuInflater: MenuInflater) {
                menu = inputMenu
                menuInflater.inflate(R.menu.folders_and_scripts_menu, menu)
                hideOption(menu, R.id.menu_switch_folder_script)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_switch_folder_script -> {
                        viewModel.switchFolderScript()
                    }

                    R.id.search -> {
                        viewModel.search(getScrollPosition())
                    }

                    R.id.order -> {
                        viewModel.switchFolderOrder()
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        collectFlow(this,viewModel.folder){ folder ->
            viewModel.updateStateFromFolder(folder)
        }

        collectFlow(this,viewModel.showScripts) { showScriptsMutable ->
            if (showScriptsMutable!=null) {
                coroutineScope{
                    showScriptsMutable.collectLatest { showScripts ->
                        menu?.findItem(R.id.menu_switch_folder_script)?.setIcon(
                            if (showScripts) R.drawable.scripts_drawable
                            else R.drawable.folder_drawable
                        )
                        viewModel.updateFolderShowScripts()
                        viewModel.loadFolderData()
                    }
                }
            }
        }

        collectFlow(this,viewModel.collapseAppBar){
            binding.appBarLayout.setExpanded(false)
        }

        collectFlow(this,viewModel.switchToFolderScript){
            viewModel.switchFolderScript(getScrollPosition())
        }

        collectFlow(this,viewModel.folderOrder) { folderOrScriptOrder ->
            if (folderOrScriptOrder!=null){
                collectFlow(currentCoroutineContext(),folderOrScriptOrder){ ascendingOrder ->
                    if (ascendingOrder) {
                        menu?.findItem(R.id.order)?.setIcon(R.drawable.arrow_up)
                    }
                    else {
                        menu?.findItem(R.id.order)?.setIcon(R.drawable.arrow_down)
                    }
                    viewModel.loadContent(ascendingOrder)
                }
            }
        }

        collectFlow(this,viewModel.scrollTo){
            setScrollPosition(it)
        }

        collectFlow(this,viewModel.foldersList){
            folderAdapter.setFolderList(it)
            viewModel.listLoaded.value = true
        }

        collectFlow(this,viewModel.scriptsList){
            scriptAdapter.setScriptList(it)
            viewModel.listLoaded.value = true
        }

        collectFlow(this,viewModel.goalsList){
            goalAdapter.setGoalList(it)
            viewModel.listLoaded.value = true
        }

        collectFlow(this,viewModel.openGoal){ id ->
            if (id != null){
                viewModel.getFolderId()?.let { parentFolderId ->
                    /*val action = FoldersAndScriptsFragmentDirections.actionFoldersAndScriptsFragmentToTaskFragment(
                        id,
                        parentFolderId,
                        viewModel.getFolderType().name
                    )
                    activity?.lifecycleScope?.launch {
                        withContext(Dispatchers.IO) {
                            viewModel.prepareToClose(getScrollPosition()) {
                                findNavController().navigate(action)
                            }
                        }
                    }*/
                }
            }
        }

        collectFlow(this,viewModel.openFolder){ id ->
            if (id != null){
                viewModel.updateFolderScrollPosition(getScrollPosition()){
                    viewModel.updateFolderShowScripts {
                        viewModel.loadFolder(id)
                    }
                }
            }
        }

        collectFlow(this,viewModel.openScript){ id ->
            if (id != null){
                viewModel.updateFolderScrollPosition(getScrollPosition()){
                    viewModel.updateFolderShowScripts {
                        viewModel.loadAndOpenScript(id,activity)
                    }
                }
            }
        }

        collectFlow(this,viewModel.editGoal){ id ->
            if (id != null){
                viewModel.getFolderId()?.let { parentFolderId ->
                    /*val action =
                        FoldersAndScriptsFragmentDirections.actionFoldersAndScriptsFragmentToEditGoalFragment(
                            id,
                            parentFolderId,
                            viewModel.getFolderType().name
                        )
                    activity?.lifecycleScope?.launch {
                        withContext(Dispatchers.IO) {
                            viewModel.prepareToClose(getScrollPosition()) {
                                findNavController().navigate(action)
                            }
                        }
                    }*/
                }
            }
        }

        return binding.root*/
    }

    data class FoldersAndScriptsLists(
        val listShown: FASListType,
        var foldersList: MutableList<Folder>,
        var scriptsList: MutableList<Script>,
        var goalsList: MutableList<Goal>,
        val onClickListeners: FoldersAndScriptsViewModel.OnClickListeners
    ){
        enum class FASListType{
            FOLDERS, SCRIPTS, GOALS
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FoldersAndScriptsFragmentView(modifier: Modifier = Modifier) {
        val mainActivityViewModel = (requireActivity() as MainActivity).viewModel
        val scope = rememberCoroutineScope()

        val scrollStateParent by viewModel.scrollStateParent.collectAsStateWithLifecycle()
        val scrollState = remember { scrollStateParent?: ScrollState(0) }

        val folder by viewModel.folder.collectAsStateWithLifecycle()
        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

        val foldersList by remember { viewModel.foldersList }
        val scriptsList by remember { viewModel.scriptsList }
        val goalsList by remember { viewModel.goalsList }

        val showScriptsParent by viewModel.showScripts.collectAsStateWithLifecycle()
        val showScripts by showScriptsParent?.collectAsStateWithLifecycle()?:MutableStateFlow(false).collectAsStateWithLifecycle()
        val orderIconParent by viewModel.folderOrder.collectAsStateWithLifecycle()
        val orderIconAscending by orderIconParent?.collectAsStateWithLifecycle()?:MutableStateFlow(false).collectAsStateWithLifecycle()

        val folderName by folder?.folderName?.collectAsStateWithLifecycle()
            ?:MutableStateFlow(
                if (viewModel.folderIsScripts()) stringResource(R.string.books)
                else stringResource(R.string.goals)
            ).collectAsStateWithLifecycle()
        val listShown by viewModel.listShown.collectAsStateWithLifecycle()

        val listsCollection = FoldersAndScriptsLists(
            listShown,
            foldersList,
            scriptsList,
            goalsList,
            FoldersAndScriptsViewModel.OnClickListeners(viewModel)
        )

        collectFlow(this,viewModel.scrollTo){
            setScrollPosition(it)
        }

        Catalog(
            listsCollection = listsCollection,
            columns = 2,
            modifier = modifier,
            collapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
            imageUri = if (folder != null) {
                folder!!.getUri(LocalContext.current).collectAsStateWithLifecycle(null).value
            } else appSettings?.getUri(LocalContext.current)?.collectAsStateWithLifecycle(null)?.value,
            navigationIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = { scope.launch{ mainActivityViewModel.toggleDrawer() } })
                {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.navigation_drawer_button)
                    )
                }
            },
            titleText = { modifier ->
                Text(folderName, modifier = modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
            },
            searchIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = { lifecycleScope.launch { viewModel.search(getScrollPosition()) } })
                {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                }
            },
            orderIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = { lifecycleScope.launch { viewModel.switchFolderOrder() } })
                {
                    Icon(
                        imageVector = if (orderIconAscending) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = stringResource(R.string.change_entry_order)
                    )
                }
            },
            folderScriptSwitchIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = { viewModel.switchFolderScript(getScrollPosition()) })
                {
                    Icon(
                        imageVector = if (showScripts) Icons.AutoMirrored.Filled.LibraryBooks
                        else Icons.Default.Folder,
                        contentDescription = stringResource(R.string.switch_between_folder_and_script_button)
                    )
                }
            }
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun CollapsingToolbarInComposeAppPreview(
        @PreviewParameter(AnimalListPreviewParameterProvider::class) animals: List<Animal>
    ) {
        AccountableTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                FoldersAndScriptsFragmentView(modifier = Modifier.padding(innerPadding))
            }
        }
    }

    companion object {
        fun hideOption(menu: Menu?,id: Int) {
            val item = menu?.findItem(id)
            item?.isVisible = false
        }

        fun showOption(menu: Menu?,id: Int) {
            val item = menu?.findItem(id)
            item?.isVisible = true
        }

        fun enableOption(menu: Menu?,id: Int){
            val item = menu?.findItem(id)
            item?.isEnabled = true
            setOptionColour(item,android.R.color.black)
        }

        fun disableOption(menu: Menu?,id: Int){
            val item = menu?.findItem(id)
            item?.isEnabled = false
            setOptionColour(item,android.R.color.darker_gray)
        }

        private fun setOptionColour(item: MenuItem?, resInt: Int){
            if (item != null) {
                val grayIcon = DrawableCompat.wrap(item.icon!!)
                val colorSelector = ResourcesCompat.getColorStateList(
                    MainActivity.ResourceProvider.resources,
                    resInt,
                    null
                )
                DrawableCompat.setTintList(grayIcon, colorSelector)
                item.icon = grayIcon
            }
        }
    }

    private fun setScrollPosition(position: Int) {
        /*val layoutManager = binding.foldersAndScriptsList.layoutManager as LinearLayoutManager
        binding.foldersAndScriptsList.post {
            layoutManager.scrollToPosition(position)
        }*/
    }

    private fun getScrollPosition(): Int{
        //val layoutManager = binding.foldersAndScriptsList.layoutManager as LinearLayoutManager
        //return layoutManager.findLastCompletelyVisibleItemPosition()
        return 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.getFolderId()
            ?.let { outState.putLong( FoldersAndScriptsViewModel.FOLDER_ID_BUNDLE , it) }
        outState.putString(FoldersAndScriptsViewModel.FOLDER_TYPE_BUNDLE, viewModel.getFolderType().name)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        activity?.lifecycleScope?.launch {
            withContext(Dispatchers.IO) {
                viewModel.prepareToClose(getScrollPosition()) {}
            }
        }
        super.onPause()
    }
}