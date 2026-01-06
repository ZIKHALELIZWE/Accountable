package com.thando.accountable.fragments

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoldersAndScriptsFragment : Fragment() {

    private val bottomBarHeightFraction = 0.14f
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FoldersAndScriptsFragmentView(modifier: Modifier = Modifier) {
        val mainActivityViewModel = (requireActivity() as MainActivity).viewModel
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val scrollStateParent by viewModel.scrollStateParent.collectAsStateWithLifecycle()

        val folder by viewModel.folder.collectAsStateWithLifecycle()
        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

        var result by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }
        LaunchedEffect(folder, appSettings) {
            result = if (folder != null) folder!!.getUri(context)
            else if (appSettings!=null) appSettings!!.getUri(context)
            else MutableStateFlow(null)
        }
        val imageUri by result.collectAsStateWithLifecycle(null)

        val showScriptsParent by viewModel.showScripts.collectAsStateWithLifecycle()
        val showScripts by showScriptsParent?.collectAsStateWithLifecycle()?:MutableStateFlow(false).collectAsStateWithLifecycle()
        val orderIconParent by viewModel.folderOrder.collectAsStateWithLifecycle()
        val orderIconAscending by orderIconParent?.collectAsStateWithLifecycle()?:MutableStateFlow(false).collectAsStateWithLifecycle()

        val folderName by folder?.folderName?.collectAsStateWithLifecycle()
            ?:MutableStateFlow(
                if (viewModel.folderIsScripts()) stringResource(R.string.books)
                else stringResource(R.string.goals)
            ).collectAsStateWithLifecycle()

        collectFlow(this,viewModel.scrollTo){
            setScrollPosition(it)
        }

        Catalog(
            columns = 2,
            scrollStateParent = scrollStateParent,
            modifier = modifier,
            collapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
            imageUri = imageUri,
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
                    onClick = { lifecycleScope.launch { viewModel.search() } })
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
                    onClick = { viewModel.switchFolderScript() })
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
        columns: Int,
        scrollStateParent: LazyListState?,
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
        scrollStateParent?.let { scrollStateParent ->
            val scope = rememberCoroutineScope()
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        toolbarState.scrollTopLimitReached = scrollStateParent.firstVisibleItemIndex == 0
                                && scrollStateParent.firstVisibleItemScrollOffset == 0
                        toolbarState.scrollOffset -= available.y
                        return Offset(0f, toolbarState.consumed)
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        if (available.y > 0) {
                            scope.launch {
                                animateDecay(
                                    initialValue = toolbarState.height + toolbarState.offset,
                                    initialVelocity = available.y,
                                    animationSpec = FloatExponentialDecaySpec()
                                ) { value, _ ->
                                    toolbarState.scrollTopLimitReached =
                                        scrollStateParent.firstVisibleItemIndex == 0
                                                && scrollStateParent.firstVisibleItemScrollOffset == 0
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

            val listShown by viewModel.listShown.collectAsStateWithLifecycle()
            val bottomSheet by viewModel.bottomSheetListeners.collectAsStateWithLifecycle()

            Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
                when (listShown) {
                    Folder.FolderListType.FOLDERS -> {
                        LazyFolderCatalog(
                            columns = columns,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                                }
                                .graphicsLayer {
                                    translationY = toolbarState.height + toolbarState.offset
                                },
                            listState = scrollStateParent,
                            contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                        )
                    }

                    Folder.FolderListType.SCRIPTS -> {
                        LazyScriptCatalog(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                                }
                                .graphicsLayer {
                                    translationY = toolbarState.height + toolbarState.offset
                                },
                            listState = scrollStateParent,
                            contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                        )
                    }

                    Folder.FolderListType.GOALS -> {

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
                        viewModel.bottomSheetListeners.update { null }
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
                                        viewModel.bottomSheetListeners.update { null }
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
                                colors = ButtonColors(
                                    Color.Red,
                                    Color.Black,
                                    Color.LightGray,
                                    Color.DarkGray
                                ),
                                onClick = {
                                    bottomSheet.onDeleteClickListener.invoke()
                                    viewModel.bottomSheetListeners.update { null }
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
    }

    @Composable
    fun LazyFolderCatalog(
        columns: Int,
        modifier: Modifier = Modifier,
        listState: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        val foldersList by viewModel.foldersList.collectAsStateWithLifecycle()
        foldersList?.let { foldersList ->
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                        ) {
                            chunk.forEach { folder ->
                                FolderCard(
                                    folder = folder,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .weight(1f)
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
    }

    @Composable
    fun LazyScriptCatalog(
        modifier: Modifier = Modifier,
        listState: LazyListState = rememberLazyListState(),
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        val list by viewModel.scriptsList.collectAsStateWithLifecycle()
        list?.let { list ->
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = modifier
            ) {
                items(items = list) { script ->
                    ScriptCard(
                        script = script,
                        modifier = Modifier
                            .padding(2.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FolderCard(
        folder: Folder,
        modifier: Modifier = Modifier,
        clickable:Boolean = true
    ) {
        val context = LocalContext.current
        val folderName by folder.folderName.collectAsStateWithLifecycle()
        var folderUriStateFlow by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }

        val numFolders by folder.numFolders.collectAsStateWithLifecycle(0)
        val numScripts by folder.numScripts.collectAsStateWithLifecycle(0)
        val numGoals by folder.numGoals.collectAsStateWithLifecycle(0)

        viewModel.getFolderContentPreview(folder)

        LaunchedEffect(folder) {
            folderUriStateFlow = folder.getUri(context)
        }

        val folderUri by folderUriStateFlow.collectAsStateWithLifecycle()
        var folderImage by remember { mutableStateOf(ImageBitmap(1,1)) }

        LaunchedEffect(folderUri) {
            folderImage = folderUri?.let { AppResources.getBitmapFromUri(context, it) }
                ?.asImageBitmap()
                ?: ImageBitmap(1, 1)
        }
        Card(
            modifier = modifier.aspectRatio(0.66f)
                .combinedClickable(
                    onLongClick = {
                        if (clickable && viewModel.setOnLongClick()){
                            viewModel.bottomSheetListeners.update {
                                FoldersAndScriptsViewModel.BottomSheetListeners(
                                    displayView = {
                                        FolderCard(folder, Modifier,false)
                                    },
                                    onEditClickListener = {
                                        viewModel.onFolderEdit(folder.folderId)
                                    },
                                    onDeleteClickListener = {
                                        viewModel.onDeleteFolder(folder.folderId)
                                    }
                                )
                            }
                        }
                    },
                    onClick = {
                        if (clickable) {
                            viewModel.onFolderClick(folder.folderId)
                        }
                    }
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = folderImage,
                    contentDescription = folderName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (folder.folderType == Folder.FolderType.SCRIPTS)
                    FolderTopBar(
                        folders = numFolders,
                        scripts = numScripts
                    )
                else if (folder.folderType == Folder.FolderType.GOALS)
                    FolderTopBar(
                        folders = numFolders,
                        goals = numGoals
                    )
                BottomBar(folderName)
            }
        }
    }

    @Composable
    private fun BoxScope.FolderTopBar(folders: Int, scripts: Int? = null, goals: Int? = null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null
                )
                Text(folders.toString())
                Icon(
                    imageVector = scripts?.let { Icons.AutoMirrored.Filled.LibraryBooks }
                        ?: goals?.let { Icons.Default.Stars } ?: Icons.Default.Error,
                    contentDescription = null
                )
                Text(scripts?.toString() ?: (goals?.toString() ?: ""))
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
        clickable:Boolean = true
    ) {
        val context = LocalContext.current
        var uriStateFlow by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }
        var contentPreview by remember { mutableStateOf<AccountableRepository.ContentPreview?>(null) }
        var timeStateFlow by remember { mutableStateOf<StateFlow<String?>>(MutableStateFlow(null)) }
        var dateStateFlow by remember { mutableStateOf<StateFlow<String?>>(MutableStateFlow(null)) }
        LaunchedEffect(script) {
            uriStateFlow = script.getUri(context)
            timeStateFlow = script.scriptDateTime.getTimeStateFlow(context)
            dateStateFlow = script.scriptDateTime.getFullDateStateFlow(context)
            contentPreview = script.scriptId?.let { viewModel.getScriptContentPreview(it) }
            contentPreview?.init()
        }
        val scriptUri by uriStateFlow.collectAsStateWithLifecycle()
        val time by timeStateFlow.collectAsStateWithLifecycle()
        val date by dateStateFlow.collectAsStateWithLifecycle()

        val title by remember { script.scriptTitle }
        val description by contentPreview?.getDescription()?.collectAsStateWithLifecycle("")
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        val displayImage by contentPreview?.getDisplayImage()?.collectAsStateWithLifecycle()
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(scriptUri,displayImage) {
            imageBitmap = (scriptUri?:displayImage)?.let { AppResources.getBitmapFromUri(context, it)?.asImageBitmap() }
        }

        val numImages by contentPreview?.getNumImages()?.collectAsStateWithLifecycle(0)
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        val numVideos by contentPreview?.getNumVideos()?.collectAsStateWithLifecycle(0)
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        val numAudios by contentPreview?.getNumAudios()?.collectAsStateWithLifecycle(0)
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        val numDocuments by contentPreview?.getNumDocuments()?.collectAsStateWithLifecycle(0)
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        val numScript by contentPreview?.getNumScripts()?.collectAsStateWithLifecycle(0)
            ?:MutableStateFlow(null).collectAsStateWithLifecycle()
        Card(
            modifier = modifier.fillMaxWidth().wrapContentHeight()
                .combinedClickable(
                    onLongClick = {
                        if (clickable && viewModel.setOnLongClick()) {
                            viewModel.bottomSheetListeners.update {
                                FoldersAndScriptsViewModel.BottomSheetListeners(
                                    displayView = {
                                        ScriptCard(script, Modifier, false)
                                    },
                                    onEditClickListener = null,
                                    onDeleteClickListener = {
                                        viewModel.onDeleteScript(script.scriptId)
                                    }
                                )
                            }
                        }
                    },
                    onClick = {
                        if (clickable) {
                            script.scriptId?.let { viewModel.onScriptClick(it) }
                        }
                    }
                ),
            shape = RectangleShape,
            colors = CardColors(Color.White,
                Color.Black,
                Color.LightGray,
                Color.DarkGray)
        ) {
            Row (modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Card(modifier = Modifier.height(113.dp)
                    .width(113.dp),
                    colors = CardColors(Color.White,
                        Color.White,
                        Color.LightGray,
                        Color.DarkGray),
                ) {
                    imageBitmap?.let { imageBitmap ->
                        Image(
                            bitmap = imageBitmap,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.height(113.dp)
                                .width(113.dp),
                            contentDescription = stringResource(R.string.script_display_image)
                        )
                    }
                }
                Column(modifier = Modifier.padding(end = 5.dp).fillMaxHeight().weight(1f),
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
                    Text(text = description?:"",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 5.dp),
                        textAlign = TextAlign.Start,
                        color = Color.Black,
                        fontSize = 12.sp) // Description
                    Row(
                        modifier = Modifier.padding(5.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        time?.let { time ->
                            Text(
                                text = time,
                                modifier = Modifier.padding(end = 5.dp),
                                textAlign = TextAlign.Start,
                                fontSize = 12.sp
                            ) // Entry Time
                        }
                        val modifier = Modifier
                        numImages?.let { numImages -> MediaIcon(numImages, Icons.Default.Image,modifier) }
                        numVideos?.let { numVideos -> MediaIcon(numVideos, Icons.Default.Videocam,modifier) }
                        numAudios?.let { numAudios -> MediaIcon(numAudios, Icons.Default.Mic,modifier) }
                        numDocuments?.let { numDocuments -> MediaIcon(numDocuments, Icons.Default.Book,modifier) }
                        numScript?.let { numScript -> MediaIcon(numScript, Icons.AutoMirrored.Filled.LibraryBooks,modifier) }
                        date?.let { date ->
                            Text(
                                text = date,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                fontSize = 12.sp
                            ) // Entry Date
                        }
                    }
                    //Text() // Entry Size
                }
            }
        }
    }

    @Composable
    private fun MediaIcon(numMedia:Int, icon: ImageVector, modifier: Modifier = Modifier){
        if (numMedia>0) {
            Row(modifier,
                verticalAlignment = Alignment.CenterVertically){
                Icon(
                    imageVector = icon,
                    modifier = Modifier.size(10.dp),
                    contentDescription = null
                )
                Text(
                    text = numMedia.toString(),
                    fontSize = 10.sp
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
        val context = LocalContext.current
        val logoPadding = with(LocalDensity.current) {
            lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
        }
        var image by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(imageUri) {
            image = imageUri?.let { AppResources.getBitmapFromUri(context, imageUri) }
                ?.asImageBitmap()
                ?: AppResources.getBitmapFromUri(
                    context,
                    AppResources.getUriFromDrawable(
                        context,
                        R.drawable.ic_stars_black_24dp
                    )
                )?.asImageBitmap()
        }

        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = Elevation
        ) {
            Box (modifier = Modifier.fillMaxSize()) {
                //#region Background Image
                image?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = stringResource(R.string.folder_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = progress * Alpha
                            }
                    )
                }
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

    private fun setScrollPosition(position: Int) {
        /*val layoutManager = binding.foldersAndScriptsList.layoutManager as LinearLayoutManager
        binding.foldersAndScriptsList.post {
            layoutManager.scrollToPosition(position)
        }*/
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
                viewModel.prepareToClose() {}
            }
        }
        super.onPause()
    }
}