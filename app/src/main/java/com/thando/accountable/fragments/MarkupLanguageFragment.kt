package com.thando.accountable.fragments


import android.os.Bundle
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.snackbar.Snackbar
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.fragments.viewmodels.MarkupLanguageViewModel
import com.thando.accountable.ui.cards.Colour
import com.thando.accountable.ui.cards.MarkupLanguageCard
import com.thando.accountable.ui.cards.TextFieldAccountable
import com.thando.accountable.ui.theme.AccountableTheme

class MarkupLanguageFragment : Fragment() {

    private val viewModel : MarkupLanguageViewModel by viewModels<MarkupLanguageViewModel> { MarkupLanguageViewModel.Factory }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mainActivity = (requireActivity() as MainActivity)
                mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                    object : OnBackPressedCallback(true){
                        override fun handleOnBackPressed() {
                            viewModel.navigateToScript(true)
                        }
                    }
                )

                val menuOpen by viewModel.menuOpen.collectAsStateWithLifecycle()
                AccountableTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(stringResource(R.string.markup_language)) },
                                navigationIcon = { IconButton(onClick = {
                                    viewModel.navigateToScript(true)
                                })  {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.navigate_back_to_script_fragment_button)
                                    )
                                }},
                                actions = {
                                    IconButton(onClick = { viewModel.toggleMenuOpen() }) {
                                        Icon(
                                            if (menuOpen) Icons.Default.KeyboardArrowUp
                                            else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.open_search_input_view)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    ) { innerPadding ->
                        MarkupLanguageFragmentView(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            menuOpen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkupLanguageFragmentView(
    modifier: Modifier = Modifier,
    viewModel: MarkupLanguageViewModel,
    menuOpen: Boolean
){
    val context = LocalContext.current
    val script by viewModel.script.collectAsStateWithLifecycle()
    val markupLanguage by viewModel.markupLanguage.collectAsStateWithLifecycle(null)

    var expanded by remember { mutableStateOf(false) }
    val markupLanguagesList = remember { viewModel.markupLanguagesList }
    val selectedOptionName by markupLanguage?.name?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf("") }

    val spinnerEnabled by remember { mutableStateOf(markupLanguagesList.isNotEmpty()) }

    val deleteButtonText by viewModel.deleteButtonText.collectAsStateWithLifecycle()

    val example by viewModel.openingClosingExampleSpannedString.spannableAnnotatedString.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
    var spinnerView by remember{ mutableStateOf<View?>(null) }
    val opening = remember { markupLanguage?.opening }
    val closing = remember { markupLanguage?.closing }

    val cardList = remember { viewModel.cardsList }

    LaunchedEffect(script) {
        if (script!=null) {
            viewModel.loadMarkupLanguage()
        }
    }

    LaunchedEffect(markupLanguagesList) {
        if (markupLanguagesList.isEmpty()) return@LaunchedEffect

        if (viewModel.script.value?.scriptMarkupLanguage == null){
            viewModel.setSelectedIndex(markupLanguagesList.size-1,markupLanguage, true)
        }
        else{
            for ((index,language) in markupLanguagesList.withIndex()){
                if (language.name.value == viewModel.script.value?.scriptMarkupLanguage){
                    viewModel.setSelectedIndex(index,markupLanguage,true)
                    break
                }
            }
        }
    }

    LaunchedEffect(selectedIndex){
        viewModel.loadMarkupLanguage(selectedIndex,markupLanguage)
    }

    LaunchedEffect(markupLanguage, markupLanguagesList){
        if (markupLanguage!=null && markupLanguagesList.isNotEmpty()){
            viewModel.setMarkupLanguageFunctions(markupLanguage!!, context)
            viewModel.setMarkupLanguage(markupLanguage!!)
        }
    }

    if (markupLanguage!=null && markupLanguagesList.isNotEmpty()){
        LaunchedEffect(opening?.selection, closing?.selection){
            viewModel.setOpeningClosingExample(context)
        }
    }

    LaunchedEffect(viewModel.showNameNotUniqueSnackBar){
        if (spinnerView==null) return@LaunchedEffect
        viewModel.showNameNotUniqueSnackBar.collect { name ->
            Snackbar.make(
                spinnerView!!,
                context.getString(
                    R.string.name_is_not_unique,
                    name
                ),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(viewModel.navigateToScript) {
        viewModel.navigateToScript.collect { save ->
            viewModel.closeMarkupLanguageFragment(
                save,
                markupLanguage,
                context
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (menuOpen) {
            ExposedDropdownMenuBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary),
                expanded = expanded,
                onExpandedChange = { if (spinnerEnabled) expanded = !expanded },
            ) {
                spinnerView = LocalView.current
                val fillMaxWidth = Modifier.fillMaxWidth()
                TextField(
                    value = selectedOptionName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = spinnerEnabled,
                    label = { Text("Choose an option",
                        color = MaterialTheme.colorScheme.onPrimary) },
                    trailingIcon = {
                        Icon(
                            imageVector = if (expanded)
                                    Icons.Filled.ArrowDropUp
                                else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = if (spinnerEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    },
                    modifier = fillMaxWidth.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        spinnerEnabled
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (spinnerEnabled) {
                    ExposedDropdownMenu(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary),
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        markupLanguagesList.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        selectionOption.name.value,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    viewModel.setSelectedIndex(markupLanguagesList.indexOf(selectionOption),markupLanguage,false)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Button(
                    onClick = {viewModel.changeLanguageName()},
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth().fillMaxHeight().padding(1.dp),
                    shape = RectangleShape,
                    colors = ButtonColors(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(stringResource(R.string.change_name),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = {viewModel.clearLanguage()},
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth().fillMaxHeight().padding(1.dp),
                    shape = RectangleShape,
                    colors = ButtonColors(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(deleteButtonText,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Text(
                text = example,
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 26.sp
            )

            Row(modifier = Modifier.height(IntrinsicSize.Min)){
                Text(
                    text = stringResource(R.string.opening),
                    modifier = Modifier.fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 26.sp
                )
                opening?.let { opening ->
                    TextFieldAccountable(
                        state = opening,
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth().fillMaxHeight().padding(1.dp),
                        inputTransformation = {
                            val maxCharacters = 10
                            if(length > maxCharacters){
                                replace(0,length, originalText.substring(0,maxCharacters))
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 26.sp
                        )
                    )
                }
                Text(
                    text = stringResource(R.string.closing),
                    modifier = Modifier.fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 26.sp
                )
                closing?.let { closing ->
                    TextFieldAccountable(
                        state = closing,
                        modifier = Modifier.weight(1f)
                            .fillMaxWidth().fillMaxHeight().padding(1.dp),
                        inputTransformation = {
                            val maxCharacters = 10
                            if(length > maxCharacters){
                                replace(0,length, originalText.take(maxCharacters))
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 26.sp
                        )
                    )
                }
            }
        }

        LazyColumn(
            state = viewModel.lazyListState,
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = cardList
            ) { spanCard ->
                MarkupLanguageSpanCard(
                    spanCard,
                    markupLanguage,
                    { viewModel.updateStates() }
                )
            }
        }
    }
}

@Composable
fun MarkupLanguageSpanCard(
    spanCard: MarkupLanguageCard,
    markupLanguage: MarkupLanguage?,
    updateStates: ()->Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor by spanCard.backgroundColour.collectAsStateWithLifecycle()
    val exampleText by spanCard.spannedString.spannableAnnotatedString.collectAsStateWithLifecycle()
    val identifier = remember { spanCard.tag.spanCharValue.first }
    val exampleValue = remember { spanCard.tag.spanCharValue.second }
    val colourButtonEnabled by spanCard.colourButtonEnabled.collectAsStateWithLifecycle()
    val errorMessageVisibility by spanCard.errorMessageVisibility.collectAsStateWithLifecycle()
    val duplicateErrorMessage by spanCard.duplicateErrorMessage.collectAsStateWithLifecycle()
    var exampleValueInputType by remember { mutableStateOf(KeyboardType.Text) }
    var buttonOnClick by remember { mutableStateOf({}) }
    var buttonText by remember { mutableStateOf(context.getString(R.string.pick_colour)) }
    var buttonView by remember { mutableStateOf<View?>(null) }

    if (spanCard.hasValue()) {
        if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_FLOAT) {
            exampleValueInputType = KeyboardType.Decimal
        }
        else if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_INT) {
            exampleValueInputType = KeyboardType.Number
        }
        else if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_COLOUR) {
            buttonText =
                context.getString(R.string.pick_colour)
            buttonOnClick = {
                Colour.showColorPickerDialog(context) { selectedColour: Int ->
                    spanCard.tag.spanCharValue.second.setTextAndPlaceCursorAtEnd(selectedColour.toString())
                }
            }
        } else if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_URL) {
            exampleValueInputType = KeyboardType.Text
        } else if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_CLICKABLE) {
            spanCard.clickableSpan = {
                Toast.makeText(context, "Clickable Clicked!!", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (spanCard.getSpanType() == MarkupLanguage.TagType.FUNCTION_STRING) {
            buttonText =
                context.getString(R.string.select_alignment)
            buttonOnClick = {
                buttonView?.let { buttonView ->
                    MarkupLanguage.getAlignmentMenuOnClick(
                        context, buttonView,
                        {
                            //Normal
                            spanCard.tag.spanCharValue.second.setTextAndPlaceCursorAtEnd(
                                Layout.Alignment.ALIGN_NORMAL.name
                            )
                        },
                        {
                            //Center
                            spanCard.tag.spanCharValue.second.setTextAndPlaceCursorAtEnd(
                                Layout.Alignment.ALIGN_CENTER.name
                            )
                        },
                        {
                            //Opposite
                            spanCard.tag.spanCharValue.second.setTextAndPlaceCursorAtEnd(
                                Layout.Alignment.ALIGN_OPPOSITE.name
                            )
                        }
                    )()
                }
            }
        }
    }

    LaunchedEffect(
        spanCard.tag.spanCharValue.first,
        spanCard.tag.spanCharValue.second
    ){
        spanCard.processText(
            markupLanguage = markupLanguage,
            context,
            updateStates
        )
    }

    spanCard.tag.setClickable(spanCard.clickableSpan)

    Card(
        modifier = modifier.fillMaxWidth()
            .background(Color(backgroundColor))
            .padding(1.dp),
        elevation = CardDefaults.cardElevation(30.dp),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
            Text(
                text = exampleText,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 26.sp,
                color = Color.Black
            )
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Text(
                    text = spanCard.tag.spanName+":",
                    modifier = Modifier.fillMaxHeight(),
                    fontSize = 26.sp,
                    color = Color.Black
                )
                TextFieldAccountable(
                    state = identifier,
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight(),
                    inputTransformation = {
                        val maxCharacters = 10
                        if(length > maxCharacters){
                            replace(0,length, originalText.take(maxCharacters))
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 26.sp,
                        color = Color.Black
                    )
                )

                if (spanCard.valueEditTextVisibility){
                    TextFieldAccountable(
                        state = exampleValue,
                        modifier = Modifier.weight(1f)
                            .fillMaxHeight(),
                        textStyle = TextStyle(
                            fontSize = 26.sp,
                            color = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = exampleValueInputType),
                        inputTransformation = {
                            if (exampleValueInputType == KeyboardType.Number  ||
                                exampleValueInputType == KeyboardType.Decimal) {
                                val digitsOnly = originalText.filter { it.isDigit() }
                                if (digitsOnly != originalText) {
                                    replace(0, length, digitsOnly)
                                }
                            }
                        }
                    )
                }

                if (spanCard.colourButtonVisibility) {
                    Button(
                        modifier = Modifier.weight(1f)
                            .fillMaxHeight(),
                        onClick = buttonOnClick,
                        enabled = colourButtonEnabled,
                        colors = ButtonColors(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.onPrimary
                        )
                    ){
                        buttonView = LocalView.current
                        Text(
                            buttonText,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (errorMessageVisibility) {
                    Text(
                        text = duplicateErrorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Red,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}