package com.dapp.vaultly.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dapp.vaultly.R
import com.dapp.vaultly.data.model.VaultlyTheme
import com.dapp.vaultly.ui.viewmodels.AutofillSettingsViewModel
import com.dapp.vaultly.ui.viewmodels.VaultlyThemeViewmodel


@Composable
fun ProfileScreen(
    vaultlyThemeViewmodel: VaultlyThemeViewmodel,
    autofillSettingsViewModel: AutofillSettingsViewModel = hiltViewModel(),  // NEW
    walletAddress: String,
    userName: String? = null,
    onThemeClick: () -> Unit,
    onLogoutClick: () -> Unit,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPaddingValues)
            .padding(16.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar + User Info
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile avatar",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userName ?: walletAddress.take(6) + "..." + walletAddress.takeLast(4),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Wallet: $walletAddress",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Profile Actions Section ---
        SectionHeader(title = "Account")
        ProfileActionItem(
            title = "Logout",
            icon = Icons.Default.ExitToApp,
            onClick = onLogoutClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Settings Section ---
        SectionHeader(title = "Settings")
        ProfileActionItem(
            title = "Language",
            icon = Icons.Rounded.AccountCircle,
            onClick = onThemeClick
        )
        // NEW: Autofill Settings
        AutofillSettingsItem(viewModel = autofillSettingsViewModel)

        SectionHeader(title = "Theme")
        ThemeSettingsSection(viewModel = vaultlyThemeViewmodel)

    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

fun themeIcon(theme: VaultlyTheme) = when (theme) {
    VaultlyTheme.LIGHT_THEME -> R.drawable.outline_light_mode_24
    VaultlyTheme.DARK_THEME -> R.drawable.baseline_dark_mode_24
    VaultlyTheme.SYSTEM_DEFAULT -> R.drawable.outline_settings_brightness_24
    else -> R.drawable.outline_light_mode_24
}

@Composable
fun ThemeSettingsSection(viewModel: VaultlyThemeViewmodel) {
    val state by viewModel.appThemeState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ThemeDropdown(
            selectedTheme = state.baseThemeOption,
            onThemeSelected = { theme -> viewModel.setBaseThemeOption(theme) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Dynamic Colors Toggle (Android 12+) ---
        val isDynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dynamic Colors")
                if (!isDynamicAvailable) {
                    Text(
                        text = "Available on Android 12+",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = state.isDynamicColorEnabled && isDynamicAvailable,
                onCheckedChange = { enabled ->
                    if (isDynamicAvailable) {
                        viewModel.setDynamicColorEnabled(enabled)
                    }
                },
                enabled = isDynamicAvailable
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDropdown(
    selectedTheme: VaultlyTheme,
    onThemeSelected: (VaultlyTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // OPTIMIZATION: Use remember to cache the painter resource.
        // This avoids reloading the drawable on every recomposition, which can occur when
        // the dropdown menu's expanded state changes. The painter will only be re-loaded
        // if the selectedTheme (and thus its icon) changes.
        val iconRes = themeIcon(selectedTheme)
        val painter = painterResource(iconRes)

        OutlinedTextField(
            value = selectedTheme.displayName,
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                Icon(
                    painter = painter,
                    contentDescription = null
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Theme") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VaultlyTheme.entries.forEach { option ->
                // OPTIMIZATION: Similarly, cache the painter for each dropdown item.
                // This prevents reloading the icon for every item on each recomposition.
                // It improves the performance when the dropdown is opened.
                val itemIconRes = themeIcon(option)
                val itemPainter = painterResource(itemIconRes)
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = itemPainter,
                            contentDescription = null
                        )
                    },
                    text = { Text(option.displayName) },
                    onClick = {
                        onThemeSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun ProfileActionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AutofillSettingsItem(viewModel: AutofillSettingsViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isEnabled by viewModel.isAutofillEnabled.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openAutofillSettings(activity ?: context) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_light_mode_24),
                contentDescription = "Autofill",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Autofill Service",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (isEnabled) "Enabled" else "Not enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = {
                viewModel.openAutofillSettings(activity ?: context)
            }
        )
    }
}