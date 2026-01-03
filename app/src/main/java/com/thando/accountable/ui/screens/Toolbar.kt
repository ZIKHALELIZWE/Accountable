package com.thando.accountable.ui.screens

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.database.tables.Script
import com.thando.accountable.ui.theme.AccountableTheme
import kotlin.math.roundToInt

private val ContentPadding = 8.dp
private val Elevation = 4.dp
private val ButtonSize = 24.dp
private const val Alpha = 0.75f

private val ExpandedPadding = 1.dp
private val CollapsedPadding = 3.dp

private val ExpandedCostaRicaHeight = 20.dp
private val CollapsedCostaRicaHeight = 16.dp

private val ExpandedWildlifeHeight = 32.dp
private val CollapsedWildlifeHeight = 24.dp

private val MapHeight = CollapsedCostaRicaHeight * 2

@Preview
@Composable
fun CollapsingToolbarCollapsedPreview() {
    AccountableTheme {
        CollapsingToolbar(
            backgroundImageResId = R.drawable.toolbar_background,
            progress = 0f,
            onPrivacyTipButtonClicked = {},
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

@Preview
@Composable
fun CollapsingToolbarHalfwayPreview() {
    AccountableTheme {
        CollapsingToolbar(
            backgroundImageResId = R.drawable.toolbar_background,
            progress = 0.5f,
            onPrivacyTipButtonClicked = {},
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

@Preview
@Composable
fun CollapsingToolbarExpandedPreview() {
    AccountableTheme {
        CollapsingToolbar(
            backgroundImageResId = R.drawable.toolbar_background,
            progress = 1f,
            onPrivacyTipButtonClicked = {},
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }
}

@Composable
fun CollapsingToolbar(
    @DrawableRes backgroundImageResId: Int,
    progress: Float,
    onPrivacyTipButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val costaRicaHeight = with(LocalDensity.current) {
        lerp(CollapsedCostaRicaHeight.toPx(), ExpandedCostaRicaHeight.toPx(), progress).toDp()
    }
    val wildlifeHeight = with(LocalDensity.current) {
        lerp(CollapsedWildlifeHeight.toPx(), ExpandedWildlifeHeight.toPx(), progress).toDp()
    }
    val logoPadding = with(LocalDensity.current) {
        lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = Elevation
    ) {
        Box (modifier = Modifier.fillMaxSize()) {
            //#region Background Image
            Image(
                painter = painterResource(id = backgroundImageResId),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = progress * Alpha
                    },
                alignment = BiasAlignment(0f, 1f - ((1f - progress) * 0.75f))
            )
            //#endregion
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = ContentPadding)
                    .fillMaxSize()
            ) {
                CollapsingToolbarLayout (progress = progress) {
                    //#region Logo Images
                    Image(
                        painter = painterResource(id = R.drawable.logo_costa_rica_map),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(logoPadding)
                            .height(MapHeight)
                            .wrapContentWidth()
                            .graphicsLayer { alpha = ((0.25f - progress) * 4).coerceIn(0f, 1f) },
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logo_costa),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(logoPadding)
                            .height(costaRicaHeight)
                            .wrapContentWidth(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logo_rica),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(logoPadding)
                            .height(costaRicaHeight)
                            .wrapContentWidth(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logo_wildlife),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(logoPadding)
                            .height(wildlifeHeight)
                            .wrapContentWidth(),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                    )
                    //#endregion
                    //#region Buttons
                    Row (
                        modifier = Modifier.wrapContentSize().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ContentPadding)
                    ) {
                        IconButton(
                            onClick = onPrivacyTipButtonClicked,
                            modifier = Modifier
                                .size(ButtonSize)
                                .background(
                                    color = LocalContentColor.current.copy(alpha = 0.0f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Rounded.PrivacyTip,
                                contentDescription = null,
                            )
                        }
                        IconButton(
                            onClick = onSettingsButtonClicked,
                            modifier = Modifier
                                .size(ButtonSize)
                                .background(
                                    color = LocalContentColor.current.copy(alpha = 0.0f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                            )
                        }
                    }
                    //#endregion
                }
            }
        }
    }
}

@Composable
private fun CollapsingToolbarLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 5) // [0]: Country Map | [1-3]: Logo Images | [4]: Buttons

        val items = measurables.map {
            it.measure(constraints)
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {

            val expandedHorizontalGuideline = (constraints.maxHeight * 0.4f).roundToInt()
            val collapsedHorizontalGuideline = (constraints.maxHeight * 0.5f).roundToInt()

            val countryMap = items[0]
            val costa = items[1]
            val rica = items[2]
            val wildlife = items[3]
            val buttons = items[4]
            countryMap.placeRelative(
                x = 0,
                y = collapsedHorizontalGuideline - countryMap.height / 2,
            )
            costa.placeRelative(
                x = lerp(
                    start = countryMap.width,
                    stop = constraints.maxWidth / 2 - costa.width,
                    fraction = progress
                ),
                y = lerp(
                    start = collapsedHorizontalGuideline - costa.height / 2,
                    stop = expandedHorizontalGuideline - costa.height,
                    fraction = progress
                )
            )
            rica.placeRelative(
                x = lerp(
                    start = countryMap.width + costa.width,
                    stop = constraints.maxWidth / 2 - rica.width,
                    fraction = progress
                ),
                y = lerp(
                    start = collapsedHorizontalGuideline - rica.height / 2,
                    stop = expandedHorizontalGuideline,
                    fraction = progress
                )
            )
            wildlife.placeRelative(
                x = lerp(
                    start = countryMap.width + costa.width + rica.width,
                    stop = constraints.maxWidth / 2,
                    fraction = progress
                ),
                y = lerp(
                    start = collapsedHorizontalGuideline - wildlife.height / 2,
                    stop = expandedHorizontalGuideline + rica.height / 2,
                    fraction = progress
                )
            )
            buttons.placeRelative(
                x = constraints.maxWidth - buttons.width,
                y = lerp(
                    start = (constraints.maxHeight - buttons.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
        }
    }
}

@Composable
fun FoldersAndScriptsCollapsingToolbar(
    modifier: Modifier = Modifier,
    progress: Float,
    imageUri: Uri?,
    navigationIcon:@Composable (Modifier)-> Unit,
    titleText:@Composable (Modifier)-> Unit,
    searchIcon:@Composable (Modifier)-> Unit,
    orderIcon:@Composable (Modifier)-> Unit,
    folderScriptSwitchIcon:@Composable (Modifier)-> Unit,
) {
    val logoPadding = with(LocalDensity.current) {
        lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = Elevation
    ) {
        Box (modifier = Modifier.fillMaxSize()) {
            //#region Background Image
            Image(
                bitmap = imageUri?.let { AppResources.getBitmapFromUri(LocalContext.current, imageUri) }
                    ?.asImageBitmap()
                    ?: AppResources.getBitmapFromUri(
                        LocalContext.current,
                        AppResources.getUriFromDrawable(
                            LocalContext.current,
                            R.drawable.ic_stars_black_24dp
                        )
                    )?.asImageBitmap()?:ImageBitmap(1, 1),
                contentDescription = stringResource(R.string.folder_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = progress * Alpha
                    }
            )
            //#endregion
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = ContentPadding)
                    .fillMaxSize()
            ) {
                FoldersAndScriptsCollapsingToolbarLayout (progress = progress) {
                    val mod = Modifier.padding(logoPadding).wrapContentWidth()
                    navigationIcon(mod)
                    titleText(mod)
                    searchIcon(mod)
                    orderIcon(mod)
                    folderScriptSwitchIcon(mod)
                }
            }
        }
    }
}

@Composable
private fun FoldersAndScriptsCollapsingToolbarLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 5)

        val items = measurables.map {
            it.measure(constraints)
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            val navigationIcon = items[0]
            val titleText = items[1]
            val searchIcon = items[2]
            val orderIcon = items[3]
            val folderScriptSwitchIcon = items[4]
            navigationIcon.placeRelative(
                x = 0,
                y = lerp(
                    start = (constraints.maxHeight - navigationIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            titleText.placeRelative(
                x = lerp(
                    start = constraints.maxWidth / 2 - titleText.width,
                    stop = 0,
                    fraction = progress
                ),
                y = lerp(
                    start = (constraints.maxHeight - titleText.height) / 2,
                    stop = constraints.maxHeight - titleText.height,
                    fraction = progress
                )
            )
            searchIcon.placeRelative(
                x = constraints.maxWidth - folderScriptSwitchIcon.width - orderIcon.width - searchIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - searchIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            orderIcon.placeRelative(
                x = constraints.maxWidth - folderScriptSwitchIcon.width - orderIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - orderIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            folderScriptSwitchIcon.placeRelative(
                x = constraints.maxWidth - folderScriptSwitchIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - folderScriptSwitchIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
        }
    }
}

@Composable
fun ScriptsCollapsingToolbar(
    modifier: Modifier = Modifier,
    progress: Float,
    progressMax: Float,
    imageUri: Uri?,
    navigationIcon:@Composable (Modifier)-> Unit,
    addContentButton:(@Composable (Modifier)-> Unit)?,
    shareIcon:@Composable (Modifier)-> Unit,
    teleprompterIcon:@Composable (Modifier)-> Unit,
    basicDropdownMenu:@Composable (Modifier)-> Unit,
) {
    val logoPadding = with(LocalDensity.current) {
        lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = Elevation
    ) {
        Box (modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            //#region Background Image
            imageUri?.let {
                Image(
                    bitmap = AppResources.getBitmapFromUri(
                            LocalContext.current,
                            imageUri
                        )
                        ?.asImageBitmap() ?: ImageBitmap(1, 1),
                    contentDescription = stringResource(R.string.script_display_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = progress
                        }
                )
            }
            //#endregion
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = ContentPadding)
                    .fillMaxSize()
            ) {
                addContentButton?.let {
                    ScriptsCollapsingToolbarLayout (progress = progress) {
                        val mod = Modifier.padding(logoPadding).wrapContentWidth()
                        navigationIcon(mod)
                        Text(modifier = mod.graphicsLayer {
                            alpha = progressMax - progress
                        }, text = stringResource(R.string.script))
                        shareIcon(mod)
                        teleprompterIcon(mod)
                        basicDropdownMenu(mod)
                        addContentButton(mod)
                    }
                }?:run{
                    ScriptsCollapsingToolbarLayout (progress = progress) {
                        val mod = Modifier.padding(logoPadding).wrapContentWidth()
                        navigationIcon(mod)
                        Text(modifier = mod.graphicsLayer {
                            alpha = progressMax - progress
                        }, text = stringResource(R.string.script))
                        shareIcon(mod)
                        teleprompterIcon(mod)
                        basicDropdownMenu(mod)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptsCollapsingToolbarLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 5 || measurables.size == 6)

        val items = measurables.map {
            it.measure(constraints)
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            val navigationIcon = items[0]
            val titleText = items[1]
            val shareIcon = items[2]
            val teleprompterIcon = items[3]
            val basicDropdownMenu = items[4]
            val addContentIcon = if (measurables.size == 6) items[5] else null
            navigationIcon.placeRelative(
                x = 0,
                y = lerp(
                    start = (constraints.maxHeight - navigationIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            titleText.placeRelative(
                x = navigationIcon.width + titleText.width/2 + ((constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width - (addContentIcon?.width
                    ?: 0) - navigationIcon.width)/2)/2,
                y = lerp(
                    start = (constraints.maxHeight - titleText.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            addContentIcon?.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width - addContentIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - addContentIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            shareIcon.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - shareIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            teleprompterIcon.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - teleprompterIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            basicDropdownMenu.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width,
                y = lerp(
                    start = (constraints.maxHeight - basicDropdownMenu.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
        }
    }
}
