package com.dapp.vaultly.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.VaultDockedSearchBar
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.UiState
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.dapp.vaultly.util.Constants
import com.reown.appkit.client.AppKit


@Composable
fun DashboardScreen(
    dashboardViewmodel: DashboardViewmodel,
    addPasswordViewmodel: AddPasswordViewmodel,
    onLogoutClick: () -> Unit = {},
    search: Boolean = false,
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val uiState by dashboardViewmodel.credentials.collectAsStateWithLifecycle()
    val blockchainStatus by dashboardViewmodel.blockchain.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPaddingValues)

    ) {
        if (search) {
            VaultDockedSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { /* filter Vault items */ }
            )
        }
        // 🔹 Categories
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(listOf("Passwords", "Notes", "Cards", "IDs")) { category ->
                FilterChip(category, onClick = { /* filter */ })
            }
        }

        when (uiState) {
            is UiState.Idle -> {
                EmptyVaultScreen()
            }

            is UiState.Loading -> {
                CircularProgressIndicator()
            }

            is UiState.Success -> {
                val credentials = (uiState as UiState.Success<List<Credential>>).data
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(credentials) { item ->
                        VaultCard(
                            item,
                            onClick = {

                                addPasswordViewmodel.selectCredential(item.id)
                                addPasswordViewmodel.openSheet = true
                                addPasswordViewmodel.editingCredential = item
                                      },
                            onDeleteClick = {
                                dashboardViewmodel.deleteCredential(
                                    AppKit.getAccount()?.address ?: "",
                                    item.website
                                )
                            }
                        )
                    }
                    item {
                        when (blockchainStatus) {
                            is UiState.Idle -> {}
                            is UiState.Loading -> {
                                //   CircularProgressIndicator()
                            }

                            is UiState.Success -> {
                                val blockchainStatus =
                                    (blockchainStatus as UiState.Success<String>).data

                                Text(text = blockchainStatus)
                            }

                            is UiState.Error -> {
                                val error = (blockchainStatus as UiState.Error).message
                                Text(text = error)
                            }
                        }

                    }
                    item {
                        Button(
                            onClick = onLogoutClick
                        ) {
                            Text(text = "Logout")
                        }
                    }
//                    item {
//                        Button(
//                            onClick = onRequestSignature
//                        ) {
//                            Text(text = "Logout")
//                        }
//                    }
                }

            }

            is UiState.Error -> {}


        }
        // 🔹 Vault items (grid like LastPass)
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
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(0.8f)
            ) {
                Text(credential.website, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = Constants.formatDate(credential.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                )

            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.weight(0.2f)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null
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
            imageVector = Icons.Filled.AddCircle, // Or Icons.Filled.Add if you prefer a simpler one
            contentDescription = "Add new password", // For accessibility
            modifier = Modifier.size(128.dp), // Make the icon large and inviting
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
            text = "Tap the '+' button in The Top to add your first password and keep it secure.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp) // Give text some breathing room
        )

        Spacer(modifier = Modifier.height(32.dp)) // More space before
    }
}