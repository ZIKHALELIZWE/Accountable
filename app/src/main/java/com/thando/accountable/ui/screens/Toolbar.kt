package com.thando.accountable.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.thando.accountable.R
import com.thando.accountable.ui.theme.AccountableTheme
import kotlin.math.roundToInt

val ContentPadding = 8.dp
val Elevation = 4.dp
private val ButtonSize = 24.dp
const val Alpha = 0.75f

val ExpandedPadding = 1.dp
val CollapsedPadding = 3.dp

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


