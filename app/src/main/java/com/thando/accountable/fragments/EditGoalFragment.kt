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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.ui.MenuItemData
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.enums.EnumEntries
import kotlin.random.Random

// Custom semantics key for background color (used in test)
val BackgroundColorKey = SemanticsPropertyKey<Color>("BackgroundColor")
var SemanticsPropertyReceiver.backgroundColor by BackgroundColorKey
fun Modifier.testBackground(color: Color, shape: Shape = RectangleShape): Modifier =
    this
        .background(color, shape = shape)
        .semantics { backgroundColor = color }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
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
        val editGoal by viewModel.editGoal.collectAsStateWithLifecycle()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text( modifier = Modifier.testTag("EditGoalTitle"), text = stringResource(
                        if (editGoal != null) R.string.edit_goal
                        else R.string.new_goal
                    )) },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.testTag("EditGoalCloseGoalButton"),
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
                        IconButton(
                            modifier = Modifier.testTag("EditGoalSaveAndCloseIconButton"),
                            onClick = {
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
                    parentGoal = viewModel.newGoal,
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
                    deleteTaskClicked = null,
                    originalTask = null,
                    task = null,
                    updateTask = viewModel::updateTask,
                    deleteDeliverableClicked = viewModel::deleteDeliverableClicked,
                    originalDeliverable = viewModel.originalDeliverable,
                    deliverable = viewModel.deliverable,
                    deleteMarkerClicked = null,
                    originalMarker = null,
                    marker = null,
                    updateMarker = viewModel::updateMarker
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun EditGoalFragmentView(
    viewModel: EditGoalViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier
) {
    val newGoal by viewModel.newGoal.collectAsStateWithLifecycle(null)
    val context = LocalContext.current

    newGoal?.let { newGoal ->
        val scrollState = remember {
            Converters().fromScrollStateLazy(newGoal.scrollPosition)
        }
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.isScrollInProgress }
                .filter { !it } // only when scrolling ends
                .collect {
                    viewModel.updateScrollPosition(
                        Converters().toScrollStateLazy(scrollState)
                    )
                }
        }

        val imageBitmap by newGoal.getImageBitmap(context).collectAsStateWithLifecycle(null)
        val goal = remember { TextFieldState(newGoal.goal) }
        val location = remember { TextFieldState(newGoal.location) }

        val scope = rememberCoroutineScope()

        val selectedGoalDeliverables by newGoal.selectedGoalDeliverables.collectAsStateWithLifecycle(emptyList())

        val notSelectedGoalDeliverables by newGoal.notSelectedGoalDeliverables.collectAsStateWithLifecycle(emptyList())

        val times by newGoal.times.collectAsStateWithLifecycle(emptyList())

        val showSelectDeliverableDialog by viewModel.selectDeliverableDialog.collectAsStateWithLifecycle()

        val triedToSave by viewModel.triedToSave.collectAsStateWithLifecycle()
        val goalFocusRequester = remember { viewModel.goalFocusRequester }
        val locationFocusRequester = remember { viewModel.locationFocusRequester }
        val colourFocusRequester = remember { viewModel.colourFocusRequester }

        LaunchedEffect(goal.text) {
            viewModel.updateGoalString(goal.text.toString())
        }

        LaunchedEffect(location.text) {
            viewModel.updateLocation(location.text.toString())
        }

        Box(modifier = modifier.fillMaxSize()) {
            if (showSelectDeliverableDialog){
                LaunchedEffect(notSelectedGoalDeliverables.isEmpty()) {
                    if (notSelectedGoalDeliverables.isEmpty()) viewModel.closeSelectDeliverableDialog()
                }
                Dialog(
                    onDismissRequest = {
                        viewModel.closeSelectDeliverableDialog()
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(),
                            colors = CardColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.DarkGray
                            ),
                            shape = RectangleShape
                        ) {
                            Text(
                                text = stringResource(
                                    if (notSelectedGoalDeliverables.size==1)
                                        R.string.select_deliverable
                                    else R.string.select_deliverables
                                ),
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
                        LazyColumn(
                            state = rememberLazyListState(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("EditGoalSelectDeliverableDialog")
                        ) {
                            items(
                                items = notSelectedGoalDeliverables,
                                key = { it.id ?: Random.nextLong() }
                            ) { deliverable ->
                                Row(
                                    modifier = Modifier
                                        .testTag("EditGoalPickDeliverableRow-${deliverable.id}")
                                        .height(IntrinsicSize.Min)
                                        .fillMaxWidth()
                                        .padding(3.dp)
                                ) {
                                    DeliverableCardView(
                                        deliverable,
                                        viewModel::editDeliverable,
                                        Modifier
                                            .weight(4f).fillMaxWidth()
                                            .wrapContentHeight()
                                    )
                                    IconButton(
                                        modifier = Modifier
                                            .weight(1f).fillMaxWidth()
                                            .testTag("EditGoalPickDeliverableButton-${deliverable.id}")
                                            .background(color = Color.Green),
                                        onClick = {
                                            scope.launch {
                                                viewModel.saveDeliverable(
                                                    deliverable.copy(
                                                        goalId = newGoal.id
                                                    )
                                                )
                                            }
                                        }
                                    ) {
                                        MainActivity.Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = stringResource(R.string.pick_deliverable)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .testTag("EditGoalLazyColumn")
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                item {
                    OutlinedTextField(
                        state = goal,
                        label = { Text(stringResource(R.string.goal)) },
                        modifier = Modifier
                            .testTag("EditGoalGoal")
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
                imageBitmap?.let { imageBitmap ->
                    item {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = stringResource(R.string.goal_image),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.testTag("EditGoalImage")
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
                                .testTag("EditGoalChooseImageButton")
                                .weight(1f)
                                .padding(8.dp),
                        ) { Text(stringResource(R.string.choose_image)) }
                        imageBitmap?.let {
                            Button(
                                onClick = { scope.launch { viewModel.removeImage() } },
                                modifier = Modifier
                                    .testTag("EditGoalRemoveImageButton")
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
                            .testTag("EditGoalLocation")
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
                                    .testTag("EditGoalColourDisplayBox")
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
                                viewModel.pickColour(
                                    Color(newGoal.colour)
                                )
                            },
                            modifier = Modifier
                                .testTag("EditGoalPickColourButton")
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
                        Converters().toLocalDateTime(
                            newGoal.endDateTime
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

                    if (viewModel.buttonDatePick.collectAsState().value) {
                        PickDate(stateDate) { pickTime ->
                            viewModel.buttonDatePick.value = false
                            viewModel.buttonTimePick.value = pickTime
                        }
                    }
                    if (viewModel.buttonTimePick.collectAsState().value) {
                        PickTime(stateTime) {
                            viewModel.buttonTimePick.value = false
                            pickedDate = pickedDate.withHour(stateTime.hour).withMinute(stateTime.minute)
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .testTag("EditGoalEndTypeButton")
                            .semantics(mergeDescendants = false) {}
                            .fillMaxWidth()
                            .padding(3.dp),
                        onClick = {
                            viewModel.endTypeOptions.value = listOf(
                                MenuItemData(Goal.GoalEndType.UNDEFINED.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.UNDEFINED)
                                    }
                                },
                                MenuItemData(Goal.GoalEndType.DATE.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.DATE)
                                    }
                                    viewModel.buttonDatePick.value = true
                                },
                                MenuItemData(Goal.GoalEndType.DELIVERABLE.name) {
                                    scope.launch {
                                        viewModel.updateEndType(Goal.GoalEndType.DELIVERABLE)
                                    }
                                }
                            )
                            viewModel.showEndTypeOptions.value = true
                        },
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        if (viewModel.showEndTypeOptions.collectAsState().value) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { viewModel.showEndTypeOptions.value = false },
                                modifier = Modifier.testTag("EditGoalEndTypeDropDownMenu")
                            ) {
                                viewModel.endTypeOptions.collectAsState().value.forEach { option ->
                                    DropdownMenuItem(
                                        modifier = Modifier.testTag("EditGoalDropdownMenuItem-${option.text}"),
                                        text = { Text(option.text) },
                                        onClick = {
                                            option.onClick.invoke()
                                            viewModel.showEndTypeOptions.value = false
                                        }
                                    )
                                }
                            }
                        }
                        when (Goal.GoalEndType.valueOf(newGoal.endType)) {
                            Goal.GoalEndType.UNDEFINED -> {
                                Text(
                                    modifier = Modifier.testTag("EditGoalEndTypeUndefinedText"),
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
                                    LaunchedEffect(
                                        pickedDate
                                    ) {
                                        viewModel.updatePickedDate(pickedDate)
                                    }
                                    Text(
                                        modifier = Modifier.testTag("EditGoalEndTypePickedDateText"),
                                        text = stringResource(
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
                                    Text(
                                        modifier = Modifier.testTag("EditGoalEndTypePickDateText"),
                                        text = stringResource(R.string.pick_a_date))
                                }
                            }

                            Goal.GoalEndType.DELIVERABLE -> {
                                Text(
                                    modifier = Modifier.testTag("EditGoalEndTypeDeliverableText"),
                                    text = stringResource(
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
                                    .testTag("EditGoalAddDeliverableButton")
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .weight(1f)
                            ) { Text(stringResource(R.string.add_deliverable)) }
                            Button(
                                onClick = { scope.launch { viewModel.selectDeliverable() } },
                                modifier = Modifier
                                    .testTag("EditGoalSelectDeliverableButton")
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .weight(1f),
                                enabled = notSelectedGoalDeliverables.isNotEmpty()
                            ) { Text(
                                stringResource(R.string.select_deliverable) +
                                        if (notSelectedGoalDeliverables.isNotEmpty()) {
                                            " (${notSelectedGoalDeliverables.size})"
                                        } else {
                                            ""
                                        }
                            ) }
                        }
                    }
                    items(
                        items = selectedGoalDeliverables,
                        key = { it.id ?: Random.nextLong() }
                    ) { deliverable ->
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            DeliverableCardView(
                                deliverable,
                                viewModel::editDeliverable,
                                Modifier
                                    .weight(4f).fillMaxSize()
                                    .testTag("TasksFragmentDeliverableCard-${deliverable.id}")
                                    .padding(3.dp)
                                    .wrapContentHeight()
                            )
                            IconButton(
                                modifier = Modifier
                                    .weight(1f).fillMaxSize()
                                    .testTag("EditGoalUnpickDeliverableButton-${deliverable.id}")
                                    .background(color = Color.Red),
                                onClick = {
                                    scope.launch {
                                        viewModel.saveDeliverable(
                                            deliverable.copy(
                                                goalId = null
                                            )
                                        )
                                    }
                                }
                            ) {
                                MainActivity.Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_deliverable)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                stickyHeader(key = "EditGoalFragmentStickyAddGoalTimeBlockButton") {
                    Button(
                        onClick = { scope.launch { viewModel.addTimeBlock() } },
                        modifier = Modifier
                            .testTag("EditGoalAddTimeBlockButton")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputView(
    time: GoalTaskDeliverableTime,
    triedToSaveStateFlow: MutableStateFlow<Boolean>,
    deleteTimeBlock: suspend (GoalTaskDeliverableTime) -> Unit,
    updateGoalTaskDeliverableTime: suspend (GoalTaskDeliverableTime) -> Unit
){
    var pickedDate by remember {
        Converters().toLocalDateTime(
            time.start
        )
    }
    var pickedDuration by remember {
        Converters().toLocalDateTime(
            time.duration
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val triedToSave by triedToSaveStateFlow.collectAsStateWithLifecycle()
    val durationPickerFocusRequester = remember { time.durationPickerFocusRequester }

    Card(
        modifier = Modifier
            .testTag("EditGoalTimeInputViewCard-${time.id}")
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)) {
                PickerMenu(
                    testTagId = time.id,
                    modifier = Modifier.weight(4f),
                    options = Goal.TimeBlockType.entries,
                    selectedOption = Goal.TimeBlockType.valueOf(time.timeBlockType)
                ) { scope.launch {
                    updateGoalTaskDeliverableTime(time.copy(timeBlockType = it.name))
                } }
                IconButton(
                    modifier = Modifier
                        .testTag("EditGoalTimeInputDeleteButton-${time.id}")
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
                                        start = Converters().fromLocalDateTime(pickedDate)
                                    )
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .testTag("EditGoalTimeInputDailyTimeButton")
                            .fillMaxWidth()
                            .padding(4.dp),
                        onClick = { buttonTimePick = true }
                    ) {
                        checkDuration(stateTime, pickedDuration){ newDuration ->
                            pickedDuration = newDuration
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        duration = Converters().fromLocalDateTime(pickedDuration)
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
                                    duration = Converters().fromLocalDateTime(pickedDuration)
                                )
                            )
                        }
                    }
                }
                Goal.TimeBlockType.WEEKLY -> {
                    var selectedDay by remember { mutableStateOf(pickedDate.let{
                        AppResources.getDayWord(context,it)
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
                                    AppResources.getDayOfWeek(context,it)
                                ))
                                scope.launch {
                                    updateGoalTaskDeliverableTime(
                                        time.copy(
                                            start = Converters().fromLocalDateTime(pickedDate)
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
                                        start = Converters().fromLocalDateTime(pickedDate)
                                    )
                                )
                            }
                        }
                    }
                    OutlinedButton(modifier = Modifier
                        .testTag("EditGoalTimeInputWeeklyAndTimeButton")
                        .fillMaxWidth()
                        .padding(4.dp),
                        onClick = { buttonWeekDayPick = true }
                    ) {
                        checkDuration(stateTime,pickedDuration){ newDuration ->
                            pickedDuration = newDuration
                            scope.launch {
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        duration = Converters().fromLocalDateTime(pickedDuration)
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
                                    duration = Converters().fromLocalDateTime(pickedDuration)
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
                                updateGoalTaskDeliverableTime(
                                    time.copy(
                                        start = Converters().fromLocalDateTime(pickedDate)
                                    )
                                )
                            }
                        }
                    }
                    OutlinedButton(modifier = Modifier
                        .testTag("EditGoalTimeInputSelectDateAndTimeButton")
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
                                            duration = Converters().fromLocalDateTime(pickedDuration)
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
                                        start = Converters().fromLocalDateTime(pickedDate)
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
                                        duration = Converters().fromLocalDateTime(pickedDuration)
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
    testTagId:Long?,
    modifier: Modifier,
    options: EnumEntries<Goal.TimeBlockType>,
    selectedOption: Goal.TimeBlockType?,
    onOptionSelected: (Goal.TimeBlockType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Trigger
        OutlinedButton(modifier = Modifier
            .testTag("EditGoalPickerMenuButton-$testTagId")
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
            modifier = Modifier.testTag("EditGoalPickerMenuDropdownMenu-$testTagId"),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.testTag("EditGoalPickerMenuItem-$testTagId-${option.name}"),
                    text = { Text(stringResource(option.stringRes)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDate(state: DatePickerState, closeDialog: (Boolean)->Unit){
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        val testFunction = remember { mutableStateOf<(()->Unit)?>(null) }
        DatePickerDialog(
            modifier = Modifier.testTag("EditGoalFragmentDatePickerDialog"),
            onDismissRequest = {
                openDialog.value = false
                closeDialog(false)
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("EditGoalDatePickerDialogOKButton"),
                    onClick = {
                        openDialog.value = false
                        testFunction.value?.invoke()
                        closeDialog(true)
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("EditGoalDatePickerDialogCANCELButton"),
                    onClick = {
                        openDialog.value = false
                        closeDialog(false)
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            MainActivity.DatePicker(
                state
            ){
                testFunction.value = it
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickTime(state: TimePickerState, closeDialog: ()->Unit) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        val testFunction = remember { mutableStateOf<(()->Unit)?>(null) }
        val currentState = rememberTimePickerState(state.hour,state.minute,state.is24hour)
        TimePickerDialog(
            modifier = Modifier.testTag("EditGoalFragmentTimePickerDialog"),
            onDismissRequest = {
                openDialog.value = false
                closeDialog()
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("EditGoalTimePickerDialogOKButton"),
                    onClick = {
                        testFunction.value?.invoke()
                        openDialog.value = false
                        state.hour = currentState.hour
                        state.minute = currentState.minute
                        state.is24hour = currentState.is24hour
                        closeDialog()
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            title = { Text(stringResource(R.string.nothing)) },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("EditGoalTimePickerDialogCANCELButton"),
                    onClick = {
                        openDialog.value = false
                        closeDialog()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            MainActivity.TimePicker(
                currentState
            ){
                testFunction.value = it
            }
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
        val daysOfWeek = AppResources.Companion.DaysOfTheWeek.entries.map { stringResource(it.day) }
        Dialog(
            onDismissRequest = {
                openDialog.value = false
                onDaySelected(null)
            }
        ){
            Box(modifier = Modifier
                .testTag("EditGoalPickWeekdayDialog")
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
                                    .testTag("EditGoalPickWeekdayDay-${day}")
                                    .clip(RoundedCornerShape(16.dp))
                                    .testBackground(if (isSelected) Color.Blue else Color.LightGray)
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
        .testTag("EditGoalTimeInputPickDurationButton")
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

// Custom semantics key for background color (used in test)
val SliderRangeKey = SemanticsPropertyKey<ClosedFloatingPointRange<Float>>("SliderRange")
var SemanticsPropertyReceiver.sliderRange by SliderRangeKey
fun Modifier.setSliderRange(range: ClosedFloatingPointRange<Float>): Modifier =
    this.semantics { sliderRange = range }
@Composable
fun TimeDurationPicker(
    pickedTime: LocalDateTime,
    durationPicked:LocalDateTime?,
    onDurationSelected: (hours: Int, minutes: Int) -> Unit
) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        val selectableHours =23 - pickedTime.hour

        var selectedHours by remember {
            mutableIntStateOf(durationPicked?.hour?.let{
                if (it>selectableHours) selectableHours else it
            }?: 0)
        }

        var selectableMinutes by remember { mutableIntStateOf(
            if (selectedHours  == selectableHours) 59 - pickedTime.minute else 59
        ) }
        var selectedMinutes by remember {
            mutableIntStateOf(durationPicked?.minute?.let {
                if (it>selectableMinutes) selectableMinutes else it
            }?: 0)
        }

        LaunchedEffect(selectedHours) {
            selectableMinutes = if (selectedHours  == selectableHours) 59 - pickedTime.minute else 59
            if (selectedMinutes>selectableMinutes) selectedMinutes = selectableMinutes
        }

        Dialog(
            onDismissRequest = {
                openDialog.value = false
                onDurationSelected(selectedHours, selectedMinutes)
            }
        ) {
            Box(
                modifier = Modifier
                    .testTag("EditGoalTimeInputDurationPickerDialog")
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
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .setSliderRange(
                                    0f..selectableHours.toFloat()// only for testing purposes
                                )
                                .testTag("EditGoalDurationPickerHourSlider")
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
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .setSliderRange(
                                    0f..selectableMinutes.toFloat()// only for testing purposes
                                )
                                .testTag("EditGoalDurationPickerMinuteSlider")
                        )
                    }
                }
            }
        }
    }
}