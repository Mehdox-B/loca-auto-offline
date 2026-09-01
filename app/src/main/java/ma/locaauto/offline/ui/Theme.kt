package ma.locaauto.offline.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E69),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F1E9),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFFE27D32),
    tertiary = Color(0xFF5C5AA8),
    background = Color(0xFFF8FAF8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7F0EE),
    onSurface = Color(0xFF17201F),
    onSurfaceVariant = Color(0xFF3E4947)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF65DBCF),
    secondary = Color(0xFFFFB77D),
    tertiary = Color(0xFFC4C2FF)
)

@Composable
fun LocaAutoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
