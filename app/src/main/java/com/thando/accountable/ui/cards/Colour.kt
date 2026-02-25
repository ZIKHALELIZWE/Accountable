package com.thando.accountable.ui.cards

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.thando.accountable.R
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot

class ColourPickerDialog {
    val pickedColour = mutableStateOf(Color.Black)
    val showColourPickerDialog = mutableStateOf(false)
    val processSelectedColour = mutableStateOf<(suspend (Int)->Unit)?>(null)

    fun pickColour(originalColour: Color? = null, processPickedColour:suspend (Int) -> Unit){
        originalColour?.let { pickedColour.value = it }
        processSelectedColour.value = { selectedColour: Int ->
            pickedColour.value = Color(selectedColour)
            processPickedColour(selectedColour)
        }
        showColourPickerDialog.value = true
    }

    @Composable
    fun ColourPicker(){
        val pickedColour by remember { pickedColour }
        var showColourPickerDialog by remember { showColourPickerDialog }
        val processSelectedColour by remember { processSelectedColour }
        val scope = rememberCoroutineScope()

        if (showColourPickerDialog) {
            ColourPickerDialog(
                inputColour = pickedColour,
                processSelectedColour = { selectedColour ->
                   scope.launch {
                       processSelectedColour?.invoke(selectedColour)
                   }
                    showColourPickerDialog = false
                },
                onDismiss = { showColourPickerDialog = false },
            )
        }
    }

    @Composable
    @SuppressLint("NotConstructor")
    fun ColourPickerDialog(
        inputColour: Color,
        processSelectedColour: (selectedColour: Int) -> Unit,
        onDismiss: () -> Unit
    ) {
        var selectedColour by remember { mutableStateOf(inputColour) }

        var hue by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var brightness by remember { mutableFloatStateOf(1f) }

        val density = LocalDensity.current
        var size by remember { mutableStateOf(0.dp) }
        val radius = with(density) { size.toPx() / 2 }
        val center = Offset(radius, radius)

        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .testTag("ColourPickerDialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp).verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.choose_a_colour),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        Modifier.fillMaxWidth().wrapContentHeight()
                            .onGloballyPositioned {
                                size = with(density) { it.size.width.toDp() }
                            }
                    ) {
                        Canvas(
                            modifier = Modifier.size(size).testTag("ColourPickerDialogCanvas")
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val (newHue, newSaturation) = calculateColour(
                                            offset,
                                            center,
                                            radius
                                        ) ?: return@detectTapGestures
                                        val colour = Color.hsv(newHue, newSaturation, brightness)
                                        if (selectedColour == colour) return@detectTapGestures
                                        hue = newHue
                                        saturation = newSaturation
                                        selectedColour = Color.hsv(hue, saturation, brightness)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val (newHue, newSaturation) = calculateColour(
                                            change.position,
                                            center,
                                            radius
                                        ) ?: return@detectDragGestures
                                        val colour = Color.hsv(newHue, newSaturation, brightness)
                                        if (selectedColour == colour) return@detectDragGestures
                                        hue = newHue
                                        saturation = newSaturation
                                        selectedColour = Color.hsv(hue, saturation, brightness)
                                    }
                                }
                        ) {
                            for (i in 0..360) {
                                drawArc(
                                    color = Color.hsv(i.toFloat(), 1f, 1f),
                                    startAngle = i.toFloat(),
                                    sweepAngle = 1f,
                                    useCenter = false,
                                    topLeft = Offset(size.value, size.value),
                                    size = Size(size.value, size.value),
                                    style = Stroke(width = radius)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            selectedColour = Color
                                .hsv(hue, saturation, brightness)
                        },
                        valueRange = 0f..1f
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .padding(3.dp)
                            .background(
                                selectedColour,
                                shape = RectangleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        TextButton(
                            modifier = Modifier.testTag("ColourPickerDialogOKButton"),
                            onClick = {
                                processSelectedColour(
                                    selectedColour.toArgb()
                                )
                            }
                        ) {
                            Text("OK")
                        }
                        TextButton(
                            modifier = Modifier.testTag("ColourPickerDialogDismissButton"),
                            onClick = onDismiss
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    private fun calculateColour(
        offset: Offset,
        center: Offset,
        radius: Float
    ): Pair<Float, Float>? {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        val distance = hypot(dx, dy)
        if (distance <= radius) {
            val angle = atan2(dy, dx)
            val hue = Math.toDegrees(angle.toDouble()).toFloat().let {
                if (it < 0) it + 360f else it
            }
            val saturation = distance / radius
            return hue to saturation
        }
        return null
    }
}


