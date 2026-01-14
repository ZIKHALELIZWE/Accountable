package com.thando.accountable

import android.content.Context
import android.content.res.Resources
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.fragments.TeleprompterController
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {

    val viewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory }
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
        ResourceProvider.init(this.applicationContext)

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

    object ResourceProvider {
        lateinit var resources: Resources

        fun init(context: Context) {
            resources = context.resources
        }

        fun getString(resId: Int): String {
            return resources.getString(resId)
        }

        fun getString(resId: Int, vararg args: Any): String {
            return resources.getString(resId,*args)
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
    AccountableTheme {
        val drawerState by remember { mainActivityViewModel.drawerState }
        val drawerEnabled by remember { mainActivityViewModel.drawerEnabled }


        var initialized by remember { mutableStateOf(false) }
        var currentFragment by remember { mutableStateOf<AccountableFragment?>(null) }
        val navController = rememberNavController()

        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val appSettings by mainActivityViewModel.appSettings.collectAsStateWithLifecycle()

        var result by remember { mutableStateOf<StateFlow<Uri?>>(MutableStateFlow(null)) }
        LaunchedEffect(appSettings) {
            result = if (appSettings!=null) appSettings!!.getUri(context)
            else MutableStateFlow(null)
        }
        val imageUri by result.collectAsStateWithLifecycle(null)

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

        // LaunchedEffect to collect SharedFlow
        LaunchedEffect(Unit) {
            mainActivityViewModel.currentFragment.collect { fragment ->
                if (fragment!=null){
                    if (!initialized){
                        (0 until navController.currentBackStack.value.size).forEach { _ ->
                            navController.popBackStack()
                        }
                        mainActivityViewModel.clearGalleryLaunchers()
                        currentFragment = fragment
                    }
                    if (AccountableNavigationController.isDrawerFragment(fragment))
                        mainActivityViewModel.enableDrawer()
                    else mainActivityViewModel.disableDrawer()
                }
            }
        }

        LaunchedEffect(Unit) {
            mainActivityViewModel.direction.collect { direction ->
                if (direction != null){
                    scope.launch { mainActivityViewModel.toggleDrawer(false) }
                    mainActivityViewModel.clearGalleryLaunchers()
                    (0 until navController.currentBackStack.value.size).forEach { _ ->
                        navController.popBackStack()
                    }
                    navController.navigate(
                        direction.name
                    ){launchSingleTop = true}
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    BackHandler(drawerState.isOpen) {
                        scope.launch {
                            mainActivityViewModel.toggleDrawer(false)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        image?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = "Navigation Drawer Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                        HorizontalDivider(modifier = Modifier.padding(16.dp))

                        Text(stringResource(R.string.activities), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            icon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = stringResource(R.string.home)
                                )
                            },
                            label = { Text(stringResource(R.string.home)) },
                            selected = navController.currentDestination?.route == AccountableFragment.HomeFragment.name,
                            onClick = { mainActivityViewModel.changeFragment(
                                AccountableFragment.HomeFragment
                            ) }
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = stringResource(R.string.books)
                                )
                            },
                            label = { Text(stringResource(R.string.books)) },
                            selected = navController.currentDestination?.route == AccountableFragment.BooksFragment.name,
                            onClick = { mainActivityViewModel.changeFragment(
                                AccountableFragment.BooksFragment
                            ) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(16.dp))

                        Text(stringResource(R.string.support), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            label = { Text("Settings") },
                            selected = navController.currentDestination?.route == AccountableFragment.AppSettingsFragment.name,
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            badge = { Text("20") }, // Placeholder
                            onClick = { mainActivityViewModel.changeFragment(
                                AccountableFragment.AppSettingsFragment
                            ) }
                        )
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            label = { Text("Help and feedback") },
                            selected = navController.currentDestination?.route == AccountableFragment.HelpFragment.name,
                            icon = { Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null) },
                            onClick = { mainActivityViewModel.changeFragment(
                                AccountableFragment.HelpFragment
                            ) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            },
            gesturesEnabled = drawerEnabled
        ) {
            currentFragment?.name?.let { startFragment ->
                mainActivityViewModel.accountableNavigationController.getAccountableActivity(
                    navController,
                    startFragment,
                    mainActivityViewModel
                )
            }
        }
    }
}