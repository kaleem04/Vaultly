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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.R
import com.dapp.vaultly.VaultDockedSearchBar
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialType
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.dapp.vaultly.util.ClipboardUtil
import com.reown.appkit.client.AppKit
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    dashboardViewmodel: DashboardViewmodel,
    addPasswordViewmodel: AddPasswordViewmodel,
    search: Boolean = false,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
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

    // Update search query in ViewModel when local query changes
    LaunchedEffect(query) {
        dashboardViewmodel.updateSearchQuery(query)
    }

    // Use LazyColumn as the main container with proper content padding
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPaddingValues,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar item
        if (search) {
            item {
                VaultDockedSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { /* Search is handled reactively via ViewModel */ }
                )
            }
        }

        // Filter chips - Only Passwords and Notes
        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    VaultlyFilterChip(
                        label = "All",
                        selected = uiState.activeTab == null,
                        onClick = { dashboardViewmodel.setActiveTab(null) }
                    )
                }
                item {
                    VaultlyFilterChip(
                        label = "Passwords",
                        selected = uiState.activeTab == CredentialType.PASSWORD,
                        onClick = { dashboardViewmodel.setActiveTab(CredentialType.PASSWORD) }
                    )
                }
                item {
                    VaultlyFilterChip(
                        label = "Notes",
                        selected = uiState.activeTab == CredentialType.NOTE,
                        onClick = { dashboardViewmodel.setActiveTab(CredentialType.NOTE) }
                    )
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Credential list - use filteredCredentials
        if (uiState.filteredCredentials.isNotEmpty()) {
            items(
                items = uiState.filteredCredentials,
                key = { "${it.website}_${it.username}" }
            ) { credential ->
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
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Empty state
        if (!uiState.isLoading && uiState.filteredCredentials.isEmpty()) {
            item {
                EmptyVaultScreen(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(300.dp)
                )
            }
        }

        // Footer: "Sync to Blockchain" button - hide in landscape to save space
        if (!isLandscape) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { dashboardViewmodel.addCidToPolygon() },
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

/**
 * Material 3 FilterChip for category selection
 */
@Composable
fun VaultlyFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Legacy FilterChip - keeping for backwards compatibility
 */
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

/**
 * Material 3 styled VaultCard with leading icon, copy button, and type-aware rendering
 */
@Composable
fun VaultCard(
    credential: Credential,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNote = credential.type == CredentialType.NOTE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon - Favicon placeholder or type icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isNote) {
                            Icon(
                                imageVector = Icons.Filled.Create,
                                contentDescription = "Note",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            // Show first letter of website as favicon placeholder
                            Text(
                                text = credential.website.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content Column - Headline and Supporting text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Headline - Website/Title
                Text(
                    text = credential.website,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Supporting text - Username or Note preview
                Text(
                    text = if (isNote) {
                        credential.note.take(50).let {
                            if (credential.note.length > 50) "$it..." else it
                        }
                    } else {
                        credential.username
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing actions - Copy and Delete
            if (!isNote) {
                // Copy password button (only for passwords)
                IconButton(
                    onClick = {
                        ClipboardUtil.copyPassword(context, credential.password)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy),
                        contentDescription = "Copy password",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${credential.website}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
