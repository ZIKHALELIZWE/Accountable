package com.thando.accountable.ui.cards

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.util.TableInfo
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.TeleprompterSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun TextCard(
    content: Content,
    isEditingScript: Boolean,
    textIndex:(Pair<Int,Content>?)->Unit,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null,
    description: Boolean = false,
    modifier: Modifier = Modifier
){
    val annotatedString by content.spannedString.spannableAnnotatedString.collectAsStateWithLifecycle()
    val textSize by teleprompterSettings?.textSize?.collectAsStateWithLifecycle()
        ?:MutableStateFlow(appSettings?.textSize?:24).collectAsStateWithLifecycle()
    val textColour by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()
        ?: MutableStateFlow(Color.Black.toArgb()).collectAsStateWithLifecycle()
    val text by if (description) content.description.collectAsStateWithLifecycle() else content.content.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = text
            )
        )
    }

    LaunchedEffect(text) {
        content.spannedString.setText(
            text,
            context,
            markupLanguage
        )
    }

    if (!isEditingScript) {
        if ((description && text.isNotEmpty()) || (!description)){
            Text(
                text = annotatedString,
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
                if (description) content.description.update { newStr.text }
                else content.content.update { newStr.text }
                textFieldValue = newStr
            },
            modifier = modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (focusState.isFocused) textIndex(textFieldValue.selection.start to content)
                else textIndex(null)
            },
            placeholder = { Text(stringResource(if (description) R.string.image_description
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
    modifier: Modifier,
    appSettings: AppSettings?,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings? = null
){
    val uri by content.getUri(LocalContext.current)?.collectAsStateWithLifecycle()
        ?: mutableStateOf(null)

    Column(modifier = Modifier.fillMaxWidth()) {
        Image(
            bitmap = uri?.let { AppResources.getBitmapFromUri(LocalContext.current, it) }
                ?.asImageBitmap()
                ?: ImageBitmap(1, 1),
            contentDescription = stringResource(R.string.image),
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth()
        )
        TextCard(
            content,
            isEditingScript,
            {},
            appSettings,
            markupLanguage,
            teleprompterSettings,
            description = true,
            modifier = Modifier.padding(start = 15.dp)
        )
    }
}

@Composable
fun VideoCard(
    content: Content,
    isEditingScript: Boolean
){

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