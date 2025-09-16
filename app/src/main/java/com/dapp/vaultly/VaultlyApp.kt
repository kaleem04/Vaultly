package com.dapp.vaultly

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.dapp.vaultly.ui.screens.WelcomeScreen
import com.dapp.vaultly.util.NavigationEvent
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.reown.appkit.ui.components.internal.AppKitComponent
import kotlinx.coroutines.launch


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialNavigationApi::class)
@Preview(showBackground = true)
@Composable
fun VaultlyApp() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var openNetworks by rememberSaveable { mutableStateOf(false) }
    val navController = rememberNavController()
    Scaffold(

    ) {
        // Collect events from AppEventBus
        LaunchedEffect(Unit) {
            NavigationEvent.navigationEvents.collect { route ->
                if (route == VaultlyRoutes.DASHBOARDSCREEN.name) {
                    navController.navigate( VaultlyRoutes.DASHBOARDSCREEN.name) {
                        popUpTo(VaultlyRoutes.WELCOMESCREEN.name) { inclusive = true }
                    }
                }
            }
        }
        NavHost(
            navController = navController,
            startDestination = VaultlyRoutes.WELCOMESCREEN.name

        ) {
            composable(VaultlyRoutes.WELCOMESCREEN.name) {
                WelcomeScreen {
                    navController.navigate(VaultlyRoutes.VAULTLYBOTTOMSHEET.name)
                }
            }
            composable(VaultlyRoutes.VAULTLYBOTTOMSHEET.name) {
                VaultlyBottomSheet(
                    onDismiss = {
                       navController.popBackStack()
                    },

                )
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultlyBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        AppKitComponent(
            false,

        ){
            onDismiss()
        }
    }

}