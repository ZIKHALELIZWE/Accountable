package com.thando.accountable

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thando.accountable.fragments.TeleprompterFragment
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {
    val viewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory }
    private val fragmentContainerViewId = 123456

    companion object{
        val REQUEST_BLUETOOTH_CONNECT = 65165
        fun log(message:String){
            Log.i("FATAL EXCEPTION",message)
        }

        fun <T> collectFlow(lifecycleScope: LifecycleCoroutineScope, flow: Flow<T>, collect: suspend(T) -> Unit): Job {
            return lifecycleScope.launch {
                flow.collectLatest(collect)
            }
        }

        fun <T> collectFlow(lifecycleScope: CoroutineScope, flow: Flow<T>, collect: suspend(T) -> Unit): Job {
            return lifecycleScope.launch {
                flow.collectLatest(collect)
            }
        }

        fun <T> collectFlow(currentCoroutineContext: CoroutineContext, flow: Flow<T>, collect: suspend(T) -> Unit): Job {
            val lifecycleScope = CoroutineScope(currentCoroutineContext)
            return lifecycleScope.launch {
                flow.collectLatest(collect)
            }
        }

        fun <T> collectFlow( viewLifecycleOwner: LifecycleOwner, flow: Flow<T>, collect: suspend(T) -> Unit): Job {
            return viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                    flow.collectLatest(collect)
                }
            }
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
            keyCode == KeyEvent.KEYCODE_CAMERA ||
            ) {
            // Trigger skip back
            skipBackAction()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    private fun skipBackAction() {
        // You can expose this via a shared ViewModel or state holder
        TeleprompterFragment.TeleprompterController.skipBack()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted → safe to use Bluetooth APIs
            } else {
                // Permission denied → show rationale or disable feature
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow app to draw behind system bars ( This is for teleprompter)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        enableEdgeToEdge()
        ResourceProvider.init(this.applicationContext)
        setContent {
            AccountableTheme {
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior (rememberTopAppBarState())
                val drawerState by remember { viewModel.drawerState }
                val toolbarVisible by remember { viewModel.toolbarVisible }
                val scope = rememberCoroutineScope()
                var initialized by remember { mutableStateOf(false) }
                var currentFragment by remember { mutableStateOf<AccountableNavigationController.AccountableFragment?>(null) }

                // LaunchedEffect to collect SharedFlow
                LaunchedEffect(Unit) {
                    viewModel.currentFragment.collect { fragment ->
                        if (fragment!=null){
                            if (!initialized) viewModel.navController.navigateTo(fragmentContainerViewId, fragment, supportFragmentManager)
                            currentFragment = fragment
                            if (AccountableNavigationController.isDrawerFragment(fragment)) viewModel.enableDrawer()
                            else viewModel.disableDrawer()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.direction.collect { direction ->
                        if (direction != null){
                            scope.launch { viewModel.toggleDrawer(false) }
                            viewModel.navController.navigateTo( fragmentContainerViewId, direction,supportFragmentManager)
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_stars_black_24dp),
                                    /*bitmap = AppResources.getBitmapFromUri(applicationContext, it)
                                        ?.asImageBitmap()
                                        ?: ImageBitmap(1, 1),*/
                                    contentDescription = "Navigation Drawer Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.app_name), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                                HorizontalDivider()

                                Text(stringResource(R.string.activities), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = stringResource(R.string.home)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.home)) },
                                    selected = currentFragment == AccountableNavigationController.AccountableFragment.HomeFragment,
                                    onClick = { viewModel.changeFragment(
                                        AccountableNavigationController.AccountableFragment.HomeFragment
                                    ) }
                                )
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.LibraryBooks,
                                            contentDescription = stringResource(R.string.books)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.books)) },
                                    selected = currentFragment == AccountableNavigationController.AccountableFragment.BooksFragment,
                                    onClick = { viewModel.changeFragment(
                                        AccountableNavigationController.AccountableFragment.BooksFragment
                                    ) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(stringResource(R.string.support), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                                NavigationDrawerItem(
                                    label = { Text("Settings") },
                                    selected = currentFragment == AccountableNavigationController.AccountableFragment.AppSettingsFragment,
                                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                    badge = { Text("20") }, // Placeholder
                                    onClick = { viewModel.changeFragment(
                                        AccountableNavigationController.AccountableFragment.AppSettingsFragment
                                    ) }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Help and feedback") },
                                    selected = currentFragment == AccountableNavigationController.AccountableFragment.HelpFragment,
                                    icon = { Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null) },
                                    onClick = { viewModel.changeFragment(
                                        AccountableNavigationController.AccountableFragment.HelpFragment
                                    ) }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    },
                    gesturesEnabled = true
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar =
                            if (toolbarVisible) {@Composable{
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.primary
                                ),
                                title = {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { viewModel.toggleDrawer() } })
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = stringResource(R.string.navigation_drawer_button)
                                        )
                                    }
                                },
                                actions = {},
                                scrollBehavior = scrollBehavior
                            )
                        }} else { @Composable{}}
                    ) { innerPadding ->
                        MainActivityView(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MainActivityView(
        modifier: Modifier = Modifier
    ){
        MaterialTheme(
            typography = Typography(headlineLarge = TextStyle(color = Color.Red))
        ) {
            AndroidViewFragment(
                modifier = modifier
            ){
                // On Back Press Handler
                finish()
            }
        }
    }

    @Composable
    fun AndroidViewFragment(
        modifier: Modifier = Modifier,
        backPressHandler: () -> Unit
    ) {
        BackHandler {
            backPressHandler()
        }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val frgId = FragmentContainerView(context).apply {
                    id = fragmentContainerViewId
                }
                frgId
            },
            update = {
                //Do not put the fragment update here, the fragment will be created multiple times
            }
        )
    }

    override fun onPause() {
        viewModel.closeUpdateSettings()
        super.onPause()
    }
}