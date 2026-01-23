package com.thando.accountable.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val goal by viewModel.goal.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val imageHeight = (
            (LocalResources.current.displayMetrics.heightPixels/3)
                    /LocalResources.current.displayMetrics.density
            ).dp

    BackHandler {
        scope.launch { viewModel.closeTasks() }
    }

    DisposableEffect(Unit) {
        onDispose {

        }
    }

    AccountableTheme {
        goal?.let { goal ->
            val goalUri by goal.getUri(context).collectAsStateWithLifecycle(null)
            val goalColour by remember { goal.colour }
            val selectedTab by remember { goal.selectedTab }
            val bottomSheetType by remember { viewModel.bottomSheetType }

            var image by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(goalUri) {
                image = goalUri?.let { imageUri ->
                    AppResources.getBitmapFromUri(
                        context,
                        imageUri
                    )
                }?.asImageBitmap() ?: ImageBitmap(1, 1)
            }
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Box(Modifier.fillMaxWidth()) {
                        image?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = stringResource(R.string.goal_display_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        LargeTopAppBar(
                            modifier = Modifier,
                            expandedHeight = imageHeight,
                            navigationIcon = {
                                IconButton(
                                    modifier = Modifier,
                                    onClick = { scope.launch { viewModel.closeTasks() }}
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.tasks_navigate_back_button),
                                        tint = Color(goalColour)
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (goalUri!=null) Color.Transparent
                                                else Color(goalColour),
                                titleContentColor = Color.White,
                                scrolledContainerColor = if (goalUri!=null) Color.Transparent
                                                else Color(goalColour)
                            ),
                            title = { Text(
                                stringResource(R.string.goal),
                                color = Color(goalColour)
                            ) },
                            actions = {
                                TextButton(
                                    onClick = {
                                        when (selectedTab) {
                                            Goal.GoalTab.TASKS -> viewModel.addTask()
                                            Goal.GoalTab.DELIVERABLES -> viewModel.addDeliverable()
                                        }
                                    },
                                ) {
                                    Text(
                                        text = when (selectedTab){
                                            Goal.GoalTab.TASKS -> stringResource(R.string.add_task)
                                            Goal.GoalTab.DELIVERABLES -> stringResource(R.string.add_deliverable)
                                        },
                                        color = Color(goalColour)
                                    )
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                TasksFragmentView(
                    viewModel,
                    mainActivityViewModel,
                    goal,
                    modifier = Modifier.padding(innerPadding)
                )
                bottomSheetType?.let { bottomSheetType ->
                    val sheetState = rememberModalBottomSheetState()
                    ModalBottomSheet(
                        onDismissRequest = {
                            viewModel.dismissBottomSheet()
                        },
                        sheetState = sheetState
                    ) {
                        when (bottomSheetType) {
                            Goal.GoalTab.TASKS -> {
                                AddTaskView(
                                    viewModel,
                                    mainActivityViewModel
                                )
                            }
                            Goal.GoalTab.DELIVERABLES -> {
                                AddDeliverableView(
                                    viewModel,
                                    mainActivityViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksFragmentView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel,
    goal: Goal,
    modifier: Modifier = Modifier
) {
    val tabListState = remember { goal.tabListState }
    val goalTitle = remember { goal.goal }
    var selectedTab by remember { goal.selectedTab }
    val tabs = listOf(stringResource(R.string.tasks), stringResource(R.string.deliverables))
    val goalColour by remember { goal.colour }

    LazyColumn(
        state = tabListState,
        modifier = modifier.fillMaxSize()
    ) {
        item( key = "The Top") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                elevation = CardDefaults.cardElevation(),
                colors = CardColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                ),
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Text(
                        text = goalTitle.text.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp, horizontal = 5.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
        stickyHeader {
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab.ordinal == index,
                        onClick = { selectedTab = Goal.GoalTab.entries[index] },
                        text = { Text(title) },
                        selectedContentColor = Color(goalColour),
                        unselectedContentColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        when (selectedTab) {
            Goal.GoalTab.TASKS -> {}
            Goal.GoalTab.DELIVERABLES -> {}
        }
    }
}

@Composable
fun AddTaskView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(),
        colors = CardColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.LightGray,
            disabledContentColor = Color.DarkGray
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = stringResource(
                    R.string.add,stringResource(R.string.task)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 5.dp),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(
                modifier = Modifier.padding(horizontal = 5.dp)
                    .align(Alignment.CenterEnd),
                onClick = { viewModel.processBottomSheetAdd() }
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.add_task_or_deliverable),
                    tint = Color.Green
                )
            }
        }
    }
    val bottomSheetLazyListState by viewModel.sheetListState.collectAsStateWithLifecycle()
    bottomSheetLazyListState?.let { bottomSheetLazyListState ->
        LazyColumn(
            state = bottomSheetLazyListState,
            modifier = Modifier.fillMaxSize()
        ) {

        }
    }
}

@Composable
fun AddDeliverableView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(),
        colors = CardColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.LightGray,
            disabledContentColor = Color.DarkGray
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = stringResource(
                    R.string.add,stringResource(R.string.deliverable)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 5.dp),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(
                modifier = Modifier.padding(horizontal = 5.dp)
                    .align(Alignment.CenterEnd),
                onClick = { viewModel.processBottomSheetAdd() }
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.add_task_or_deliverable),
                    tint = Color.Green
                )
            }
        }
    }
    val bottomSheetLazyListState by viewModel.sheetListState.collectAsStateWithLifecycle()
    bottomSheetLazyListState?.let { bottomSheetLazyListState ->
        LazyColumn(
            state = bottomSheetLazyListState,
            modifier = Modifier.fillMaxSize()
        ) {

        }
    }
}