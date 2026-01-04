package com.thando.accountable.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thando.accountable.R

val MinToolbarHeight = 64.dp
val MaxToolbarHeight = 300.dp

data class MenuItemData(
    var text:String = "No Name",
    val onClick:()->Unit
)

fun basicDropdownMenu(options:List<MenuItemData>):@Composable ((Modifier) -> Unit) = @Composable {
    modifier ->
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = !expanded }) {
             Icon(
                 Icons.Default.MoreVert,
                 contentDescription = stringResource(R.string.more_options)
             )
        }
        DropdownMenu(
             expanded = expanded,
             onDismissRequest = { expanded = false }
        ) {
             options.forEach { option ->
                 DropdownMenuItem(
                     text = { Text(option.text) },
                     onClick = {
                         option.onClick.invoke()
                         expanded = false
                     }
                 )
             }
        }
    }
}