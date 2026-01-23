package com.thando.accountable.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scope = rememberCoroutineScope()

    BackHandler {
        scope.launch { viewModel.closeTasks() }
    }

    AccountableTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.goal)) },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier,
                            onClick = { scope.launch { viewModel.closeTasks() } }
                        )
                        {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back_to_goals)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            TasksFragmentView(
                viewModel,
                mainActivityViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun TasksFragmentView(
    viewModel: TaskViewModel,
    mainActivityViewModel: MainActivityViewModel,
    modifier: Modifier = Modifier
) {

}