package com.dapp.vaultly.ui.screens

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.ui.viewmodels.LockUiState
import com.dapp.vaultly.ui.viewmodels.LockViewModel

@Composable
fun LockScreen(
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.lockIfNeeded()
        if (viewModel.isLockEnabled()) {
            viewModel.startAuth(activity)
        }
    }

    // If already unlocked, trigger the callback and return early so the overlay is not shown.
    val state = uiState.value
    if (state is LockUiState.Unlocked) {
        LaunchedEffect(state) {
            onUnlocked()
        }
        return
    }

    // Full-screen opaque overlay that hides the dashboard content until unlocked
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Consume all pointer events (clicks) so taps don't pass through to the underlying UI
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume click */ }
            // Use surface/onSurface to ensure readable contrast in both light/dark themes
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val textColor = MaterialTheme.colorScheme.onSurface
        when (state) {
            LockUiState.PromptBiometric -> {
                // Don't show text during biometric prompt - the system dialog is showing
                // Just show the locked icon or app logo
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.displayLarge
                )
            }
            LockUiState.Locked -> {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Tap to unlock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(onClick = { viewModel.startAuth(activity) }, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Unlock")
                }
            }
            is LockUiState.Error -> {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = "Error: ${state.message}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> {
                // No-op for other states (including Unlocked which is returned earlier)
            }
        }
    }
}
