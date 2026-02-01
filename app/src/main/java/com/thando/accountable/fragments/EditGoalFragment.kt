package com.thando.accountable.fragments

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.compose.ui.window.Dialog
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
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.ui.MenuItemData
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.collections.forEach
import kotlin.enums.EnumEntries
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalView(
    viewModel: EditGoalViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var showErrorMessage by remember { viewModel.showErrorMessage }
    val errorMessage by remember { viewModel.errorMessage }

    LaunchedEffect (showErrorMessage) {
        if (showErrorMessage) { delay(2000) // message disappears after 2 seconds
            showErrorMessage = false
        }
    }

    mainActivityViewModel.setGalleryLauncherReturn{ galleryUri ->
        try{
            scope.launch {
                viewModel.setImage(galleryUri)
            }
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    BackHandler {
        scope.launch { viewModel.closeGoal() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    scope.launch {
                        viewModel.saveDeliverable()
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
        val bottomSheetType by remember { viewModel.bottomSheetType }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.new_goal)) },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier,
                            onClick = { scope.launch { viewModel.closeGoal() } }
                        )
                        {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_to_goals)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch { viewModel.saveAndCloseGoal() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = stringResource(R.string.add_goal)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                viewModel.colourPickerDialog.ColourPicker()
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
                EditGoalFragmentView(
                    viewModel,
                    mainActivityViewModel
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
                    deleteTaskClicked = null,
                    originalTask = null,
                    task = null,
                    deleteDeliverableClicked = viewModel::deleteDeliverableClicked,
                    originalDeliverable = viewModel.originalDeliverable,
                    deliverable = viewModel.deliverable,
                    deleteMarkerClicked = null,
                    originalMarker = null,
                    marker = null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalFragmentView(
    viewModel: EditGoalViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier
){
    val newGoalStateFlow by viewModel.newGoal.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(newGoalStateFlow) {
        MainActivity.log("New Goal State Flow: $newGoalStateFlow")
    }

    newGoalStateFlow?.let { newGoalFlow ->
        val newGoal by newGoalFlow.collectAsStateWithLifecycle(null)

        newGoal?.let { newGoal ->
            val scrollState = remember {
                LazyListState(
                    unpackInt1(newGoal.scrollPosition),
                    unpackInt2(newGoal.scrollPosition)
                )
            }
            val uri by newGoal.getUri(context).collectAsStateWithLifecycle()
            val goal = remember { TextFieldState(newGoal.goal) }
            val location = remember { TextFieldState(newGoal.location) }

            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val selectedGoalDeliverablesFlow by newGoal.selectedGoalDeliverables.collectAsStateWithLifecycle()
            val selectedGoalDeliverables by (selectedGoalDeliverablesFlow
                ?: MutableStateFlow(emptyList()))
                .collectAsStateWithLifecycle(emptyList())
            val goalDeliverablesFlow by newGoal.goalDeliverables.collectAsStateWithLifecycle()
            val goalDeliverables by (goalDeliverablesFlow
                ?: MutableStateFlow(emptyList()))
                .collectAsStateWithLifecycle(emptyList())
            val timesFlow by newGoal.times.collectAsStateWithLifecycle()
            val times by (timesFlow ?: MutableStateFlow(emptyList()))
                .collectAsStateWithLifecycle(emptyList())

            val triedToSave by viewModel.triedToSave.collectAsStateWithLifecycle()
            val goalFocusRequester = remember { viewModel.goalFocusRequester }
            val locationFocusRequester = remember { viewModel.locationFocusRequester }
            val colourFocusRequester = remember { viewModel.colourFocusRequester }
            val bottomSheetType by remember { viewModel.bottomSheetType }

            LaunchedEffect(goal.text) {
                viewModel.updateGoalString(goal.text.toString())
            }

            LaunchedEffect(location.text) {
                viewModel.updateLocation(location.text.toString())
            }

            LazyColumn(
                state = scrollState,
                modifier = modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                item {
                    OutlinedTextField(
                        state = goal,
                        label = { Text(stringResource(R.string.goal)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp)
                            .focusRequester(goalFocusRequester),
                        trailingIcon = {
                            if (triedToSave && goal.text.isEmpty()) {
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
                uri?.let {
                    item {
                        Image(
                            bitmap = AppResources.getBitmapFromUri(context, it)?.asImageBitmap()
                                ?: ImageBitmap(1, 1),
                            contentDescription = stringResource(R.string.goal_image),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                mainActivityViewModel.launchGalleryLauncher(
                                    AppResources.ContentType.IMAGE
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                        ) { Text(stringResource(R.string.choose_image)) }
                        uri?.let {
                            Button(
                                onClick = { scope.launch { viewModel.removeImage() } },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                                // enabled =
                            ) { Text(stringResource(R.string.remove_image)) }
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
                            .padding(horizontal = 3.dp)
                            .focusRequester(locationFocusRequester),
                        trailingIcon = {
                            if (triedToSave && location.text.isEmpty()) {
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
                            .height(IntrinsicSize.Max),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (newGoal.colour != -1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(12.dp)
                                    .background(
                                        Color(newGoal.colour),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                MainActivity.log("Button Colour: ${newGoal.colour}")
                                viewModel.pickColour(
                                    Color(newGoal.colour)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .weight(2f)
                                .focusRequester(colourFocusRequester)
                        ) {
                            Row {
                                Text(stringResource(R.string.pick_colour))
                                if (triedToSave && newGoal.colour == -1) {
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
                    var pickedDate by remember {
                        mutableStateOf(
                            LocalDateTime.ofEpochSecond(
                                newGoal.endDateTime/1000, 0,
                                ZoneOffset.UTC
                            )
                        )
                    }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )
                    val stateDate = rememberDatePickerState(
                        initialSelectedDateMillis = pickedDate
                            .toInstant(ZoneOffset.UTC).toEpochMilli()
                    )
                    var buttonDatePick by remember { mutableStateOf(false) }
                    var buttonTimePick by remember { mutableStateOf(false) }
                    if (buttonDatePick) {
                        PickDate(stateDate) { pickTime ->
                            buttonDatePick = false
                            buttonTimePick = pickTime
                        }
                    }
                    if (buttonTimePick) {
                        PickTime(stateTime) {
                            buttonTimePick = false
                            pickedDate =
                                pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                        }
                    }
                    var showEndTypeOptions by remember { mutableStateOf(false) }
                    var endTypeOptions by remember { mutableStateOf(listOf<MenuItemData>()) }
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        onClick = {
                            endTypeOptions = listOf(
                                MenuItemData(Goal.GoalEndType.UNDEFINED.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.UNDEFINED)
                                    }
                                },
                                MenuItemData(Goal.GoalEndType.DATE.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.DATE)
                                    }
                                    buttonDatePick = true
                                },
                                MenuItemData(Goal.GoalEndType.DELIVERABLE.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.DELIVERABLE)
                                    }
                                }
                            )
                            showEndTypeOptions = true
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, Color.DarkGray)
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
                        when (Goal.GoalEndType.valueOf(newGoal.endType)) {
                            Goal.GoalEndType.UNDEFINED -> {
                                Text(
                                    modifier = Modifier,
                                    text = stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.undefined),
                                        ""
                                    ).trim()
                                )
                            }

                            Goal.GoalEndType.DATE -> {
                                stateDate.getSelectedDate()?.let { localDate ->
                                    pickedDate = LocalDateTime.of(
                                        localDate,
                                        LocalTime.of(stateTime.hour, stateTime.minute)
                                    )
                                    LaunchedEffect(pickedDate) {
                                        viewModel.updatePickedDate(pickedDate)
                                    }
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
                                } ?: run {
                                    Text(stringResource(R.string.pick_a_date))
                                }
                            }

                            Goal.GoalEndType.DELIVERABLE -> {
                                Text(
                                    stringResource(
                                        R.string.end_type,
                                        stringResource(R.string.deliverable),
                                        ""
                                    ).trim()
                                )
                            }
                        }
                    }
                }
                if (Goal.GoalEndType.valueOf(newGoal.endType) == Goal.GoalEndType.DELIVERABLE) {
                    stickyHeader {
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { scope.launch { viewModel.addDeliverable() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp).weight(1f)
                            ) { Text(stringResource(R.string.add_deliverable)) }
                            Button(
                                onClick = { scope.launch { viewModel.selectDeliverable() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp).weight(1f)
                            ) { Text(stringResource(R.string.select_deliverable)) }
                        }
                    }
                    items(
                        items = selectedGoalDeliverables,
                        key = { it.id ?: Random.nextLong() }) { deliverable ->
                        DeliverableCardView(
                            deliverable,
                            viewModel::editDeliverable
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                stickyHeader {
                    Button(
                        onClick = { scope.launch { viewModel.addTimeBlock() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) { Text(stringResource(R.string.add_time_block)) }
                }
                items(items = times, key = { it.id ?: Random.nextLong() }) { item ->
                    TimeInputView(
                        item,
                        viewModel.triedToSave,
                        viewModel::deleteTimeBlock,
                        viewModel::updateTimeBlock
                    )
                    if (times.indexOf(item) != times.lastIndex) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            }
        } ?: run {
            // New Goal not loaded yet
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Indeterminate spinner
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputView(
    time: GoalTaskDeliverableTime,
    triedToSaveStateFlow: MutableStateFlow<Boolean>,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateGoalTaskDeliverableTime: suspend (GoalTaskDeliverableTime) -> Unit
){
    var pickedDate by remember { mutableStateOf(LocalDateTime.ofEpochSecond(time.start/1000,0,
        ZoneOffset.UTC)) }
    var pickedDuration by remember { mutableStateOf(LocalDateTime.ofEpochSecond(time.duration/1000,0,
        ZoneOffset.UTC)) }
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val triedToSave by triedToSaveStateFlow.collectAsStateWithLifecycle()
    val durationPickerFocusRequester = remember { time.durationPickerFocusRequester }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)) {
                PickerMenu(
                    modifier = Modifier.weight(4f),
                    options = Goal.TimeBlockType.entries,
                    selectedOption = Goal.TimeBlockType.valueOf(time.timeBlockType)
                ) { scope.launch {
                    updateGoalTaskDeliverableTime(time.copy(timeBlockType = it.name))
                } }
                IconButton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .weight(1f)
                        .background(color = Color.Red),
                    onClick = { scope.launch { deleteTimeBlock(time) } }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_time_block)
                    )
                }
            }
            when(Goal.TimeBlockType.valueOf(time.timeBlockType)){
                Goal.TimeBlockType.DAILY -> {
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )

                    var buttonTimePick by remember { mutableStateOf(false) }
                    if (buttonTimePick) {
                        PickTime(stateTime){
                            buttonTimePick = false
                            pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        start = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                        }
                    }
                    OutlinedButton(modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                        onClick = { buttonTimePick = true }
                    ) {
                        checkDuration(stateTime, pickedDuration){ newDuration ->
                            pickedDuration = newDuration
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                        }
                        Text(stringResource(R.string.start_time, getTime(pickedDate)))
                    }
                    DurationPickerButton(
                        pickedDate,
                        pickedDuration,
                        triedToSave,
                        durationPickerFocusRequester
                    ){ newDuration ->
                        pickedDuration = newDuration
                        scope.launch {
                            updateGoalTaskDeliverableTime(
                                time.copy(
                                    duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                )
                            )
                        }
                    }
                }
                Goal.TimeBlockType.WEEKLY -> {
                    var selectedDay by remember { mutableStateOf(pickedDate.let{
                        when(it.dayOfWeek) {
                            DayOfWeek.MONDAY -> resources.getString(R.string.Mon)
                            DayOfWeek.TUESDAY -> resources.getString(R.string.Tue)
                            DayOfWeek.WEDNESDAY -> resources.getString(R.string.Wed)
                            DayOfWeek.THURSDAY -> resources.getString(R.string.Thu)
                            DayOfWeek.FRIDAY -> resources.getString(R.string.Fri)
                            DayOfWeek.SATURDAY -> resources.getString(R.string.Sat)
                            DayOfWeek.SUNDAY -> resources.getString(R.string.Sun)
                        }
                    }) }
                    val stateTime = rememberTimePickerState(
                        pickedDate.hour,
                        pickedDate.minute,
                        true
                    )

                    var buttonWeekDayPick by remember { mutableStateOf(false) }
                    var buttonTimePick by remember { mutableStateOf(false) }
                    if (buttonWeekDayPick) {
                        PickWeekday(selectedDay){ day ->
                            buttonWeekDayPick = false
                            day?.let {
                                selectedDay = it
                                pickedDate = pickedDate.with(TemporalAdjusters.nextOrSame(
                                    when(it) {
                                        resources.getString(R.string.Mon) -> DayOfWeek.MONDAY
                                        resources.getString(R.string.Tue) -> DayOfWeek.TUESDAY
                                        resources.getString(R.string.Wed) -> DayOfWeek.WEDNESDAY
                                        resources.getString(R.string.Thu) -> DayOfWeek.THURSDAY
                                        resources.getString(R.string.Fri) -> DayOfWeek.FRIDAY
                                        resources.getString(R.string.Sat) -> DayOfWeek.SATURDAY
                                        resources.getString(R.string.Sun) -> DayOfWeek.SUNDAY
                                        else -> DayOfWeek.MONDAY
                                    }
                                ))
                                scope.launch {
                                    updateGoalTaskDeliverableTime(
                                        time.copy(
                                            start = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
                                        )
                                    )
                                }
                                buttonTimePick = true
                            }
                        }
                    }
                    if (buttonTimePick) {
                        PickTime(stateTime){
                            buttonTimePick = false
                            pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)

                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        start = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                        }
                    }
                    OutlinedButton(modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                        onClick = { buttonWeekDayPick = true }
                    ) {
                        checkDuration(stateTime,pickedDuration){ newDuration ->
                            pickedDuration = newDuration
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                        }
                        Text(
                            stringResource(
                                R.string.start_time_and_weekday,
                                getTime(stateTime),
                                selectedDay
                            ))
                    }
                    DurationPickerButton(
                        pickedDate,
                        pickedDuration,
                        triedToSave,
                        durationPickerFocusRequester
                    ){ newDuration ->
                        pickedDuration = newDuration
                        scope.launch {
                            updateGoalTaskDeliverableTime(
                                time.copy(
                                    duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                )
                            )
                        }
                    }
                }
                Goal.TimeBlockType.MONTHLY,
                Goal.TimeBlockType.ONCE,
                Goal.TimeBlockType.YEARLY -> {
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

                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        start = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
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
                            checkDuration(stateTime,pickedDuration){ newDuration ->
                                pickedDuration = newDuration
                                scope.launch {
                                    updateGoalTaskDeliverableTime(
                                        time.copy(
                                            duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                        )
                                    )
                                }
                            }
                            pickedDate = LocalDateTime.of(
                                localDate,
                                LocalTime.of(stateTime.hour,stateTime.minute)
                            )
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        start = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.start_time_and_date,
                                    getTime(pickedDate),
                                    getStandardDate(context, pickedDate)
                                ))
                        }?: run {
                            Text(stringResource(R.string.pick_time_frequency))
                        }
                    }
                    stateDate.selectedDateMillis?.let {
                        DurationPickerButton(
                            pickedDate,
                            pickedDuration,
                            triedToSave,
                            durationPickerFocusRequester
                        ){ newDuration ->
                            pickedDuration = newDuration
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        duration = pickedDuration.toInstant(ZoneOffset.UTC).toEpochMilli()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun checkDuration(newTime: TimePickerState, duration: LocalDateTime, changeDuration: (LocalDateTime) -> Unit){
    val selectableHours = 23 - newTime.hour
    val selectedHours = duration.hour.let{
        if (it>selectableHours) selectableHours else it
    }

    val selectableMinutes = if (selectedHours  == selectableHours) 59 - newTime.minute else 59
    val selectedMinutes = duration.minute.let {
        if (it>selectableMinutes) selectableMinutes else it
    }

    if (selectedHours!=0 || selectedMinutes!=0){
        changeDuration(duration.withHour(selectedHours).withMinute(selectedMinutes))
    }
}

@Composable
fun PickerMenu(
    modifier: Modifier,
    options: EnumEntries<Goal.TimeBlockType>,
    selectedOption: Goal.TimeBlockType?,
    onOptionSelected: (Goal.TimeBlockType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Trigger
        OutlinedButton(modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
            onClick = { expanded = true }) {
            selectedOption?.name?.let {
                Text(stringResource(R.string.time_frequency, it))
            }?: run {
                Text(stringResource(R.string.pick_time_frequency))
            }
        }

        // Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PickDate(state: DatePickerState, closeDialog: (Boolean)->Unit){
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        DatePickerDialog(
            onDismissRequest = {
                openDialog.value = false
                closeDialog(false)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        closeDialog(true)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        closeDialog(false)
                    }
                ) {
                    Text("CANCEL")
                }
            }
        ) {
            DatePicker(
                state = state
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTime(state: TimePickerState, closeDialog: ()->Unit) {
    val openDialog = remember { mutableStateOf(true) }


    if (openDialog.value) {
        val currentState = rememberTimePickerState(state.hour,state.minute,state.is24hour)
        TimePickerDialog(
            onDismissRequest = {
                openDialog.value = false
                closeDialog()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        state.hour = currentState.hour
                        state.minute = currentState.minute
                        state.is24hour = currentState.is24hour
                        closeDialog()
                    }
                ) {
                    Text("OK")
                }
            },
            title = { Text(stringResource(R.string.nothing)) },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                        closeDialog()
                    }
                ) {
                    Text("CANCEL")
                }
            }
        ) {
            TimePicker(
                state = currentState
            )
        }
    }
}

@Composable
fun PickWeekday(
    selectedDay: String,
    onDaySelected: (String?) -> Unit
) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        val daysOfWeek = listOf(
            stringResource(R.string.Mon),
            stringResource(R.string.Tue),
            stringResource(R.string.Wed),
            stringResource(R.string.Thu),
            stringResource(R.string.Fri),
            stringResource(R.string.Sat),
            stringResource(R.string.Sun)
        )
        Dialog(
            onDismissRequest = {
                openDialog.value = false
                onDaySelected(null)
            }
        ){
            Box(modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        items(daysOfWeek) { day ->
                            val isSelected = day == selectedDay
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color.Blue else Color.LightGray)
                                    .clickable {
                                        openDialog.value = false
                                        onDaySelected(day)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = day,
                                    color = if (isSelected) Color.White else Color.Black,
                                    style = MaterialTheme.typography.bodySmall
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
fun DurationPickerButton(
    datePicked:LocalDateTime,
    duration:LocalDateTime,
    triedToSave:Boolean,
    durationPickerFocusRequester: FocusRequester,
    changeDuration:(LocalDateTime)->Unit
){
    val buttonDurationPick = remember { mutableStateOf(false) }
    if (buttonDurationPick.value) {
        TimeDurationPicker(datePicked, duration) { hours, minutes ->
            changeDuration(datePicked.withHour(hours).withMinute(minutes))
            buttonDurationPick.value = false
        }
    }

    OutlinedButton(modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp)
        .focusRequester(durationPickerFocusRequester),
        onClick = { buttonDurationPick.value = true }
    ) {
        if (duration.hour != 0 || duration.minute != 0){
            Text(
                text = "Duration:",
                style = MaterialTheme.typography.titleMedium
            )
            if (duration.hour != 0 || duration.minute != 0) {
                Text(
                    (if (duration.hour == 1) "${duration.hour} Hour"
                    else if (duration.hour != 0) "${duration.hour} Hours" else "")
                            +
                            (if (duration.hour != 0 && duration.minute != 0) " and " else "")
                            +
                            (if (duration.minute == 1) "${duration.minute} Minute"
                            else if (duration.minute != 0) "${duration.minute} Minutes" else ""),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        else {
            Row {
                Text(stringResource(R.string.please_select_a_duration))
                if (triedToSave){
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

@Composable
fun TimeDurationPicker(
    pickedTime: LocalDateTime,
    durationPicked:LocalDateTime?,
    onDurationSelected: (hours: Int, minutes: Int) -> Unit
) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        var selectableHours by remember { mutableIntStateOf(23 - pickedTime.hour) }
        var selectedHours by remember {
            mutableIntStateOf(durationPicked?.hour?.let{
                if (it>selectableHours) selectableHours else it
            }?: 0)
        }

        var selectableMinutes by remember { mutableIntStateOf(if (selectedHours  == selectableHours) 59 - pickedTime.minute else 59) }
        var selectedMinutes by remember {
            mutableIntStateOf(durationPicked?.minute?.let {
                if (it>selectableMinutes) selectableMinutes else it
            }?: 0)
        }

        selectableMinutes = if (selectedHours  == selectableHours) 59 - pickedTime.minute else 59
        if (selectedMinutes>selectableMinutes) selectedMinutes = selectableMinutes

        Dialog(
            onDismissRequest = {
                openDialog.value = false
                onDurationSelected(selectedHours, selectedMinutes)
            }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var textWidth by remember { mutableIntStateOf(0) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Duration:",
                            style = MaterialTheme.typography.titleMedium,
                            onTextLayout = {
                                if (it.size.width > textWidth) textWidth = it.size.width
                            },
                            modifier = Modifier.width(
                                if (textWidth == 0) Dp.Unspecified else with(
                                    LocalDensity.current
                                ) { textWidth.toDp() })
                        )
                        if (selectedHours != 0 || selectedMinutes != 0) {
                            Text(
                                (if (selectedHours == 1) "$selectedHours Hour"
                                else if (selectedHours != 0) "$selectedHours Hours" else "")
                                        +
                                        (if (selectedHours != 0 && selectedMinutes != 0) " and " else "")
                                        +
                                        (if (selectedMinutes == 1) "$selectedMinutes Minute"
                                        else if (selectedMinutes != 0) "$selectedMinutes Minutes" else ""),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Hours Picker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hours: ",
                            style = MaterialTheme.typography.bodyMedium,
                            onTextLayout = {
                                if (it.size.width > textWidth) textWidth = it.size.width
                            },
                            modifier = Modifier
                                .width(
                                    if (textWidth == 0) Dp.Unspecified else with(
                                        LocalDensity.current
                                    ) { textWidth.toDp() })
                                .padding(start = 8.dp)
                        )
                        Slider(
                            value = selectedHours.toFloat(),
                            onValueChange = {
                                selectedHours = it.toInt()
                                //onDurationSelected(selectedHours, selectedMinutes)
                            },
                            valueRange = 0f..selectableHours.toFloat(),
                            steps = selectableHours-1, // 23 hours total
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minutes Picker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Minutes: ",
                            style = MaterialTheme.typography.bodyMedium,
                            onTextLayout = {
                                if (it.size.width > textWidth) textWidth = it.size.width
                            },
                            modifier = Modifier.width(
                                if (textWidth == 0) Dp.Unspecified else with(
                                    LocalDensity.current
                                ) { textWidth.toDp() })
                        )
                        Slider(
                            value = selectedMinutes.toFloat(),
                            onValueChange = {
                                selectedMinutes = it.toInt()
                                //onDurationSelected(selectedHours, selectedMinutes)
                            },
                            valueRange = 0f..selectableMinutes.toFloat(),
                            steps = selectableMinutes-1, // 59 minutes total
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}