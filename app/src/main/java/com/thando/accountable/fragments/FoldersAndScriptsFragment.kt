package com.thando.accountable.fragments

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AppResources
import com.thando.accountable.IntentActivity
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Script
import com.thando.accountable.fragments.viewmodels.FoldersAndScriptsViewModel
import com.thando.accountable.ui.management.states.toolbar.FixedScrollFlagState
import com.thando.accountable.ui.management.states.toolbar.ToolbarState
import com.thando.accountable.ui.screens.Alpha
import com.thando.accountable.ui.screens.CollapsedPadding
import com.thando.accountable.ui.screens.ContentPadding
import com.thando.accountable.ui.screens.Elevation
import com.thando.accountable.ui.screens.ExpandedPadding
import com.thando.accountable.ui.screens.MaxToolbarHeight
import com.thando.accountable.ui.screens.MinToolbarHeight
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FoldersAndScriptsFragment : Fragment() {

    private val bottomBarHeightFraction = 0.14f
    private val topBarHeightFraction = bottomBarHeightFraction / 2
    private val barColor = Color(red = 255f, green = 255f, blue = 255f, alpha = 0.5f)

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Catalog(
        listsCollection: FoldersAndScriptsLists,
        columns: Int,
        modifier: Modifier = Modifier,
        collapseType: ToolbarState.CollapseType = ToolbarState.CollapseType.Scroll,
        imageUri: Uri?,
        navigationIcon:@Composable (Modifier)-> Unit,
        titleText:@Composable (Modifier)-> Unit,
        searchIcon:@Composable (Modifier)-> Unit,
        orderIcon:@Composable (Modifier)-> Unit,
        folderScriptSwitchIcon:@Composable (Modifier)-> Unit
    ) {
        val toolbarHeightRange = with(LocalDensity.current) {
            MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
        }
        val toolbarState = ToolbarState.rememberToolbarState( collapseType, toolbarHeightRange)
        val listState = rememberLazyListState()

        val scope = rememberCoroutineScope()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                            && listState.firstVisibleItemScrollOffset == 0
                    toolbarState.scrollOffset -= available.y
                    return Offset(0f, toolbarState.consumed)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (available.y > 0){
                        scope.launch {
                            animateDecay(
                                initialValue = toolbarState.height + toolbarState.offset,
                                initialVelocity = available.y,
                                animationSpec = FloatExponentialDecaySpec()
                            ){ value, _ ->
                                toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                                        && listState.firstVisibleItemScrollOffset == 0
                                toolbarState.scrollOffset -= value - (toolbarState.height + toolbarState.offset)
                                if (toolbarState.scrollOffset == 0f) scope.coroutineContext.cancelChildren()
                            }
                        }
                    }
                    return super.onPostFling(consumed, available)
                }
            }
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val bottomSheet by listsCollection.onClickListeners.bottomSheetListeners.collectAsStateWithLifecycle()

        Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
            when(listsCollection.listShown){
                FoldersAndScriptsLists.FASListType.FOLDERS -> {
                    LazyFolderCatalog(
                        foldersList = listsCollection.foldersList,
                        onClickListeners = listsCollection.onClickListeners,
                        columns = columns,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                            }
                            .graphicsLayer { translationY = toolbarState.height + toolbarState.offset },
                        listState = listState,
                        contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                    )
                }
                FoldersAndScriptsLists.FASListType.SCRIPTS -> {
                    LazyScriptCatalog(
                        scriptsList = listsCollection.scriptsList,
                        onClickListeners = listsCollection.onClickListeners,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                            }
                            .graphicsLayer { translationY = toolbarState.height + toolbarState.offset },
                        listState = listState,
                        contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                    )
                }
                FoldersAndScriptsLists.FASListType.GOALS -> {

                }
            }
            FoldersAndScriptsCollapsingToolbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { toolbarState.height.toDp() })
                    .graphicsLayer { translationY = toolbarState.offset },
                progress = toolbarState.progress,
                imageUri,
                navigationIcon,
                titleText,
                searchIcon,
                orderIcon,
                folderScriptSwitchIcon
            )
        }
        bottomSheet?.let { bottomSheet ->
            ModalBottomSheet(
                onDismissRequest = {
                    listsCollection.onClickListeners.bottomSheetListeners.update { null }
                },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    bottomSheet.displayView.invoke()
                    Row {
                        bottomSheet.onEditClickListener?.let {
                            Button(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RectangleShape,
                                colors = ButtonColors(
                                    Color.Blue,
                                    Color.Black,
                                    Color.LightGray,
                                    Color.DarkGray
                                ),
                                onClick = {
                                    bottomSheet.onEditClickListener.invoke()
                                    listsCollection.onClickListeners.bottomSheetListeners.update { null }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.bottom_sheet_edit_button)
                                )
                            }
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RectangleShape,
                            colors = ButtonColors(Color.Red,Color.Black,Color.LightGray,Color.DarkGray),
                            onClick = {
                                bottomSheet.onDeleteClickListener.invoke()
                                listsCollection.onClickListeners.bottomSheetListeners.update { null }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.bottom_sheet_delete_button)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LazyFolderCatalog(
        foldersList: MutableList<Folder>,
        onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
        columns: Int,
        modifier: Modifier = Modifier,
        listState: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        val chunkedItems = remember(
            foldersList
        ) { foldersList.chunked(columns) }

        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = modifier
        ) {
            chunkedItems.forEach { chunk ->
                item {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                    ) {
                        chunk.forEach { folder ->
                            FolderCard(
                                folder = folder,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .weight(1f),
                                onClickListeners
                            )
                        }
                        val emptyCells = columns - chunk.size
                        if (emptyCells > 0) {
                            Spacer(modifier = Modifier.weight(emptyCells.toFloat()))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LazyScriptCatalog(
        scriptsList: MutableList<Script>,
        onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
        modifier: Modifier = Modifier,
        listState: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = modifier
        ) {
            items(scriptsList, key = {it.scriptId?:Random.nextLong()}) { script ->
                ScriptCard(
                    script = script,
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth(),
                    onClickListeners
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FolderCard(
        folder: Folder,
        modifier: Modifier = Modifier,
        onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
        clickable:Boolean = true
    ) {
        val folderName by folder.folderName.collectAsStateWithLifecycle()
        val folderUri by folder.getUri(LocalContext.current).collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            onClickListeners.viewModel.getFolderContentPreview(folder)
        }
        Card(
            modifier = modifier.aspectRatio(0.66f)
                .combinedClickable(
                    onLongClick = {
                        if (clickable && onClickListeners.setOnLongClick){
                            onClickListeners.bottomSheetListeners.update {
                                FoldersAndScriptsViewModel.OnClickListeners.BottomSheetListeners(
                                    displayView = {
                                        FolderCard(folder, Modifier, onClickListeners,false)
                                    },
                                    onEditClickListener = {
                                        onClickListeners.folderOnEditClickListener(folder.folderId)
                                    },
                                    onDeleteClickListener = {
                                        onClickListeners.folderOnDeleteClickListener(folder.folderId)
                                    }
                                )
                            }
                        }
                    },
                    onClick = {
                        if (clickable) {
                            onClickListeners.folderClickListener(folder.folderId)
                        }
                    }
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = folderUri?.let { AppResources.getBitmapFromUri(LocalContext.current, it) }
                        ?.asImageBitmap()
                        ?: ImageBitmap(1, 1),
                    contentDescription = folderName,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                if (folder.folderType == Folder.FolderType.SCRIPTS)
                    FolderTopBar(
                        folders = folder.numFolders.collectAsStateWithLifecycle(0),
                        scripts = folder.numScripts.collectAsStateWithLifecycle()
                    )
                else if (folder.folderType == Folder.FolderType.GOALS)
                    FolderTopBar(
                        folders = folder.numFolders.collectAsStateWithLifecycle(0),
                        goals = folder.numGoals.collectAsStateWithLifecycle()
                    )
                BottomBar(folderName)
            }
        }
    }

    @Composable
    private fun BoxScope.FolderTopBar(folders: State<Int>, scripts: State<Int>? = null, goals: State<Int>? = null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(topBarHeightFraction)
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .wrapContentWidth()
                    .align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .background(
                            color = LocalContentColor.current.copy(alpha = 0.0f),
                            shape = RectangleShape
                        ),
                    imageVector = Icons.Default.Folder,
                    contentDescription = null
                )
                Text(folders.value.toString(),
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .background(
                            color = LocalContentColor.current.copy(alpha = 0.0f),
                            shape = RectangleShape
                        )
                )
                Icon(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .background(
                            color = LocalContentColor.current.copy(alpha = 0.0f),
                            shape = RectangleShape
                        ),
                    imageVector = scripts?.let { Icons.AutoMirrored.Filled.LibraryBooks }
                        ?: goals?.let { Icons.Default.Stars } ?: Icons.Default.Error,
                    contentDescription = null
                )
                Text(scripts?.value?.toString() ?: (goals?.value?.toString() ?: ""),
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .background(
                            color = LocalContentColor.current.copy(alpha = 0.0f),
                            shape = RectangleShape
                        )
                )
            }
        }
    }

    @Composable
    private fun BoxScope.BottomBar(text: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(bottomBarHeightFraction)
                .background(barColor)
                .align(Alignment.BottomCenter)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScriptCard(
        script: Script,
        modifier: Modifier = Modifier,
        onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
        clickable:Boolean = true
    ) {
        val scriptUri by script.getUri(LocalContext.current).collectAsStateWithLifecycle()
        var contentPreview by remember { mutableStateOf<AccountableRepository.ContentPreview?>(null) }

        LaunchedEffect(Unit) {
            contentPreview = script.scriptId?.let { onClickListeners.viewModel.getScriptContentPreview(it) }
            contentPreview?.init()
        }

        contentPreview?.let { contentPreview ->
            val time by script.scriptDateTime.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()
            val date by script.scriptDateTime.getFullDateStateFlow(LocalContext.current).collectAsStateWithLifecycle()

            val title by remember { script.scriptTitle }
            val description by contentPreview.getDescription().collectAsStateWithLifecycle("")
            val displayImage by contentPreview.getDisplayImage().collectAsStateWithLifecycle()

            val numImages by contentPreview.getNumImages().collectAsStateWithLifecycle(0)
            val numVideos by contentPreview.getNumVideos().collectAsStateWithLifecycle(0)
            val numAudios by contentPreview.getNumAudios().collectAsStateWithLifecycle(0)
            val numDocuments by contentPreview.getNumDocuments().collectAsStateWithLifecycle(0)
            val numScript by contentPreview.getNumScripts().collectAsStateWithLifecycle(0)
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight()
                    .combinedClickable(
                        onLongClick = {
                            if (clickable && onClickListeners.setOnLongClick) {
                                onClickListeners.bottomSheetListeners.update {
                                    FoldersAndScriptsViewModel.OnClickListeners.BottomSheetListeners(
                                        displayView = {
                                            ScriptCard(script, Modifier, onClickListeners, false)
                                        },
                                        onEditClickListener = null,
                                        onDeleteClickListener = {
                                            onClickListeners.scriptOnDeleteClickListener(script.scriptId)
                                        }
                                    )
                                }
                            }
                        },
                        onClick = {
                            if (clickable) {
                                script.scriptId?.let { onClickListeners.scriptClickListener(it) }
                            }
                        }
                    ),
                shape = RectangleShape,
                colors = CardColors(Color.White,
                    Color.Black,
                    Color.LightGray,
                    Color.DarkGray)
            ) {
                Row {
                    Card(modifier = Modifier.height(113.dp)
                        .width(113.dp),
                        colors = CardColors(Color.White,
                            Color.White,
                            Color.LightGray,
                            Color.DarkGray),
                    ) {
                        scriptUri?:displayImage?.let {
                            Image(
                                bitmap = AppResources.getBitmapFromUri(LocalContext.current, it)
                                    ?.asImageBitmap()
                                    ?: ImageBitmap(1, 1),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.height(113.dp)
                                    .width(113.dp),
                                contentDescription = stringResource(R.string.script_display_image)
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(end = 5.dp).height(113.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth().padding(5.dp),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp) // Title
                        Text(text = description,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(5.dp),
                            textAlign = TextAlign.Start,
                            color = Color.Black) // Description
                        Row(modifier = Modifier
                            .height(IntrinsicSize.Max).padding(5.dp)
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(time,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                fontSize = 12.sp) // Entry Time
                            val modifier = Modifier.weight(0.1f)
                            MediaIcon(numImages, Icons.Default.Image,modifier)
                            MediaIcon(numVideos, Icons.Default.Videocam,modifier)
                            MediaIcon(numAudios, Icons.Default.Mic,modifier)
                            MediaIcon(numDocuments, Icons.Default.Book,modifier)
                            MediaIcon(numScript, Icons.AutoMirrored.Filled.LibraryBooks,modifier)
                            Text(date,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                fontSize = 12.sp) // Entry Date
                        }
                        //Text() // Entry Size
                    }
                }
            }
        }
    }

    @Composable
    private fun MediaIcon(numMedia:Int, icon: ImageVector, modifier: Modifier = Modifier){
        if (numMedia>0) {
            Row{
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
                Text(
                    numMedia.toString(),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun FoldersAndScriptsCollapsingToolbar(
        modifier: Modifier = Modifier,
        progress: Float,
        imageUri: Uri?,
        navigationIcon:@Composable (Modifier)-> Unit,
        titleText:@Composable (Modifier)-> Unit,
        searchIcon:@Composable (Modifier)-> Unit,
        orderIcon:@Composable (Modifier)-> Unit,
        folderScriptSwitchIcon:@Composable (Modifier)-> Unit,
    ) {
        val logoPadding = with(LocalDensity.current) {
            lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
        }

        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = Elevation
        ) {
            Box (modifier = Modifier.fillMaxSize()) {
                //#region Background Image
                Image(
                    bitmap = imageUri?.let { AppResources.getBitmapFromUri(LocalContext.current, imageUri) }
                        ?.asImageBitmap()
                        ?: AppResources.getBitmapFromUri(
                            LocalContext.current,
                            AppResources.getUriFromDrawable(
                                LocalContext.current,
                                R.drawable.ic_stars_black_24dp
                            )
                        )?.asImageBitmap()?:ImageBitmap(1, 1),
                    contentDescription = stringResource(R.string.folder_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = progress * Alpha
                        }
                )
                //#endregion
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = ContentPadding)
                        .fillMaxSize()
                ) {
                    FoldersAndScriptsCollapsingToolbarLayout (progress = progress) {
                        val mod = Modifier.padding(logoPadding).wrapContentWidth()
                        navigationIcon(mod)
                        titleText(mod)
                        searchIcon(mod)
                        orderIcon(mod)
                        folderScriptSwitchIcon(mod)
                    }
                }
            }
        }
    }

    @Composable
    private fun FoldersAndScriptsCollapsingToolbarLayout(
        progress: Float,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Layout(
            modifier = modifier,
            content = content
        ) { measurables, constraints ->
            check(measurables.size == 5)

            val items = measurables.map {
                it.measure(constraints)
            }
            layout(
                width = constraints.maxWidth,
                height = constraints.maxHeight
            ) {
                val navigationIcon = items[0]
                val titleText = items[1]
                val searchIcon = items[2]
                val orderIcon = items[3]
                val folderScriptSwitchIcon = items[4]
                navigationIcon.placeRelative(
                    x = 0,
                    y = lerp(
                        start = (constraints.maxHeight - navigationIcon.height) / 2,
                        stop = 0,
                        fraction = progress
                    )
                )
                titleText.placeRelative(
                    x = lerp(
                        start = constraints.maxWidth / 2 - titleText.width,
                        stop = 0,
                        fraction = progress
                    ),
                    y = lerp(
                        start = (constraints.maxHeight - titleText.height) / 2,
                        stop = constraints.maxHeight - titleText.height,
                        fraction = progress
                    )
                )
                searchIcon.placeRelative(
                    x = constraints.maxWidth - folderScriptSwitchIcon.width - orderIcon.width - searchIcon.width,
                    y = lerp(
                        start = (constraints.maxHeight - searchIcon.height) / 2,
                        stop = 0,
                        fraction = progress
                    )
                )
                orderIcon.placeRelative(
                    x = constraints.maxWidth - folderScriptSwitchIcon.width - orderIcon.width,
                    y = lerp(
                        start = (constraints.maxHeight - orderIcon.height) / 2,
                        stop = 0,
                        fraction = progress
                    )
                )
                folderScriptSwitchIcon.placeRelative(
                    x = constraints.maxWidth - folderScriptSwitchIcon.width,
                    y = lerp(
                        start = (constraints.maxHeight - folderScriptSwitchIcon.height) / 2,
                        stop = 0,
                        fraction = progress
                    )
                )
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