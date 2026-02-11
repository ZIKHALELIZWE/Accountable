package com.thando.accountable

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.thando.accountable.database.tables.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.Calendar


sealed class AppResources {

    companion object{
        fun uriExists(context: Context, uri: Uri): Boolean {
            return try {
                context.contentResolver.openInputStream(uri)?.use {
                    // Successfully opened, so it exists
                }
                true
            } catch (e: FileNotFoundException) {
                false // File not found
            } catch (e: SecurityException) {
                false // No permission to access
            } catch (e: IOException) {
                false // Other I/O error
            }
        }

        fun getBitmapFromByteArray(byteArray: ByteArray?) : Bitmap?{
            var bitmap: Bitmap? = null
            val option = BitmapFactory.Options()
            if (byteArray != null)
                bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, option)
            return bitmap
        }

        fun getUriFromDrawable(context:Context,@AnyRes resourceId: Int): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.resources.getResourcePackageName(resourceId))
                .appendPath(context.resources.getResourceTypeName(resourceId))
                .appendPath(context.resources.getResourceEntryName(resourceId))
                .build()
        }

        @SuppressLint("DiscouragedApi")
        fun getDrawableFromUri(
            context: Context,
            inputUri: Uri
        ) : Drawable? {
            if(inputUri.toString().contains(ContentResolver.SCHEME_ANDROID_RESOURCE)
                && inputUri.toString().contains("/drawable/")) {
                val resourceName = inputUri.lastPathSegment?.substringBefore(".")
                if (resourceName != null) {
                    val imageResource = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                    return ResourcesCompat.getDrawable(context.resources, imageResource, null)
                } else return null
            }
            else return Drawable.createFromStream(context.contentResolver.openInputStream(inputUri),null)
        }

        @SuppressLint("DiscouragedApi")
        fun getBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
            return try {
                if(imageUri.toString().contains(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    && imageUri.toString().contains("/drawable/")) {
                    return getDrawableFromUri(context,imageUri)?.toBitmap()
                }
                else {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        val ContentTypeAccessor = mapOf(
            ContentType.IMAGE to "image/*",
            ContentType.DOCUMENT to "text/*",
            ContentType.AUDIO to "audio/*",
            ContentType.VIDEO to "video/*"
        )

        fun getTime(localDateTime: LocalDateTime): String{
            return getDoubleDigitString(
                localDateTime.hour.toString()
            ) + ":" + getDoubleDigitString(localDateTime.minute.toString())
        }

        @OptIn(ExperimentalMaterial3Api::class)
        fun getTime(timePickerState: TimePickerState): String{
            return getDoubleDigitString(
                timePickerState.hour.toString()
            ) + ":" + getDoubleDigitString(timePickerState.minute.toString())
        }

        private fun getDoubleDigitString(value:String): String{
            return when (value) {
                "0" -> "00"
                "1" -> "01"
                "2" -> "02"
                "3" -> "03"
                "4" -> "04"
                "5" -> "05"
                "6" -> "06"
                "7" -> "07"
                "8" -> "08"
                "9" -> "09"
                else -> value
            }
        }

        fun getStandardDate(context: Context, localDateTime: LocalDateTime): String{
            return getDayNum(localDateTime)+" "+getMonthYear(context, localDateTime)
        }

        fun getMonthYear(context: Context,localDateTime: LocalDateTime): String{
            return getMonth(
                localDateTime.monthValue.toString(),
                context
            ) + " " + localDateTime.year.toString()
        }

        private fun getMonth(month: String?, context: Context): String {
            return when (month) {
                "1", "01" -> context.getString(R.string.Jan)
                "2", "02" -> context.getString(R.string.Feb)
                "3", "03" -> context.getString(R.string.Mar)
                "4", "04" -> context.getString(R.string.Apr)
                "5", "05" -> context.getString(R.string.May)
                "6", "06" -> context.getString(R.string.Jun)
                "7", "07" -> context.getString(R.string.Jul)
                "8", "08" -> context.getString(R.string.Aug)
                "9", "09" -> context.getString(R.string.Sep)
                "10" -> context.getString(R.string.Oct)
                "11" -> context.getString(R.string.Nov)
                else -> context.getString(R.string.Dec)
            }
        }

        fun getFullDate(context: Context, localDateTime: LocalDateTime): String{
            return getDayWord(
                context,
                localDateTime
            )+" "+getDayNum(localDateTime)+" "+getMonthYear(context,localDateTime)
        }

        fun getTimeFullDate(context: Context, localDateTime: LocalDateTime): String {
            return getTime(localDateTime)+" "+getFullDate(context,localDateTime)
        }

        fun getDaysFromToday(localDateTime: LocalDateTime): String{
            val now = LocalDateTime.now()
            return Period.between(
                now.toLocalDate(),
                localDateTime.toLocalDate()
            ).days.toString()
        }

        fun getDayNum(localDateTime: LocalDateTime): String{
            return getDoubleDigitString(localDateTime.dayOfMonth.toString())
        }

        fun getDayWord(context: Context, localDateTime: LocalDateTime): String{
            return when (localDateTime.dayOfWeek) {
                DayOfWeek.FRIDAY -> context.getString(R.string.Fri)
                DayOfWeek.MONDAY -> context.getString(R.string.Mon)
                DayOfWeek.SATURDAY -> context.getString(R.string.Sat)
                DayOfWeek.SUNDAY -> context.getString(R.string.Sun)
                DayOfWeek.THURSDAY -> context.getString(R.string.Thu)
                DayOfWeek.TUESDAY -> context.getString(R.string.Tue)
                DayOfWeek.WEDNESDAY -> context.getString(R.string.Wed)
            }
        }

        enum class DaysOfTheWeek(val day:Int) {
            Mon(R.string.Mon),
            Tue(R.string.Tue),
            Wed(R.string.Wed),
            Thu(R.string.Thu),
            Fri(R.string.Fri),
            Sat(R.string.Sat),
            Sun(R.string.Sun)
        }

        fun getDayOfWeek(context: Context, day: String): DayOfWeek{
            return when(day) {
                context.getString(R.string.Mon) -> DayOfWeek.MONDAY
                context.getString(R.string.Tue) -> DayOfWeek.TUESDAY
                context.getString(R.string.Wed) -> DayOfWeek.WEDNESDAY
                context.getString(R.string.Thu) -> DayOfWeek.THURSDAY
                context.getString(R.string.Fri) -> DayOfWeek.FRIDAY
                context.getString(R.string.Sat) -> DayOfWeek.SATURDAY
                context.getString(R.string.Sun) -> DayOfWeek.SUNDAY
                else -> DayOfWeek.MONDAY
            }
        }

        @Composable
        fun getDurationString(duration: LocalDateTime): String{
            val builder = StringBuilder()
            builder.append(if (duration.hour == 1) stringResource(
                R.string.hour_pre_arg,
                duration.hour
            )
            else if (duration.hour != 0) stringResource(R.string.hours_pre_arg, duration.hour) else "")
            builder.append(if (duration.hour != 0 && duration.minute != 0) stringResource(R.string.and_with_spaces) else "")
            builder.append(if (duration.minute == 1) stringResource(
                R.string.minute_pre_arg,
                duration.minute
            )
            else if (duration.minute != 0) stringResource(R.string.minutes_pre_arg, duration.minute) else "")
            builder.append(if (duration.hour==0 && duration.minute==0) stringResource(R.string.please_select_a_duration) else "")
            return builder.toString()
        }

        fun getDurationString(context: Context, duration: LocalDateTime): String{
            val builder = StringBuilder()
            builder.append(if (duration.hour == 1) context.getString(
                R.string.hour_pre_arg,
                duration.hour
            )
            else if (duration.hour != 0) context.getString(R.string.hours_pre_arg, duration.hour) else "")
            builder.append(if (duration.hour != 0 && duration.minute != 0) context.getString(R.string.and_with_spaces) else "")
            builder.append(if (duration.minute == 1) context.getString(
                R.string.minute_pre_arg,
                duration.minute
            )
            else if (duration.minute != 0) context.getString(R.string.minutes_pre_arg, duration.minute) else "")
            builder.append(if (duration.hour==0 && duration.minute==0) context.getString(R.string.please_select_a_duration) else "")
            return builder.toString()
        }
    }

    enum class ContentType{
        IMAGE, DOCUMENT, AUDIO, VIDEO
    }

    class CalendarResource(inputCalendar: Calendar) {
        private val calendar = MutableStateFlow(inputCalendar)
        private var time = MutableStateFlow(getTime())
        private var dayNum = MutableStateFlow(getDayNum())
        private lateinit var dayWord : MutableStateFlow<String>
        private lateinit var monthYear : MutableStateFlow<String>
        private lateinit var fullDate : MutableStateFlow<String>
        private lateinit var daysFromToday: MutableStateFlow<String>
        private var dateHasNotBeenInitialized = true

        fun setCalendar(inputCalendar: Calendar){
            calendar.value = inputCalendar
        }

        fun convertToLong():Long{
            return calendar.value.time.time
        }

        fun getCalendar():Calendar{
            val c: Calendar = Calendar.getInstance()
            c.setTimeInMillis(convertToLong())
            return c
        }

        private fun getDayMonthYear(): Triple<Int,Int,Int>{
            return Triple(
                calendar.value.get(Calendar.DAY_OF_MONTH),
                calendar.value.get(Calendar.MONTH),
                calendar.value.get(Calendar.YEAR)
            )
        }

        fun pickDate(context: Context){
            val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.value.set(Calendar.YEAR, year)
                calendar.value.set(Calendar.MONTH, month)
                calendar.value.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                initializeDatetime(context)
            }
            val (dayOfMonth, month, year) = getDayMonthYear()
            val datePickerDialog = DatePickerDialog(context, listener, year, month, dayOfMonth)
            datePickerDialog.show()
        }

        private fun getTime(): String{
            return getDoubleDigitString(calendar.value.get(Calendar.HOUR_OF_DAY).toString()) + ":" + getDoubleDigitString(calendar.value.get(Calendar.MINUTE).toString())
        }

        private fun getDayNum(): String{
            return getDoubleDigitString(calendar.value.get(Calendar.DAY_OF_MONTH).toString())
        }

        private fun getDoubleDigitString(value:String): String{
            return when (value) {
                "0" -> "00"
                "1" -> "01"
                "2" -> "02"
                "3" -> "03"
                "4" -> "04"
                "5" -> "05"
                "6" -> "06"
                "7" -> "07"
                "8" -> "08"
                "9" -> "09"
                else -> value
            }
        }

        private fun getDayWord(context: Context): String{
            return when (calendar.value.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> context.getString(R.string.Mon)
                Calendar.TUESDAY -> context.getString(R.string.Tue)
                Calendar.WEDNESDAY -> context.getString(R.string.Wed)
                Calendar.THURSDAY -> context.getString(R.string.Thu)
                Calendar.FRIDAY -> context.getString(R.string.Fri)
                Calendar.SATURDAY -> context.getString(R.string.Sat)
                else -> context.getString(R.string.Sun)
            }
        }

        private fun initializeDatetime(context: Context){
            if (dateHasNotBeenInitialized) {
                dayWord = MutableStateFlow(getDayWord(context))
                monthYear = MutableStateFlow(getMonthYear(context))
                fullDate = MutableStateFlow(getFullDate(context))
                time = MutableStateFlow(getTime())
                dayNum = MutableStateFlow(getDayNum())
                daysFromToday = MutableStateFlow(getDaysFromToday())
                dateHasNotBeenInitialized = false
            }
            else{
                dayWord.update { getDayWord(context) }
                monthYear.update { getMonthYear(context) }
                fullDate.update { getFullDate(context) }
                time.update { getTime() }
                dayNum.update { getDayNum() }
                daysFromToday.update { getDaysFromToday() }
            }
        }

        fun getDayWordStateFlow(context: Context): MutableStateFlow<String>{
            initializeDatetime(context)
            return dayWord
        }

        fun getMonthYearStateFlow(context: Context): MutableStateFlow<String>{
            initializeDatetime(context)
            return monthYear
        }

        fun getFullDateStateFlow(context: Context): MutableStateFlow<String>{
            initializeDatetime(context)
            return fullDate
        }

        fun getTimeStateFlow(context: Context): MutableStateFlow<String>{
            initializeDatetime(context)
            return time
        }

        fun getDayNumStateFlow(context: Context) : MutableStateFlow<String>{
            initializeDatetime(context)
            return dayNum
        }

        fun getDaysFromTodayStateFlow(context: Context): MutableStateFlow<String>{
            initializeDatetime(context)
            return daysFromToday
        }

        private fun getMonthYear(context: Context): String{
            return getMonth(
                calendar.value.get(Calendar.MONTH).toString(),
                context
            ) + " " + calendar.value.get(Calendar.YEAR)
        }

        private fun getMonth(month: String?, context: Context): String {
            return when (month) {
                "1", "01" -> context.getString(R.string.Jan)
                "2", "02" -> context.getString(R.string.Feb)
                "3", "03" -> context.getString(R.string.Mar)
                "4", "04" -> context.getString(R.string.Apr)
                "5", "05" -> context.getString(R.string.May)
                "6", "06" -> context.getString(R.string.Jun)
                "7", "07" -> context.getString(R.string.Jul)
                "8", "08" -> context.getString(R.string.Aug)
                "9", "09" -> context.getString(R.string.Sep)
                "10" -> context.getString(R.string.Oct)
                "11" -> context.getString(R.string.Nov)
                else -> context.getString(R.string.Dec)
            }
        }

        fun getStandardDate(context: Context): String{
            return getDayNum()+" "+getMonthYear(context)
        }

        private fun getFullDate(context: Context): String{
            return getDayWord(context)+" "+getDayNum()+" "+getMonthYear(context)
        }

        private fun getDaysFromToday(): String{
            val now = Calendar.getInstance()
            return Period.between(
                LocalDate.of(
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH)+1,
                    now.get(Calendar.DAY_OF_MONTH)
                ),
                LocalDate.of(
                    calendar.value.get(Calendar.YEAR),
                    calendar.value.get(Calendar.MONTH)+1,
                    calendar.value.get(Calendar.DAY_OF_MONTH)
                )
            ).days.toString()
        }
    }

    class ImageResource(inputFileName: String):FileResource(inputFileName){
        companion object{
            const val DESTINATION_FOLDER = "image"
        }
        override val destinationFolder: String = DESTINATION_FOLDER
        private var isDrawable = false

        init {
            if (inputFileName == AppSettings.DEFAULT_IMAGE_ID) isDrawable = true
        }

        fun setDefaultImage(context: Context):String{
            deleteFile(context)
            isDrawable = true
            setUri(context)
            return AppSettings.DEFAULT_IMAGE_ID
        }

        fun getAbsoluteUri(context: Context):Uri{
            return getUriFromStorage(context)?:getDefaultUri(context)?:getUriFromDrawable(context, R.mipmap.ic_launcher)
        }

        override fun getDefaultUri(context: Context): Uri? {
            return if (isDrawable) getUriFromDrawable(context, R.mipmap.ic_launcher)
            else null
        }

        fun fileFromContentUri(context: Context): Uri? {
            val contentUri: Uri = getUriFromStorage(context)?:return null

            val imageFolder = File(context.cacheDir, "images")
            var tempFile: File? = null
            try {
                imageFolder.mkdirs()
                tempFile = File(imageFolder, "$fileName.jpg")
                tempFile.createNewFile()

                try {
                    val oStream = FileOutputStream(tempFile)
                    val inputStream = context.contentResolver.openInputStream(contentUri)

                    inputStream?.let {
                        copy(inputStream, oStream)
                    }

                    oStream.flush()
                    oStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "" + e.message, Toast.LENGTH_LONG).show()
            }

            return tempFile?.let { FileProvider.getUriForFile(
                context,
                "com.thando.accountable",
                it
            )
            }
        }

        @Throws(IOException::class)
        private fun copy(source: InputStream, target: OutputStream) {
            val buf = ByteArray(8192)
            var length: Int
            while (source.read(buf).also { length = it } > 0) {
                target.write(buf, 0, length)
            }
        }
    }

    class AudioResource(inputFileName: String):FileResource(inputFileName){
        companion object{
            const val DESTINATION_FOLDER = "audio"
        }
        override val destinationFolder: String = DESTINATION_FOLDER

        override fun getDefaultUri(context: Context): Uri? {
            return null
        }
    }

    class VideoResource(inputFileName: String):FileResource(inputFileName){
        companion object{
            const val DESTINATION_FOLDER = "video"
        }
        override val destinationFolder: String = DESTINATION_FOLDER

        override fun getDefaultUri(context: Context): Uri? {
            return null
        }
    }

    class DocumentResource(inputFileName: String):FileResource(inputFileName){
        companion object{
            const val DESTINATION_FOLDER = "document"
        }
        override val destinationFolder: String = DESTINATION_FOLDER

        override fun getDefaultUri(context: Context): Uri? {
            return null
        }
    }

    abstract class FileResource(var fileName: String) {

        private var uri: MutableStateFlow<Uri?> = MutableStateFlow(null)
        abstract val destinationFolder:String
        abstract fun getDefaultUri(context: Context):Uri?
        private var notInitialized = true

        fun saveFile(context: Context, inputUri: Uri?, contentPrefix: String, id: Long?):String?{
            if (inputUri == null || id == null) return null
            deleteFile(context)
            fileName = contentPrefix + id
            saveFile( context, inputUri)
            setUri(context)
            return fileName
        }

        fun setUri(context: Context){
            val file = getUriFromStorage(context)?:getDefaultUri(context)
            notInitialized = false
            uri.value = file
        }

        open fun getUri(context: Context): StateFlow<Uri?> {
            if (notInitialized) {
                setUri(context)
            }
            return uri
        }

        fun getUriFromStorage(context: Context): Uri? {
            if (fileName.isEmpty()) return null
            val file = File(context.filesDir.toString()+"/$destinationFolder", fileName)
            return if (file.exists()) {
                Uri.fromFile(file)
            } else {
                null
            }
        }

        fun deleteFile(context: Context):Boolean {
            val deleted = deleteFile(context, fileName)
            if (deleted) {
                fileName = ""
                uri.value = null
            }
            return deleted
        }

        private fun deleteFile(context: Context,destFileName: String):Boolean{
            val destinationDir = context.filesDir.toString()+ File.separator + destinationFolder
            val destination = destinationDir + File.separator + destFileName
            val file = File(destination)
            return if (file.exists()) {
                file.delete()
            } else {
                true
            }
        }

        private fun saveFile(
            context: Context,
            sourceUri: Uri
        ): Boolean {
            val destinationDir = context.filesDir.toString()+ File.separator + destinationFolder
            val bis: BufferedInputStream?
            var bos: BufferedOutputStream? = null
            val input: InputStream?
            var hasError = fileName.isEmpty()

            if (hasError) return true
            try {
                input = context.contentResolver.openInputStream(sourceUri)

                val directorySetupResult: Boolean
                val destDir = File(destinationDir)
                directorySetupResult = if (!destDir.exists()) {
                    destDir.mkdirs()
                } else if (!destDir.isDirectory) {
                    replaceFileWithDir(destinationDir)
                } else {
                    true
                }

                if (!directorySetupResult) {
                    hasError = true
                } else {
                    val destination = destinationDir + File.separator + fileName
                    val originalSize = input!!.available()

                    bis = BufferedInputStream(input)
                    bos = BufferedOutputStream(FileOutputStream(destination))
                    val buf = ByteArray(originalSize)
                    bis.read(buf)
                    do {
                        bos.write(buf)
                    } while (bis.read(buf) != -1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasError = true
            } finally {
                try {
                    if (bos != null) {
                        bos.flush()
                        bos.close()
                    }
                } catch (ignored: Exception) {
                }
            }

            return !hasError
        }

        private fun replaceFileWithDir(path: String): Boolean {
            val file = File(path)
            if (!file.exists()) {
                if (file.mkdirs()) {
                    return true
                }
            } else if (file.delete()) {
                val folder = File(path)
                if (folder.mkdirs()) {
                    return true
                }
            }
            return false
        }
    }
}