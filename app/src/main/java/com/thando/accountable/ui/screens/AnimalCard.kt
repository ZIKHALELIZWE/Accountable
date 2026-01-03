package com.thando.accountable.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AnimalListPreviewParameterProvider
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Script
import com.thando.accountable.fragments.viewmodels.FoldersAndScriptsViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.flow.update

private const val BottomBarHeightFraction = 0.14f
private const val TopBarHeightFraction = BottomBarHeightFraction / 2
private val BarColor = Color(red = 255f, green = 255f, blue = 255f, alpha = 0.5f)

@Preview(showBackground = true)
@Composable
fun AnimalCardPreview(
    @PreviewParameter(AnimalListPreviewParameterProvider::class) animals: List<Animal>
) {
    AccountableTheme {
        Row(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
        ) {
            animals.take(2).forEach {
                AnimalCard(
                    animal = it,
                    modifier = Modifier
                        .padding(2.dp)
                        .weight(1f)
                        .wrapContentHeight()
                )
            }
        }
    }
}

@Composable
fun AnimalCard(
    animal: Animal,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(0.66f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(animal.imageResId),
                contentDescription = animal.name,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            BottomBar(animal.name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderCard(
    folder: Folder,
    modifier: Modifier = Modifier,
    onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
    clickable:Boolean = true
) {
    val folderName by folder.folderName.collectAsStateWithLifecycle()
    val folderUri by folder.getUri(LocalContext.current).collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        onClickListeners.viewModel.getFolderContentPreview(folder)
    }
    Card(
        modifier = modifier.aspectRatio(0.66f)
            .combinedClickable(
                onLongClick = {
                    if (clickable && onClickListeners.setOnLongClick){
                        onClickListeners.bottomSheetListeners.update {
                            FoldersAndScriptsViewModel.OnClickListeners.BottomSheetListeners(
                                displayView = {
                                    FolderCard(folder, Modifier, onClickListeners,false)
                                },
                                onEditClickListener = {
                                    onClickListeners.folderOnEditClickListener(folder.folderId)
                                },
                                onDeleteClickListener = {
                                    onClickListeners.folderOnDeleteClickListener(folder.folderId)
                                }
                            )
                        }
                    }
                },
                onClick = {
                    if (clickable) {
                        onClickListeners.folderClickListener(folder.folderId)
                    }
                }
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = folderUri?.let { AppResources.getBitmapFromUri(LocalContext.current, it) }
                    ?.asImageBitmap()
                    ?: ImageBitmap(1, 1),
                contentDescription = folderName,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            if (folder.folderType == Folder.FolderType.SCRIPTS)
                FolderTopBar(
                    folders = folder.numFolders.collectAsStateWithLifecycle(0),
                    scripts = folder.numScripts.collectAsStateWithLifecycle()
                )
            else if (folder.folderType == Folder.FolderType.GOALS)
                FolderTopBar(
                    folders = folder.numFolders.collectAsStateWithLifecycle(0),
                    goals = folder.numGoals.collectAsStateWithLifecycle()
                )
            BottomBar(folderName)
        }
    }
}

@Composable
private fun BoxScope.FolderTopBar(folders: State<Int>, scripts: State<Int>? = null, goals: State<Int>? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(TopBarHeightFraction)
            .background(BarColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .align(Alignment.TopCenter)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .wrapContentWidth()
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(
                        color = LocalContentColor.current.copy(alpha = 0.0f),
                        shape = RectangleShape
                    ),
                imageVector = Icons.Default.Folder,
                contentDescription = null
            )
            Text(folders.value.toString(),
                modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(
                    color = LocalContentColor.current.copy(alpha = 0.0f),
                    shape = RectangleShape
                )
            )
            Icon(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(
                        color = LocalContentColor.current.copy(alpha = 0.0f),
                        shape = RectangleShape
                    ),
                imageVector = scripts?.let { Icons.AutoMirrored.Filled.LibraryBooks }
                    ?: goals?.let { Icons.Default.Stars } ?: Icons.Default.Error,
                contentDescription = null
            )
            Text(scripts?.value?.toString() ?: (goals?.value?.toString() ?: ""),
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .background(
                        color = LocalContentColor.current.copy(alpha = 0.0f),
                        shape = RectangleShape
                    )
            )
        }
    }
}

