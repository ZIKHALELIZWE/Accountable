package com.thando.accountable.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.NormalQuantityTimeTextField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class DeliverableAdderDialog {
    val deliverableState = MutableStateFlow<Flow<Deliverable?>>(emptyFlow())
    @OptIn(ExperimentalCoroutinesApi::class)
    val deliverable = deliverableState.flatMapLatest { it }
    val showDeliverableAdderDialog = mutableStateOf(false)
    val taskType = mutableStateOf(Task.TaskType.NORMAL)
    val triedToSaveState = MutableStateFlow(MutableStateFlow(false))
    @OptIn(ExperimentalCoroutinesApi::class)
    val triedToSave = triedToSaveState.flatMapLatest { it }
    val updateDeliverableState = MutableStateFlow<(suspend (Deliverable) -> Unit)?>(null)

    val onDismiss = MutableStateFlow<suspend () -> Unit> {
        showDeliverableAdderDialog.value = false
    }

    val saveAndClose = MutableStateFlow<suspend () -> Unit> {
        showDeliverableAdderDialog.value = false
    }

    fun addDeliverable(
        inputTaskType: Task.TaskType,
        deliverable: Flow<Deliverable?>,
        updateDeliverable: suspend (Deliverable) -> Unit,
        deleteDeliverable: suspend (Deliverable) -> Unit,
        triedToSave: MutableStateFlow<Boolean>
    ) {
        deliverableState.value = deliverable
        taskType.value = inputTaskType
        triedToSaveState.value = triedToSave
        updateDeliverableState.value = updateDeliverable
        onDismiss.value = {
            deliverable.first()?.let { deleteDeliverable(it) }
            showDeliverableAdderDialog.value = false
        }
        saveAndClose.value = {
            deliverable.first()?.let { updateDeliverable(it) }
            showDeliverableAdderDialog.value = false
        }
        showDeliverableAdderDialog.value = true
    }

    @Composable
    fun DeliverableDialog() {
        var showDeliverableAdderDialog by remember { showDeliverableAdderDialog }
        if (showDeliverableAdderDialog) {
            val taskType by remember { taskType }

            val deliverable by deliverable.collectAsStateWithLifecycle(null)
            deliverable?.let { deliverable ->
                val deliverableWorkText = remember { TextFieldState(deliverable.deliverable) }
                val deliverableWorkTextFocusRequester =
                    remember { deliverable.deliverableTextFocusRequester }
                var deliverableTime by remember {
                    Converters().toLocalDateTime(
                        deliverable.time
                    )
                }
                val triedToSave by triedToSave.collectAsStateWithLifecycle(false)
                val updateDeliverable by updateDeliverableState.collectAsStateWithLifecycle()
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
                            .testTag("DeliverableAdderDialog")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.enter_deliverable_information),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            NormalQuantityTimeTextField(
                                modifier = Modifier
                                    .testTag("TasksFragmentTaskDeliverableWorkInputTextField")
                                    .focusRequester(deliverableWorkTextFocusRequester),
                                taskType = taskType,
                                textFieldState = deliverableWorkText,
                                triedToSave = triedToSave,
                                updateTextFieldState = { newValue, newQuantity ->
                                    if (newValue != deliverable.deliverable) {
                                        updateDeliverable?.invoke(
                                            deliverable.copy(
                                                deliverable = newValue,
                                                quantity = newQuantity
                                            )
                                        )
                                        deliverableWorkText.edit {
                                            replace(
                                                0,
                                                length,
                                                newValue
                                            )
                                        }
                                    }
                                },
                                isDeliverable = true,
                                updateTaskTask = { newTaskTask, newQuantity ->
                                    updateDeliverable?.invoke(
                                        deliverable.copy(
                                            deliverable = newTaskTask,
                                            quantity = newQuantity
                                        )
                                    )
                                },
                                updateTaskTime = { newTime ->
                                    updateDeliverable?.invoke(deliverable.copy(time = newTime))
                                },
                                taskTime = deliverableTime,
                                changeTaskTime = { newTaskTime ->
                                    deliverableTime = newTaskTime
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { scope.launch { saveAndClose() } },
                                    modifier = Modifier
                                        .testTag("DeliverableAdderDialogOKButton")
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .weight(1f),
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green,
                                        contentColor = Color.Black
                                    )
                                ) { Text(stringResource(R.string.add_deliverable)) }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .testTag("DeliverableAdderDialogDismissButton")
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
}

@Preview(showBackground = true)
@Composable
fun DeliverableAdderDialogPreview() {
    val dialog = DeliverableAdderDialog()

    // Fake deliverable for preview
    val fakeDeliverable = Deliverable(
        deliverableId = 1L,
        parent = 1L,
        position = 0L,
        endDateTime = Converters().fromLocalDateTime(LocalDateTime.now().plusDays(5)),
        endType = Deliverable.DeliverableEndType.WORK.name,
        deliverable = "Complete Task Deliverable Module 2 times",
        location = "At My Desk",
    ).apply {
        timesState.value = flowOf(listOf(
            GoalTaskDeliverableTime(
                id = 1L,
                parent = 1L,
                type = GoalTaskDeliverableTime.TimesType.DELIVERABLE.name,
                timeBlockType = Goal.TimeBlockType.DAILY.name,
                duration = Converters().fromLocalDateTime(LocalDateTime.now().withHour(0).withMinute(1)),
            )
        ))
    }

    // Set up preview state
    LaunchedEffect(Unit) {
        dialog.addDeliverable(
            inputTaskType = Task.TaskType.QUANTITY,
            deliverable = flowOf(fakeDeliverable),
            updateDeliverable = { /* no-op for preview */ },
            deleteDeliverable = { /* no-op for preview */ },
            triedToSave = MutableStateFlow(false)
        )
    }

    dialog.DeliverableDialog()
}