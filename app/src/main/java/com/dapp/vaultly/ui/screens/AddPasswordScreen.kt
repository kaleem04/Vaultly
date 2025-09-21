package com.dapp.vaultly.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.model.UiState
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.reown.appkit.client.AppKit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    viewModel: AddPasswordViewmodel = hiltViewModel(),
    dashboardViewmodel : DashboardViewmodel
) {
    val uiState = viewModel.uiState.value
    val addPasswordUiState by dashboardViewmodel.addPasswordUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when(addPasswordUiState){
        is UiState.Idle -> {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Add New Password", style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.website,
                        onValueChange = viewModel::onWebsiteChange,
                        label = { Text("Website / App") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

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
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.showPassword) Icons.Default.Lock else Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChange,
                        label = { Text("Note") },
                        placeholder = {Text("Optional Note")},
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))
                    val walletAddress = AppKit.getAccount()?.address ?: "";
                    val credential = Credential(
                        website = uiState.website,
                        username = uiState.username,
                        password = uiState.password,
                        note = uiState.note
                    )
                    Button(
                        onClick = {
                            if (viewModel.credentialsValidation()) {
                                dashboardViewmodel.addOrUpdateCredential(
                                    userId = walletAddress,
                                    credential = credential
                                )
                                viewModel.clearFields()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.clearFields()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
        is UiState.Loading -> {
            CircularProgressIndicator()
        }
        is UiState.Success<Unit> -> {
            Text("Password added successfully!")

        }
        is UiState.Error -> {
            Text("Error:")
        }
    }

}

