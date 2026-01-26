package com.dapp.vaultly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.data.model.VaultlyTheme
import com.dapp.vaultly.ui.viewmodels.VaultlyThemeViewmodel

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = OnGold,
    primaryContainer = GoldDark,
    onPrimaryContainer = OnDarkBrown,
    secondary = GoldLight,
    onSecondary = OnGold,
    secondaryContainer = DarkBrownLight,
    onSecondaryContainer = OnDarkBrown,
    tertiary = GoldLight,
    onTertiary = OnGold,
    background = DarkBrown,
    onBackground = OnDarkBrown,
    surface = DarkBrownSurface,
    onSurface = OnDarkBrown,
    surfaceVariant = DarkBrownLight,
    onSurfaceVariant = OnDarkBrown
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = Color.White,
    primaryContainer = GoldLight,
    onPrimaryContainer = DarkBrown,
    secondary = Gold,
    onSecondary = DarkBrown,
    secondaryContainer = GoldLight,
    onSecondaryContainer = DarkBrown,
    tertiary = GoldDark,
    onTertiary = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = DarkBrown,
    surface = Color(0xFFFFFBFE),
    onSurface = DarkBrown,
    surfaceVariant = Color(0xFFF5F0E8),
    onSurfaceVariant = DarkBrownLight
)

@Composable
fun VaultlyTheme(
    vaultlyThemeViewmodel: VaultlyThemeViewmodel,
    content: @Composable () -> Unit
) {
    val themeState by vaultlyThemeViewmodel.appThemeState.collectAsStateWithLifecycle()

    val useDarkTheme = when (themeState.baseThemeOption) {
        VaultlyTheme.DARK_THEME -> true
        VaultlyTheme.LIGHT_THEME -> false
        VaultlyTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeState.isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}