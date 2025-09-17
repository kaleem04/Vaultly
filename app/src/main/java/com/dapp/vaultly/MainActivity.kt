package com.dapp.vaultly

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.dapp.vaultly.ui.theme.VaultlyTheme
import com.dapp.vaultly.util.NavigationEvent
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.presets.AppKitChainsPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var isSessionAlive = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connectionType = ConnectionType.AUTOMATIC
        val projectId =
            "2606e4f31e91b2483da4fbd60d14969f" // Get Project ID at https://dashboard.reown.com/
        val appMetaData = Core.Model.AppMetaData(
            name = "Vaultly",
            description = "Vaultly is a Decentralized Password Manager",
            url = "https://com.dapp.vaultly",
            icons = listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
            redirect = "kotlin-modal-wc://request"
        )

        CoreClient.initialize(
            projectId = projectId,
            connectionType = connectionType,
            application = application,
            metaData = appMetaData
        ) {

        }

        AppKit.initialize(
            init = Modal.Params.Init(CoreClient),
            onSuccess = {
                // Callback will be called if initialization is successful
            },
            onError = { error ->
                // Error will be thrown if there's an issue during initialization
            }
        )
        AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())
        AppKit.setDelegate(appKitModalDelegate)


        enableEdgeToEdge()
        setContent {
            VaultlyTheme {
                VaultlyApp(isSessionAlive)
            }
        }


    }

    val appKitModalDelegate = object : AppKit.ModalDelegate {
        override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
            // Triggered when receives the session approval from wallet
            CoroutineScope(Dispatchers.Default).launch {
                NavigationEvent.navigationEvents.emit(VaultlyRoutes.DASHBOARDSCREEN.name)
            }

            Log.d("@@", "onSessionApproved: $approvedSession")


        }

        override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) {
            // Triggered when receives the session rejection from wallet
        }

        override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {
            // Triggered when receives the session update from wallet
        }

        override fun onSessionExtend(session: Modal.Model.Session) {
            // Triggered when receives the session extend from wallet
        }

        @Deprecated("")
        override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) {
            // Triggered when the peer emits events that match the list of events agreed upon session settlement
        }

        override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
            // Triggered when receives the session delete from wallet
        }

        override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
            // Triggered when receives the session request response from wallet
        }

        override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) {
            // Triggered when a proposal becomes expired
        }

        override fun onRequestExpired(request: Modal.Model.ExpiredRequest) {
            // Triggered when a request becomes expired
        }

        override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {
            //Triggered whenever the connection state is changed
            isSessionAlive = state.isAvailable
            Log.d("@@", "Connection STATE CHANGED $state")
        }

        override fun onError(error: Modal.Model.Error) {
            // Triggered whenever there is an issue inside the SDK
        }
    }
}
