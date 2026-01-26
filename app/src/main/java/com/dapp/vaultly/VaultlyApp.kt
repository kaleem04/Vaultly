package com.dapp.vaultly

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.dapp.vaultly.data.model.WalletUiState
import com.dapp.vaultly.ui.screens.AddPasswordBottomSheetContent
import com.dapp.vaultly.ui.screens.DashboardScreen
import com.dapp.vaultly.ui.screens.LockScreen
import com.dapp.vaultly.ui.screens.ProfileScreen
import com.dapp.vaultly.ui.screens.SplashScreen
import com.dapp.vaultly.ui.screens.WelcomeScreen
import com.dapp.vaultly.ui.viewmodels.AddPasswordViewmodel
import com.dapp.vaultly.ui.viewmodels.AuthViewmodel
import com.dapp.vaultly.ui.viewmodels.DashboardViewmodel
import com.dapp.vaultly.ui.viewmodels.LockUiState
import com.dapp.vaultly.ui.viewmodels.LockViewModel
import com.dapp.vaultly.ui.viewmodels.VaultlyThemeViewmodel
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.client.models.request.SentRequestResult
import com.reown.appkit.ui.components.internal.AppKitComponent
import kotlinx.coroutines.launch
import org.json.JSONArray

val noBottomNavRoutes = listOf(
    VaultlyRoutes.WELCOMESCREEN.name,
    VaultlyRoutes.VAULTLYBOTTOMSHEET.name,
    VaultlyRoutes.SPLASHSCREEN.name
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialNavigationApi::class)
@Composable
fun VaultlyApp(
    authViewmodel: AuthViewmodel,
    vaultlyThemeViewmodel: VaultlyThemeViewmodel
) {
    val addPasswordViewmodel: AddPasswordViewmodel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    // Pass `skipPartiallyExpanded = true` to prevent it getting stuck in a middle state.
    val modalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val dashboardViewmodel: DashboardViewmodel = hiltViewModel()
    val lockViewModel: LockViewModel = hiltViewModel()
    var onSearchClick by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val authState by authViewmodel.uiState.collectAsStateWithLifecycle()
    // Observe lock state early so navigation composables can decide whether to render secured content
    val lockState by lockViewModel.uiState.collectAsStateWithLifecycle()

    // Landscape detection
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // TopAppBar scroll behavior for auto-hide on scroll
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Read the state from the ViewModel. This will trigger recomposition when it changes.
    val openAddPasswordSheet = addPasswordViewmodel.openSheet
    val shouldShowBars by remember(currentRoute) {
        derivedStateOf {
            noBottomNavRoutes.none { route ->
                currentRoute?.startsWith(route) == true
            }
        }
    }
    // *** FIX 2: Add the navigation logic to move away from the splash screen ***
    LaunchedEffect(authState) {
        // When the ViewModel determines the auth state, navigate accordingly.
        when (authState) {
            WalletUiState.Welcome -> {
                navController.navigate(VaultlyRoutes.WELCOMESCREEN.name) {
                    // Clear the splash screen from the back stack
                    popUpTo(VaultlyRoutes.SPLASHSCREEN.name) { inclusive = true }
                }
            }
            is WalletUiState.DashboardReady, is WalletUiState.DashboardPendingSignature -> {
                navController.navigate(VaultlyRoutes.DASHBOARDSCREEN.name) {
                    // Clear the splash screen from the back stack
                    popUpTo(VaultlyRoutes.SPLASHSCREEN.name) { inclusive = true }
                }
            }
            // Do nothing while it's Idle (still loading), keeping the user on the splash screen
            WalletUiState.Idle -> {}
            WalletUiState.Loading -> {}
        }
    }


    // This is the unified and simplified dismissal logic.
    val dismissSheet: () -> Unit = {
        coroutineScope.launch {
            modalSheetState.hide()
        }.invokeOnCompletion {
            // Important: Only update the ViewModel's state AFTER the sheet is no longer visible.
            if (!modalSheetState.isVisible) {
                // Assuming you have this function in your ViewModel
                // to set openSheet = false
                addPasswordViewmodel.onSheetDismiss()
            }
        }
    }

    // Navigation callbacks
    val onHomeClick = { navController.navigate(VaultlyRoutes.DASHBOARDSCREEN.name) }
    val onProfileClick = { navController.navigate(VaultlyRoutes.PROFILESCREEN.name) }

    // Use Row layout for landscape with NavigationRail
    if (isLandscape && shouldShowBars) {
        Row(modifier = Modifier.fillMaxSize()) {
            // NavigationRail on the left in landscape
            VaultlyNavigationRail(
                selectedItem = currentRoute ?: "",
                onHomeClick = onHomeClick,
                onProfileClick = onProfileClick
            )

            // Scaffold without bottom bar in landscape
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    VaultlyTopAppBar(
                        onSearchClick = { onSearchClick = !onSearchClick },
                        onAddClick = { addPasswordViewmodel.prepareForNewCredential() },
                        isSearchActive = onSearchClick,
                        scrollBehavior = scrollBehavior,
                        showAddButton = currentRoute != VaultlyRoutes.PROFILESCREEN.name
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                VaultlyNavHost(
                    navController = navController,
                    authState = authState,
                    lockState = lockState,
                    addPasswordViewmodel = addPasswordViewmodel,
                    dashboardViewmodel = dashboardViewmodel,
                    vaultlyThemeViewmodel = vaultlyThemeViewmodel,
                    authViewmodel = authViewmodel,
                    onSearchClick = onSearchClick,
                    snackbarHostState = snackbarHostState,
                    paddingValues = innerPadding,
                    isLandscape = true
                )
            }
        }
    } else {
        // Portrait mode with bottom navigation
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (shouldShowBars) {
                    VaultlyTopAppBar(
                        onSearchClick = { onSearchClick = !onSearchClick },
                        onAddClick = { addPasswordViewmodel.prepareForNewCredential() },
                        isSearchActive = onSearchClick,
                        scrollBehavior = scrollBehavior,
                        showAddButton = currentRoute != VaultlyRoutes.PROFILESCREEN.name
                    )
                }
            },
            bottomBar = {
                if (shouldShowBars) {
                    VaultlyBottomAppBar(
                        selectedItem = currentRoute ?: "",
                        onHomeClick = onHomeClick,
                        onProfileClick = onProfileClick
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            VaultlyNavHost(
                navController = navController,
                authState = authState,
                lockState = lockState,
                addPasswordViewmodel = addPasswordViewmodel,
                dashboardViewmodel = dashboardViewmodel,
                vaultlyThemeViewmodel = vaultlyThemeViewmodel,
                authViewmodel = authViewmodel,
                onSearchClick = onSearchClick,
                snackbarHostState = snackbarHostState,
                paddingValues = innerPadding,
                isLandscape = false
            )
        }
    }

    // *** THE MAIN FIX IS HERE ***
    // We display the ModalBottomSheet conditionally.
    // Compose handles showing/hiding it correctly based on whether it's in the composition.
    if (openAddPasswordSheet) {
        ModalBottomSheet(
            onDismissRequest = dismissSheet, // This is called when swiping down or pressing back.
            sheetState = modalSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            AddPasswordBottomSheetContent(
                onDismiss = dismissSheet, // This is for your internal buttons (e.g., "Save").
                dashboardViewmodel = dashboardViewmodel,
                viewModel = addPasswordViewmodel
            )
        }
    }

    // Lock overlay: show LockScreen when authState is DashboardReady and lockViewModel requires unlock
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    if (activity != null && (authState is WalletUiState.DashboardReady) && lockState !is LockUiState.Unlocked) {
        LockScreen(activity = activity, onUnlocked = { /* no-op, LockViewModel handles state */ })
    }
}

/**
 * Extracted NavHost composable for reuse in both portrait and landscape layouts
 */
@Composable
private fun VaultlyNavHost(
    navController: androidx.navigation.NavHostController,
    authState: WalletUiState,
    lockState: LockUiState,
    addPasswordViewmodel: AddPasswordViewmodel,
    dashboardViewmodel: DashboardViewmodel,
    vaultlyThemeViewmodel: VaultlyThemeViewmodel,
    authViewmodel: AuthViewmodel,
    onSearchClick: Boolean,
    snackbarHostState: SnackbarHostState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    isLandscape: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = VaultlyRoutes.SPLASHSCREEN.name
    ) {
        composable(VaultlyRoutes.WELCOMESCREEN.name) {
            WelcomeScreen {
                navController.navigate(VaultlyRoutes.VAULTLYBOTTOMSHEET.name)
            }
        }

        composable(VaultlyRoutes.SPLASHSCREEN.name) {
            SplashScreen()
        }
        composable(VaultlyRoutes.VAULTLYBOTTOMSHEET.name) {
            VaultlyBottomSheet(onDismiss = { navController.popBackStack() })
        }
        composable(VaultlyRoutes.DASHBOARDSCREEN.name) {
            if (authState is WalletUiState.DashboardPendingSignature) {
                SignatureDialog(
                    address = AppKit.getAccount()?.address ?: "",
                    onConfirm = { requestPersonalSign(AppKit.getAccount()?.address ?: "") }
                )
            } else {
                // Only render the dashboard when unlocked. When locked, render an empty placeholder
                if (lockState is LockUiState.Unlocked) {
                    DashboardScreen(
                        addPasswordViewmodel = addPasswordViewmodel,
                        search = onSearchClick,
                        contentPaddingValues = paddingValues,
                        dashboardViewmodel = dashboardViewmodel,
                        snackbarHostState = snackbarHostState,
                        isLandscape = isLandscape
                    )
                } else {
                    // Placeholder while locked (keeps content from rendering)
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) { }
                }
            }
        }
        composable(route = VaultlyRoutes.PROFILESCREEN.name) {
            ProfileScreen(
                vaultlyThemeViewmodel = vaultlyThemeViewmodel,
                walletAddress = AppKit.getAccount()?.address ?: "",
                userName = "",
                onThemeClick = {},
                onLogoutClick = {
                    AppKit.disconnect(
                        onSuccess = { Log.d("@@", "Logout SuccessFull") },
                        onError = { Log.d("@@", "Logout Failed") }
                    )
                    authViewmodel.onLogout()
                },
                contentPaddingValues = paddingValues
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
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        AppKitComponent(true) {
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
    var active by rememberSaveable { mutableStateOf(false) }
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
        colors = colors,
        tonalElevation = SearchBarDefaults.TonalElevation,
        shadowElevation = SearchBarDefaults.ShadowElevation,
    ) {
        // Empty content - no recent searches
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultlyTopAppBar(
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    isSearchActive: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showAddButton: Boolean = true
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
            if (showAddButton) {
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Password")
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

/**
 * NavigationRail for landscape mode - displays on the left side
 */
@Composable
fun VaultlyNavigationRail(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationRail {
        NavigationRailItem(
            selected = selectedItem == VaultlyRoutes.DASHBOARDSCREEN.name,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationRailItem(
            selected = selectedItem == VaultlyRoutes.PROFILESCREEN.name,
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

@Composable
fun VaultlyBottomAppBar(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == VaultlyRoutes.DASHBOARDSCREEN.name,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedItem == VaultlyRoutes.PROFILESCREEN.name,
            onClick = onProfileClick,
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
