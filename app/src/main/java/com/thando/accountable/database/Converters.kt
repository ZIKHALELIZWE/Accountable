package com.thando.accountable.database

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.room.TypeConverter
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AppResources
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Deliverable.DeliverableEndType
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Goal.TimeBlockType
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.Task
import com.thando.accountable.database.tables.Task.TaskEndType
import com.thando.accountable.database.tables.Task.TaskParentType
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

class Converters {
    @TypeConverter
    fun toCalendarResource(l: Long): AppResources.CalendarResource {
        val c: Calendar = Calendar.getInstance()
        c.setTimeInMillis(l)
        return AppResources.CalendarResource(c)
    }

    @TypeConverter
    fun fromCalendarResource(c: AppResources.CalendarResource): Long {
        return c.convertToLong()
    }

    @TypeConverter
    fun fromContentType(contentType: Content.ContentType): String{
        return contentType.name
    }

    @TypeConverter
    fun toContentType(contentType: String): Content.ContentType{
        return Content.ContentType.valueOf(contentType)
    }

    @TypeConverter
    fun fromScriptParentType(parentType: Script.ScriptParentType): String{
        return parentType.name
    }

    @TypeConverter
    fun toScriptParentType(parentType: String): Script.ScriptParentType{
        return Script.ScriptParentType.valueOf(parentType)
    }

    @TypeConverter
    fun fromFolderType(folderType: Folder.FolderType): String{
        return folderType.name
    }

    @TypeConverter
    fun toFolderType(folderType: String): Folder.FolderType{
        return Folder.FolderType.valueOf(folderType)
    }

    @TypeConverter
    fun toString(mutableStateFlow: MutableStateFlow<String?>): String? {
        return mutableStateFlow.value
    }

    @TypeConverter
    fun fromString(string: String?): MutableStateFlow<String?> {
        return MutableStateFlow(string)
    }

    @TypeConverter
    fun toStateFlowString(mutableStateFlow: MutableStateFlow<String>): String {
        return mutableStateFlow.value
    }

    @TypeConverter
    fun fromStateFlowString(string: String): MutableStateFlow<String> {
        return MutableStateFlow(string)
    }

    @TypeConverter
    fun toStateString(mutableState: MutableState<String>): String {
        return mutableState.value
    }

    @TypeConverter
    fun fromStateString(string: String): MutableState<String> {
        return mutableStateOf(string)
    }

    @TypeConverter
    fun toTextFieldState(textFieldState: TextFieldState): String {
        return textFieldState.text.toString()
    }

    @TypeConverter
    fun fromTextFieldState(string: String): TextFieldState {
        return TextFieldState(string)
    }

    @TypeConverter
    fun toStateFlowInt(mutableStateFlow: MutableStateFlow<Int>): Int {
        return mutableStateFlow.value
    }

    @TypeConverter
    fun fromStateFlowInt(int: Int): MutableStateFlow<Int> {
        return MutableStateFlow(int)
    }

    @TypeConverter
    fun toStateInt(mutableState: MutableState<Int>): Int {
        return mutableState.value
    }

    @TypeConverter
    fun fromStateInt(int: Int): MutableState<Int> {
        return mutableIntStateOf(int)
    }

    @TypeConverter
    fun toStateLong(mutableState: MutableState<Long>): Long {
        return mutableState.value
    }

    @TypeConverter
    fun fromStateLong(long: Long): MutableState<Long> {
        return mutableLongStateOf(long)
    }

