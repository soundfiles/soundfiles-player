package eu.ulubmp3.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UlubBlue = Color(0xFF0099FF)
val UlubBlueDark = Color(0xFF007ACB)
val UlubText = Color(0xFF20252A)
val UlubMuted = Color(0xFF6F7880)
val UlubSurface = Color(0xFFF5F7F9)
val UlubBorder = Color(0xFFE1E8ED)

private val LightScheme = lightColorScheme(
    primary = UlubBlue,
    onPrimary = Color.White,
    background = Color(0xFFF6F7F8),
    onBackground = UlubText,
    surface = Color.White,
    onSurface = UlubText,
    surfaceVariant = UlubSurface,
    outline = UlubBorder
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF33BBFF),
    onPrimary = Color(0xFF001F2E),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EDF0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8EDF0),
    surfaceVariant = Color(0xFF22282C),
    outline = Color(0xFF344047)
)

@Composable
fun UlubTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
}
