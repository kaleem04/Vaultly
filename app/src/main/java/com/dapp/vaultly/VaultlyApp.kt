package com.dapp.vaultly

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.dapp.vaultly.data.model.WalletUiState
import com.dapp.vaultly.ui.screens.AddPasswordBottomSheet
import com.dapp.vaultly.ui.screens.DashboardScreen
import com.dapp.vaultly.ui.screens.WelcomeScreen
import com.dapp.vaultly.ui.viewmodels.AuthViewModel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import com.reown.appkit.ui.components.internal.AppKitComponent
import org.json.JSONArray


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialNavigationApi::class)

@Composable
fun VaultlyApp(
    authViewmodel: AuthViewModel
) {

    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val dashboardViewmodel: DashboardViewmodel = hiltViewModel()
    var onSearchClick by rememberSaveable { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val authState by authViewmodel.uiState.collectAsStateWithLifecycle()
    val noBottomNavRoutes = listOf(
        VaultlyRoutes.WELCOMESCREEN.name,
        VaultlyRoutes.VAULTLYBOTTOMSHEET.name
    )
    val shouldShowBars = noBottomNavRoutes.none { route ->
        currentRoute?.startsWith(route) == true
    }

    Scaffold(
        topBar = {
            if (shouldShowBars) {
                VaultlyTopAppBar(
                    onSearchClick = {
                        onSearchClick = !onSearchClick
                    },
                    onAddClick = {
                        showSheet = true
                    },
                    onSettingsClick = {},
                    isSearchActive = onSearchClick

                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                VaultlyBottomAppBar(
                    selectedItem = "home",
                    onItemSelected = {}
                )
            }
        },
    ) { paddingValues ->
        // Collect events from AppEventBus
        Box {
            NavHost(
                navController = navController,
                startDestination = when (authState) {
                    WalletUiState.Idle -> VaultlyRoutes.WELCOMESCREEN.name
                    WalletUiState.Welcome -> VaultlyRoutes.WELCOMESCREEN.name
                    WalletUiState.DashboardReady -> VaultlyRoutes.DASHBOARDSCREEN.name
                    is WalletUiState.DashboardPendingSignature -> VaultlyRoutes.DASHBOARDSCREEN.name
                    else -> VaultlyRoutes.WELCOMESCREEN.name
                }

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
                composable(VaultlyRoutes.DASHBOARDSCREEN.name) {
                    if (authState is WalletUiState.DashboardPendingSignature) {

                        SignatureDialog(
                            address = AppKit.getAccount()?.address.toString(),
                            onConfirm = {
                                requestPersonalSign(AppKit.getAccount()?.address)
                            }
                        )
                    } else {
                        DashboardScreen(
                            onItemClick = {
                                Log.d("@@", "Item clicked: $it")
                            },
                            onLogoutClick = {

                                AppKit.disconnect(
                                    onSuccess = {
                                        Log.d("@@", "Logout SuccessFull")


                                    },
                                    onError = {
                                        Log.d("@@", "Logout Failed")
                                    }
                                )

                                //navController.navigate(VaultlyRoutes.WELCOMESCREEN.name)
                                authViewmodel.onLogout()

                            },
                            search = onSearchClick,
                            contentPaddingValues = paddingValues,
                            dashboardViewmodel = dashboardViewmodel
                        )
                    }
                }
            }
        }

        if (showSheet) {
            AddPasswordBottomSheet(
                sheetState = modalSheetState,
                onDismiss = {
                    showSheet = false
                    navController.navigate(VaultlyRoutes.DASHBOARDSCREEN.name) {
                        popUpTo(VaultlyRoutes.DASHBOARDSCREEN.name) {
                            inclusive = true
                        }
                    }
                },
                dashboardViewmodel = dashboardViewmodel
            )
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
            true,

            ) {
            onDismiss()
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDockedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }
    val colors = SearchBarDefaults.colors()

    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    onSearch(it)
                    active = false
                },
                expanded = active,
                onExpandedChange = { active = it },
                placeholder = { Text("Search Vault") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (active) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                active = false
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = colors.inputFieldColors,
            )
        },
        expanded = active,
        onExpandedChange = { active = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        //   shape = SearchBarDefaults.inputFieldShape,
        colors = colors,
        tonalElevation = SearchBarDefaults.TonalElevation,
        shadowElevation = SearchBarDefaults.ShadowElevation,
    ) {
        // Optional: dropdown suggestions
        Text(
            "Recent searches will go here",
            fontSize = 10.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultlyTopAppBar(
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isSearchActive: Boolean = false
) {
    TopAppBar(
        title = {
            Text(
                text = "Vaultly",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    if (!isSearchActive) Icons.Default.Search else Icons.Default.Close,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Password")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    )
}

@Composable
fun VaultlyBottomAppBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == "home",
            onClick = { onItemSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedItem == "favorites",
            onClick = { onItemSelected("favorites") },
            icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
            label = { Text("Favorites") }
        )
        NavigationBarItem(
            selected = selectedItem == "profile",
            onClick = { onItemSelected("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

@Composable
fun SignatureDialog(
    address: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Signature Required")
        },
        text = {
            Text(
                text = "To secure your Vaultly account, please sign a message with your wallet address:\n\n$address"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun requestPersonalSign(account: String?, message: String = "Vaultly unlock request") {

    val params = JSONArray()
        .put(message)   // data to sign
        .put(account)   // wallet address
        .toString()

    val request = Request(
        method = "personal_sign",
        params = params
    )

    AppKit.request(
        request,
        onSuccess = { result: SentRequestResult ->
            println("✅ Sign request sent to wallet")
        },
        onError = { error: Throwable ->
            println("❌ Error sending sign request: ${error.localizedMessage}")
        }
    )
}