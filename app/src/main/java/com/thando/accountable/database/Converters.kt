package com.thando.accountable.database

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.room.TypeConverter
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AppResources
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Goal.TimeBlockType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
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
    fun toScrollState(lazyListState: LazyListState?): Int {
        return lazyListState?.firstVisibleItemIndex?:0
    }

    @TypeConverter
    fun fromScrollStateLazy(scrollValue: Int): LazyListState {
        return LazyListState(scrollValue)
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
    fun toGoalStatus(mutableState: MutableState<Goal.GoalStatus>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromGoalStatus(goalStatus: String): MutableState<Goal.GoalStatus> {
        return mutableStateOf(Goal.GoalStatus.valueOf(goalStatus))
    }

    @TypeConverter
    fun toStateLocalDateTime(time: MutableState<LocalDateTime>): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return time.value.format(formatter)
    }

    @TypeConverter
    fun fromStateLocalDateTime(time: String): MutableState<LocalDateTime> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        try {
            return mutableStateOf(LocalDateTime.parse(time, formatter))
        } catch (e: Exception) {
            e.printStackTrace()
            return mutableStateOf(LocalDateTime.now())
        }
    }

    @TypeConverter
    fun toTimeBlockTimeStatus(mutableState: MutableState<TimeBlockType>): String {
        return mutableState.value.name
    }

    @TypeConverter
    fun fromTimeBlockTimeStatus(goalStatus: String): MutableState<TimeBlockType> {
        return mutableStateOf(TimeBlockType.valueOf(goalStatus))
    }
}