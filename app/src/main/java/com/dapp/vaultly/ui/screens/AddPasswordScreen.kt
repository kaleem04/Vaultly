package com.dapp.vaultly.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.R
import com.dapp.vaultly.R.drawable.visibility
import com.dapp.vaultly.R.drawable.visibility_off
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.CredentialType
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.reown.appkit.client.AppKit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordBottomSheetContent(
    onDismiss: () -> Unit,
    viewModel: AddPasswordViewmodel,
    dashboardViewmodel: DashboardViewmodel
) {
    // 1. Get the UI state from the correct ViewModel (AddPasswordViewmodel).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // We get the overall dashboard state to know if an operation is in progress.
    val dashboardState by dashboardViewmodel.uiState.collectAsStateWithLifecycle()

    val editingCredential = viewModel.editingCredential
    val isEditMode = editingCredential != null

    // 2. A LaunchedEffect to pre-fill the fields when in edit mode.
    // This runs only when the bottom sheet is first shown for editing.
    LaunchedEffect(editingCredential) {
        if (editingCredential != null) {
            viewModel.loadCredentialForEditing(editingCredential)
        } else {
            viewModel.clearFields()
        }
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()), // Make scrollable for landscape
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (isEditMode) "Edit ${if (uiState.type == CredentialType.PASSWORD) "Password" else "Note"}"
            else "Add New Item",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(16.dp))

        // Type selector (only show when creating new, not editing)
        if (!isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.type == CredentialType.PASSWORD,
                    onClick = { viewModel.onTypeChange(CredentialType.PASSWORD) },
                    label = { Text("Password") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = uiState.type == CredentialType.NOTE,
                    onClick = { viewModel.onTypeChange(CredentialType.NOTE) },
                    label = { Text("Note") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Website/Title field (always show)
        OutlinedTextField(
            value = uiState.website,
            onValueChange = viewModel::onWebsiteChange,
            label = { Text(if (uiState.type == CredentialType.NOTE) "Note Title" else "Website / App") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Show username and password fields only for PASSWORD type
        if (uiState.type == CredentialType.PASSWORD) {
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username / Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (uiState.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        val icon =
                            if (uiState.showPassword) painterResource(visibility_off) else painterResource(
                                visibility
                            )
                        Icon(painter = icon, contentDescription = "Toggle password visibility")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

        // Note field
        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::onNoteChange,
            label = { Text(if (uiState.type == CredentialType.NOTE) "Content" else "Note (Optional)") },
            placeholder = { Text(if (uiState.type == CredentialType.NOTE) "Enter your note..." else "Optional Note") },
            maxLines = if (uiState.type == CredentialType.NOTE) 10 else 5,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // 3. Simplified Save/Update button.
        Button(
            // Use the isLoading flag from the DashboardViewModel to show a loading state.
            enabled = !dashboardState.isLoading,
            onClick = {
                // Construct the credential object right here before sending.
                val newOrUpdatedCredential = Credential(
                    id = editingCredential?.id ?: 0, // Keep original ID for updates
                    website = uiState.website,
                    username = uiState.username,
                    password = uiState.password,
                    note = uiState.note,
                    type = uiState.type
                )

                if (viewModel.credentialsValidation()) {
                    val userId = AppKit.getAccount()?.address
                    if (userId != null) {
                        dashboardViewmodel.addOrUpdateCredential(
                            userId = userId,
                            credential = newOrUpdatedCredential
                        )
                    }
                    // The success of the operation will be handled by the DashboardScreen.
                    // We can just close the sheet.
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (dashboardState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditMode) "Update" else "Save")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                onDismiss() // Simply dismiss the sheet.
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
