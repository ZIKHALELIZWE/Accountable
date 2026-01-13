package com.thando.accountable

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.navigation.compose.rememberNavController
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.MainActivity.ResourceProvider
import com.thando.accountable.ui.theme.AccountableTheme


class IntentActivity : ComponentActivity() {

    val viewModel: IntentActivityViewModel by viewModels {
        IntentActivityViewModel.Factory(
            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            } else null
        )
    }
    val mainActivityViewModel: MainActivityViewModel by viewModels { MainActivityViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ResourceProvider.init(this.applicationContext)

        setContent {
            val navController = rememberNavController()
            var initialized by remember { mutableStateOf(false) }
            var currentFragment by remember { mutableStateOf<AccountableFragment?>(null) }
            val context = LocalContext.current
            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                if(!intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrEmpty()) {
                    LaunchedEffect(Unit) {
                        viewModel.currentFragment.collect { fragment ->
                            if (fragment!=null){
                                if (!initialized){
                                    (0 until navController.currentBackStack.value.size).forEach { _ ->
                                        navController.popBackStack()
                                    }
                                    currentFragment =
                                        if (
                                            fragment == AccountableFragment.BooksFragment ||
                                            fragment == AccountableFragment.SearchFragment
                                        ) fragment else AccountableFragment.BooksFragment
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.direction.collect { direction ->
                            if (direction != null){
                                (0 until navController.currentBackStack.value.size).forEach { _ ->
                                    navController.popBackStack()
                                }
                                navController.navigate(
                                    direction.name
                                ){launchSingleTop = true}
                            }
                        }
                    }

                    AccountableTheme {
                        val height = LocalResources.current.displayMetrics.heightPixels*0.8f
                        val width = LocalResources.current.displayMetrics.widthPixels*0.8f
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Transparent)
                                .combinedClickable(onClick = { finish() }),
                            contentAlignment = Alignment.Center
                        ) {
                            currentFragment?.name?.let { startFragment ->
                                mainActivityViewModel.accountableNavigationController.getAccountableActivity(
                                    navController,
                                    startFragment,
                                    mainActivityViewModel,
                                    modifier = Modifier.height(
                                        with(density){height.toDp()}
                                    ).width(
                                        with(density){width.toDp()}
                                    )
                                )
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(context,
                        getString(R.string.no_text_shared),Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else{
                Toast.makeText(context,
                    getString(R.string.no_text_shared),Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        AccountableRepository.getInstance(application).intentString = null
        super.onDestroy()
    }
}