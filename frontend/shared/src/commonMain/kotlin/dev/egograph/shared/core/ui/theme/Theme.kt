package dev.egograph.shared.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Zinc Palette（モノクロ）
val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

// Cyan Palette（ブランドカラー）
val Cyan50 = Color(0xFFECFEFF)
val Cyan100 = Color(0xFFCFFAFE)
val Cyan200 = Color(0xFFA5F3FC)
val Cyan300 = Color(0xFF67E8F9)
val Cyan400 = Color(0xFF22D3EE)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)
val Cyan700 = Color(0xFF0E7490)
val Cyan800 = Color(0xFF155E75)
val Cyan900 = Color(0xFF164E63)
val Cyan950 = Color(0xFF083344)

// === Error Colors ===
// Light Theme
private val ErrorRed = Color(0xFFBA1A1A)
private val OnErrorRed = Color(0xFFFFFFFF)
private val ErrorContainerRed = Color(0xFFFFDAD6)
private val OnErrorContainerRed = Color(0xFF410002)

// Dark Theme
private val ErrorRedDark = Color(0xFFFFB4AB)
private val OnErrorRedDark = Color(0xFF690005)
private val ErrorContainerRedDark = Color(0xFF93000A)
private val OnErrorContainerRedDark = ErrorContainerRed

private val LightColorScheme =
    lightColorScheme(
        // === Primary: Cyan ===
        primary = Cyan600,
        onPrimary = Color.White,
        primaryContainer = Cyan100,
        onPrimaryContainer = Cyan950,
        // === Secondary: Cyan ===
        secondary = Cyan500,
        onSecondary = Color.White,
        secondaryContainer = Cyan300,
        onSecondaryContainer = Cyan950,
        // === Tertiary: Zinc ===
        tertiary = Zinc700,
        onTertiary = Zinc50,
        tertiaryContainer = Zinc200,
        onTertiaryContainer = Zinc900,
        // === Error ===
        error = ErrorRed,
        onError = OnErrorRed,
        errorContainer = ErrorContainerRed,
        onErrorContainer = OnErrorContainerRed,
        // === Background / Surface ===
        background = Color.White,
        onBackground = Zinc900,
        surface = Zinc50,
        onSurface = Zinc900,
        surfaceVariant = Zinc100,
        onSurfaceVariant = Zinc700,
        // === Surface Container ===
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Zinc50,
        surfaceContainer = Zinc100,
        surfaceContainerHigh = Zinc200,
        surfaceContainerHighest = Zinc300,
        // === Outline ===
        outline = Zinc300,
        outlineVariant = Zinc200,
    )

private val DarkColorScheme =
    darkColorScheme(
        // === Primary: Cyan ===
        primary = Cyan500,
        onPrimary = Cyan50,
        primaryContainer = Cyan700,
        onPrimaryContainer = Cyan50,
        // === Secondary: Cyan ===
        secondary = Cyan400,
        onSecondary = Cyan950,
        secondaryContainer = Cyan600,
        onSecondaryContainer = Cyan100,
        // === Tertiary: Zinc ===
        tertiary = Zinc300,
        onTertiary = Zinc900,
        tertiaryContainer = Zinc700,
        onTertiaryContainer = Zinc50,
        // === Error ===
        error = ErrorRedDark,
        onError = OnErrorRedDark,
        errorContainer = ErrorContainerRedDark,
        onErrorContainer = OnErrorContainerRedDark,
        // === Background / Surface ===
        background = Zinc950,
        onBackground = Zinc50,
        surface = Zinc900,
        onSurface = Zinc50,
        surfaceVariant = Zinc800,
        onSurfaceVariant = Zinc300,
        // === Surface Container ===
        surfaceContainerLowest = Zinc800,
        surfaceContainerLow = Zinc900,
        surfaceContainer = Zinc900,
        surfaceContainerHigh = Zinc800,
        surfaceContainerHighest = Zinc700,
        // === Outline ===
        outline = Zinc700,
        outlineVariant = Zinc700,
    )

@Composable
fun EgoGraphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalEgoGraphDimens provides EgoGraphDimens(),
        LocalEgoGraphShapes provides EgoGraphShapes(),
        LocalEgoGraphExtendedColors provides EgoGraphExtendedColors(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
