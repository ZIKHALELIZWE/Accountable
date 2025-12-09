package com.thando.accountable.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.GoalsViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlin.apply
import kotlin.random.Random

class GoalsFragment : Fragment() {
    private val viewModel : GoalsViewModel by viewModels { GoalsViewModel.Factory }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
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

    override fun onResume() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object  : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    viewModel.closeGoals()
                }
            })
        super.onResume()
    }
}