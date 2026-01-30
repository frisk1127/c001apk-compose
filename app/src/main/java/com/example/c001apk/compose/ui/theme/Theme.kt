package com.example.c001apk.compose.ui.theme

import android.app.Activity
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Build.VERSION_CODES.S
import androidx.annotation.FloatRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.seedColors
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

enum class ColorSchemeMode {
    LIGHT,
    DARK,
    BLACK
}

@Composable
fun C001apkComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeType: ThemeType = ThemeType.Default,
    seedColor: String? = null,
    materialYou: Boolean = true,
    pureBlack: Boolean = false,
    paletteStyle: Int = 0,
    fontScale: Float = 1.00f,
    contentScale: Float = 1.00f,
    content: @Composable () -> Unit
) {

    val colorSchemeMode =
        when (darkTheme) {
            true -> when (pureBlack) {
                true -> ColorSchemeMode.BLACK
                false -> ColorSchemeMode.DARK
            }

            false -> ColorSchemeMode.LIGHT
        }

    val colorScheme = if (SDK_INT >= S && materialYou) {
        val context = LocalContext.current
        when (colorSchemeMode) {
            ColorSchemeMode.LIGHT -> dynamicLightColorScheme(context)
            ColorSchemeMode.DARK -> dynamicDarkColorScheme(context)
            ColorSchemeMode.BLACK -> dynamicDarkColorScheme(context).toAmoled()
        }
    } else {
        val color = Color(
            seedColors.getOrNull(ThemeType.entries.indexOf(themeType))
                ?: "FF$seedColor".toLongOrNull(16) ?: seedColors[0]
        )
        when (colorSchemeMode) {
            ColorSchemeMode.LIGHT ->
                rememberDynamicColorScheme(color, false, style = PaletteStyle.entries[paletteStyle])

            ColorSchemeMode.DARK ->
                rememberDynamicColorScheme(color, true, style = PaletteStyle.entries[paletteStyle])

            ColorSchemeMode.BLACK ->
                rememberDynamicColorScheme(
                    color,
                    true,
                    style = PaletteStyle.entries[paletteStyle]
                ).toAmoled()
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            if (SDK_INT >= Q) {
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    LocalDensity.current.density * contentScale,
                    LocalDensity.current.fontScale * fontScale,
                )
            ) {
                content()
            }
        }
    )

}

fun Color.darken(fraction: Float = 0.5f): Color =
    Color(toArgb().blend(Color.Black.toArgb(), fraction))

fun Int.blend(
    color: Int,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.5f,
): Int = ColorUtils.blendARGB(this, color, fraction)

fun ColorScheme.toAmoled(): ColorScheme {
    return copy(
        primary = primary.darken(0.3f),
        onPrimary = onPrimary.darken(0.3f),
        primaryContainer = primaryContainer.darken(0.3f),
        onPrimaryContainer = onPrimaryContainer.darken(0.3f),
        inversePrimary = inversePrimary.darken(0.3f),
        secondary = secondary.darken(0.3f),
        onSecondary = onSecondary.darken(0.3f),
        secondaryContainer = secondaryContainer.darken(0.3f),
        onSecondaryContainer = onSecondaryContainer.darken(0.3f),
        tertiary = tertiary.darken(0.3f),
        onTertiary = onTertiary.darken(0.3f),
        tertiaryContainer = tertiaryContainer.darken(0.3f),
        onTertiaryContainer = onTertiaryContainer.darken(0.2f),
        background = Color.Black,
        onBackground = onBackground.darken(0.15f),
        surface = Color.Black,
        onSurface = onSurface.darken(0.15f),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface.darken(),
        inverseOnSurface = inverseOnSurface.darken(0.2f),
        outline = outline.darken(0.2f),
        outlineVariant = outlineVariant.darken(0.2f),
        surfaceContainer = surfaceContainer.darken(),
        surfaceContainerHigh = surfaceContainerHigh.darken(),
        surfaceContainerHighest = surfaceContainerHighest.darken(0.4f),
    )
}
