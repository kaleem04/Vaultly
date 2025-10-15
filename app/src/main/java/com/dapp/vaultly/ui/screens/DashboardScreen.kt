package com.dapp.vaultly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.VaultDockedSearchBar
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.reown.appkit.client.AppKit
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    dashboardViewmodel: DashboardViewmodel,
    addPasswordViewmodel: AddPasswordViewmodel,
    search: Boolean = false,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    snackbarHostState: SnackbarHostState, // Accept SnackbarHostState from the root composable
    modifier: Modifier = Modifier,
) {
    // 1. COLLECT THE SINGLE, CONSOLIDATED STATE
    val uiState by dashboardViewmodel.uiState.collectAsStateWithLifecycle()

    // UI-specific state remains here
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // 2. TRIGGER INITIAL DATA LOAD (runs only once)
    LaunchedEffect(Unit) {
        dashboardViewmodel.onScreenReady()
    }

    // 3. SHOW SNACKBAR MESSAGES (runs only when uiState.userMessage changes)
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                // Notify the ViewModel that the message has been shown
                dashboardViewmodel.userMessageShown()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPaddingValues)
    ) {
        if (search) {
            VaultDockedSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { /* TODO: Implement search filtering */ }
            )
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("Passwords", "Notes", "Cards", "IDs")) { category ->
                FilterChip(category, onClick = { /* TODO: Implement category filtering */ })
            }
        }

        // 4. MAIN CONTENT AREA: A Box allows layering content (list, empty screen, loader)
        Box(modifier = Modifier.weight(1f)) {

            // The credential list is always present if the list is not empty
            if (uiState.credentials.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.credentials, key = { it.website }) { credential ->
                        VaultCard(
                            credential = credential,
                            onClick = {
                                addPasswordViewmodel.selectCredential(credential)
                            },
                            onDeleteClick = {
                                val userId = AppKit.getAccount()?.address
                                if (userId != null) {
                                    dashboardViewmodel.deleteCredential(userId, credential.website)
                                }
                            }
                        )
                    }
                }
            }

            // Show empty screen only when not loading and the list is truly empty
            if (!uiState.isLoading && uiState.credentials.isEmpty()) {
                EmptyVaultScreen()
            }

            // Show a full-screen loader when performing a blocking action
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // 5. FOOTER: The "Sync to Blockchain" button at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { dashboardViewmodel.addCidToPolygon() },
                // Disable the button during a sync operation to prevent multiple clicks
                enabled = !uiState.isSyncing
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                }
                Text("Sync to Blockchain")
            }
        }
    }
}


@Composable
fun FilterChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun VaultCard(
    credential: Credential,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically // Center items vertically in the row
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f) // Give column all available space
            ) {
                Text(credential.website, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodySmall,
                )
                // Removed the hardcoded date, assuming it's not in the model yet
                // To re-add it, make sure `updatedAt` is part of your Credential model
            }

            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${credential.website}" // Improved accessibility
                )
            }
        }
    }
}

@Composable
fun EmptyVaultScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AddCircle,
            contentDescription = "Empty Vault",
            modifier = Modifier.size(128.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your Vault is Empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Add your first password to keep it secure.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
