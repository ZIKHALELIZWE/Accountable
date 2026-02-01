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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.AppResources.Companion.getStandardDate
import com.thando.accountable.AppResources.Companion.getTime
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    val goalFlow by viewModel.goal.collectAsStateWithLifecycle()
    val goal by (goalFlow?:MutableStateFlow(null)).collectAsStateWithLifecycle(null)

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
            val goalUri by goal.getUri(context).collectAsStateWithLifecycle(null)
            val goalColour by remember { mutableIntStateOf( goal.colour) }
            val selectedTab by remember { mutableStateOf(GoalTab.valueOf(goal.selectedTab)) }
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
                                        text = when (selectedTab){
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
                    bottomSheetType = bottomSheetType,
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
                    deleteTaskClicked = viewModel::deleteTaskClicked,
                    originalTask = viewModel.originalTask,
                    task = viewModel.task,
                    deleteDeliverableClicked = viewModel::deleteDeliverableClicked,
                    originalDeliverable = viewModel.originalDeliverable,
                    deliverable = viewModel.deliverable,
                    deleteMarkerClicked = viewModel::deleteMarkerClicked,
                    originalMarker = viewModel.originalMarker,
                    marker = viewModel.marker
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDeliverableMarkerBottomSheet(
    bottomSheetType: GoalTab?,
    dismissBottomSheet: suspend () -> Unit,
    triedToSaveInput: MutableStateFlow<Boolean>,
    colourPickerDialog: ColourPickerDialog,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    pickColour: (Color?) -> Unit,
    addTimeBlock: suspend () -> Unit,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    deleteTaskClicked: (suspend () -> Unit)?=null,
    originalTask: MutableStateFlow<Task?>?=null,
    task: MutableStateFlow<Task?>?=null,
    deleteDeliverableClicked: (suspend () -> Unit)?=null,
    originalDeliverable: MutableStateFlow<Deliverable?>?=null,
    deliverable: MutableStateFlow<Deliverable?>?=null,
    deleteMarkerClicked: (suspend () -> Unit)?=null,
    originalMarker: MutableStateFlow<Marker?>?=null,
    marker: MutableStateFlow<Marker?>?=null
){
    val scope = rememberCoroutineScope()
    bottomSheetType?.let { bottomSheetType ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { dismissBottomSheet() }
            },
            sheetState = sheetState
        ) {
            when (bottomSheetType) {
                GoalTab.TASKS -> {
                    if (originalTask != null && task != null && deleteTaskClicked != null)
                    AddTaskView(
                        originalTask = originalTask,
                        task = task,
                        triedToSaveInput = triedToSaveInput,
                        colourPickerDialog = colourPickerDialog,
                        processBottomSheetAdd = processBottomSheetAdd,
                        showErrorMessage = showErrorMessage,
                        errorMessage = errorMessage,
                        pickColour = pickColour,
                        addTimeBlock = addTimeBlock,
                        deleteTimeBlock = deleteTimeBlock,
                        updateTimeBlock = updateTimeBlock,
                        deleteTaskClicked = deleteTaskClicked
                    )
                }
                GoalTab.DELIVERABLES -> {
                    if (originalDeliverable != null && deliverable != null && deleteDeliverableClicked != null)
                    AddDeliverableView(
                        originalDeliverable = originalDeliverable,
                        deliverable = deliverable,
                        triedToSaveInput = triedToSaveInput,
                        processBottomSheetAdd = processBottomSheetAdd,
                        showErrorMessage = showErrorMessage,
                        errorMessage = errorMessage,
                        addTimeBlock = addTimeBlock,
                        deleteTimeBlock = deleteTimeBlock,
                        deleteDeliverableClicked = deleteDeliverableClicked,
                        updateTimeBlock = updateTimeBlock
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
                            deleteMarkerClicked = deleteMarkerClicked
                        )
                }
            }
        }
    }
}

