package com.thando.accountable.fragments

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.AppResources.Companion.getStandardDate
import com.thando.accountable.AppResources.Companion.getTime
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Goal.GoalTab
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.ui.MenuItemData
import com.thando.accountable.ui.cards.ColourPickerDialog
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun TaskView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    val goal by viewModel.goal.collectAsStateWithLifecycle(null)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val imageHeight = (
            (LocalResources.current.displayMetrics.heightPixels/3)
                    /LocalResources.current.displayMetrics.density
            ).dp

    BackHandler {
        scope.launch { viewModel.closeTasks() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    scope.launch {
                        viewModel.saveTask()
                        viewModel.saveDeliverable()
                        viewModel.saveMarker()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    if (activity?.isChangingConfigurations == false) {
                        runBlocking {
                            viewModel.dismissBottomSheet()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AccountableTheme {
        goal?.let { goal ->
            val image by goal.getUri(context).mapLatest {
                withContext(MainActivity.IO) {
                    it?.let { imageUri ->
                        AppResources.getBitmapFromUri(
                            context,
                            imageUri
                        )
                    }?.asImageBitmap()
                }
            }.collectAsStateWithLifecycle(null)
            val goalColour by remember { mutableIntStateOf( goal.colour) }

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
                                containerColor = if (image!=null) Color.Transparent
                                                else Color(goalColour),
                                titleContentColor = Color.White,
                                scrolledContainerColor = if (image!=null) Color.Transparent
                                                else Color(goalColour)
                            ),
                            title = { Text(
                                stringResource(R.string.goal),
                                color = Color(goalColour)
                            ) },
                            actions = {
                                TextButton(
                                    onClick = {
                                        when (GoalTab.valueOf(goal.selectedTab)) {
                                            GoalTab.TASKS -> scope.launch {
                                                viewModel.addTask()
                                            }
                                            GoalTab.DELIVERABLES -> scope.launch {
                                                viewModel.addDeliverable()
                                            }
                                            GoalTab.MARKERS -> scope.launch {
                                                viewModel.addMarker()
                                            }
                                        }
                                    },
                                ) {
                                    Text(
                                        text = when (GoalTab.valueOf(goal.selectedTab)){
                                            GoalTab.TASKS -> stringResource(R.string.add_task)
                                            GoalTab.DELIVERABLES -> stringResource(R.string.add_deliverable)
                                            GoalTab.MARKERS -> stringResource(R.string.add_marker)
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
                    goal,
                    modifier = Modifier.padding(innerPadding)
                )
                TaskDeliverableMarkerBottomSheet(
                    parentGoal = viewModel.goal,
                    bottomSheetTypeState = viewModel.bottomSheetType,
                    dismissBottomSheet = viewModel::dismissBottomSheet,
                    triedToSaveInput = viewModel.triedToSave,
                    colourPickerDialog = viewModel.colourPickerDialog,
                    processBottomSheetAdd = viewModel::processBottomSheetAdd,
                    showErrorMessage = viewModel.showErrorMessage,
                    errorMessage = viewModel.errorMessage,
                    pickColour = viewModel::pickColour,
                    addTimeBlock = viewModel::addTimeBlock,
                    deleteTimeBlock = viewModel::deleteTimeBlock,
                    updateTimeBlock = viewModel::updateTimeBlock,
                    updateDeliverable = viewModel::updateDeliverable,
                    deleteTaskClicked = viewModel::deleteTaskClicked,
                    originalTask = viewModel.originalTask,
                    task = viewModel.task,
                    deleteDeliverableClicked = viewModel::deleteDeliverableClicked,
                    originalDeliverable = viewModel.originalDeliverable,
                    deliverable = viewModel.deliverable,
                    deleteMarkerClicked = viewModel::deleteMarkerClicked,
                    originalMarker = viewModel.originalMarker,
                    marker = viewModel.marker,
                    updateTask = viewModel::updateTask,
                    updateMarker = viewModel::updateMarker
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDeliverableMarkerBottomSheet(
    bottomSheetTypeState: MutableStateFlow<GoalTab?>,
    dismissBottomSheet: suspend () -> Unit,
    triedToSaveInput: MutableStateFlow<Boolean>,
    colourPickerDialog: ColourPickerDialog,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    pickColour: (Color?) -> Unit,
    updateTask: suspend (Task) -> Unit,
    addTimeBlock: suspend () -> Unit,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateDeliverable: suspend (Deliverable) -> Unit,
    deleteTaskClicked: (suspend () -> Unit)?=null,
    originalTask: MutableStateFlow<Flow<Task?>?>?=null,
    task: Flow<Task?>?=null,
    deleteDeliverableClicked: (suspend () -> Unit)?=null,
    originalDeliverable: Flow<Deliverable?>?=null,
    deliverable: Flow<Deliverable?>?=null,
    parentGoal: Flow<Goal?>,
    deleteMarkerClicked: (suspend () -> Unit)?=null,
    originalMarker: MutableStateFlow<Flow<Marker?>?>?=null,
    marker: Flow<Marker?>?=null,
    updateMarker: suspend (Marker) -> Unit
){
    val scope = rememberCoroutineScope()
    val bottomSheetType by bottomSheetTypeState.collectAsStateWithLifecycle()
    bottomSheetType?.let { bottomSheetType ->
        val sheetState = rememberModalBottomSheetState()
        MainActivity.ModalBottomSheet(
            onDismissRequest = {
                scope.launch { dismissBottomSheet() }
            },
            sheetState = sheetState,
            modifier = Modifier.testTag("TasksFragmentDeliverablesBottomSheet")
        ) {
            when (bottomSheetType) {
                GoalTab.TASKS -> {
                    if (originalTask != null && task != null && deleteTaskClicked != null)
                    AddTaskView(
                        originalTask = originalTask,
                        taskStateFlow = task,
                        triedToSaveInput = triedToSaveInput,
                        colourPickerDialog = colourPickerDialog,
                        processBottomSheetAdd = processBottomSheetAdd,
                        showErrorMessage = showErrorMessage,
                        errorMessage = errorMessage,
                        pickColour = pickColour,
                        addTimeBlock = addTimeBlock,
                        deleteTimeBlock = deleteTimeBlock,
                        updateTimeBlock = updateTimeBlock,
                        deleteTaskClicked = deleteTaskClicked,
                        updateTask = updateTask
                    )
                }
                GoalTab.DELIVERABLES -> {
                    if (originalDeliverable != null && deliverable != null && deleteDeliverableClicked != null)
                    AddDeliverableView(
                        originalDeliverableStateFlow = originalDeliverable,
                        deliverableStateFlow = deliverable,
                        parentGoal = parentGoal,
                        triedToSaveInput = triedToSaveInput,
                        processBottomSheetAdd = processBottomSheetAdd,
                        showErrorMessage = showErrorMessage,
                        errorMessage = errorMessage,
                        addTimeBlock = addTimeBlock,
                        deleteTimeBlock = deleteTimeBlock,
                        deleteDeliverableClicked = deleteDeliverableClicked,
                        updateTimeBlock = updateTimeBlock,
                        updateDeliverable = updateDeliverable
                    )
                }
                GoalTab.MARKERS -> {
                    if (originalMarker != null && marker != null && deleteMarkerClicked != null)
                        AddMarkerView(
                            originalMarker = originalMarker,
                            marker = marker,
                            triedToSaveInput = triedToSaveInput,
                            processBottomSheetAdd = processBottomSheetAdd,
                            showErrorMessage = showErrorMessage,
                            errorMessage = errorMessage,
                            deleteMarkerClicked = deleteMarkerClicked,
                            updateMarker = updateMarker
                        )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun TasksFragmentView(
    viewModel: TaskViewModel,
    goal: Goal,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tabListState = remember {
        Converters().fromScrollStateLazy(goal.tabListState)
    }
    LaunchedEffect(tabListState) {
        snapshotFlow { tabListState.isScrollInProgress }
            .filter { !it } // only when scrolling ends
            .collect {
                viewModel.updateGoal(goal.copy(tabListState = Converters().toScrollStateLazy(tabListState)))
            }
    }
    val goalTitle = remember { TextFieldState(goal.goal) }
    val tabs = listOf(
        stringResource(R.string.tasks),
        stringResource(R.string.deliverables),
        stringResource(R.string.markers)
    )
    val goalColour by remember { mutableIntStateOf(goal.colour) }

    val tasksList by goal.goalTasks.collectAsStateWithLifecycle(emptyList())
    val deliverablesList by goal.goalDeliverables.collectAsStateWithLifecycle(emptyList())
    val markersList by goal.goalMarkers.collectAsStateWithLifecycle(emptyList())

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
                selectedTabIndex = GoalTab.valueOf(goal.selectedTab).ordinal
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = GoalTab.valueOf(goal.selectedTab).ordinal == index,
                        onClick = { scope.launch {
                            viewModel.updateGoal(goal.copy(selectedTab = GoalTab.entries[index].name))
                        } },
                        text = { Text(title) },
                        selectedContentColor = Color(goalColour),
                        unselectedContentColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        when (GoalTab.valueOf(goal.selectedTab)) {
            GoalTab.TASKS -> {
                items(items = tasksList){ task ->
                    TaskCardView(task, viewModel)
                }
            }
            GoalTab.DELIVERABLES -> {
                items(items = deliverablesList) { deliverable ->
                    DeliverableCardView(
                        deliverable,
                        viewModel::editDeliverable,
                        Modifier.fillMaxWidth()
                            .padding(3.dp)
                            .wrapContentHeight()
                    )
                }
            }
            GoalTab.MARKERS -> {
                items(items = markersList) { marker ->
                    MarkerCardView(marker, viewModel)
                }
            }
        }
    }
}

fun Color.darker(factor: Float = 0.5f): Color{
    return Color(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = alpha
    )
}

@Composable
fun TaskCardView(
    task: Task,
    viewModel: TaskViewModel
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskText = remember { TextFieldState(task.task) }
    val timeCreated by remember {
        Converters().toLocalDateTime(
            task.initialDateTime
        )
    }
    val endDateTime by remember {
        Converters().toLocalDateTime(
            task.endDateTime
        )
    }
    val location = remember { TextFieldState(task.location) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .wrapContentHeight()
            .combinedClickable(onClick = {
                scope.launch { viewModel.editTask(task) }
            }),
        elevation = CardDefaults.cardElevation(),
        colors = CardColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.LightGray,
            disabledContentColor = Color.DarkGray
        ),
        shape = RectangleShape,
        border = BorderStroke(4.dp, if (task.colour!=-1)
            Color(task.colour)
        else Color.Black)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .wrapContentHeight()
        ) {
            Text(text = taskText.text.toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 16.sp
            )
            Row(Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(
                    text = stringResource(
                        R.string.location),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Start,
                    color = if (task.colour!=-1 && Color(task.colour)!=Color.White)
                        Color(task.colour).darker()
                    else Color.Black
                )
                Text(
                    text = location.text.toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(
                    text = stringResource(R.string.status) +":",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Start,
                    color = if (task.colour!=-1 && Color(task.colour)!=Color.White)
                        Color(task.colour).darker()
                    else Color.Black
                )
                Text(
                    text = task.status,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(
                    text = stringResource(R.string.time_created),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = if (task.colour!=-1 && Color(task.colour)!=Color.White)
                        Color(task.colour).darker()
                    else Color.Black
                )
                Text(
                    text = AppResources.getTimeFullDate(
                        context,
                        timeCreated
                    )+"\n"+stringResource(
                        R.string.days_ago,
                        AppResources.getDaysFromToday(timeCreated)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(
                    text = stringResource(R.string.end_type_without_args),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textAlign = TextAlign.Start,
                    color = if (task.colour!=-1 && Color(task.colour)!=Color.White)
                        Color(task.colour).darker()
                    else Color.Black
                )
                Text(
                    text = task.endType + if (Task.TaskEndType.valueOf(task.endType) == Task.TaskEndType.DATE)
                        "\n${AppResources.getTimeFullDate(context,endDateTime)}"
                    else "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun getOnlyOneQuantityText(inputString: String) : String?{
    val regex = Regex("\\d+")
    val numbers = regex.findAll(inputString).toMutableList()
    if (numbers.size > 1) {
        val firstNumber = numbers.first()
        val builder = StringBuilder()

        builder.append(
            inputString.take(firstNumber.range.first)
        )
        builder.append(firstNumber.value)
        val afterFirst = inputString.substring(
            firstNumber.range.last + 1
        )
        builder.append(
            regex.replace(
                afterFirst, ""
            )
        )
        return builder.toString()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun AddTaskView(
    originalTask: MutableStateFlow<Flow<Task?>?>,
    taskStateFlow: Flow<Task?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    colourPickerDialog: ColourPickerDialog,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    pickColour: (Color?) -> Unit,
    updateTask: suspend(Task) -> Unit,
    addTimeBlock: suspend () -> Unit,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    deleteTaskClicked: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val originalTask by originalTask.collectAsStateWithLifecycle()
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
                text = originalTask?.let {
                    stringResource(
                        R.string.edit_with_arg,
                        stringResource(R.string.task)
                    ) }?: stringResource(
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
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterEnd),
                onClick = { scope.launch { processBottomSheetAdd() } }
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.add_task_deliverable_or_marker),
                    tint = Color.Green
                )
            }
        }
    }
    val task by taskStateFlow.collectAsStateWithLifecycle(null)
    task?.let { task ->
        val context = LocalContext.current

        val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
        var showErrorMessage by remember { showErrorMessage }
        val errorMessage by remember { errorMessage }

        val bottomSheetLazyListState = remember {
            Converters().fromScrollStateLazy(task.scrollPosition)
        }
        LaunchedEffect(bottomSheetLazyListState) {
            snapshotFlow { bottomSheetLazyListState.isScrollInProgress }
                .filter { !it } // only when scrolling ends
                .collect {
                    updateTask(task.copy(
                        scrollPosition = Converters().toScrollStateLazy(bottomSheetLazyListState)
                    ))
                }
        }
        val taskText = remember { TextFieldState(task.task) }
        val taskTextFocusRequester = remember { task.taskTextFocusRequester }
        val location = remember { TextFieldState(task.location) }
        val locationFocusRequester = remember { task.locationFocusRequester }
        val times by task.times.collectAsStateWithLifecycle(emptyList())
        val colourFocusRequester = remember { task.colourFocusRequester }
        var taskTime by remember {
            Converters().toLocalDateTime(
                task.time
            )
        }
        LaunchedEffect (showErrorMessage) {
            if (showErrorMessage) { delay(2000) // message disappears after 2 seconds
                showErrorMessage = false
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            colourPickerDialog.ColourPicker()
            AnimatedVisibility(visible = showErrorMessage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Red)
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(errorMessage),
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
            }
            LazyColumn(
                state = bottomSheetLazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Task.TaskType.entries.forEach { boxType ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .height(TopAppBarDefaults.MediumAppBarCollapsedHeight) // makes it a big square
                                    .background(
                                        if (Task.TaskType.valueOf(task.type) == boxType) Color.Blue else Color.LightGray,
                                        shape = RectangleShape
                                    )
                                    .clickable { scope.launch { updateTask(task.copy(type = boxType.name)) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(when(boxType){
                                        Task.TaskType.NORMAL -> R.string.normal
                                        Task.TaskType.QUANTITY -> R.string.quantity
                                        Task.TaskType.TIME -> R.string.time_string
                                    }),
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
                item {
                    LaunchedEffect(taskText.text) {
                        updateTask(task.copy(task = taskText.text.toString()))
                    }
                    when(Task.TaskType.valueOf(task.type)){
                        Task.TaskType.QUANTITY,
                        Task.TaskType.NORMAL -> {
                            var styledText by remember { mutableStateOf<AnnotatedString?>(null) }
                            if (Task.TaskType.valueOf(task.type) == Task.TaskType.QUANTITY){
                                LaunchedEffect(Unit) {
                                    getOnlyOneQuantityText(taskText.text.toString())?.let{
                                        if (it != task.task) updateTask(task.copy(task = it))
                                    }
                                }
                                LaunchedEffect(taskText.text) {
                                    updateTask(task.copy(quantity =
                                        Regex("\\d+").find(taskText.text)?.value?.toLongOrNull()?:0
                                    ))
                                }
                                styledText = buildAnnotatedString {
                                    val regex = Regex("\\d+")
                                    val match = regex.find(taskText.text)
                                    if (match != null) {
                                        val start = match.range.first
                                        val end = match.range.last + 1
                                        // Text before number
                                        append(taskText.text.substring(0, start))
                                        // Highlighted number
                                        withStyle(
                                            SpanStyle(
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(taskText.text.substring(start, end))
                                        }
                                        // Text after number
                                        append(taskText.text.substring(end))
                                    } else {
                                        append(stringResource(R.string.please_enter_a_quantity))
                                    }
                                }
                            } else styledText = null

                            OutlinedTextField(
                                state = taskText,
                                supportingText = {styledText?.let { styledText -> Text(styledText)}},
                                label = { Text(stringResource(
                                    if (Task.TaskType.valueOf(task.type) == Task.TaskType.QUANTITY) R.string.task_with_quantity
                                        else R.string.task
                                )) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp)
                                    .focusRequester(taskTextFocusRequester),
                                trailingIcon = {
                                    if (triedToSave && taskText.text.isEmpty()){
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = stringResource(R.string.empty_field),
                                            tint = Color.Red
                                        )
                                    }
                                },
                                inputTransformation = InputTransformation {
                                    if(Task.TaskType.valueOf(task.type) == Task.TaskType.QUANTITY) {
                                        val cleaned = getOnlyOneQuantityText(toString())
                                        cleaned?.let { cleaned ->
                                            val start = selection.start.coerceIn(0, cleaned.length)
                                            val end = selection.end.coerceIn(0, cleaned.length)

                                            replace(0, length, cleaned)
                                            // Fix cursor if out of bounds
                                            selection = TextRange(
                                                start - 1,
                                                end - 1
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        Task.TaskType.TIME -> {
                            val firstText = rememberTextFieldState("")
                            val secondText = rememberTextFieldState("")

                            LaunchedEffect(Unit) {
                                val initialTime = AppResources.getDurationString(context, taskTime)
                                val index = taskText.text.toString().indexOf(initialTime)
                                if (index != -1) {
                                    val before = taskText.text.toString().substring(0, index)
                                    val after = taskText.text.toString().substring(index + initialTime.length)
                                    firstText.setTextAndPlaceCursorAtEnd(before.trim())
                                    secondText.setTextAndPlaceCursorAtEnd(after.trim())
                                }
                            }

                            val datePicked = LocalDateTime.now().withHour(0).withMinute(0)
                            val context = LocalContext.current

                            LaunchedEffect(firstText.text, taskTime, secondText.text) {
                                taskText.edit {
                                    replace(0, length,
                                        "${firstText.text.trim()} ${
                                            AppResources.getDurationString(context, taskTime)
                                        } ${secondText.text.trim()}")
                                }
                            }

                            val buttonDurationPick = remember { mutableStateOf(false) }
                            if (buttonDurationPick.value) {
                                TimeDurationPicker(datePicked, taskTime) { hours, minutes ->
                                    taskTime = taskTime.withHour(hours).withMinute(minutes)
                                    scope.launch {
                                        updateTask(task.copy(time = Converters().fromLocalDateTime(taskTime)))
                                    }
                                    buttonDurationPick.value = false
                                }
                            }
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                maxItemsInEachRow = Int.MAX_VALUE // allow natural wrapping
                            ) {
                                OutlinedTextField(
                                    state = firstText,
                                    label = { Text(stringResource(R.string.task_with_time)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { buttonDurationPick.value = true },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp,Color.DarkGray)
                                ) {
                                    Text(
                                        text = AppResources.getDurationString(taskTime),
                                        modifier = Modifier
                                    )
                                }
                                OutlinedTextField(
                                    state = secondText,
                                    placeholder = { Text("Second part") },
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {Text(taskText.text.toString())}
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                item {
                    LaunchedEffect(location.text) {
                        updateTask(task.copy(location = location.text.toString()))
                    }
                    OutlinedTextField(
                        state = location,
                        label = { Text(stringResource(R.string.location)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp)
                            .focusRequester(locationFocusRequester),
                        trailingIcon = {
                            if (triedToSave && location.text.isEmpty()){
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.empty_field),
                                    tint = Color.Red
                                )
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.colour != -1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(end = 3.dp)
                                    .background(
                                        Color(task.colour),
                                        shape = RectangleShape
                                    )
                                    .weight(1f)
                            )
                        }
                        Button(
                            onClick = { pickColour(
                                Color(task.colour)
                            ) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .weight(2f)
                                .focusRequester(colourFocusRequester),
                            shape = RectangleShape
                        ) {
                            Row {
                                Text(stringResource(R.string.pick_colour))
                                if (triedToSave && task.colour==-1){
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.empty_field),
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                item {
                    var pickedDate by remember { mutableStateOf(
                        LocalDateTime.ofEpochSecond(task.endDateTime/1000,0,
                        ZoneOffset.UTC)) }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = Converters().fromLocalDateTime(pickedDate)
                    )
                    var buttonDatePick by remember { mutableStateOf(false) }
                    var buttonTimePick by remember { mutableStateOf(false) }
                    if (buttonDatePick) {
                        PickDate(stateDate){ pickTime ->
                            buttonDatePick = false
                            buttonTimePick = pickTime
                        }
                    }
                    if (buttonTimePick) {
                        PickTime(stateTime){
                            buttonTimePick = false
                            pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                        }
                    }
                    var showEndTypeOptions by remember { mutableStateOf(false) }
                    var endTypeOptions by remember { mutableStateOf(listOf<MenuItemData>())}
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        onClick = {
                            endTypeOptions = listOf(
                                MenuItemData(Task.TaskEndType.UNDEFINED.name){
                                    scope.launch {
                                        updateTask(task.copy(endType = Task.TaskEndType.UNDEFINED.name))
                                    }
                                },
                                MenuItemData(Task.TaskEndType.DATE.name){
                                    scope.launch {
                                        updateTask(task.copy(endType = Task.TaskEndType.DATE.name))
                                    }
                                    buttonDatePick = true
                                },
                                MenuItemData(Task.TaskEndType.GOAL.name){
                                    scope.launch {
                                        updateTask(task.copy(endType = Task.TaskEndType.GOAL.name))
                                    }
                                },
                                MenuItemData(Task.TaskEndType.DELIVERABLE.name){
                                    scope.launch {
                                        updateTask(task.copy(endType = Task.TaskEndType.DELIVERABLE.name))
                                    }
                                },
                                MenuItemData(Task.TaskEndType.MARKER.name){
                                    scope.launch {
                                        updateTask(task.copy(endType = Task.TaskEndType.MARKER.name))
                                    }
                                }
                            )
                            showEndTypeOptions = true
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp,Color.DarkGray)
                    ) {
                        if (showEndTypeOptions) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showEndTypeOptions = false }
                            ) {
                                endTypeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.text) },
                                        onClick = {
                                            option.onClick.invoke()
                                            showEndTypeOptions = false
                                        }
                                    )
                                }
                            }
                        }
                        when (Task.TaskEndType.valueOf(task.endType)) {
                            Task.TaskEndType.UNDEFINED -> {
                                Text(
                                    modifier = Modifier,
                                    text = stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.undefined),
                                        ""
                                    ).trim()
                                )
                            }
                            Task.TaskEndType.DATE -> {
                                stateDate.getSelectedDate()?.let { localDate ->
                                    pickedDate = LocalDateTime.of(
                                        localDate,
                                        LocalTime.of(stateTime.hour,stateTime.minute)
                                    )
                                    Text(
                                        stringResource(
                                            R.string.end_type,
                                            stringResource(R.string.date),
                                            stringResource(
                                                R.string.end_time_and_date,
                                                getTime(pickedDate),
                                                getStandardDate(context, pickedDate)
                                            )
                                        )
                                    )
                                }?: run {
                                    Text(stringResource(R.string.pick_a_date))
                                }
                            }
                            Task.TaskEndType.GOAL -> {
                                Text(
                                    stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.goal),
                                        ""
                                    ).trim()
                                )
                            }
                            Task.TaskEndType.DELIVERABLE -> {
                                Text(
                                    stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.deliverable),
                                        ""
                                    ).trim()
                                )
                            }
                            Task.TaskEndType.MARKER -> {
                                Text(
                                    stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.marker),
                                        ""
                                    ).trim()
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                stickyHeader {
                    Button(
                        onClick = { scope.launch { addTimeBlock() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        shape = RectangleShape
                    ) { Text(stringResource(R.string.add_time_block)) }
                }
                items(items = times, key = { it.id?:Random.nextLong() }) { item ->
                    TimeInputView(
                        item,
                        triedToSaveInput,
                        deleteTimeBlock,
                        updateTimeBlock
                    )
                    if (times.indexOf(item) != times.lastIndex) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
                originalTask?.let {
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    item {
                        IconButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                                .background(color = Color.Red),
                            onClick = { scope.launch { deleteTaskClicked() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_marker)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliverableCardView(
    deliverable: Deliverable,
    editDeliverable: suspend (Deliverable) -> Unit,
    modifier: Modifier
){
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .combinedClickable(onClick = {
                scope.launch { editDeliverable(deliverable) }
            }),
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
                text = deliverable.deliverable,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun AddDeliverableView(
    originalDeliverableStateFlow: Flow<Deliverable?>,
    deliverableStateFlow: Flow<Deliverable?>,
    parentGoal: Flow<Goal?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    addTimeBlock: suspend () -> Unit,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    deleteDeliverableClicked: suspend () -> Unit,
    updateTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateDeliverable: suspend (Deliverable) -> Unit
) {
    val scope = rememberCoroutineScope()
    val originalDeliverable by originalDeliverableStateFlow.collectAsStateWithLifecycle(null)
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
                text = originalDeliverable?.let {
                    stringResource(
                        R.string.edit_with_arg,
                        stringResource(R.string.deliverable)
                    ) }?: stringResource(
                    R.string.add,stringResource(R.string.deliverable)
                ),
                modifier = Modifier
                    .testTag("TasksFragmentDeliverableTitle")
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 5.dp),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(
                modifier = Modifier
                    .testTag("TasksFragmentDeliverableProcessDeliverable")
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterEnd),
                onClick = { scope.launch { processBottomSheetAdd() } }
            ) {
                MainActivity.Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.add_task_deliverable_or_marker),
                    tint = Color.Green
                )
            }
        }
    }
    val parentGoal by parentGoal.collectAsStateWithLifecycle(null)
    val deliverable by deliverableStateFlow.collectAsStateWithLifecycle(null)
    parentGoal?.let{ parentGoal ->
        deliverable?.let { deliverable ->
            val context = LocalContext.current

            val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
            var showErrorMessage by remember { showErrorMessage }
            val errorMessage by remember { errorMessage }

            val bottomSheetLazyListState = remember {
                Converters().fromScrollStateLazy(deliverable.scrollPosition)
            }
            LaunchedEffect(bottomSheetLazyListState) {
                snapshotFlow { bottomSheetLazyListState.isScrollInProgress }
                    .filter { !it } // only when scrolling ends
                    .collect {
                        updateDeliverable(deliverable.copy(
                            scrollPosition = Converters().toScrollStateLazy(bottomSheetLazyListState)))
                    }
            }

            val deliverableText = remember { TextFieldState(deliverable.deliverable) }
            val deliverableTextFocusRequester = remember { deliverable.deliverableTextFocusRequester }
            val location = remember { TextFieldState(deliverable.location) }
            val locationFocusRequester = remember { deliverable.locationFocusRequester }
            val times by deliverable.times.collectAsStateWithLifecycle(emptyList())

            LaunchedEffect(deliverableText.text) {
                updateDeliverable(deliverable.copy(deliverable = deliverableText.text.toString()))
            }

            LaunchedEffect(location.text) {
                updateDeliverable(deliverable.copy(location = location.text.toString()))
            }

            LaunchedEffect(showErrorMessage) {
                if (showErrorMessage) {
                    delay(2000) // message disappears after 2 seconds
                    showErrorMessage = false
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = showErrorMessage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(Color.Red)
                            .padding(4.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = stringResource(errorMessage),
                            modifier = Modifier
                                .padding(4.dp)
                                .align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                LazyColumn(
                    state = bottomSheetLazyListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        OutlinedTextField(
                            state = deliverableText,
                            label = { Text(stringResource(R.string.deliverable)) },
                            modifier = Modifier
                                .testTag("TaskFragmentDeliverableDeliverableText")
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp)
                                .focusRequester(deliverableTextFocusRequester),
                            trailingIcon = {
                                if (triedToSave && deliverableText.text.isEmpty()){
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.empty_field),
                                        tint = Color.Red
                                    )
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    item {
                        OutlinedTextField(
                            state = location,
                            label = { Text(stringResource(R.string.location)) },
                            modifier = Modifier
                                .testTag("TaskFragmentDeliverableLocationText")
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp)
                                .focusRequester(locationFocusRequester),
                            trailingIcon = {
                                if (triedToSave && location.text.isEmpty()){
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.empty_field),
                                        tint = Color.Red
                                    )
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    item {
                        var pickedDate by remember {
                            Converters().toLocalDateTime(
                                deliverable.endDateTime
                            )
                        }
                        val stateTime = rememberTimePickerState(
                            pickedDate.hour,
                            pickedDate.minute,
                            true
                        )
                        val stateDate = rememberDatePickerState(
                            initialSelectedDateMillis = Converters().fromLocalDateTime(pickedDate)
                        )
                        var buttonDatePick by remember { mutableStateOf(false) }
                        var buttonTimePick by remember { mutableStateOf(false) }
                        if (buttonDatePick) {
                            PickDate(stateDate){ pickTime ->
                                buttonDatePick = false
                                buttonTimePick = pickTime
                            }
                        }
                        if (buttonTimePick) {
                            PickTime(stateTime){
                                buttonTimePick = false
                                pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                                scope.launch { updateDeliverable(deliverable.copy(endDateTime = pickedDate.toInstant(
                                    ZoneOffset.UTC).toEpochMilli())) }
                            }
                        }
                        var showEndTypeOptions by remember { mutableStateOf(false) }
                        var endTypeOptions by remember { mutableStateOf(listOf<MenuItemData>())}
                        OutlinedButton(modifier = Modifier
                            .testTag("TasksFragmentDeliverableEndTypeButton")
                            .fillMaxWidth()
                            .padding(4.dp),
                            onClick = {
                                endTypeOptions = listOf(
                                    MenuItemData(Deliverable.DeliverableEndType.UNDEFINED.name){
                                        scope.launch {
                                            updateDeliverable(deliverable.copy(endType = Deliverable.DeliverableEndType.UNDEFINED.name))
                                        }
                                    },
                                    MenuItemData(Deliverable.DeliverableEndType.DATE.name){
                                        scope.launch {
                                            updateDeliverable(deliverable.copy(endType = Deliverable.DeliverableEndType.DATE.name))
                                            buttonDatePick = true
                                        }
                                    },
                                    MenuItemData(Deliverable.DeliverableEndType.GOAL.name){
                                        scope.launch {
                                            updateDeliverable(deliverable.copy(
                                                endType = Deliverable.DeliverableEndType.GOAL.name,
                                                goalId = null
                                            ))
                                        }
                                    },
                                    MenuItemData(Deliverable.DeliverableEndType.WORK.name){
                                        scope.launch {
                                            updateDeliverable(deliverable.copy(endType = Deliverable.DeliverableEndType.WORK.name))
                                        }
                                    }
                                )
                                showEndTypeOptions = true
                            }
                        ) {
                            if (showEndTypeOptions) {
                                DropdownMenu(
                                    expanded = true,
                                    onDismissRequest = { showEndTypeOptions = false },
                                    modifier = Modifier.testTag("TasksFragmentDeliverableEndTypeDropDownMenu")
                                ) {
                                    endTypeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            modifier = Modifier.testTag("TasksFragmentDeliverableDropdownMenuItem-${option.text}"),
                                            text = { Text(option.text) },
                                            onClick = {
                                                option.onClick.invoke()
                                                showEndTypeOptions = false
                                            }
                                        )
                                    }
                                }
                            }
                            when (Deliverable.DeliverableEndType.valueOf(deliverable.endType)) {
                                Deliverable.DeliverableEndType.UNDEFINED -> {
                                    Text(
                                        stringResource(
                                            R.string.end_type,
                                            stringResource(R.string.undefined),
                                            ""
                                        )
                                    )
                                }
                                Deliverable.DeliverableEndType.DATE -> {
                                    stateDate.getSelectedDate()?.let { localDate ->
                                        pickedDate = LocalDateTime.of(
                                            localDate,
                                            LocalTime.of(stateTime.hour,stateTime.minute)
                                        )
                                        scope.launch { updateDeliverable(deliverable.copy(endDateTime = pickedDate.toInstant(
                                            ZoneOffset.UTC).toEpochMilli())) }
                                        Text(
                                            stringResource(
                                                R.string.end_type,
                                                stringResource(R.string.date),
                                                stringResource(
                                                    R.string.start_time_and_date,
                                                    getTime(pickedDate),
                                                    getStandardDate(context, pickedDate)
                                                )
                                            )
                                        )
                                    }?: run {
                                        Text(stringResource(R.string.pick_a_date))
                                    }
                                }
                                Deliverable.DeliverableEndType.GOAL -> {
                                    Text(
                                        stringResource(
                                            R.string.end_type,
                                            stringResource(R.string.goal),
                                            ""
                                        )
                                    )
                                }
                                Deliverable.DeliverableEndType.WORK -> {
                                    Text(
                                        stringResource(
                                            R.string.end_type,
                                            stringResource(R.string.work),
                                            ""
                                        )
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    item{
                        if (Deliverable.DeliverableEndType.valueOf(deliverable.endType) !=
                            Deliverable.DeliverableEndType.GOAL
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stringResource(R.string.required_to_complete_goal))
                                Switch(
                                    modifier = Modifier.testTag("TasksFragmentDeliverableSwitch"),
                                    checked = deliverable.goalId!=null,
                                    onCheckedChange = { checked -> scope.launch {
                                        updateDeliverable(deliverable.copy(goalId = if (checked) parentGoal.id else null))
                                    }}
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    stickyHeader {
                        Button(
                            onClick = { scope.launch { addTimeBlock() } },
                            modifier = Modifier.testTag("TasksFragmentDeliverableAddTimeBlockButton")
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) { Text(stringResource(R.string.add_time_block)) }
                    }
                    items(items = times, key = { it.id?:Random.nextLong() }) { item ->
                        TimeInputView(
                            item,
                            triedToSaveInput,
                            deleteTimeBlock,
                            updateTimeBlock
                        )
                        if (times.indexOf(item) != times.lastIndex) {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    originalDeliverable?.let {
                        item {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        item {
                            IconButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(color = Color.Red),
                                onClick = { scope.launch { deleteDeliverableClicked() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_marker)
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
fun MarkerCardView(
    marker: Marker,
    viewModel: TaskViewModel
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateTime = Converters().toLocalDateTime(
            marker.dateTime
        ).value
    val daysFromNow = Period.between(
        LocalDateTime.now().toLocalDate(),
        dateTime.toLocalDate()).days
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .wrapContentHeight()
            .combinedClickable(onClick = {
                scope.launch { viewModel.editMarker(marker) }
            }),
        elevation = CardDefaults.cardElevation(),
        colors = CardColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.LightGray,
            disabledContentColor = Color.DarkGray
        ),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
            Text(text = marker.marker,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 16.sp)
            Row(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Text(
                    text = AppResources.getFullDate(context, dateTime),
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .weight(1f),
                    textAlign = TextAlign.Start,
                    fontSize = 12.sp
                )
                Text(
                    text = when {
                        daysFromNow>0 -> {
                            stringResource(R.string.days_left, daysFromNow)
                        }
                        daysFromNow==0 -> {
                            stringResource(R.string.today)
                        }
                        else -> {
                            stringResource(R.string.days_since, daysFromNow * -1)
                        }
                    },
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .weight(1f),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun AddMarkerView(
    originalMarker: MutableStateFlow<Flow<Marker?>?>,
    marker: Flow<Marker?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    deleteMarkerClicked: suspend () -> Unit,
    updateMarker: suspend (Marker) -> Unit
) {
    val scope = rememberCoroutineScope()
    val originalMarker by originalMarker.collectAsStateWithLifecycle()
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
                text = originalMarker?.let {
                    stringResource(
                        R.string.edit_with_arg,
                        stringResource(R.string.marker)
                    ) }?: stringResource(
                    R.string.add,stringResource(R.string.marker)
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
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterEnd),
                onClick = { scope.launch { processBottomSheetAdd() } }
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.add_task_deliverable_or_marker),
                    tint = Color.Green
                )
            }
        }
    }
    val marker by marker.collectAsStateWithLifecycle(null)
    marker?.let { marker ->
        val context = LocalContext.current

        val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
        var showErrorMessage by remember { showErrorMessage }
        val errorMessage by remember { errorMessage }

        val bottomSheetLazyListState = remember {
            Converters().fromScrollStateLazy(marker.scrollPosition)
        }
        LaunchedEffect(bottomSheetLazyListState) {
            snapshotFlow { bottomSheetLazyListState.isScrollInProgress }
                .filter { !it } // only when scrolling ends
                .collect {
                    updateMarker(marker.copy(
                        scrollPosition = Converters().toScrollStateLazy(bottomSheetLazyListState)
                    ))
                }
        }
        val markerText = remember { TextFieldState(marker.marker) }
        val markerTextFocusRequester = remember { marker.markerTextFocusRequester }

        LaunchedEffect(markerText.text) {
            updateMarker(marker.copy(marker = markerText.text.toString()))
        }

        LaunchedEffect (showErrorMessage) {
            if (showErrorMessage) { delay(2000) // message disappears after 2 seconds
                showErrorMessage = false
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = showErrorMessage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Red)
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(errorMessage),
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
            }
            LazyColumn(
                state = bottomSheetLazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    OutlinedTextField(
                        state = markerText,
                        label = { Text(stringResource(R.string.marker)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp)
                            .focusRequester(markerTextFocusRequester),
                        trailingIcon = {
                            if (triedToSave && markerText.text.isEmpty()){
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.empty_field),
                                    tint = Color.Red
                                )
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                item {
                    var pickedDate by remember {
                        Converters().toLocalDateTime(
                            marker.dateTime
                        )
                    }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = Converters().fromLocalDateTime(pickedDate)
                    )
                    var buttonDatePick by remember { mutableStateOf(false) }
                    var buttonTimePick by remember { mutableStateOf(false) }
                    if (buttonDatePick) {
                        PickDate(stateDate){ pickTime ->
                            buttonDatePick = false
                            buttonTimePick = pickTime
                        }
                    }
                    if (buttonTimePick) {
                        PickTime(stateTime){
                            buttonTimePick = false
                            pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                            scope.launch {
                                updateMarker(
                                    marker.copy(dateTime = Converters().fromLocalDateTime(pickedDate))
                                )
                            }
                        }
                    }
                    OutlinedButton(modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                        onClick = { buttonDatePick = true }
                    ) {
                        stateDate.getSelectedDate()?.let { localDate ->
                            pickedDate = LocalDateTime.of(
                                localDate,
                                LocalTime.of(stateTime.hour,stateTime.minute)
                            )
                            scope.launch {
                                updateMarker(
                                    marker.copy(dateTime = Converters().fromLocalDateTime(pickedDate))
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.start_time_and_date,
                                    getTime(pickedDate),
                                    getStandardDate(context, pickedDate)
                                ))
                        }?: run {
                            Text(stringResource(R.string.pick_a_date))
                        }
                    }
                }
                originalMarker?.let {
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    item {
                        IconButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .background(color = Color.Red),
                            onClick = { scope.launch { deleteMarkerClicked() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_marker)
                            )
                        }
                    }
                }
            }
        }
    }
}