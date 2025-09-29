package com.dapp.vaultly

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dapp.vaultly.ui.theme.VaultlyTheme
import com.dapp.vaultly.ui.viewmodels.AuthViewmodel
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val authViewmodel by viewModels<AuthViewmodel>()
    private val context = this
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
                Log.d("@@", "APPKIT INITIALIZED SUCCESSFULLY")
            },
            onError = { error ->
                // Error will be thrown if there's an issue during initialization
                Log.d("@@", "APPKIT INITIALIZED FAILED")
            }
        )
        val amoyChain = Modal.Model.Chain(
            chainName = "Polygon Amoy",
            chainNamespace = "eip155",             // always eip155 for EVM chains
            chainReference = "80002",              // Amoy chain ID
            requiredMethods = listOf(
                "eth_sendTransaction",
                "personal_sign",
                "eth_signTypedData"
            ),
            optionalMethods = listOf(
                "eth_call",
                "eth_getBalance"
            ),
            events = listOf("chainChanged", "accountsChanged"),
            token = Modal.Model.Token(
                name = "Polygon",
                symbol = "POL",                     // native token symbol
                decimal = 16
            ),
            rpcUrl = "https://rpc-amoy.polygon.technology",   // Amoy public RPC
            blockExplorerUrl = "https://www.oklink.com/amoy",  // Block explorer for Amoy
        )
        val chainList = listOf(
            amoyChain
        )
        AppKit.setChains(
            chainList
        )
        AppKit.setDelegate(vaultlyDelegate(authViewmodel))


        enableEdgeToEdge()
        setContent {
            VaultlyTheme {
                VaultlyApp(authViewmodel)

            }
        }


    }

    fun vaultlyDelegate(authViewModel: AuthViewmodel): AppKit.ModalDelegate {
        val appKitModalDelegate = object : AppKit.ModalDelegate {
            override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
                // Triggered when receives the session approval from wallet
                val walletAddress = AppKit.getAccount()?.address
                if (walletAddress != null) {
                    authViewModel.onWalletConnected(walletAddress)
                }

                //  Log.d("@@", "onSessionApproved: $selectedChain");
                Log.d("@@", "onSessionApproved: $walletAddress");
                Log.d("@@", "onSessionApproved: $approvedSession")


            }

            override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) {
                // Triggered when receives the session rejection from wallet
                Log.d("@@", "onSessionRejected: $rejectedSession")
            }

            override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {
                // Triggered when receives the session update from wallet
                Log.d("@@", "onSessionUpdate: $updatedSession")
            }

            override fun onSessionExtend(session: Modal.Model.Session) {
                // Triggered when receives the session extend from wallet
                Log.d("@@", "onSessionExtend: $session")
            }

            @Deprecated("")
            override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) {
                // Triggered when the peer emits events that match the list of events agreed upon session settlement
                Log.d("@@", "onSessionEvent: $sessionEvent")
            }

            override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
                // Triggered when receives the session delete from wallet
                Log.d("@@", "onSessionDeleted: $deletedSession")
            }

            override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
                // Triggered when receives the session request response from wallet
                Log.d("@@", "onSessionRequestResponse: $response")

                when (val result = response.result) {
                    is Modal.Model.JsonRpcResponse.JsonRpcResult -> {
                        val signature = result.result ?: return
                        Log.d("@@", "✅ Signature received: $signature")

                        // 🔑 Derive AES key
                        authViewmodel.onSignatureApproved(signature)

                        Log.d("@@", "AES key derived & stored in memory")
                    }

                    is Modal.Model.JsonRpcResponse.JsonRpcError -> {
                        Log.e("@@", "❌ Sign request failed: ${result.message}")
                    }
                }
                Log.d("@@", "onSessionRequestResponse: $response")
            }

            override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) {
                // Triggered when a proposal becomes expired
                Log.d("@@", "onProposalExpired: $proposal")
            }

            override fun onRequestExpired(request: Modal.Model.ExpiredRequest) {
                // Triggered when a request becomes expired
                Log.d("@@", "onRequestExtend: $request")
            }

            override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {
                //Triggered whenever the connection state is changed
                Log.d("@@", "Connection STATE CHANGED $state")
            }

            override fun onError(error: Modal.Model.Error) {
                // Triggered whenever there is an issue inside the SDK
                Log.d("@@", "onSdkError: ${error.throwable.message}")
            }

        }
        return appKitModalDelegate
    }


}
