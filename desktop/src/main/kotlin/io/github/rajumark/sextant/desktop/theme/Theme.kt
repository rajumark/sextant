package io.github.rajumark.sextant.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.rememberDynamicColorScheme

// Material 3 scheme generated around the Sextant accent (#B4532A, the terracotta from the
// project report). Every color in the app comes from these roles; no raw colors elsewhere.

/** The seed color the scheme is built from. The default is the Sextant terracotta. */
val DefaultSeed: Color = Color(0xFFB4532A)

/** Seeds the user can pick in Settings. Each one generates a full light and dark Material 3 scheme. */
val SeedPresets: List<Pair<String, Color>> =
    listOf(
        "Terracotta" to DefaultSeed,
        "Indigo" to Color(0xFF3F51B5),
        "Ocean" to Color(0xFF0277BD),
        "Teal" to Color(0xFF00897B),
        "Forest" to Color(0xFF2E7D32),
        "Amber" to Color(0xFFFF8F00),
        "Plum" to Color(0xFF8E24AA),
        "Slate" to Color(0xFF546E7A),
    )

enum class ThemeMode { System, Light, Dark }

/** Monospace for package names, hashes, JSON and manifest XML. */
val MonoFamily: FontFamily = FontFamily.Monospace

private val SextantShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

private fun sextantTypography(): Typography {
    val base = Typography()
    return base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    )
}

/** Code text used by viewers: body-small metrics in the mono family. */
@Composable
fun codeTextStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily, fontSize = 12.5.sp, lineHeight = 19.sp)

@Composable
fun SextantTheme(
    mode: ThemeMode = ThemeMode.System,
    seed: Color = DefaultSeed,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    // Every role (primary, surfaces, containers, error…) is generated from the seed, so one color changes the whole theme.
    val scheme = rememberDynamicColorScheme(seedColor = seed, isDark = dark)
    MaterialTheme(
        colorScheme = scheme,
        typography = sextantTypography(),
        shapes = SextantShapes,
        // Expressive motion: springs with overshoot for movement, no overshoot for color and opacity.
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
