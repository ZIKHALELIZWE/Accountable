package com.thando.accountable.fragments

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.AppSettingsViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAtomicApi::class,
    ExperimentalCoroutinesApi::class
)
@Composable
fun AppSettingsView(
    viewModel: AppSettingsViewModel,
    mainActivityViewModel: MainActivityViewModel
){
    mainActivityViewModel.setGalleryLauncherReturn{ galleryUri ->
        try{
            viewModel.setCustomImage(galleryUri)
        }catch(e:Exception){
            e.printStackTrace()
        }
    }

    mainActivityViewModel.setRestoreBackupReturn {
        result,
        pushNotificationPermissionLauncher,
        pushNotificationUnit ->
            viewModel.restoreFromBackup(
                result,
                pushNotificationPermissionLauncher,
                pushNotificationUnit
            )
    }

    mainActivityViewModel.setMakeBackupReturn {
        result,
        pushNotificationPermissionLauncher,
        pushNotificationUnit ->
            viewModel.makeBackup(
                result,
                pushNotificationPermissionLauncher,
                pushNotificationUnit
            )
    }

    AccountableTheme {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val appSettings by mainActivityViewModel.appSettings.collectAsStateWithLifecycle()

        val textSize by (appSettings?.textSize?:MutableStateFlow(18)).collectAsStateWithLifecycle()
        val mainPicture by (appSettings?.mainPicture?: MutableStateFlow("app_picture")).collectAsStateWithLifecycle()
        val image by (appSettings?.getUri(context)?: flowOf(null)).mapLatest {
            withContext(MainActivity.IO) {
                it?.let { imageUri -> AppResources.getBitmapFromUri(context, imageUri) }
                    ?.asImageBitmap()
                    ?: AppResources.getBitmapFromUri(
                        context,
                        AppResources.getUriFromDrawable(
                            context,
                            R.mipmap.ic_launcher
                        )
                    )?.asImageBitmap()
            }
        }.collectAsStateWithLifecycle(null)

        LaunchedEffect(Unit) {
            viewModel.navigateToChooseImage.collect { chooseImage ->
                if (chooseImage) {
                    mainActivityViewModel.launchGalleryLauncher(
                        AppResources.ContentType.IMAGE
                    )
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { mainActivityViewModel.toggleDrawer() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.navigation_drawer_menu)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            var buttonHeight by remember { mutableStateOf(90.dp) }
            val scope = rememberCoroutineScope()
            var showImagePickerDialog by remember { mutableStateOf(false) }
            var showTextSizeDialog by remember { mutableStateOf(false) }
            Box {
                if (showImagePickerDialog) {
                    OptionsPickerDialog(
                        if (mainPicture=="app_picture") 0 else 1,
                        listOf(
                            stringResource(R.string.default_image),
                            stringResource(R.string.custom)
                        ),
                        {
                            scope.launch {
                                showImagePickerDialog = false
                            }
                        }
                    ) { choice ->
                        scope.launch {
                            showImagePickerDialog = false
                        }
                        if (choice==0) {
                            // Restore default image
                            viewModel.chooseDefaultImage()
                        }
                        else{
                            // Select custom image
                            viewModel.chooseImage()
                        }
                    }
                }
                if (showTextSizeDialog) {
                    TextSizePickerDialog(
                        textSize.toFloat(),
                        {
                            scope.launch {
                                showTextSizeDialog = false
                            }
                        }
                    ){ newFloatTextSize ->
                        scope.launch {
                            showTextSizeDialog = false
                            viewModel.setTextSize(newFloatTextSize.toInt())
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                ) {
                    Text(
                        stringResource(R.string.appearance),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Cyan
                    )
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)) {
                        image?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = "Navigation Drawer Image",
                                modifier = Modifier.height(buttonHeight),
                            )
                        }
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight)
                                .weight(1f),
                            onClick = { scope.launch {
                                showImagePickerDialog = true
                            } },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black
                            )
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.main_display_image)
                                )
                                Text(
                                    text = if (mainPicture=="app_picture")
                                        stringResource(R.string.default_image)
                                    else stringResource(R.string.custom),
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = buttonHeight)
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        onClick = {
                            showTextSizeDialog = true
                        },
                        shape = RectangleShape
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.select_text_size)
                            )
                            Text(
                                text = textSize.toString(),
                                color = Color.Gray
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(16.dp))
                    Text(
                        stringResource(R.string.backup),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Cyan
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = buttonHeight)
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            mainActivityViewModel.launchRestoreBackup(intent)
                        },
                        shape = RectangleShape
                    ) {
                        Text(
                            text = stringResource(R.string.restore_from_downloads_backup_folder),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(minHeight = buttonHeight)
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.Black
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            mainActivityViewModel.launchMakeBackup(intent)
                        },
                        shape = RectangleShape
                    ) {
                        Text(
                            text = stringResource(R.string.backup_data_to_folder),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionsPickerDialog(
    currentSelection: Int,
    options: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableStateOf(currentSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(stringResource(R.string.main_display_image)) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index }
                        )
                        Text(option)
                    }
                }
            }
        }
    )
}

@Composable
fun TextSizePickerDialog(
    currentSize: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var selectedSize by remember { mutableStateOf(currentSize) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSize) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.select_text_size)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.text_size_preview),
                    fontSize = selectedSize.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.text_size, selectedSize.toInt()),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Slider(
                    value = selectedSize,
                    onValueChange = { selectedSize = it },
                    valueRange = 12f..50f, // range of text sizes
                )

            }
        }

    )
}