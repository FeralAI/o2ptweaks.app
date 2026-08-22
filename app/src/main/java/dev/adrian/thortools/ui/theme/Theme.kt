package dev.adrian.thortools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ThorTealDark,
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504B),
    onPrimaryContainer = Color(0xFF98F3E9),
    secondary = ThorAmberDark,
    onSecondary = Color(0xFF3C2F00),
    secondaryContainer = Color(0xFF584500),
    onSecondaryContainer = Color(0xFFFFE39A),
    tertiary = ThorBlueDark,
    onTertiary = Color(0xFF26324A),
    tertiaryContainer = Color(0xFF3C4965),
    onTertiaryContainer = Color(0xFFDBE2FF),
    background = ThorBackgroundDark,
    onBackground = ThorOnBackgroundDark,
    surface = ThorBackgroundDark,
    onSurface = ThorOnBackgroundDark,
    surfaceVariant = ThorSurfaceVariantDark,
    onSurfaceVariant = ThorOnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = ThorTealLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8CF4EA),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = ThorAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = ThorBlueLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E2FF),
    onTertiaryContainer = Color(0xFF001A41),
    background = ThorBackgroundLight,
    onBackground = ThorOnBackgroundLight,
    surface = ThorBackgroundLight,
    onSurface = ThorOnBackgroundLight,
    surfaceVariant = ThorSurfaceVariantLight,
    onSurfaceVariant = ThorOnSurfaceVariantLight,
)

@Composable
fun ThorToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
