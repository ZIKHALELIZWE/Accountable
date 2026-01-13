package com.thando.accountable.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.thando.accountable.AnimalListPreviewParameterProvider
import com.thando.accountable.ui.theme.AccountableTheme

private const val BottomBarHeightFraction = 0.14f
private const val TopBarHeightFraction = BottomBarHeightFraction / 2
private val BarColor = Color(red = 255f, green = 255f, blue = 255f, alpha = 0.5f)

@Preview(showBackground = true)
@Composable
fun AnimalCardPreview(
    @PreviewParameter(AnimalListPreviewParameterProvider::class) animals: List<Animal>
) {
    AccountableTheme {
        Row(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
        ) {
            animals.take(2).forEach {
                AnimalCard(
                    animal = it,
                    modifier = Modifier
                        .padding(2.dp)
                        .weight(1f)
                        .wrapContentHeight()
                )
            }
        }
    }
}

@Composable
fun AnimalCard(
    animal: Animal,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(0.66f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(animal.imageResId),
                contentDescription = animal.name,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
            BottomBar(animal.name)
        }
    }
}

@Composable
private fun BoxScope.BottomBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(BottomBarHeightFraction)
            .background(BarColor)
            .align(Alignment.BottomCenter)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        )
    }
}

