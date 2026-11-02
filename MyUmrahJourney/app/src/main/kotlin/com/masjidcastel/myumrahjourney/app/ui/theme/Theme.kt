package com.masjidcastel.myumrahjourney.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Raw palette: dark emerald surfaces, turquoise accent, a muted sand tone for small highlights. */
object MyUmrahColors {
    val EmeraldDeep = Color(0xFF04251C)
    val EmeraldSurface = Color(0xFF062B22)
    val EmeraldContainer = Color(0xFF0A3329)
    val EmeraldContainerHigh = Color(0xFF0F3D32)
    val EmeraldContainerHighest = Color(0xFF14473B)
    val EmeraldMid = Color(0xFF0B5D4B)
    val Turquoise = Color(0xFF2DD4BF)
    val TurquoiseDeep = Color(0xFF0E7C7B)
    val TurquoiseContainer = Color(0xFF0B5D52)
    val Sand = Color(0xFFE6D3A3)

    val TextPrimary = Color(0xFFDDEFE8)
    val TextSecondary = Color(0xFFA8C7BC)

    val HeroGradient: List<Color> = listOf(EmeraldMid, TurquoiseDeep)
}

private val MyUmrahDarkColorScheme = darkColorScheme(
    primary = MyUmrahColors.Turquoise,
    onPrimary = Color(0xFF00201C),
    primaryContainer = MyUmrahColors.TurquoiseContainer,
    onPrimaryContainer = Color(0xFFB4F5EC),
    secondary = Color(0xFF5ED3A8),
    onSecondary = Color(0xFF00281C),
    secondaryContainer = Color(0xFF0E4D39),
    onSecondaryContainer = Color(0xFFBFEBD9),
    tertiary = MyUmrahColors.Sand,
    onTertiary = Color(0xFF3A2F0B),
    tertiaryContainer = Color(0xFF52441D),
    onTertiaryContainer = Color(0xFFF5E6BE),
    background = MyUmrahColors.EmeraldDeep,
    onBackground = MyUmrahColors.TextPrimary,
    surface = MyUmrahColors.EmeraldSurface,
    onSurface = MyUmrahColors.TextPrimary,
    surfaceVariant = Color(0xFF123D33),
    onSurfaceVariant = MyUmrahColors.TextSecondary,
    surfaceContainerLowest = Color(0xFF031C15),
    surfaceContainerLow = Color(0xFF07281F),
    surfaceContainer = MyUmrahColors.EmeraldContainer,
    surfaceContainerHigh = MyUmrahColors.EmeraldContainerHigh,
    surfaceContainerHighest = MyUmrahColors.EmeraldContainerHighest,
    outline = Color(0xFF5F8579),
    outlineVariant = Color(0xFF2A5246),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val MyUmrahTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
)

private val MyUmrahShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** The app is dark-only: dark emerald surfaces with turquoise accents. */
@Composable
fun MyUmrahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyUmrahDarkColorScheme,
        typography = MyUmrahTypography,
        shapes = MyUmrahShapes,
        content = content,
    )
}