    @TypeConverter
    fun toScrollStateLazyNull(lazyListState: LazyListState?): Long? {
        return lazyListState?.firstVisibleItemIndex?.let { firstVisibleItemIndex ->
            packInts(firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
        }
    }

    @TypeConverter
    fun fromScrollStateLazyNull(scrollValue: Long?): LazyListState? {
        return scrollValue?.let{LazyListState(unpackInt1(it),unpackInt2(it))}
    }

    @TypeConverter
    fun toScrollStateLazy(lazyListState: LazyListState): Long {
        return packInts(lazyListState.firstVisibleItemIndex,lazyListState.firstVisibleItemScrollOffset)
    }

    @TypeConverter
    fun fromScrollStateLazy(scrollValue: Long): LazyListState {
        return LazyListState(unpackInt1(scrollValue), unpackInt2(scrollValue))
    }

    @TypeConverter
    fun toScrollState(scrollState: ScrollState): Int {
        return scrollState.value
    }

    @TypeConverter
    fun fromScrollState(scrollValue: Int): ScrollState {
        return ScrollState(scrollValue)
    }

    @TypeConverter
    fun toStateBoolean(mutableStateFlow: MutableStateFlow<Boolean>): Boolean {
        return mutableStateFlow.value
    }

    @TypeConverter
    fun fromStateBoolean(boolean: Boolean): MutableStateFlow<Boolean> {
        return MutableStateFlow(boolean)
    }

    @TypeConverter
    fun toStateFlowFloat(mutableStateFlow: MutableStateFlow<Float>): Float {
        return mutableStateFlow.value
    }

    @TypeConverter
    fun fromStateFlowFloat(float: Float): MutableStateFlow<Float> {
        return MutableStateFlow(float)
    }

    @TypeConverter
    fun toStateFloat(mutableState: MutableState<Float>): Float {
        return mutableState.value
    }

    @TypeConverter
    fun fromStateFloat(float: Float): MutableState<Float> {
        return mutableFloatStateOf(float)
    }

    @TypeConverter
    fun toAccountableFragment(accountableFragment: AccountableNavigationController.AccountableFragment): Int {
        return AccountableNavigationController.getFragmentId(accountableFragment)
    }

    @TypeConverter
    fun fromStateFloat(fragmentInt: Int): AccountableNavigationController.AccountableFragment {
        return AccountableNavigationController.getFragmentFromId(fragmentInt)
    }

    @TypeConverter
    fun toStateCalendar(mutableStateFlow: MutableStateFlow<AppResources.CalendarResource>): Long {
        return fromCalendarResource(mutableStateFlow.value)
    }

    @TypeConverter
    fun fromStateCalendar(long: Long): MutableStateFlow<AppResources.CalendarResource> {
        return MutableStateFlow(toCalendarResource(long))
    }

    @TypeConverter
    fun toStatePairs(string: String): Pair<MutableStateFlow<String>,MutableStateFlow<String>> {
        val index = string.indexOf(MarkupLanguage.VALUE_OPENING)
        return if (index==-1) {
            Pair(MutableStateFlow(string), MutableStateFlow(""))
        } else{
            Pair(MutableStateFlow(string.substring(0, index)),MutableStateFlow(string.substring(index + 2, string.length - 1)))
        }
    }

    @TypeConverter
    fun fromStatePairs(pair: Pair<MutableStateFlow<String>,MutableStateFlow<String>>): String {
        return if (pair.second.value.isEmpty()) pair.first.value
        else StringBuilder(pair.first.value)
            .append(MarkupLanguage.VALUE_OPENING)
            .append(pair.second.value)
            .append(MarkupLanguage.VALUE_CLOSING)
            .toString()
    }

    @TypeConverter
    fun toStateTextPairs(string: String): Pair<TextFieldState,TextFieldState> {
        val index = string.indexOf(MarkupLanguage.VALUE_OPENING)
        return if (index==-1) {
            Pair(TextFieldState(string), TextFieldState(""))
        } else{
            Pair(TextFieldState(string.substring(0, index)),TextFieldState(string.substring(index + 2, string.length - 1)))
        }
    }

    @TypeConverter
    fun fromStateTextPairs(pair: Pair<TextFieldState,TextFieldState>): String {
        return if (pair.second.text.isEmpty()) pair.first.text.toString()
        else StringBuilder(pair.first.text.toString())
            .append(MarkupLanguage.VALUE_OPENING)
            .append(pair.second.text.toString())
            .append(MarkupLanguage.VALUE_CLOSING)
            .toString()
    }

    @TypeConverter
    fun toGoalStatus(mutableState: MutableState<Goal.Status>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromGoalStatus(goalStatus: String): MutableState<Goal.Status> {
        return mutableStateOf(Goal.Status.valueOf(goalStatus))
    }

    @TypeConverter
    fun toGoalTab(mutableState: MutableState<Goal.GoalTab>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromGoalTab(goalStatus: String): MutableState<Goal.GoalTab> {
        return mutableStateOf(Goal.GoalTab.valueOf(goalStatus))
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): Long {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    @TypeConverter
    fun fromLocalDateTimeMutable(dateTime: MutableState<LocalDateTime>): Long {
        return dateTime.value.toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    @TypeConverter
    fun toLocalDateTime(millis: Long): MutableState<LocalDateTime> {
        return mutableStateOf(LocalDateTime.ofEpochSecond(
            millis / 1000, 0, ZoneOffset.UTC
        ))
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: MutableState<LocalDateTime?>): Long? {
        return dateTime.value?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }
    @TypeConverter
    fun toLocalDateTime(millis: Long?): MutableState<LocalDateTime?> {
        return mutableStateOf(millis?.let { millis -> LocalDateTime.ofEpochSecond(
            millis / 1000, 0, ZoneOffset.UTC
        )})
    }

    @TypeConverter
    fun toTimeBlockTimeStatus(mutableState: MutableState<TimeBlockType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTimeBlockTimeStatus(goalStatus: String): MutableState<TimeBlockType> {
        return mutableStateOf(TimeBlockType.valueOf(goalStatus))
    }

    @TypeConverter
    fun toTaskParentType(mutableState: MutableState<TaskParentType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTaskParentType(taskParentType: String): MutableState<TaskParentType> {
        return mutableStateOf(TaskParentType.valueOf(taskParentType))
    }

    @TypeConverter
    fun toTaskEndType(mutableState: MutableState<TaskEndType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTaskEndType(taskEndType: String): MutableState<TaskEndType> {
        return mutableStateOf(TaskEndType.valueOf(taskEndType))
    }

    @TypeConverter
    fun toDeliverableEndType(mutableState: MutableState<DeliverableEndType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromDeliverableEndType(deliverableEndType: String): MutableState<DeliverableEndType> {
        return mutableStateOf(DeliverableEndType.valueOf(deliverableEndType))
    }

    @TypeConverter
    fun toTimesType(mutableState: MutableState<GoalTaskDeliverableTime.TimesType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTimesType(timesType: String): MutableState<GoalTaskDeliverableTime.TimesType> {
        return mutableStateOf(GoalTaskDeliverableTime.TimesType.valueOf(timesType))
    }

    @TypeConverter
    fun toTaskType(mutableState: MutableState<Task.TaskType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTaskType(taskType: String): MutableState<Task.TaskType> {
        return mutableStateOf(Task.TaskType.valueOf(taskType))
    }

    @TypeConverter
    fun toStateLongNull(mutableState: MutableState<Long?>): Long? {
        return mutableState.value
    }

    @TypeConverter
    fun fromStateLongNull(long: Long?): MutableState<Long?> {
        return mutableStateOf(long)
    }

    @TypeConverter
    fun toGoalEndType(mutableState: MutableState<Goal.GoalEndType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromGoalEndType(goalEndType: String): MutableState<Goal.GoalEndType> {
        return mutableStateOf(Goal.GoalEndType.valueOf(goalEndType))
    }
}