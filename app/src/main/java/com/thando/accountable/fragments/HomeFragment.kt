package com.thando.accountable.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView( viewModel : HomeViewModel, mainActivityViewModel: MainActivityViewModel) {
    mainActivityViewModel.enableDrawer()
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AccountableTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.home)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { mainActivityViewModel.toggleDrawer() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.navigation_drawer_menu)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuOpen = !menuOpen }) {
                            Icon(
                                if (menuOpen) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.navigation_drawer_menu)
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                viewModel.loadGoals()
                            }
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_stars_black_24dp),
                                contentDescription = stringResource(R.string.navigate_to_goals_button)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            HomeFragmentView(modifier = Modifier.padding(innerPadding), menuOpen)
        }
    }
}

@Composable
fun HomeFragmentView(modifier: Modifier = Modifier, menuOpen: Boolean) {
    // State to hold the current date and time
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now())}
    var isWeekView by remember { mutableStateOf(true)}
    val scrollState = rememberScrollState()

    var chosenDateTime:LocalDateTime? by remember { mutableStateOf(null) }

    // Update every second
    LaunchedEffect(Unit){
        while(true){
            currentDateTime = LocalDateTime.now()
            delay(1000) // 1 second
        }
    }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(8.dp)
    ) {
        if (menuOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Button(
                    onClick = { isWeekView = !isWeekView },
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                ) {
                    Text(if (isWeekView) stringResource(R.string.week) else stringResource(R.string.day))
                }
                if (chosenDateTime != null) {
                    Button(
                        onClick = { chosenDateTime = null },
                        Modifier
                            .weight(2f)
                            .padding(horizontal = 1.dp)
                    ) {
                        Text(stringResource(R.string.current_date))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        chosenDateTime = changeChosenDate(true, isWeekView, chosenDateTime)
                        if (chosenDateTime?.toLocalDate() == currentDateTime.toLocalDate()) {
                            chosenDateTime = null
                        }
                    },
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowLeft,
                        contentDescription = stringResource(R.string.previous_date_button)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2196F3),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp)
                        .weight(2f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${chosenDateTime?.month ?: currentDateTime.month} ${chosenDateTime?.year ?: currentDateTime.year}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Button(
                    onClick = {
                        chosenDateTime = changeChosenDate(false, isWeekView, chosenDateTime)
                        if (chosenDateTime?.toLocalDate() == currentDateTime.toLocalDate()) {
                            chosenDateTime = null
                        }
                    },
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = stringResource(R.string.next_date_button)
                    )
                }
            }
        }
        DateTimeView(currentDate = chosenDateTime?:currentDateTime, todayDate = currentDateTime, scrollState, isWeekView)
    }
}

@Composable
fun DateTimeView(currentDate: LocalDateTime, todayDate: LocalDateTime, scrollState: ScrollState, isWeekView: Boolean) {
    val weekDates = getWeekDates(currentDate)
    val weightModifier = Modifier.padding(horizontal = 1.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayItem(null,false, weightModifier.weight(1f))
        if (isWeekView){
            weekDates.forEach { day ->
                DayItem(day = day, isToday = day.toLocalDate() == todayDate.toLocalDate(), weightModifier.weight(1f))
            }
        }
        else {
            DayItem(day = currentDate, isToday = currentDate.toLocalDate() == todayDate.toLocalDate(), weightModifier.weight(1f))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        for (i in 0..23) {
            if (i != 0){
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp, color = Color.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HourItem(i, isHour = i == todayDate.hour, weightModifier.weight(1f))
                if (isWeekView){
                    weekDates.forEach { _ ->
                        HourItem(null,false, weightModifier.weight(1f))
                    }
                }
                else{
                    HourItem(null,false, weightModifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HourItem(hour: Int?, isHour: Boolean, modifier: Modifier) {
    val hourItemModifier = if (hour != null) Modifier.width(48.dp) else modifier
    Column(
        modifier = hourItemModifier
            .background(
                color = if (hour == null || !isHour) Color.Transparent else Color(0xFF2196F3),
            )
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hour != null) {
            Text(
                text = hour.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHour) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun DayItem(day: LocalDateTime?, isToday: Boolean, modifier: Modifier) {
    val dayItemModifier = if (day == null) Modifier.width(48.dp) else modifier
    Column(
        modifier = dayItemModifier
            .background(
                color = if (day == null) Color.Transparent else if (isToday) Color(0xFF2196F3) else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (day != null) {
            Text(
                text = day.dayOfWeek.name.take(3), // Mon, Tue, etc.
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) Color.White else Color.Black
            )
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) Color.White else Color.Black
            )
        }
    }
}

fun getWeekDates(date: LocalDateTime): List<LocalDateTime> {
    val startOfWeek = date.with(DayOfWeek.MONDAY)
    return (0..6).map { startOfWeek.plusDays(it.toLong()) }
}

fun changeChosenDate(minus:Boolean, isWeekView: Boolean, chosenDateTime: LocalDateTime?):LocalDateTime?{
    if (isWeekView){
        return if (chosenDateTime!=null){
            if (minus) chosenDateTime.minusWeeks(1)
            else chosenDateTime.plusWeeks(1)
        } else{
            if (minus) LocalDateTime.now().minusWeeks(1)
            else LocalDateTime.now().plusWeeks(1)
        }
    }
    else{
        return if (chosenDateTime!=null){
            if (minus) chosenDateTime.minusDays(1)
            else chosenDateTime.plusDays(1)
        } else{
            if (minus) LocalDateTime.now().minusDays(1)
            else LocalDateTime.now().plusDays(1)
        }
    }
}