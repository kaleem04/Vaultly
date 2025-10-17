package com.dapp.vaultly.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.dapp.vaultly.autofill.ui.AuthenticateBeforeAutofillActivity
import com.dapp.vaultly.data.model.AutofillCredential
import com.dapp.vaultly.data.repository.VaultlyAutofillRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VaultlyAutofillService : AutofillService() {
    @Inject
    lateinit var autofillRepository: VaultlyAutofillRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel the scope to avoid leaks
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        serviceScope.launch {
            try {
                val context = request.fillContexts
                val structure = context.last().structure
                val clientPackageName = context.last().structure.activityComponent?.packageName
                    ?: "unknown"

                Log.d("VaultlyAutofill", "Fill request from: $clientPackageName")

                // Parse the view structure to find autofill fields
                val fieldMap = parseViewStructure(structure)

                if (fieldMap.isEmpty()) {
                    Log.d("VaultlyAutofill", "No autofill fields found in structure")
                    callback.onSuccess(null)
                    return@launch
                }

                Log.d("VaultlyAutofill", "Found fields: ${fieldMap.keys}")

                // Get matching credentials from your vault
                val credentials = autofillRepository.getMatchingCredentials(clientPackageName)

                if (credentials.isEmpty()) {
                    Log.d("VaultlyAutofill", "No matching credentials found")
                    callback.onSuccess(null)
                    return@launch
                }

                Log.d("VaultlyAutofill", "Found ${credentials.size} matching credentials")

                // Build and send fill response with authentication gate
                val fillResponse = buildFillResponse(credentials, fieldMap)
                callback.onSuccess(fillResponse)

            } catch (e: Exception) {
                Log.e("VaultlyAutofill", "Error in onFillRequest", e)
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback
    ) {
        // Optional: Implement credential saving from forms
        // For now, just acknowledge
        Log.d("VaultlyAutofill", "Save request received")
        callback.onSuccess()
    }

    private fun parseViewStructure(structure: AssistStructure): Map<String, AutofillId> {
        val fieldMap = mutableMapOf<String, AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            val autofillId = node.autofillId
            val hints = node.autofillHints

            if (autofillId != null && !hints.isNullOrEmpty()) {
                for (hint in hints) {
                    val hintLower = hint.lowercase() // Use lowercase for comparison
                    if (hintLower.contains("username") && !fieldMap.containsKey("username")) {
                        fieldMap["username"] = autofillId
                    } else if (hintLower.contains("email") && !fieldMap.containsKey("email")) {
                        fieldMap["email"] = autofillId
                    } else if (hintLower.contains("password") && !fieldMap.containsKey("password")) {
                        fieldMap["password"] = autofillId
                    }
                }
            }

            // Traverse child nodes
            for (i in 0 until node.childCount) {
                node.getChildAt(i)?.let { traverse(it) } // Use safe call
            }
        }


        traverse(structure.getWindowNodeAt(0).rootViewNode)
        return fieldMap
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun buildFillResponse(
        credentials: List<AutofillCredential>,
        fieldMap: Map<String, AutofillId>
    ): android.service.autofill.FillResponse? {
        val responseBuilder = android.service.autofill.FillResponse.Builder()

        for (credential in credentials) {
            // 1. Create the presentation (what the user sees in the dropdown)
            val presentation = createPresentation(credential.username)

            // 2. Create an authentication IntentSender. This is crucial.
            // When the user taps the suggestion, the system fires this intent.
            // Your app (via an Activity) will receive it, authenticate the user
            // (e.g., biometrics), and then return the full dataset.
            val authIntent = Intent(this, AuthenticateBeforeAutofillActivity::class.java).apply {
                putExtra("CREDENTIAL_ID", credential.id) // Pass an ID to fetch the credential
                putExtra("FIELD_MAP", HashMap(fieldMap)) // Pass the field map
            }
            val authIntentSender = PendingIntent.getActivity(
                this,
                credential.id.hashCode(), // Unique request code
                authIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).intentSender


            // 3. Build a dataset that requires authentication.
            // DO NOT add sensitive data here.
            val datasetBuilder = android.service.autofill.Dataset.Builder(presentation)
                .setAuthentication(authIntentSender)

            // Associate the IDs to the dataset so the system knows which fields this dataset can fill.
            fieldMap["username"]?.let { datasetBuilder.setField(it, null) }
            fieldMap["email"]?.let { datasetBuilder.setField(it, null) }
            fieldMap["password"]?.let { datasetBuilder.setField(it, null) }


            responseBuilder.addDataset(datasetBuilder.build())
        }

        val response = responseBuilder.build()

        return response
    }


    private fun createPresentation(label: String): RemoteViews {
        val remoteViews = RemoteViews(
            packageName,
            android.R.layout.simple_list_item_1
        )
        remoteViews.setTextViewText(android.R.id.text1, label)
        return remoteViews
    }
}