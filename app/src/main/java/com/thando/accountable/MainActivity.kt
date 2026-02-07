package com.thando.accountable

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.fragments.TeleprompterController
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {

    val viewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory(false) }
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { galleryUri ->
        viewModel.processGalleryLauncherResult(galleryUri)
    }

    private val galleryLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { list ->
        viewModel.processGalleryLauncherMultipleReturn(list)
    }

    private var pushNotificationUnit:AtomicReference<(()->Unit)?> = AtomicReference(null)
    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pushNotificationUnit.get()?.invoke()
        }
    }
    private val getResultRestoreBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == RESULT_OK)
            viewModel.processRestoreBackupResult(
                result.data,
                pushNotificationPermissionLauncher,
                pushNotificationUnit
            )
    }
    private val getResultMakeBackup = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == RESULT_OK)
            viewModel.processMakeBackupResult(
                result.data,
                pushNotificationPermissionLauncher,
                pushNotificationUnit
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow app to draw behind system bars ( This is for teleprompter)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                viewModel.galleryLauncherEvent.collect { accessorType ->
                    galleryLauncher.launch(accessorType)
                }
            }
            LaunchedEffect(Unit) {
                viewModel.galleryLauncherMultipleEvent.collect { accessorType ->
                    galleryLauncherMultiple.launch(accessorType)
                }
            }
            LaunchedEffect(Unit) {
                viewModel.makeBackupEvent.collect { intent ->
                    getResultMakeBackup.launch(intent)
                }
            }
            LaunchedEffect(Unit) {
                viewModel.restoreBackupEvent.collect { intent ->
                    getResultRestoreBackup.launch(intent)
                }
            }
            MainActivityView(viewModel){
                finish()
            }
        }
    }

    companion object{
        fun log(message:String){
            Log.i("FATAL EXCEPTION",message)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Detect Bluetooth remote click (often volume or camera key)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_CAMERA
        ) {
            // Trigger skip back
            skipBackAction()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun skipBackAction() {
        // You can expose this via a shared ViewModel or state holder
        TeleprompterController.skipBack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityView(
    mainActivityViewModel: MainActivityViewModel,
    finish: ()->Unit
){
    DisposableEffect(Unit) {
        onDispose {
            mainActivityViewModel.closeUpdateSettings()
        }
    }

    BackHandler {
        finish()
    }

    val currentFragment by mainActivityViewModel.currentFragment.collectAsStateWithLifecycle(
        AccountableFragment.HomeFragment
    )

    val drawerStateValue by remember { mainActivityViewModel.drawerState }
    val drawerState = remember { DrawerState(drawerStateValue) }
    LaunchedEffect(drawerStateValue) {
        when (drawerStateValue){
            DrawerValue.Closed -> drawerState.close()
            DrawerValue.Open -> drawerState.open()
        }
    }


    val drawerEnabled by remember { mainActivityViewModel.drawerEnabled }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val appSettings by mainActivityViewModel.appSettings.collectAsStateWithLifecycle()

    AccountableTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.testTag("MainActivityModalNavigationDrawerContent")) {
                    BackHandler(drawerState.isOpen) {
                        scope.launch {
                            mainActivityViewModel.toggleDrawer(false)
                        }
                    }
                    Column(
                        modifier = Modifier.testTag("MainActivityNavigationDrawerColumn")
                            .verticalScroll(rememberScrollState())
                    ) {
                        appSettings?.let { appSettings ->
                            val imageUri by appSettings.getUri(context).collectAsStateWithLifecycle(null)
                            var image by remember { mutableStateOf<ImageBitmap?>(null) }

                            LaunchedEffect(imageUri) {
                                image = imageUri?.let { imageUri -> AppResources.getBitmapFromUri(context, imageUri) }
                                    ?.asImageBitmap()
                                    ?: AppResources.getBitmapFromUri(
                                        context,
                                        AppResources.getUriFromDrawable(
                                            context,
                                            R.drawable.ic_stars_black_24dp
                                        )
                                    )?.asImageBitmap()
                            }

                            image?.let { image ->
                                Image(
                                    bitmap = image,
                                    contentDescription = "Navigation Drawer Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider(modifier = Modifier.padding(16.dp))

                        Text(stringResource(R.string.activities), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .testTag("NavigationDrawerItemHomeFragment"),
                            icon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = stringResource(R.string.home)
                                )
                            },
                            label = { Text(stringResource(R.string.home)) },
                            selected = currentFragment == AccountableFragment.HomeFragment,
                            onClick = {
                                mainActivityViewModel.changeFragment(
                                AccountableFragment.HomeFragment
                                )
                            }
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .testTag("NavigationDrawerItemBooksFragment"),
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = stringResource(R.string.books)
                                )
                            },
                            label = { Text(stringResource(R.string.books)) },
                            selected = currentFragment == AccountableFragment.BooksFragment,
                            onClick = {
                                mainActivityViewModel.changeFragment(
                                AccountableFragment.BooksFragment
                            ) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(16.dp))

                        Text(stringResource(R.string.support), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .testTag("NavigationDrawerItemAppSettingsFragment"),
                            label = { Text("Settings") },
                            selected = currentFragment == AccountableFragment.AppSettingsFragment,
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            badge = { Text("20") }, // Placeholder
                            onClick = {
                                mainActivityViewModel.changeFragment(
                                AccountableFragment.AppSettingsFragment
                            ) }
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .testTag("NavigationDrawerItemHelpFragment"),
                            label = { Text("Help and feedback") },
                            selected = currentFragment == AccountableFragment.HelpFragment,
                            icon = { Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null) },
                            onClick = {
                                mainActivityViewModel.changeFragment(
                                AccountableFragment.HelpFragment
                            ) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            },
            gesturesEnabled = drawerEnabled
        ) {
            mainActivityViewModel.accountableNavigationController.GetAccountableActivity()
        }
    }
}