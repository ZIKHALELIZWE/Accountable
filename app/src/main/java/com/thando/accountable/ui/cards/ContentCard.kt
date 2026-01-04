package com.thando.accountable.ui.cards

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.compose.PlayerSurface
import com.thando.accountable.AccountableRepository.Companion.accountablePlayer
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.SpannedString
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.player.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState

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
        ContentType.DOCUMENT -> DocumentCard(content, isEditingScript)
        ContentType.AUDIO -> AudioCard(content, isEditingScript)
        ContentType.SCRIPT -> ScriptCard(content, isEditingScript)
    }
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
    filename: String? = null
){
    val annotatedString by content.spannedString.spannableAnnotatedString.collectAsStateWithLifecycle()
    val textSize by teleprompterSettings?.textSize?.collectAsStateWithLifecycle()
        ?:MutableStateFlow(appSettings?.textSize?:24).collectAsStateWithLifecycle()
    val textColour by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()
        ?: MutableStateFlow(Color.Black.toArgb()).collectAsStateWithLifecycle()
    val contentFilename by content.filename.collectAsStateWithLifecycle()
    val text by if (filename!=null){
        if (contentFilename.isNotEmpty()) content.filename.collectAsStateWithLifecycle()
        else MutableStateFlow(filename).collectAsStateWithLifecycle()
    }
    else if (description) content.description.collectAsStateWithLifecycle()
    else content.content.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = text
            )
        )
    }

    LaunchedEffect(text) {
        if (filename == null){
            content.spannedString.setText(
                text,
                context,
                markupLanguage
            )
        }
    }

    if (!isEditingScript) {
        if ((description && text.isNotEmpty()) || (!description)){
            Text(
                text = if (filename!=null) AnnotatedString(contentFilename) else annotatedString,
                style = TextStyle(
                    textAlign = TextAlign.Start,
                    fontSize = textSize.sp,
                    color = Color(textColour)
                ),
                modifier = modifier.fillMaxWidth()
            )
        }
    }
    else {
        TextField(
            value = textFieldValue,
            onValueChange = { newStr ->
                if (filename!=null) content.filename.update { newStr.text }
                else if (description) content.description.update { newStr.text }
                else content.content.update { newStr.text }
                textFieldValue = newStr
                textIndex(
                    Triple(
                        textFieldValue.selection.start,
                        content
                    ) { newStr, newSelection ->
                        if (filename!=null) content.filename.update { newStr }
                        else if (description) content.description.update { newStr }
                        else content.content.update { newStr }
                        textFieldValue = TextFieldValue(
                            newStr,
                            TextRange(newSelection)
                        )
                    }
                )
            },
            modifier = modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (focusState.isFocused) textIndex(
                    Triple(
                        textFieldValue.selection.start,
                        content
                    ) { newStr, newSelection ->
                        if (filename!=null) content.filename.update { newStr }
                        else if (description) content.description.update { newStr }
                        else content.content.update { newStr }
                        textFieldValue = TextFieldValue(
                            newStr,
                            TextRange(newSelection)
                        )
                    }
                )
                else textIndex(null)
            },
            placeholder = { Text(stringResource(if (filename!=null) R.string.video_name
            else if (description) R.string.image_description
                    else R.string.edit_script)) },
            textStyle = TextStyle(
                textAlign = TextAlign.Start,
                color = Color.Black,
                fontSize = textSize.sp
            ),
            maxLines = Int.MAX_VALUE,
            singleLine = false,
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
                val duration = trackItem.duration.toLongOrNull()
                var playerViewVisibility by remember { mutableStateOf(false) }
                var thumbnailVisibility by remember { mutableStateOf(true) }
                var buttonDrawable by remember { mutableStateOf(Icons.Filled.PlayArrow) }


                if (isPlaying){
                    playerViewVisibility = true
                    thumbnailVisibility = false
                    buttonDrawable = Icons.Filled.Close
                }
                else{
                    if (accountablePlayer.isContentPlaying(content)) {
                        content.isPlaying.update { true }
                    }
                    else{
                        accountablePlayer.close(content)
                        thumbnailVisibility = true
                        playerViewVisibility = false
                        buttonDrawable = Icons.Filled.PlayArrow
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    var imageHeightPx by remember { mutableStateOf(0) }
                    val density = LocalDensity.current
                    if (thumbnailVisibility) {
                        trackItem.thumbnail?.let { thumbnail ->
                            Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = stringResource(R.string.video),
                                contentScale = ContentScale.FillWidth,
                                modifier = if (isEditingScript) modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                                    // Get height in pixels
                                    imageHeightPx = coordinates.size.height
                                }
                                else Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                                    // Get height in pixels
                                    imageHeightPx = coordinates.size.height
                                }
                            )
                        }
                    }
                    if (playerViewVisibility){
                        var playerView = PlayerView(context)
                        val imageHeightDp = with(density) { imageHeightPx.toDp() }
                        AndroidView(
                            factory = { ctx ->
                                playerView = PlayerView(ctx)
                                playerView.keepScreenOn = true
                                accountablePlayer.addAndPlay(content, playerView,ctx)
                                //playerView.setLayerType(SURFACE_TYPE_SURFACE_VIEW,null)
                                playerView
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeightDp) // Set your desired height
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
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
                                Text(if (duration==null) trackItem.duration else formatMillisToMinutesAndSeconds(duration))
                            }
                            TextCard(
                                content,
                                isEditingScript,
                                textIndex,
                                appSettings,
                                markupLanguage,
                                teleprompterSettings = teleprompterSettings,
                                description = false,
                                filename = trackItem.title + " : " + trackItem.artistName
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
            retriever.frameAtTime
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

suspend fun getMediaMetadata(id:Long,uri: Uri, context: Context): TrackItem {
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
            AppResources.getBitmapFromByteArray(retriever.embeddedPicture)
        )
        retriever.release()
        trackItem
    }
}

@Composable
fun DocumentCard(
    content: Content,
    isEditingScript: Boolean
){

}

@Composable
fun AudioCard(
    content: Content,
    isEditingScript: Boolean
){

}

@Composable
fun ScriptCard(
    content: Content,
    isEditingScript: Boolean
){

}