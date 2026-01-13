package com.thando.accountable.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.GoalsViewModel
import com.thando.accountable.ui.theme.AccountableTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsView( viewModel: GoalsViewModel, mainActivityViewModel: MainActivityViewModel) {
    BackHandler {
        viewModel.closeGoals()
    }
    AccountableTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.goals)) },
                    actions = {
                        IconButton(onClick = {
                            viewModel.loadEditGoal()
                        }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = stringResource(R.string.add_goal)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            GoalsFragmentView(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun GoalsFragmentView(modifier: Modifier = Modifier) {
    /*val times = remember { newGoal.times }
    LazyColumn(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .height((LocalWindowInfo.current.containerSize.height - buttonHeightPx * 2).dp)
    ) {
        items(items = times, key = { it.id?.toInt()?:Random.nextInt() }) { item ->
            TimeInputView(item)
            if (times.indexOf(item) != times.lastIndex) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }*/
}