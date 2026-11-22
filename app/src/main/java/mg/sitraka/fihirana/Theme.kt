package mg.sitraka.fihirana

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class Accent(val name: String, val light: Color, val dark: Color)

val Accents = listOf(
    Accent("Teal", Color(0xFF0F766E), Color(0xFF2DD4BF)),
    Accent("Indigo", Color(0xFF3949AB), Color(0xFF9FA8FF)),
    Accent("Emeraude", Color(0xFF15803D), Color(0xFF4ADE80)),
    Accent("Rose", Color(0xFFBE185D), Color(0xFFF472B6)),
    Accent("Ambre", Color(0xFFB45309), Color(0xFFFBBF24)),
    Accent("Violet", Color(0xFF6D28D9), Color(0xFFC4B5FD)),
)

val HighlightColors = listOf(
    Color(0x66FFD54F), // 1 mavo
    Color(0x6681C784), // 2 maitso
    Color(0x6664B5F6), // 3 manga
    Color(0x66F48FB1), // 4 mavokely
    Color(0x66FFB74D), // 5 volomboasary
)

@Immutable
class Extra(
    val dark: Boolean,
    val heading: Color,
    val verseNum: Color,
    val card: Color,
    val muted: Color,
    val heroA: Color,
    val heroB: Color,
    val hairline: Color,
)

val LocalExtra = staticCompositionLocalOf<Extra> { error("Extra non fourni") }

@Composable
fun isDarkMode(): Boolean = when (Prefs.themeMode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isDarkMode()
    val acc = Accents[Prefs.accent.coerceIn(0, Accents.size - 1)]
    val bgDark = Color(0xFF0E1114)
    val scheme = if (dark) {
        val prim = acc.dark
        darkColorScheme(
            primary = prim,
            onPrimary = Color(0xFF06201D),
            primaryContainer = lerp(Color(0xFF161B20), prim, 0.24f),
            onPrimaryContainer = Color(0xFFEAF6F4),
            secondary = lerp(prim, Color.White, 0.25f),
            onSecondary = Color(0xFF06201D),
            secondaryContainer = Color(0xFF232B33),
            onSecondaryContainer = Color(0xFFE3E8EC),
            background = bgDark,
            onBackground = Color(0xFFE9ECEF),
            surface = Color(0xFF161B20),
            onSurface = Color(0xFFE9ECEF),
            surfaceVariant = Color(0xFF1F262D),
            onSurfaceVariant = Color(0xFFA9B2BB),
            outline = Color(0xFF3A444E),
            outlineVariant = Color(0xFF2A323A),
        )
    } else {
        val prim = acc.light
        lightColorScheme(
            primary = prim,
            onPrimary = Color.White,
            primaryContainer = lerp(Color.White, prim, 0.14f),
            onPrimaryContainer = lerp(Color.Black, prim, 0.7f),
            secondary = lerp(prim, Color.Black, 0.2f),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8ECF0),
            onSecondaryContainer = Color(0xFF1B2127),
            background = Color(0xFFF5F6F8),
            onBackground = Color(0xFF14181C),
            surface = Color.White,
            onSurface = Color(0xFF14181C),
            surfaceVariant = Color(0xFFEAEEF2),
            onSurfaceVariant = Color(0xFF55606B),
            outline = Color(0xFFB4BCC4),
            outlineVariant = Color(0xFFDCE1E6),
        )
    }
    val main = if (dark) acc.light else acc.light
    val extra = Extra(
        dark = dark,
        heading = if (dark) Color(0xFF4DA3FF) else Color(0xFF1565C0),
        verseNum = if (dark) Color(0xFFFFB300) else Color(0xFFB26A00),
        card = if (dark) Color(0xFF161B20) else Color.White,
        muted = if (dark) Color(0xFF8E98A3) else Color(0xFF6B7682),
        heroA = main,
        heroB = lerp(main, Color(0xFF070B12), 0.72f),
        hairline = if (dark) Color(0xFF262D34) else Color(0xFFE2E6EA),
    )
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography) {
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}
