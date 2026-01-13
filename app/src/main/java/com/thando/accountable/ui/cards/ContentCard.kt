package com.thando.accountable.ui.cards

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorLong
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.thando.accountable.AccountableRepository.Companion.accountablePlayer
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.SpannedString
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.player.Media3PlayerView
import com.thando.accountable.player.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GetContentCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    modifier: Modifier = Modifier,
    teleprompterSettings: TeleprompterSettings? = null
) {
    when (content.type) {
        ContentType.TEXT -> TextCard(content,
            isEditingScript,
            textIndex,
            appSettings,
            markupLanguage,
            teleprompterSettings = teleprompterSettings)
        ContentType.IMAGE -> ImageCard(
            content,
            isEditingScript,
            textIndex,
            modifier,
            appSettings,
            markupLanguage,
            teleprompterSettings
        )
        ContentType.VIDEO -> VideoCard(
            content,
            isEditingScript,
            textIndex,
            modifier,
            appSettings,
            markupLanguage,
            teleprompterSettings
        )
        ContentType.DOCUMENT -> DocumentCard(
            content,
            isEditingScript,
            textIndex,
            modifier,
            appSettings,
            markupLanguage,
            teleprompterSettings
        )
        ContentType.AUDIO -> AudioCard(
            content,
            isEditingScript,
            textIndex,
            modifier,
            appSettings,
            markupLanguage,
            teleprompterSettings
        )
        ContentType.SCRIPT -> ScriptCard(
            content,
            isEditingScript,
            textIndex,
            modifier,
            appSettings,
            markupLanguage,
            teleprompterSettings
        )
    }
}

