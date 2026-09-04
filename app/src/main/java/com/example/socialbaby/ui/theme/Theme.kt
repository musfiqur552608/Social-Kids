package com.example.socialbaby.ui.theme

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

private val SocialKidsLightScheme = lightColorScheme(
    primary = KidYellowDark,
    onPrimary = KidTextOnYellow,
    primaryContainer = KidYellow,
    onPrimaryContainer = KidTextDark,
    secondary = KidCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF57160D),
    tertiary = KidSkyDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC2E8FF),
    onTertiaryContainer = Color(0xFF001E2F),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = KidError,
    onError = Color.White,
    errorContainer = KidErrorContainerLight,
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000)
)

private val SocialKidsDarkScheme = darkColorScheme(
    primary = KidYellow,
    onPrimary = KidTextDark,
    primaryContainer = KidYellowDark,
    onPrimaryContainer = KidTextDark,
    secondary = KidCoral,
    onSecondary = Color(0xFF57160D),
    secondaryContainer = Color(0xFF7A2A1F),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = KidSky,
    onTertiary = Color(0xFF003549),
    tertiaryContainer = Color(0xFF004D6A),
    onTertiaryContainer = Color(0xFFC2E8FF),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = KidErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000)
)

@Composable
fun SocialBabyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // system auto default
    dynamicColor: Boolean = false, // keep brand, not dynamic, but allow opt-in
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SocialKidsDarkScheme
        else -> SocialKidsLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KidTypography,
        shapes = KidShapes,
        content = content
    )
}

// Alias for spec compliance
@Composable
fun KidTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SocialBabyTheme(darkTheme, dynamicColor, content)
