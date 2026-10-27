package com.thando.accountable.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.DeliverableWithLists
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Task
import com.thando.accountable.database.tables.TaskDeliverable
import com.thando.accountable.fragments.TaskCardView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class DeliverableWarningDialog {
    val deliverableState = MutableStateFlow<Flow<DeliverableWithLists?>>(emptyFlow())
    @OptIn(ExperimentalCoroutinesApi::class)
    val deliverable = deliverableState.flatMapLatest { it }

    val newType = mutableStateOf(Task.TaskType.NORMAL)
    val warningTaskDeliverables = mutableStateOf<List<TaskDeliverable>>(emptyList())

    val showDeliverableWarningDialog = mutableStateOf(false)
    val updateDeliverableState = MutableStateFlow<(suspend (Deliverable) -> Unit)?>(null)
    val getTask = MutableStateFlow<((Long) -> Flow<Task?>)?>(null)

    val onDismiss = MutableStateFlow<suspend () -> Unit> {
        showDeliverableWarningDialog.value = false
    }

    val saveAndClose = MutableStateFlow<suspend () -> Unit> {
        showDeliverableWarningDialog.value = false
    }

    fun showWarning(
        deliverable: Flow<DeliverableWithLists?>,
        inputNewType: Task.TaskType,
        inputWarningTaskDeliverables: MutableList<TaskDeliverable>,
        updateDeliverable: suspend (Deliverable) -> Unit,
        inputGetTask: (Long) -> Flow<Task?>,
        deleteTaskDeliverable: suspend (TaskDeliverable) -> Unit
    ) {
        deliverableState.value = deliverable
        newType.value = inputNewType
        warningTaskDeliverables.value = inputWarningTaskDeliverables
        updateDeliverableState.value = updateDeliverable
        getTask.value = inputGetTask
        onDismiss.value = {
            showDeliverableWarningDialog.value = false
        }
        saveAndClose.value = {
            warningTaskDeliverables.value.forEach{ deleteTaskDeliverable(it) }
            deliverableState.value.first()?.let { updateDeliverable(it.deliverable.copy(
                type = newType.value.name,
                workType = TaskDeliverable.WorkType.CompleteTasks.name
            )) }
            showDeliverableWarningDialog.value = false
        }
        showDeliverableWarningDialog.value = true
    }

    @Composable
    fun DeliverableWarningDialogView() {
        var showDeliverableWarningDialog by remember { showDeliverableWarningDialog }
        val getTask by getTask.collectAsStateWithLifecycle()
        if (showDeliverableWarningDialog) {
            val taskDeliverablesList by remember { warningTaskDeliverables }
            val height = LocalResources.current.displayMetrics.heightPixels*0.6f
            val density = LocalDensity.current

            val onDismiss by onDismiss.collectAsStateWithLifecycle()
            val saveAndClose by saveAndClose.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()

            Dialog(
                onDismissRequest = {
                    scope.launch {
                        onDismiss()
                    }
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .testTag("DeliverableWarningDialog")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.the_relations_this_deliverable_has_with_the_tasks_below_will_be_deleted_and_the_progress_will_be_removed_do_you_want_to_continue),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(
                            state = rememberLazyListState(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth().height(
                                    with(density){height.toDp()}
                                )
                                .testTag("DeliverableWarningDialogLazyColumn")
                        ) {
                            items(
                                items = taskDeliverablesList,
                                key = { "${it.taskId}:${it.deliverableId}" }
                            ) { taskDeliverable ->
                                getTask?.let { getTask ->
                                    val task by getTask(taskDeliverable.taskId).collectAsStateWithLifecycle(null)
                                    task?.let { task ->
                                        TaskCardView(
                                            task,
                                            {},
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(3.dp)
                                                .wrapContentHeight()
                                        )
                                    }?: run {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        ) {
                                            // Indeterminate spinner
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { scope.launch { saveAndClose() } },
                                modifier = Modifier
                                    .testTag("DeliverableWarningDialogOKButton")
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .weight(1f),
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Green,
                                    contentColor = Color.Black
                                )
                            ) { Text(stringResource(R.string.ok)) }
                            Button(
                                onClick = {
                                    scope.launch {
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .testTag("DeliverableWarningDialogDismissButton")
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .weight(1f),
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.Black
                                )
                            ) { Text(
                                stringResource(R.string.cancel)
                            ) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeliverableWarningDialogPreview() {
    val dialog = DeliverableWarningDialog()

    // Fake deliverable for preview
    val fakeDeliverable = DeliverableWithLists(Deliverable(
        deliverableId = 1L,
        parent = 1L,
        position = 0L,
        type = Task.TaskType.QUANTITY.name,
        endDateTime = Converters().fromLocalDateTime(LocalDateTime.now().plusDays(5)),
        endType = Deliverable.DeliverableEndType.WORK.name,
        deliverable = "Complete Task Deliverable Module 2 times",
        location = "At My Desk",
    ),
        timesRaw = listOf(
            GoalTaskDeliverableTime(
                id = 1L,
                parent = 1L,
                type = GoalTaskDeliverableTime.TimesType.DELIVERABLE.name,
                timeBlockType = Goal.TimeBlockType.DAILY.name,
                duration = Converters().fromLocalDateTime(LocalDateTime.now().withHour(0).withMinute(1)),
            )
        ))

    val warningTaskDeliverables = mutableListOf<TaskDeliverable>()

    // Set up preview state
    LaunchedEffect(Unit) {
        dialog.showWarning(
            inputNewType = Task.TaskType.QUANTITY,
            deliverable = flowOf(fakeDeliverable),
            inputWarningTaskDeliverables = warningTaskDeliverables,
            updateDeliverable = { /* no-op for preview */ },
            inputGetTask = { _ -> flowOf(null) },
            deleteTaskDeliverable = {}
        )
    }

    dialog.DeliverableWarningDialogView()
}