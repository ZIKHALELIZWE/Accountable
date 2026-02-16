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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.EditFolderViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFolderView(
    viewModel: EditFolderViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()

    mainActivityViewModel.setGalleryLauncherReturn{ galleryUri ->
        try{
            scope.launch {
                viewModel.setImage(galleryUri)
            }
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    BackHandler {
        scope.launch { viewModel.closeFolder() }
    }

    AccountableTheme {
        val newEditFolder by viewModel.newEditFolder.collectAsStateWithLifecycle()
        val folderName = newEditFolder?.let { remember { it.folderName } }
        val updateButtonTextResId by viewModel.updateButtonText.collectAsStateWithLifecycle()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.edit_folder)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { viewModel.closeFolder() } }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_to_home_fragment)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { scope.launch { viewModel.saveAndCloseFolder() } },
                            enabled = viewModel.setUpdateFolderButtonEnabled(
                                if(newEditFolder != null && folderName?.text?.isNotEmpty() == true) folderName.text.toString()
                                else null
                            )
                        ) {
                            Text(stringResource(updateButtonTextResId ?: R.string.add_folder))
                        }
                    }
                )
            }
        ) { innerPadding ->
            EditFolderFragmentView(
                viewModel,
                mainActivityViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun EditFolderFragmentView(
    viewModel: EditFolderViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier
){
    val editFolder by viewModel.editFolder.collectAsStateWithLifecycle()
    LaunchedEffect(editFolder) {
        viewModel.initializeEditFolder(editFolder)
    }

    val newEditFolder by viewModel.newEditFolder.collectAsStateWithLifecycle()
    val updateButtonTextResId by viewModel.updateButtonText.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (newEditFolder != null) {
        val folderImage by newEditFolder!!.getUri(context).collectAsStateWithLifecycle(null)
        val folderName = remember { newEditFolder!!.folderName }
        val listState = rememberScrollState()

        Column(
            modifier = modifier
                .imePadding()
                .verticalScroll(listState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // folder image
            folderImage?.let { folderImage ->
                Image(
                    bitmap = folderImage,
                    contentDescription = stringResource(R.string.folder_image),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            // folder name edit text
            TextField(
                state = folderName,
                label = { Text(stringResource(R.string.enter_folder_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp)
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
            if (folderImage != null) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.removeImage()
                        }
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
                    scope.launch {
                        viewModel.saveAndCloseFolder()
                    }
                },
                enabled = viewModel.setUpdateFolderButtonEnabled(if(folderName.text.isNotEmpty()) folderName.text.toString() else null)
            ) {
                Text(stringResource(updateButtonTextResId ?: R.string.add_folder))
            }
        }
    }
}