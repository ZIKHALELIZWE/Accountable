package com.thando.accountable.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.EditFolderViewModel
import com.thando.accountable.ui.cards.TextFieldAccountable
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch

@Composable
fun EditFolderView(
    viewModel: EditFolderViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    mainActivityViewModel.setGalleryLauncherReturn{ galleryUri ->
        try{
            viewModel.setImage(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    BackHandler {
        viewModel.closeFolder()
    }

    AccountableTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            EditFolderFragmentView(
                viewModel,
                mainActivityViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun EditFolderFragmentView(
    viewModel: EditFolderViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier
){
    val editFolder by viewModel.editFolder.collectAsStateWithLifecycle()
    var notInitialized by remember { mutableStateOf(true) }

    if (notInitialized) {
        viewModel.initializeEditFolder(editFolder)
        notInitialized = false
    }
    val newEditFolder by viewModel.newEditFolder.collectAsStateWithLifecycle()
    val updateButtonTextResId by viewModel.updateButtonText.collectAsStateWithLifecycle()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val context = LocalContext.current

    if (newEditFolder != null) {
        val uri by newEditFolder!!.getUri(context).collectAsStateWithLifecycle()
        val folderName = remember { newEditFolder!!.folderName }
        val scope = rememberCoroutineScope()
        val listState = rememberScrollState()
        Column(
            modifier = modifier.imePadding().verticalScroll(listState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // folder image
            uri?.let {
                Image(
                    bitmap = AppResources.getBitmapFromUri(context, it)?.asImageBitmap()
                        ?: ImageBitmap(1,1),
                    contentDescription = stringResource(R.string.folder_image),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            // folder name edit text
            TextFieldAccountable(
                state = folderName,
                label = { Text(stringResource(R.string.enter_folder_name)) },
                modifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester).onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                    .fillMaxWidth().padding(horizontal = 3.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            // choose image button
            Button(
                onClick = {
                    mainActivityViewModel.launchGalleryLauncher(AppResources.ContentType.IMAGE)
                },
                enabled = viewModel.newEditFolderViewEnabled(newEditFolder)
            ) {
                Text(stringResource(R.string.choose_image))
            }
            Spacer(modifier = Modifier.width(2.dp))
            // remove image button
            if (uri != null) {
                Button(
                    onClick = {
                        viewModel.removeImage()
                    },
                    enabled = viewModel.newEditFolderViewEnabled(newEditFolder)
                ) {
                    Text(stringResource(R.string.remove_image))
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
            // update folder button
            Button(
                onClick = {
                    viewModel.saveAndCloseFolder()
                },
                enabled = viewModel.setUpdateFolderButtonEnabled(if(folderName.text.isNotEmpty()) folderName.text.toString() else null)
            ) {
                Text(stringResource(updateButtonTextResId?.resId ?: R.string.add_folder))
            }
        }
    }
}