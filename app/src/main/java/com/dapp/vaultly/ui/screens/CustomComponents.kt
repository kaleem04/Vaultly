package com.dapp.vaultly.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


sealed class ButtonState {
    object Idle : ButtonState()
    object Loading : ButtonState()
    object Success : ButtonState()
    data class Failed(val message: String? = null) : ButtonState()
}

@Composable
fun CustomButton(
    state: ButtonState,
    onClick: () -> Unit,
    idleText: String = "Submit",
    modifier: Modifier = Modifier,

) {
    Button(
        onClick = { if (state is ButtonState.Idle) onClick() },
        enabled = state is ButtonState.Idle,
        modifier = modifier
    ) {
        Crossfade(targetState = state, label = "button-state") { target ->
            when (target) {
                is ButtonState.Idle -> {
                    Text(idleText)
                }

                is ButtonState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }

                is ButtonState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Password Added Successfully", color = Color.Green)
                    }
                }

                is ButtonState.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Failed",
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(target.message ?: "Failed", color = Color.Red)
                    }
                }
            }
        }
    }
}