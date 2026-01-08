package com.thando.accountable.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlin.enums.EnumEntries
import kotlin.random.Random

class EditGoalFragment: Fragment() {

    private val viewModel: EditGoalViewModel by viewModels { EditGoalViewModel.Factory }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { galleryUri ->
        try{
            viewModel.setImage(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AccountableTheme {
                    Scaffold (modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(stringResource(R.string.new_goal)) },
                                actions = {
                                    IconButton(onClick = {
                                        viewModel.saveAndCloseGoal()
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
                        EditGoalFragmentView(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    @Composable
    fun EditGoalFragmentView(modifier: Modifier = Modifier){
        val newGoal by remember { viewModel.newGoal }
        newGoal?.let { newGoal ->
            val scrollState = remember { newGoal.scrollPosition }
            val uri by remember { newGoal.getStateUri(requireContext()) }
            var goal by remember { newGoal.goal }
            var colour by remember { newGoal.colour }
            var location by remember { newGoal.location }
            val times = remember { newGoal.times }
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val scope = rememberCoroutineScope()

            Column(
                modifier = modifier.imePadding().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text(stringResource(R.string.goal)) },
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                uri?.let {
                    Image(
                        bitmap = AppResources.getBitmapFromUri(LocalContext.current, it)
                            ?.asImageBitmap()
                            ?: ImageBitmap(1, 1),
                        contentDescription = stringResource(R.string.goal_image),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        //enabled =
                    ) { Text(stringResource(R.string.choose_image)) }
                    uri?.let {
                        Button(
                            onClick = { viewModel.removeImage() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            // enabled =
                        ) { Text(stringResource(R.string.remove_image)) }
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.location)) },
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (colour != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(12.dp)
                                .background(Color(colour), shape = RoundedCornerShape(8.dp))
                                .weight(1f)
                        )
                    }
                    Button(
                        onClick = { viewModel.pickColour(requireContext()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .weight(2f)
                    ) {
                        Text(stringResource(R.string.pick_colour))
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                var buttonHeightPx by remember { mutableIntStateOf(0) }
                Button(
                    onClick = {viewModel.addTimeBlock()},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .onGloballyPositioned { coordinates ->
                            // Get height in pixels
                            buttonHeightPx = coordinates.size.height
                        }
                ) { Text(stringResource(R.string.add_time_block)) }
                LazyColumn(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .height((LocalWindowInfo.current.containerSize.height - buttonHeightPx * 2).dp)
                ) {
                    items(items = times, key = { it.id?.toInt()?:Random.nextInt() }) { item ->
                        TimeInputView(item)
                        if (times.indexOf(item) != times.lastIndex) {
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }
        }?: run {
            // New Goal not loaded yet
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()) {
                // Indeterminate spinner
                CircularProgressIndicator()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TimeInputView(time: GoalTaskDeliverableTime){
        var timeBlockType by remember { time.timeBlockType }
        var pickedDate by remember { time.start }
        var pickedDuration by remember { time.duration }
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
                        selectedOption = timeBlockType
                    ) { timeBlockType = it }
                    IconButton(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .weight(1f)
                            .background(color = Color.Red),
                        onClick = { viewModel.deleteTimeBlock(time) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_time_block)
                        )
                    }
                }
                when(timeBlockType){
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
                            }
                        }
                        OutlinedButton(modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                            onClick = { buttonTimePick = true }
                        ) {
                            checkDuration(stateTime, pickedDuration){ newDuration ->
                                pickedDuration = newDuration
                            }
                            val c = Calendar.getInstance()
                            c.set(Calendar.HOUR_OF_DAY, pickedDate.hour)
                            c.set(Calendar.MINUTE, pickedDate.minute)
                            val date = AppResources.CalendarResource(c)
                            Text("Start Time: ${date.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()}")
                        }
                        DurationPickerButton(pickedDate, pickedDuration){ newDuration ->
                            pickedDuration = newDuration
                        }
                    }
                    Goal.TimeBlockType.WEEKLY -> {
                        var selectedDay by remember { mutableStateOf(pickedDate.let{
                            when(it.dayOfWeek) {
                                DayOfWeek.MONDAY -> requireContext().getString(R.string.Mon)
                                DayOfWeek.TUESDAY -> requireContext().getString(R.string.Tue)
                                DayOfWeek.WEDNESDAY -> requireContext().getString(R.string.Wed)
                                DayOfWeek.THURSDAY -> requireContext().getString(R.string.Thu)
                                DayOfWeek.FRIDAY -> requireContext().getString(R.string.Fri)
                                DayOfWeek.SATURDAY -> requireContext().getString(R.string.Sat)
                                DayOfWeek.SUNDAY -> requireContext().getString(R.string.Sun)
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
                                            requireContext().getString(R.string.Mon) -> DayOfWeek.MONDAY
                                            requireContext().getString(R.string.Tue) -> DayOfWeek.TUESDAY
                                            requireContext().getString(R.string.Wed) -> DayOfWeek.WEDNESDAY
                                            requireContext().getString(R.string.Thu) -> DayOfWeek.THURSDAY
                                            requireContext().getString(R.string.Fri) -> DayOfWeek.FRIDAY
                                            requireContext().getString(R.string.Sat) -> DayOfWeek.SATURDAY
                                            requireContext().getString(R.string.Sun) -> DayOfWeek.SUNDAY
                                            else -> DayOfWeek.MONDAY
                                        }
                                    ))
                                    buttonTimePick = true
                                }
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
                            onClick = { buttonWeekDayPick = true }
                        ) {
                            checkDuration(stateTime,pickedDuration){ newDuration ->
                                pickedDuration = newDuration
                            }
                            val c = Calendar.getInstance()
                            c.set( Calendar.HOUR_OF_DAY, stateTime.hour)
                            c.set( Calendar.MINUTE, stateTime.minute)
                            val date = AppResources.CalendarResource(c)
                            Text("Start Time and Weekday: ${date.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()} $selectedDay")
                        }
                        DurationPickerButton(pickedDate,pickedDuration){ newDuration ->
                            pickedDuration = newDuration
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
                        val stateDate = rememberDatePickerState()
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
                            stateDate.selectedDateMillis?.let {
                                val c = Calendar.getInstance()
                                c.timeInMillis = it
                                c.set( Calendar.HOUR_OF_DAY, stateTime.hour)
                                c.set( Calendar.MINUTE, stateTime.minute)
                                val l = LocalDateTime.of(
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH),
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE))
                                checkDuration(stateTime,pickedDuration){ newDuration ->
                                    pickedDuration = newDuration
                                }
                                pickedDate = l
                                val date = AppResources.CalendarResource(c)
                                Text("Start Time and Date: ${date.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()} ${date.getStandardDate(requireContext())}")
                            }?: run {
                                Text(stringResource(R.string.pick_time_frequency))
                            }
                        }
                        stateDate.selectedDateMillis?.let {
                            DurationPickerButton(pickedDate,pickedDuration){ newDuration ->
                                pickedDuration = newDuration
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
    fun DurationPickerButton(datePicked:LocalDateTime, duration:LocalDateTime, changeDuration:(LocalDateTime)->Unit){
        val buttonDurationPick = remember { mutableStateOf(false) }
        if (buttonDurationPick.value) {
            TimeDurationPicker(datePicked, duration) { hours, minutes ->
                changeDuration(datePicked.withHour(hours).withMinute(minutes))
                buttonDurationPick.value = false
            }
        }

        OutlinedButton(modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
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
                Text("Please Select A Duration")
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
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
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
                                modifier = Modifier.width(
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

    override fun onResume() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    viewModel.closeGoal()
                }
            }
        )
        super.onResume()
    }
}