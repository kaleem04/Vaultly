package com.dapp.vaultly.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.VaultDockedSearchBar
import com.dapp.vaultly.data.model.CredentialEntity
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import java.util.Date


@Composable
fun DashboardScreen(
    dashboardViewmodel: DashboardViewmodel = hiltViewModel(),
    onItemClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    search: Boolean = false,
    onRequestSignature: () -> Unit = {},
    contentPaddingValues: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val credentials by dashboardViewmodel.credentials.collectAsStateWithLifecycle()
    val selected by dashboardViewmodel.selectedCredential.collectAsState()
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
        // 🔹 Vault items (grid like LastPass)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(credentials) { item ->
                VaultCard(
                    item,
                    onClick = onItemClick,
                    onDeleteClick = {}
                )
            }
//                    item {
//                        Button(
//                            onClick = onLogoutClick
//                        ) {
//                            Text(text = "Logout")
//                        }
//                    }
//                    item {
//                        Button(
//                            onClick = onRequestSignature
//                        ) {
//                            Text(text = "Logout")
//                        }
//                    }
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
    item: CredentialEntity,
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
                Text(item.website, style = MaterialTheme.typography.titleMedium)
                Text(
                    Date(item.createdAt).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
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


data class VaultItem(
    val title: String,
    val userName: String = "",
    val password: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

val sampleVaultItems = listOf(
    VaultItem("Gmail sgwrgergege", "user@gmail.comegrgegegegesgeg"),
    VaultItem("Facebookgregeg", "user123"),
    VaultItem("Bank", "****1234"),
    VaultItem("Work Email", "work@company.com"),
    VaultItem("Github", "devUser"),
    VaultItem("Netflix", "user@mail.com"),
    VaultItem("Bank", "****1234"),
    VaultItem("Work Email", "work@company.com"),
    VaultItem("Github", "devUser"),
    VaultItem("Netflix", "user@mail.com"),
)

