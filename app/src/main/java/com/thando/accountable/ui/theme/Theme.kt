package com.thando.accountable.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = DeepSapphire,
    secondary = MineShaft,
    background = Black,
    surface = DeepSapphire,
    onPrimary = White,
    onSecondary = White,
    onSurface = White,
    onBackground = White
)

private val LightColorScheme = lightColorScheme(
    primary = Denim,
    secondary = Scorpion,
    background = White,
    surface = Denim,
    onPrimary = White,
    onSecondary = White,
    onSurface = White,
    onBackground = Black
)

@Composable
fun AccountableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !darkTheme

    systemUiController.setSystemBarsColor(
        color = if (darkTheme) Color.Black else Color.White,
        darkIcons = useDarkIcons
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
        shapes = Shapes
    )
}