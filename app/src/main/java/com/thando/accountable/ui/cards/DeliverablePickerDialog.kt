package com.thando.accountable.ui.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.thando.accountable.database.tables.TaskDeliverable
import com.thando.accountable.fragments.DeliverableCardView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DeliverablePickerDialog {

    val showDeliverablePickerDialog = mutableStateOf(false)
    val notSelectedListState = MutableStateFlow<Flow<List<Deliverable>>>(emptyFlow())
    val notSelectedList = notSelectedListState.flatMapLatest { it }

    val onDismiss = MutableStateFlow<suspend () -> Unit> {
        showDeliverablePickerDialog.value = false
    }
    fun pickDeliverable(
        notSelectedList: Flow<List<Deliverable>>
    ) {
        notSelectedListState.value = notSelectedList
        showDeliverablePickerDialog.value = true
    }

    @Composable
    fun DeliverableListDialog() {
        var showDeliverablePickerDialog by remember { showDeliverablePickerDialog }
        if (showDeliverablePickerDialog) {
            val notSelectedList by notSelectedList.collectAsStateWithLifecycle(emptyList())
            val onDismiss by onDismiss.collectAsStateWithLifecycle()
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
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.pick_deliverables),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(items = notSelectedList, key = {it.deliverableId?:Random.nextLong()}) { deliverable ->
                            DeliverableCardView(
                                deliverable,
                                {},
                                Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeliverablePickerDialogPreview() {
    val dialog = DeliverablePickerDialog()

    // Fake deliverable for preview
    val fakeDeliverable = Deliverable(
        deliverableId = 1L,
        parent = 1L,
        position = 0L,
        endDateTime = Converters().fromLocalDateTime(LocalDateTime.now().plusDays(5)),
        endType = Deliverable.DeliverableEndType.WORK.name,
        deliverable = "Complete Task Deliverable Module 2 times",
        location = "At My Desk",
        workType = TaskDeliverable.WorkType.QuantityTaskOnce.name
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
    val fakeDeliverableTwo = Deliverable(
        deliverableId = 2L,
        parent = 1L,
        position = 1L,
        endDateTime = Converters().fromLocalDateTime(LocalDateTime.now().plusDays(6)),
        endType = Deliverable.DeliverableEndType.WORK.name,
        deliverable = "Jump 2 Times",
        location = "On My Desk",
        workType = TaskDeliverable.WorkType.QuantityTaskOnce.name
    ).apply {
        timesState.value = flowOf(listOf(
            GoalTaskDeliverableTime(
                id = 2L,
                parent = 2L,
                type = GoalTaskDeliverableTime.TimesType.DELIVERABLE.name,
                timeBlockType = Goal.TimeBlockType.DAILY.name,
                duration = Converters().fromLocalDateTime(LocalDateTime.now().withHour(0).withMinute(1)),
            )
        ))
    }

    // Set up preview state
    LaunchedEffect(Unit) {
        dialog.pickDeliverable(
            flowOf(
                listOf(
                    fakeDeliverable,
                    fakeDeliverableTwo
                )
            )
        )
    }

    dialog.DeliverableListDialog()
}