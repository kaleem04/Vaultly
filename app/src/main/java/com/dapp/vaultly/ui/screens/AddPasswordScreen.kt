package com.dapp.vaultly.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    viewModel: AddPasswordViewmodel = viewModel(),
    onSaveClick: (String, String, String) -> Unit
) {
    val uiState = viewModel.uiState.value

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

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onSaveClick(uiState.website, uiState.username, uiState.password)
                    viewModel.clearFields()
                    onDismiss()
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AddPasswordBottomSheetPreview() {
    val sheetState = rememberModalBottomSheetState()
    AddPasswordBottomSheet(
        sheetState = sheetState,
        onDismiss = {},
        onSaveClick = { _, _, _ -> }
    )
}


