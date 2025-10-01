package com.dapp.vaultly.data.model

enum class VaultlyTheme(val displayName: String) {
    DARK_THEME("Dark"),
    LIGHT_THEME("Light"),
    SYSTEM_DEFAULT("Follow System")
}
data class AppThemeState(
    val baseThemeOption: VaultlyTheme = VaultlyTheme.SYSTEM_DEFAULT,
    val isDynamicColorEnabled: Boolean = true // Default to dynamic color enabled
)