@Composable
fun TasksFragmentView(
    viewModel: TaskViewModel,
    goal: Goal,
    modifier: Modifier = Modifier
) {
    val tabListState = remember { LazyListState(
        unpackInt1(goal.tabListState),
        unpackInt2(goal.tabListState)
    ) }
    val goalTitle = remember { TextFieldState(goal.goal) }
    var selectedTab by remember { mutableStateOf(GoalTab.valueOf(goal.selectedTab)) }
    val tabs = listOf(
        stringResource(R.string.tasks),
        stringResource(R.string.deliverables),
        stringResource(R.string.markers)
    )
    val goalColour by remember { mutableIntStateOf(goal.colour) }

    val tasksListFlow by goal.goalTasks.collectAsStateWithLifecycle()
    val tasksList by (tasksListFlow
        ?: MutableStateFlow(emptyList()))
        .collectAsStateWithLifecycle(emptyList())
    val deliverablesListFlow by goal.goalDeliverables.collectAsStateWithLifecycle()
    val deliverablesList by (deliverablesListFlow
        ?: MutableStateFlow(emptyList()))
        .collectAsStateWithLifecycle(emptyList())
    val markersListFlow by goal.goalMarkers.collectAsStateWithLifecycle()
    val markersList by (markersListFlow
        ?: MutableStateFlow(emptyList()))
        .collectAsStateWithLifecycle(emptyList())

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
                        onClick = { selectedTab = GoalTab.entries[index] },
                        text = { Text(title) },
                        selectedContentColor = Color(goalColour),
                        unselectedContentColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        when (selectedTab) {
            GoalTab.TASKS -> {
                items(items = tasksList){ task ->
                    TaskCardView(task, viewModel)
                }
            }
            GoalTab.DELIVERABLES -> {
                items(items = deliverablesList) { deliverable ->
                    DeliverableCardView(deliverable, viewModel::editDeliverable)
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
    val taskText = remember { task.task }
    val taskColour by remember { task.colour }
    val timeCreated by remember { task.initialDateTime }
    val endType by remember { task.endType }
    val endDateTime by remember { task.endDateTime }
    val status by remember { task.status }
    val location = remember { task.location }

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
        border = BorderStroke(4.dp, if (taskColour!=-1)
            Color(taskColour)
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
                    color = if (taskColour!=-1 && Color(taskColour)!=Color.White)
                        Color(taskColour).darker()
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
                    color = if (taskColour!=-1 && Color(taskColour)!=Color.White)
                        Color(taskColour).darker()
                    else Color.Black
                )
                Text(
                    text = status.name,
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
                    color = if (taskColour!=-1 && Color(taskColour)!=Color.White)
                        Color(taskColour).darker()
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
                    color = if (taskColour!=-1 && Color(taskColour)!=Color.White)
                        Color(taskColour).darker()
                    else Color.Black
                )
                Text(
                    text = endType.name + if (endType == Task.TaskEndType.DATE)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskView(
    originalTask: MutableStateFlow<Task?>,
    task: MutableStateFlow<Task?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    colourPickerDialog: ColourPickerDialog,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    pickColour: (Color?) -> Unit,
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
    val task by task.collectAsStateWithLifecycle()
    task?.let { task ->
        val context = LocalContext.current

        val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
        var showErrorMessage by remember { showErrorMessage }
        val errorMessage by remember { errorMessage }

        val bottomSheetLazyListState = remember { task.scrollPosition }
        var taskType by remember { task.type }
        val taskText = remember { task.task }
        val taskTextFocusRequester = remember { task.taskTextFocusRequester }
        val location = remember { task.location }
        val locationFocusRequester = remember { task.locationFocusRequester }
        val timesFlow by task.times.collectAsStateWithLifecycle()
        val times by (timesFlow?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle(emptyList())
        var colour by remember { task.colour }
        val colourFocusRequester = remember { task.colourFocusRequester }
        var endType by remember { task.endType }
        var quantity by remember { task.quantity }
        var taskTime by remember { task.time }

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
                        modifier = Modifier.fillMaxWidth().padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Task.TaskType.entries.forEach { boxType ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .height(TopAppBarDefaults.MediumAppBarCollapsedHeight) // makes it a big square
                                    .background(
                                        if (taskType == boxType) Color.Blue else Color.LightGray,
                                        shape = RectangleShape
                                    )
                                    .clickable { taskType = boxType },
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
                    when(taskType){
                        Task.TaskType.QUANTITY,
                        Task.TaskType.NORMAL -> {
                            var styledText by remember { mutableStateOf<AnnotatedString?>(null) }
                            if (taskType == Task.TaskType.QUANTITY){
                                LaunchedEffect(Unit) {
                                    getOnlyOneQuantityText(taskText.text.toString())?.let{
                                        taskText.setTextAndPlaceCursorAtEnd(it)
                                    }
                                }
                                LaunchedEffect(taskText.text) {
                                    quantity = Regex("\\d+").find(taskText.text)?.value?.toLongOrNull()?:0
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
                                    if (taskType == Task.TaskType.QUANTITY) R.string.task_with_quantity
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
                                    if(taskType == Task.TaskType.QUANTITY) {
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
                                    buttonDurationPick.value = false
                                }
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth()
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
                            .fillMaxWidth().padding(3.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (colour != -1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(end = 3.dp)
                                    .background(
                                        Color(colour),
                                        shape = RectangleShape
                                    )
                                    .weight(1f)
                            )
                        }
                        Button(
                            onClick = { pickColour(
                                Color(task.colour.value)
                            ) },
                            modifier = Modifier
                                .fillMaxWidth().fillMaxHeight()
                                .weight(2f)
                                .focusRequester(colourFocusRequester),
                            shape = RectangleShape
                        ) {
                            Row {
                                Text(stringResource(R.string.pick_colour))
                                if (triedToSave && colour==-1){
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
                    var pickedDate by remember { task.endDateTime }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
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
                                    endType = Task.TaskEndType.UNDEFINED
                                },
                                MenuItemData(Task.TaskEndType.DATE.name){
                                    endType = Task.TaskEndType.DATE
                                    buttonDatePick = true
                                },
                                MenuItemData(Task.TaskEndType.GOAL.name){
                                    endType = Task.TaskEndType.GOAL
                                },
                                MenuItemData(Task.TaskEndType.DELIVERABLE.name){
                                    endType = Task.TaskEndType.DELIVERABLE
                                },
                                MenuItemData(Task.TaskEndType.MARKER.name){
                                    endType = Task.TaskEndType.MARKER
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
                        when (endType) {
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
    editDeliverable: suspend (Deliverable) -> Unit
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliverableText = remember { deliverable.deliverable }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .wrapContentHeight()
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
        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
            Text(text = deliverableText,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeliverableView(
    originalDeliverable: MutableStateFlow<Deliverable?>,
    deliverable: MutableStateFlow<Deliverable?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    addTimeBlock: suspend () -> Unit,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    deleteDeliverableClicked: suspend () -> Unit,
    updateTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit
) {
    val scope = rememberCoroutineScope()
    val originalDeliverable by originalDeliverable.collectAsStateWithLifecycle()
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
    val deliverable by deliverable.collectAsStateWithLifecycle()
    deliverable?.let { deliverable ->
        val context = LocalContext.current

        val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
        var showErrorMessage by remember { showErrorMessage }
        val errorMessage by remember { errorMessage }

        val bottomSheetLazyListState = remember {
            LazyListState(
                unpackInt1(deliverable.scrollPosition),
                unpackInt2(deliverable.scrollPosition)
            )
        }
        val deliverableText = remember { TextFieldState(deliverable.deliverable) }
        val deliverableTextFocusRequester = remember { deliverable.deliverableTextFocusRequester }
        val location = remember { TextFieldState(deliverable.location) }
        val locationFocusRequester = remember { deliverable.locationFocusRequester }
        val timesFlow by deliverable.times.collectAsStateWithLifecycle()
        val times by (timesFlow?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle(emptyList())
        var endType by remember { mutableStateOf(Deliverable.DeliverableEndType.valueOf(deliverable.endType)) }

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
                    var pickedDate by remember { mutableStateOf(LocalDateTime.ofEpochSecond(
                        deliverable.endDateTime/1000,0,
                        ZoneOffset.UTC)) }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
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
                    OutlinedButton(modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                        onClick = {
                            endTypeOptions = listOf(
                                MenuItemData(Deliverable.DeliverableEndType.UNDEFINED.name){
                                    endType = Deliverable.DeliverableEndType.UNDEFINED
                                },
                                MenuItemData(Deliverable.DeliverableEndType.DATE.name){
                                    endType = Deliverable.DeliverableEndType.DATE
                                    buttonDatePick = true
                                },
                                MenuItemData(Deliverable.DeliverableEndType.GOAL.name){
                                    endType = Deliverable.DeliverableEndType.GOAL
                                },
                                MenuItemData(Deliverable.DeliverableEndType.WORK.name){
                                    endType = Deliverable.DeliverableEndType.WORK
                                }
                            )
                            showEndTypeOptions = true
                        }
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
                        when (endType) {
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
                stickyHeader {
                    Button(
                        onClick = { scope.launch { addTimeBlock() } },
                        modifier = Modifier
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

@Composable
fun MarkerCardView(
    marker: Marker,
    viewModel: TaskViewModel
){
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val markerText = remember { marker.marker }
    val dateTime by remember { marker.dateTime }
    val daysFromNow by remember { mutableStateOf(Period.between(
        LocalDateTime.now().toLocalDate(),
        dateTime.toLocalDate()).days) }
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
            Text(text = markerText.text.toString(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMarkerView(
    originalMarker: MutableStateFlow<Marker?>,
    marker: MutableStateFlow<Marker?>,
    triedToSaveInput: MutableStateFlow<Boolean>,
    processBottomSheetAdd: suspend () -> Unit,
    showErrorMessage: MutableState<Boolean>,
    errorMessage: MutableIntState,
    deleteMarkerClicked: suspend () -> Unit
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
    val marker by marker.collectAsStateWithLifecycle()
    marker?.let { marker ->
        val context = LocalContext.current

        val triedToSave by triedToSaveInput.collectAsStateWithLifecycle()
        var showErrorMessage by remember { showErrorMessage }
        val errorMessage by remember { errorMessage }

        val bottomSheetLazyListState = remember { marker.scrollPosition }
        val markerText = remember { marker.marker }
        val markerTextFocusRequester = remember { marker.markerTextFocusRequester }

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
                    var pickedDate by remember { marker.dateTime }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
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