@Composable
private fun BoxScope.BottomBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(BottomBarHeightFraction)
            .background(BarColor)
            .align(Alignment.BottomCenter)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptCard(
    script: Script,
    modifier: Modifier = Modifier,
    onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
    clickable:Boolean = true
) {
    val scriptUri by script.getUri(LocalContext.current).collectAsStateWithLifecycle()
    var contentPreview by remember { mutableStateOf<AccountableRepository.ContentPreview?>(null) }

    LaunchedEffect(Unit) {
        contentPreview = script.scriptId?.let { onClickListeners.viewModel.getScriptContentPreview(it) }
        contentPreview?.init()
    }

    contentPreview?.let { contentPreview ->
        val time by script.scriptDateTime.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()
        val date by script.scriptDateTime.getFullDateStateFlow(LocalContext.current).collectAsStateWithLifecycle()

        val title by remember { script.scriptTitle }
        val description by contentPreview.getDescription().collectAsStateWithLifecycle("")

        val numImages by contentPreview.getNumImages().collectAsStateWithLifecycle(0)
        val numVideos by contentPreview.getNumVideos().collectAsStateWithLifecycle(0)
        val numAudios by contentPreview.getNumAudios().collectAsStateWithLifecycle(0)
        val numDocuments by contentPreview.getNumDocuments().collectAsStateWithLifecycle(0)
        val numScript by contentPreview.getNumScripts().collectAsStateWithLifecycle(0)
        Card(
            modifier = modifier.fillMaxWidth().wrapContentHeight()
                .combinedClickable(
                    onLongClick = {
                        if (clickable && onClickListeners.setOnLongClick) {
                            onClickListeners.bottomSheetListeners.update {
                                FoldersAndScriptsViewModel.OnClickListeners.BottomSheetListeners(
                                    displayView = {
                                        ScriptCard(script, Modifier, onClickListeners, false)
                                    },
                                    onEditClickListener = null,
                                    onDeleteClickListener = {
                                        onClickListeners.scriptOnDeleteClickListener(script.scriptId)
                                    }
                                )
                            }
                        }
                    },
                    onClick = {
                        if (clickable) {
                            script.scriptId?.let { onClickListeners.scriptClickListener(it) }
                        }
                    }
                ),
            shape = RectangleShape,
            colors = CardColors(Color.White,
                Color.Black,
                Color.LightGray,
                Color.DarkGray)
        ) {
            Row {
                Card(modifier = Modifier.height(113.dp)
                    .width(113.dp),
                    colors = CardColors(Color.White,
                        Color.White,
                        Color.LightGray,
                        Color.DarkGray),
                ) {
                    scriptUri?.let {
                        Image(
                            bitmap = AppResources.getBitmapFromUri(LocalContext.current, it)
                                ?.asImageBitmap()
                                ?: ImageBitmap(1, 1),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.height(113.dp)
                                .width(113.dp),
                            contentDescription = stringResource(R.string.script_display_image)
                        )
                    }
                }
                Column(modifier = Modifier.padding(end = 5.dp).height(113.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp) // Title
                    Text(text = description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
                        textAlign = TextAlign.Start,
                        color = Color.Black) // Description
                    Row(modifier = Modifier
                        .height(IntrinsicSize.Max).padding(5.dp)
                        .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(time,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            fontSize = 12.sp) // Entry Time
                        val modifier = Modifier.weight(0.1f)
                        MediaIcon(numImages, Icons.Default.Image,modifier)
                        MediaIcon(numVideos, Icons.Default.Videocam,modifier)
                        MediaIcon(numAudios, Icons.Default.Mic,modifier)
                        MediaIcon(numDocuments, Icons.Default.Book,modifier)
                        MediaIcon(numScript, Icons.AutoMirrored.Filled.LibraryBooks,modifier)
                        Text(date,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontSize = 12.sp) // Entry Date
                    }
                    //Text() // Entry Size
                }
            }
        }
    }
}

@Composable
private fun MediaIcon(numMedia:Int, icon: ImageVector, modifier: Modifier = Modifier){
    if (numMedia>0) {
        Icon(
            modifier = modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(
                    color = LocalContentColor.current.copy(alpha = 0.0f),
                    shape = RectangleShape
                ),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            numMedia.toString(),
            modifier = modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(
                    color = LocalContentColor.current.copy(alpha = 0.0f),
                    shape = RectangleShape
                ),
            fontSize = 12.sp
        )
    }
}