@Composable
fun TextFieldAccountable(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Attached(),
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues = if (label == null || labelPosition is TextFieldLabelPosition.Above) {
        TextFieldDefaults.contentPaddingWithoutLabel()
    } else {
        TextFieldDefaults.contentPaddingWithLabel()
    },
    interactionSource: MutableInteractionSource? = null,
    onTextSelect: (TextFieldState, (TextFieldState)-> Unit)->Unit = { _, _ ->},
){
    onTextSelect(state) { newTextFieldState ->
        state.edit {
            replace(0,length, newTextFieldState.text.toString())
            selection = newTextFieldState.selection
        }
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var lastLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(state.selection) {
        val layout = lastLayout ?: return@LaunchedEffect
        var offset = state.selection.start
        if (offset < 0) return@LaunchedEffect
        if (offset>layout.size.height) offset = layout.size.height
        val caretRect = layout.getCursorRect(offset)
        // Ask parent scroll container(s) to bring this rect into view
        bringIntoViewRequester.bringIntoView(caretRect)
    }

    val myTextLayout: Density.(getResult: () -> TextLayoutResult?) -> Unit = { getResult ->
        onTextLayout?.invoke(density,getResult)
        val result = getResult()
        if (result != null) {
            lastLayout = result
        }
    }

   TextField(
       state = state,
       modifier = modifier.bringIntoViewRequester(bringIntoViewRequester).onFocusChanged{
           if (it.isFocused) onTextSelect(state) { newTextFieldState ->
               state.edit {
                   replace(0,length, newTextFieldState.text.toString())
                   selection = newTextFieldState.selection
               }
           }
       },
       enabled = enabled,
       readOnly = readOnly,
       textStyle = textStyle,
       labelPosition = labelPosition,
       label = label,
       placeholder = placeholder,
       leadingIcon = leadingIcon,
       trailingIcon = trailingIcon,
       prefix = prefix,
       suffix = suffix,
       supportingText = supportingText,
       isError = isError,
       inputTransformation = inputTransformation,
       outputTransformation = outputTransformation,
       keyboardOptions = keyboardOptions,
       onKeyboardAction = onKeyboardAction,
       lineLimits = lineLimits,
       onTextLayout = myTextLayout,
       scrollState = scrollState,
       shape = shape,
       colors = colors,
       contentPadding = contentPadding,
       interactionSource = interactionSource
   )
}

@Composable
fun TextCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    modifier: Modifier = Modifier,
    teleprompterSettings: TeleprompterSettings? = null,
    description: Boolean = false,
    contentFileName: String? = null,
    textAlign: TextAlign = TextAlign.Start
){
    val textSize by teleprompterSettings?.textSize?.let { remember { it } }
        ?:(appSettings?.textSize?:MutableStateFlow(24)).collectAsStateWithLifecycle()
    val textColour by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()
        ?: MutableStateFlow(Color.Black.toArgb()).collectAsStateWithLifecycle()
    val teleprompterBackgroundColour by teleprompterSettings?.backgroundColour?.collectAsStateWithLifecycle()?:remember { mutableStateOf(null) }
    val backgroundModifier = teleprompterBackgroundColour?.let { modifier.background(Color(it)) }?:modifier
    val fileName = remember { content.filename }
    var useAnnotatedString by remember { mutableStateOf(false) }
    var useFileNameAnnotatedString by remember { mutableStateOf(false) }
    val text =
        if (contentFileName!=null){
            useAnnotatedString = false
            useFileNameAnnotatedString = false
            if (fileName.text.isNotEmpty()) {
                useFileNameAnnotatedString = true
                remember { content.filename }
            }
            else remember{ TextFieldState(contentFileName) }
        }
        else if (description){
            useAnnotatedString = true
            useFileNameAnnotatedString = false
            remember { content.description }
        }
        else{
            useAnnotatedString = true
            useFileNameAnnotatedString = false
            remember { content.content }
        }
    val contentAnnotatedString by content.spannedString.spannableAnnotatedString.collectAsStateWithLifecycle()
    val contentFileNameAnnotatedString by content.spannedStringFileName.spannableAnnotatedString.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(markupLanguage, textSize, context, text.text.toString(), teleprompterSettings?.specialCharactersList) {
        content.replace(
            specialCharactersList = teleprompterSettings?.specialCharactersList?: mutableStateListOf(),
            context,
            markupLanguage,
            isEditingScript,
            scope,
            textSize.toFloat()
        )
    }

    if (!isEditingScript) {
        if ((description && text.text.isNotEmpty()) || (!description)){
            Text(
                text =  if (useAnnotatedString) contentAnnotatedString
                        else if (useFileNameAnnotatedString) contentFileNameAnnotatedString
                        else run{
                            val annotatedString by SpannedString(
                                content.getNewStringWithModifications(
                                    newInputString = text.text.toString(),
                                    specialCharactersList = teleprompterSettings?.specialCharactersList?: mutableStateListOf(),
                                    isEditing = isEditingScript,
                                    markupLanguage = markupLanguage,
                                ),
                                context,
                                markupLanguage,
                                textSize.toFloat()
                            ).spannableAnnotatedString.collectAsStateWithLifecycle()
                            annotatedString
                },
                style = TextStyle(
                    textAlign = textAlign,
                    fontSize = textSize.sp,
                    color = Color(textColour)
                ),
                modifier = backgroundModifier.fillMaxWidth()
            )
        }
    }
    else {
        TextFieldAccountable(
            state = if (contentFileName!=null) fileName else text,
            onTextSelect = { textFieldState, updateTextFieldState ->
                textIndex(
                    Triple(
                        textFieldState.selection.start,
                        content
                    ) { newStr, newSelection ->
                        updateTextFieldState(TextFieldState(
                                newStr,
                                TextRange(newSelection)
                            )
                        )
                    }
                )
            },
            modifier = modifier.fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) textIndex(null)
            },
            placeholder = { Text(stringResource(if (contentFileName!=null) R.string.video_name
            else if (description) R.string.image_description
                    else R.string.edit_script)) },
            textStyle = TextStyle(
                textAlign = textAlign,
                color = Color.Black,
                fontSize = textSize.sp
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                hintLocales = LocaleList.current
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, // Removes gray background
                unfocusedContainerColor = Color.Transparent, // Removes gray background
                focusedIndicatorColor = Color.Transparent, // Removes underline when focused
                unfocusedIndicatorColor = Color.Transparent, // Removes underline when unfocused
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ImageCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){
    content.getUri(LocalContext.current)?.let { getContentUri ->
        val uri by getContentUri.collectAsStateWithLifecycle()

        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap = uri?.let { AppResources.getBitmapFromUri(LocalContext.current, it) }
                    ?.asImageBitmap()
                    ?: ImageBitmap(1, 1),
                contentDescription = stringResource(R.string.image),
                contentScale = ContentScale.FillWidth,
                modifier = if (isEditingScript) modifier.fillMaxWidth() else Modifier.fillMaxWidth()
            )
            TextCard(
                content,
                isEditingScript,
                textIndex,
                appSettings,
                markupLanguage,
                teleprompterSettings = teleprompterSettings,
                description = true,
                modifier = Modifier.padding(start = 15.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val trackItem by content.trackItem.collectAsStateWithLifecycle()
    val isPlaying by content.isPlaying.collectAsStateWithLifecycle()
    val textColour by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()
        ?:MutableStateFlow(Color.Black.toArgb()).collectAsStateWithLifecycle()

    content.getUri(LocalContext.current)?.let { getContentUri ->
        val uri by getContentUri.collectAsStateWithLifecycle()
        uri?.let { uri ->
            scope.launch {
                withContext(Dispatchers.IO){
                    content.trackItem.value =
                        content.id?.let { getVideoMetadata(it, uri, context) }
                }
            }
            trackItem?.let { trackItem ->
                var buttonDrawable by remember { mutableStateOf(Icons.Filled.PlayArrow) }

                if (isPlaying){
                    buttonDrawable = Icons.Filled.Close
                }
                else{
                    if (accountablePlayer.isContentPlaying(content)) {
                        content.isPlaying.update { true }
                    }
                    else{
                        accountablePlayer.close(content)
                        buttonDrawable = Icons.Filled.PlayArrow
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Media3PlayerView(
                        content, trackItem.thumbnail,accountablePlayer, isPlaying,
                        isEditingScript,
                        modifier
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardColors(MaterialTheme.colorScheme.secondary,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically){
                            Column (horizontalAlignment = Alignment.CenterHorizontally){
                                IconButton(onClick = {
                                    content.isPlaying.update { isPlaying.not() }
                                })
                                {
                                    Icon(
                                        imageVector = buttonDrawable,
                                        contentDescription = stringResource(R.string.play_pause)
                                    )
                                }
                                Text(trackItem.duration.toLongOrNull()?.let { formatMillisToMinutesAndSeconds(it)}?:trackItem.duration,
                                    color = Color(textColour))
                            }
                            TextCard(
                                content,
                                isEditingScript,
                                textIndex,
                                appSettings,
                                markupLanguage,
                                teleprompterSettings = teleprompterSettings,
                                description = false,
                                contentFileName = trackItem.title + " : " + trackItem.artistName
                            )
                        }
                    }
                    TextCard(
                        content,
                        isEditingScript,
                        textIndex,
                        appSettings,
                        markupLanguage,
                        teleprompterSettings = teleprompterSettings,
                        description = true,
                        modifier = Modifier.padding(start = 15.dp)
                    )
                }
            }
        }
    }
}

suspend fun getVideoMetadata(id:Long,uri: Uri, context: Context): TrackItem{
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        val trackItem = TrackItem(
            id,
            uri,
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?: "Unknown",
            retriever.getFrameAtTime(0)
        )
        retriever.release()
        trackItem
    }
}

@SuppressLint("DefaultLocale")
fun formatMillisToMinutesAndSeconds(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
}

suspend fun getMediaMetadata(id:Long,uri: Uri, context: Context): TrackItem? {
    return withContext(Dispatchers.IO) {
        if (!AppResources.uriExists(context,uri)) return@withContext null
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        val trackItem = TrackItem(
            id,
            uri,
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?: "Unknown",
            AppResources.getBitmapFromByteArray(retriever.embeddedPicture)
        )
        retriever.release()
        trackItem
    }
}

@Composable
fun AudioCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val trackItem by content.trackItem.collectAsStateWithLifecycle()
    val isPlaying by content.isPlaying.collectAsStateWithLifecycle()
    val textColour by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()
        ?:MutableStateFlow(Color.Black.toArgb()).collectAsStateWithLifecycle()

    content.getUri(LocalContext.current)?.let { getContentUri ->
        val uri by getContentUri.collectAsStateWithLifecycle()
        uri?.let { uri ->
            scope.launch {
                withContext(Dispatchers.IO){
                    content.trackItem.value =
                        content.id?.let { getMediaMetadata(it, uri, context) }
                }
            }
            trackItem?.let { trackItem ->
                var buttonDrawable by remember { mutableStateOf(Icons.Filled.PlayArrow) }

                if (isPlaying){
                    buttonDrawable = Icons.Filled.Close
                }
                else{
                    if (accountablePlayer.isContentPlaying(content)) {
                        content.isPlaying.update { true }
                    }
                    else{
                        accountablePlayer.close(content)
                        buttonDrawable = Icons.Filled.PlayArrow
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Media3PlayerView(
                        content, trackItem.thumbnail,accountablePlayer, isPlaying,
                        isEditingScript,
                        modifier
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardColors(MaterialTheme.colorScheme.secondary,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically){
                            Column (horizontalAlignment = Alignment.CenterHorizontally){
                                IconButton(onClick = {
                                    content.isPlaying.update { isPlaying.not() }
                                })
                                {
                                    Icon(
                                        imageVector = buttonDrawable,
                                        contentDescription = stringResource(R.string.play_pause)
                                    )
                                }
                                Text(
                                    text = trackItem.duration.toLongOrNull()?.let { formatMillisToMinutesAndSeconds(it)}?:trackItem.duration,
                                    color = Color.fromColorLong(textColour.toColorLong())
                                )
                            }
                            Column (Modifier.weight(1f)){
                                TextCard(
                                    content,
                                    isEditingScript,
                                    textIndex,
                                    appSettings,
                                    markupLanguage,
                                    modifier = Modifier.weight(1f),
                                    teleprompterSettings = teleprompterSettings,
                                    description = false,
                                    contentFileName = trackItem.title + " : " + trackItem.artistName,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    stringResource(R.string.album, trackItem.album),
                                    color = Color(textColour),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                    TextCard(
                        content,
                        isEditingScript,
                        textIndex,
                        appSettings,
                        markupLanguage,
                        teleprompterSettings = teleprompterSettings,
                        description = true,
                        modifier = Modifier.padding(start = 15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val trackItem by content.trackItem.collectAsStateWithLifecycle()

    content.getUri(LocalContext.current)?.let { getContentUri ->
        val uri by getContentUri.collectAsStateWithLifecycle()
        uri?.let { uri ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    content.trackItem.value =
                        content.id?.let { getMediaMetadata(it, uri, context) }
                }
            }
            trackItem?.let { trackItem ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    trackItem.thumbnail?.let { thumbnail ->
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = stringResource(R.string.document),
                            contentScale = ContentScale.FillWidth,
                            modifier = if (isEditingScript) modifier.fillMaxWidth()
                            else Modifier.fillMaxWidth()
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardColors(
                            MaterialTheme.colorScheme.secondary,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {/*todo*/}
                    ) {
                        TextCard(
                            content,
                            isEditingScript,
                            textIndex,
                            appSettings,
                            markupLanguage,
                            teleprompterSettings = teleprompterSettings,
                            description = false,
                            contentFileName = trackItem.title + " : " + trackItem.artistName
                        )
                    }
                    TextCard(
                        content,
                        isEditingScript,
                        textIndex,
                        appSettings,
                        markupLanguage,
                        teleprompterSettings = teleprompterSettings,
                        description = true,
                        modifier = Modifier.padding(start = 15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){

}