package com.thando.accountable.fragments

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Script
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

private val bottomBarHeightFraction = 0.14f
private val barColor = Color(red = 255f, green = 255f, blue = 255f, alpha = 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksView( viewModel : BooksViewModel, mainActivityViewModel: MainActivityViewModel) {
    mainActivityViewModel.enableDrawer()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.prepareToClose {}
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var navigationIcon by remember { mutableStateOf<(@Composable (Modifier) -> Unit)?>(null) }
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    if (viewModel.intentString == null) {
        //WindowCompat.setDecorFitsSystemWindows(mainActivity.window, false)
        navigationIcon = if (viewModel.folderIsScripts()) {
            {
                IconButton(
                    modifier = Modifier,
                    onClick = { coroutineScope.launch { mainActivityViewModel.toggleDrawer() } })
                {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.navigation_drawer_button),
                        tint = Color.White
                    )
                }
            }
        }
        else {
            {
                IconButton(
                    modifier = Modifier,
                    onClick = {
                        if (folder!=null) viewModel.onBackPressed()
                        else viewModel.navigateToHome()
                    })
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back_to_home_fragment),
                        tint = Color.White
                    )
                }
            }
        }

        if (viewModel.folderIsScripts())
            mainActivityViewModel.enableDrawer()
        else mainActivityViewModel.disableDrawer()
    } else {
        mainActivityViewModel.disableDrawer()
    }

    val booksString = stringResource(R.string.books)
    val goalsString = stringResource(R.string.goals)
    AccountableTheme {
        val folderName = folder?.folderName?.let { remember { it } }
            ?: remember {
                TextFieldState(
                    if (viewModel.folderIsScripts()) booksString
                    else goalsString
                )
            }

        BackHandler(folder!=null || !viewModel.folderIsScripts()) {
            if (!viewModel.onBackPressed()) viewModel.navigateToHome()
        }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        val showScriptsParent by viewModel.showScripts.collectAsStateWithLifecycle()
        val showScripts by showScriptsParent?.collectAsStateWithLifecycle() ?: MutableStateFlow(
            false
        ).collectAsStateWithLifecycle()
        val orderIconParent by viewModel.folderOrder.collectAsStateWithLifecycle()
        val orderIconAscending by orderIconParent?.collectAsStateWithLifecycle()
            ?: MutableStateFlow(false).collectAsStateWithLifecycle()

        val imageHeight = (
                (LocalResources.current.displayMetrics.heightPixels / 3)
                        / LocalResources.current.displayMetrics.density
                ).dp

        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
        val context = LocalContext.current

        var result by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }
        LaunchedEffect(folder, appSettings) {
            result = if (folder != null) folder!!.getUri(context)
            else if (appSettings != null) appSettings!!.getUri(context)
            else MutableStateFlow(null)
        }
        val imageUri by result.collectAsStateWithLifecycle(null)

        var image by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(imageUri) {
            image = imageUri?.let { imageUri -> AppResources.getBitmapFromUri(context, imageUri) }
                ?.asImageBitmap()
                ?: AppResources.getBitmapFromUri(
                    context,
                    AppResources.getUriFromDrawable(
                        context,
                        R.mipmap.ic_launcher
                    )
                )?.asImageBitmap()
        }
        Scaffold(
            floatingActionButton = if (viewModel.intentString == null) {
                @Composable {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch {
                            viewModel.addFolderScript()
                        } },
                        modifier = Modifier.padding(16.dp)
                            .testTag("BooksFloatingActionButton"),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(if(showScripts){
                                if (viewModel.folderIsScripts()) R.string.add_script
                                else R.string.add_goal
                            } else R.string.add_folder)
                        )
                    }
                }
            } else {
                @Composable {}
            },
            floatingActionButtonPosition = FabPosition.End,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Box(Modifier.fillMaxWidth()) {
                    image?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = stringResource(R.string.folder_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    LargeTopAppBar(
                        expandedHeight = imageHeight,
                        title = {
                            Text(
                                folderName.text.toString(),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            navigationIcon?.invoke(Modifier)
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            scrolledContainerColor = Color.Transparent
                        ),
                        actions = {
                            if (viewModel.folderIsScripts()){
                                IconButton(
                                    modifier = Modifier,
                                    onClick = { coroutineScope.launch { viewModel.search() } })
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.search),
                                        tint = Color.White
                                    )
                                }
                            }
                            IconButton(
                                modifier = Modifier,
                                onClick = { coroutineScope.launch { viewModel.switchFolderOrder() } })
                            {
                                Icon(
                                    imageVector = if (orderIconAscending) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = stringResource(R.string.change_entry_order),
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                modifier = Modifier.testTag("BooksSwitchFolderScriptButton"),
                                onClick = { viewModel.switchFolderScript() })
                            {
                                Icon(
                                    imageVector = if (showScripts) {
                                        if (viewModel.folderIsScripts())
                                            Icons.AutoMirrored.Filled.LibraryBooks
                                        else ImageVector.vectorResource(R.drawable.ic_stars_black_24dp)
                                    }
                                    else Icons.Default.Folder,
                                    contentDescription = stringResource(R.string.switch_between_folder_and_script_button),
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            FoldersAndScriptsFragmentView(
                modifier = Modifier.padding(innerPadding),
                scrollBehavior,
                viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersAndScriptsFragmentView(modifier: Modifier = Modifier,
                                  scrollBehavior: TopAppBarScrollBehavior,
                                  viewModel : BooksViewModel) {
    val scrollStateParent by viewModel.scrollStateParent.collectAsStateWithLifecycle()
    scrollStateParent?.let { scrollStateParent ->
        val gridState = rememberLazyGridState(
            scrollStateParent.firstVisibleItemIndex,
            scrollStateParent.firstVisibleItemScrollOffset
        )

        LaunchedEffect(
            gridState.firstVisibleItemIndex,
            gridState.firstVisibleItemScrollOffset
        ) {
            scrollStateParent.requestScrollToItem(
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset
            )
        }

        val listShown by viewModel.listShown.collectAsStateWithLifecycle()

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val bottomSheet by viewModel.bottomSheetListeners.collectAsStateWithLifecycle()
        var folderHeight by remember { mutableStateOf(0.dp) }
        var scriptHeight by remember { mutableStateOf(0.dp) }
        val density = LocalResources.current.displayMetrics.density
        val activity = LocalActivity.current
        val scope = rememberCoroutineScope()

        when (listShown) {
            Folder.FolderListType.FOLDERS -> {
                val foldersListFlow by viewModel.foldersList.collectAsStateWithLifecycle()
                val foldersList by (foldersListFlow?:MutableStateFlow(emptyList())).collectAsStateWithLifecycle(emptyList())

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(0.dp),
                    modifier = modifier.fillMaxSize()
                ) {
                    items(items = foldersList) { folder ->
                        FolderCard(
                            folder = folder,
                            modifier = Modifier
                                .padding(2.dp)
                                .onGloballyPositioned {
                                    if (folderHeight == 0.dp) folderHeight =
                                        (it.size.height / density).dp
                                },
                            viewModel = viewModel
                        )
                    }
                }
            }

            Folder.FolderListType.SCRIPTS -> {
                val scriptsListFlow by viewModel.scriptsList.collectAsStateWithLifecycle()
                val scriptsList by (scriptsListFlow?:MutableStateFlow(emptyList())).collectAsStateWithLifecycle(emptyList())
                LazyColumn(
                    state = scrollStateParent,
                    contentPadding = PaddingValues(0.dp),
                    modifier = modifier.fillMaxSize()
                ) {
                    items(items = scriptsList, key = {it.scriptId?:Random.nextLong()}) { script ->
                        ScriptCard(
                            script = script,
                            modifier = Modifier
                                .padding(2.dp)
                                .fillMaxWidth()
                                .padding(2.dp)
                                .onGloballyPositioned {
                                    if (scriptHeight == 0.dp) scriptHeight =
                                        (it.size.height / density).dp
                                },
                            getScriptContentPreview = { viewModel.getScriptContentPreview(it) },
                            onLongClick = {
                                if (viewModel.setOnLongClick()) {
                                    viewModel.bottomSheetListeners.update {
                                        BooksViewModel.BottomSheetListeners(
                                            displayView = {
                                                ScriptCard(
                                                    script,
                                                    modifier = Modifier,
                                                    clickable = false,
                                                    onLongClick = {},
                                                    onClick = {},
                                                    getScriptContentPreview = { viewModel.getScriptContentPreview(it) }
                                                )
                                            },
                                            onEditClickListener = null,
                                            onDeleteClickListener = {
                                                scope.launch {
                                                    viewModel.onDeleteScript(script.scriptId)
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            onClick = {
                                script.scriptId?.let { viewModel.onScriptClick(it, activity) }
                            }
                        )
                    }
                }
            }

            Folder.FolderListType.GOALS -> {
                val goalsListStateFlow by viewModel.goalsList.collectAsStateWithLifecycle()
                goalsListStateFlow?.let { goalsListFlow ->
                    val goalsList by goalsListFlow.collectAsStateWithLifecycle(emptyList())
                    LazyColumn(
                        state = scrollStateParent,
                        contentPadding = PaddingValues(0.dp),
                        modifier = modifier.fillMaxSize()
                    ) {
                        items(items = goalsList, key = {it.id?:Random.nextLong()}) { goal ->
                            GoalCard(
                                goal = goal,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .onGloballyPositioned {
                                        if (scriptHeight == 0.dp) scriptHeight =
                                            (it.size.height / density).dp
                                    },
                                getGoalContentPreview = { viewModel.getGoalContentPreview(it) },
                                onLongClick = {
                                    if (viewModel.setOnLongClick()) {
                                        viewModel.bottomSheetListeners.update {
                                            BooksViewModel.BottomSheetListeners(
                                                displayView = {
                                                    GoalCard(
                                                        goal,
                                                        modifier = Modifier,
                                                        clickable = false,
                                                        onLongClick = {},
                                                        onClick = {},
                                                        getGoalContentPreview = { viewModel.getGoalContentPreview(it) }
                                                    )
                                                },
                                                onEditClickListener = {
                                                    scope.launch {
                                                        viewModel.onGoalEdit(goal.id)
                                                    }
                                                },
                                                onDeleteClickListener = {
                                                    scope.launch {
                                                        viewModel.onDeleteGoal(goal.id)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    goal.id?.let { viewModel.onGoalClick(it) }
                                }
                            )
                        }
                    }
                }
            }
        }

        var initialized by remember { viewModel.initialized }
        LaunchedEffect(initialized, folderHeight, scriptHeight) {
            if (!initialized){
                when(listShown){
                    Folder.FolderListType.FOLDERS -> {
                        if (folderHeight != 0.dp) {
                            val size =
                                gridState.firstVisibleItemIndex.toFloat() * folderHeight.value + gridState.firstVisibleItemScrollOffset.toFloat()
                            if (-size <= scrollBehavior.state.heightOffsetLimit)
                                scrollBehavior.state.heightOffset =
                                    scrollBehavior.state.heightOffsetLimit
                            else scrollBehavior.state.heightOffset = 0f
                            viewModel.initialized()
                        }
                    }
                    Folder.FolderListType.SCRIPTS,
                    Folder.FolderListType.GOALS -> {
                        if (scriptHeight != 0.dp) {
                            val size =
                                scrollStateParent.firstVisibleItemIndex * scriptHeight.value + scrollStateParent.firstVisibleItemScrollOffset
                            if (-size <= scrollBehavior.state.heightOffsetLimit)
                                scrollBehavior.state.heightOffset =
                                    scrollBehavior.state.heightOffsetLimit
                            else scrollBehavior.state.heightOffset = 0f
                            viewModel.initialized()
                        }
                    }
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(
    folder: Folder,
    modifier: Modifier = Modifier,
    clickable:Boolean = true,
    viewModel: BooksViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folderName = remember { folder.folderName }
    var folderUriStateFlow by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }

    val numFolders by folder.numFolders.collectAsStateWithLifecycle(0)
    val numScripts by folder.numScripts.collectAsStateWithLifecycle(0)
    val numGoals by folder.numGoals.collectAsStateWithLifecycle(0)


    LaunchedEffect(folder) {
        viewModel.getFolderContentPreview(folder)
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
        modifier = modifier
            .aspectRatio(0.66f)
            .combinedClickable(
                onLongClick = {
                    if (clickable && viewModel.setOnLongClick()) {
                        viewModel.bottomSheetListeners.update {
                            BooksViewModel.BottomSheetListeners(
                                displayView = {
                                    FolderCard(folder, Modifier, false, viewModel)
                                },
                                onEditClickListener = {
                                    viewModel.onFolderEdit(folder.folderId)
                                },
                                onDeleteClickListener = {
                                    scope.launch {
                                        viewModel.onDeleteFolder(folder.folderId)
                                    }
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
                contentDescription = folderName.text.toString(),
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
            BottomBar(folderName.text.toString())
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
    getScriptContentPreview: (Long) -> AccountableRepository.ContentPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable:Boolean = true
) {
    var contentPreviewAsync: Job? = null
    DisposableEffect(Unit) {
        onDispose {
            contentPreviewAsync?.cancel()
        }
    }

    var contentPreview by remember { mutableStateOf<AccountableRepository.ContentPreview?>(null) }
    val context = LocalContext.current
    var uriStateFlow by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }

    LaunchedEffect(script) {
        uriStateFlow = script.getUri(context)
        contentPreview = script.scriptId?.let { getScriptContentPreview(it) }
        contentPreview?.init {
            contentPreviewAsync = null
        }
    }
    val scriptUri by uriStateFlow.collectAsStateWithLifecycle()

    val scriptDateTime by remember { script.scriptDateTime }
    val title = remember { script.scriptTitle }
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
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .combinedClickable(
                onLongClick = {
                    if (clickable) {
                        onLongClick()
                    }
                },
                onClick = {
                    if (clickable) {
                        onClick()
                    }
                }
            ),
        shape = RectangleShape,
        colors = CardColors(Color.White,
            Color.Black,
            Color.LightGray,
            Color.DarkGray)
    ) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)) {
            Card(modifier = Modifier
                .height(113.dp)
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
                        modifier = Modifier
                            .height(113.dp)
                            .width(113.dp),
                        contentDescription = stringResource(R.string.script_display_image)
                    )
                }
            }
            Column(modifier = Modifier
                .padding(end = 5.dp)
                .fillMaxHeight()
                .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title.text.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp) // Title
                Text(text = description?:"",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 5.dp),
                    textAlign = TextAlign.Start,
                    color = Color.Black,
                    fontSize = 12.sp) // Description
                Row(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = AppResources.getTime(scriptDateTime),
                        modifier = Modifier.padding(end = 5.dp),
                        textAlign = TextAlign.Start,
                        fontSize = 12.sp
                    ) // Entry Time
                    val modifier = Modifier
                    numImages?.let { numImages -> MediaIcon(numImages, Icons.Default.Image,modifier) }
                    numVideos?.let { numVideos -> MediaIcon(numVideos, Icons.Default.Videocam,modifier) }
                    numAudios?.let { numAudios -> MediaIcon(numAudios, Icons.Default.Mic,modifier) }
                    numDocuments?.let { numDocuments -> MediaIcon(numDocuments, Icons.Default.Book,modifier) }
                    numScript?.let { numScript -> MediaIcon(numScript, Icons.AutoMirrored.Filled.LibraryBooks,modifier) }

                    Text(
                        text = AppResources.getFullDate(context, scriptDateTime),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp
                    ) // Entry Date
                }
                //Text() // Entry Size
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    goal: Goal,
    getGoalContentPreview: (Long) -> AccountableRepository.GoalContentPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable:Boolean = true
) {
    var contentPreviewAsync: Job? = null
    DisposableEffect(Unit) {
        onDispose {
            contentPreviewAsync?.cancel()
        }
    }

    var contentPreview by remember { mutableStateOf<AccountableRepository.GoalContentPreview?>(null) }
    val context = LocalContext.current
    var uriStateFlow by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }

    LaunchedEffect(goal) {
        uriStateFlow = goal.getUri(context)
        contentPreview = goal.id?.let { getGoalContentPreview(it) }
        contentPreview?.init {
            contentPreviewAsync = null
        }
    }
    val scriptUri by uriStateFlow.collectAsStateWithLifecycle()

    val goalInitialDateTime by remember {
        Converters().toLocalDateTime(
            goal.initialDateTime
        )
    }
    val goalDateOfCompletion by remember {
        Converters().toLocalDateTime(
            goal.dateOfCompletion
        )
    }
    val title = remember { TextFieldState(goal.goal) }
    val location = remember { TextFieldState(goal.location) }
    val goalColour by remember { mutableIntStateOf(goal.colour) }
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
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .combinedClickable(
                onLongClick = {
                    if (clickable) {
                        onLongClick()
                    }
                },
                onClick = {
                    if (clickable) {
                        onClick()
                    }
                }
            ),
        shape = RectangleShape,
        colors = CardColors(Color.White,
            Color.Black,
            Color.LightGray,
            Color.DarkGray)
    ) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)) {
            Card(modifier = Modifier
                .height(113.dp)
                .width(113.dp),
                colors = CardColors(Color(goalColour),
                    Color.White,
                    Color.LightGray,
                    Color.DarkGray),
            ) {
                imageBitmap?.let { imageBitmap ->
                    Image(
                        bitmap = imageBitmap,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(113.dp)
                            .width(113.dp).padding(2.dp),
                        contentDescription = stringResource(R.string.script_display_image)
                    )
                }
            }
            Column(modifier = Modifier
                .padding(end = 5.dp)
                .fillMaxHeight()
                .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title.text.toString(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(5.dp),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp) // Title
                Text(text = stringResource(
                    R.string.location_with_string,
                    location.text.toString()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp),
                    textAlign = TextAlign.Start,
                    color = Color.Black,
                    fontSize = 12.sp) // Location
                Row(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.start_date,
                            AppResources.getFullDate(context, goalInitialDateTime)),
                        modifier = Modifier.padding(end = 5.dp),
                        textAlign = TextAlign.Start,
                        fontSize = 12.sp
                    )
                    val modifier = Modifier
                    numImages?.let { numImages -> MediaIcon(numImages, Icons.Default.Image,modifier) }
                    numVideos?.let { numVideos -> MediaIcon(numVideos, Icons.Default.Videocam,modifier) }
                    numAudios?.let { numAudios -> MediaIcon(numAudios, Icons.Default.Mic,modifier) }
                    numDocuments?.let { numDocuments -> MediaIcon(numDocuments, Icons.Default.Book,modifier) }
                    numScript?.let { numScript -> MediaIcon(numScript, Icons.AutoMirrored.Filled.LibraryBooks,modifier) }
                    goalDateOfCompletion?.let { goalDateOfCompletion ->
                        Text(
                            text = AppResources.getFullDate(context, goalDateOfCompletion),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontSize = 12.sp
                        )
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