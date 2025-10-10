//package com.dapp.vaultly.autofill
//
//// In app/src/main/java/com/dapp/vaultly/autofill/VaultlyAutofillService.kt
//
//import android.app.assist.AssistStructure
//import android.os.CancellationSignal
//import android.service.autofill.*
//import android.util.Log
//import android.view.autofill.AutofillId
//import android.view.autofill.AutofillValue
//import android.widget.RemoteViews
//import androidx.compose.foundation.gestures.forEach
//import androidx.compose.foundation.layout.size
//import androidx.compose.ui.geometry.isEmpty
//import androidx.compose.ui.semantics.password
//import com.dapp.vaultly.R // You'll need to create a layout file for this
//import com.dapp.vaultly.data.repository.CredentialRepository
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class VaultlyAutofillService : AutofillService() {
//
//    // Hilt injects the repository so the service can access your data.
//    // NOTE: Injected fields in Android components cannot be private.
//    @Inject
//    lateinit var credentialRepository: CredentialRepository
//
//    private val serviceJob = SupervisorJob()
//    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
//
//    override fun onFillRequest(
//        request: FillRequest,
//        cancellationSignal: CancellationSignal,
//        callback: FillCallback
//    ) {
//        // 1. Get the screen structure and the app's package name
//        val structure = request.fillContexts.lastOrNull()?.structure ?: return
//        val packageName = structure.activityComponent.packageName
//        Log.d("AutofillService", "Fill request received for package: $packageName")
//
//        // 2. Parse the screen to find where to put the username and password
//        val (usernameId, passwordId) = findUsernamePasswordIds(structure)
//        if (usernameId == null || passwordId == null) {
//            callback.onFailure("Could not find username or password fields.")
//            return
//        }
//
//        // 3. Fetch matching credentials from your database asynchronously
//        serviceScope.launch {
//            try {
//                val credentials = withContext(Dispatchers.IO) {
//                    credentialRepository.findCredentialsForPackage(packageName)
//                }
//
//                if (credentials.isEmpty()) {
//                    Log.d("AutofillService", "No credentials found for $packageName")
//                    callback.onSuccess(null) // Important: Send null to show no suggestions
//                    return@launch
//                }
//
//                Log.d("AutofillService", "Found ${credentials.size} credential(s) for $packageName")
//                val responseBuilder = FillResponse.Builder()
//
//                // 4. Create a suggestion (Dataset) for each credential found
//                credentials.forEach { credential ->
//                    val presentation = RemoteViews(packageName, R.layout.autofill_suggestion_item).apply {
//                        setTextViewText(R.id.suggestion_username, credential.username)
//                        setTextViewText(R.id.suggestion_website, credential.website)
//                    }
//
//                    val dataset = Dataset.Builder(presentation)
//                        .setValue(usernameId, AutofillValue.forText(credential.username))
//                        .setValue(passwordId, AutofillValue.forText(credential.password))
//                        .build()
//
//                    responseBuilder.addDataset(dataset)
//                }
//
//                // 5. Send the suggestions back to the Android system
//                callback.onSuccess(responseBuilder.build())
//
//            } catch (e: Exception) {
//                Log.e("AutofillService", "Error fetching credentials", e)
//                callback.onFailure("Failed to retrieve credentials.")
//            }
//        }
//    }
//
//    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
//        // This is called when the user enters a NEW password in an app.
//        // For now, we will do nothing. In the future, you'd launch a confirmation screen here.
//        callback.onSuccess()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceJob.cancel() // Clean up coroutines when service is destroyed
//    }
//
//    private fun findUsernamePasswordIds(structure: AssistStructure): Pair<AutofillId?, AutofillId?> {
//        var usernameId: AutofillId? = null
//        var passwordId: AutofillId? = null
//        val nodes = ArrayDeque<AssistStructure.ViewNode>()
//        nodes.add(structure.getWindowNodeAt(0).rootViewNode)
//
//        // Traverse all views on the screen to find input fields
//        while (nodes.isNotEmpty()) {
//            val node = nodes.removeFirst()
//            val hints = node.autofillHints
//            if (hints != null) {
//                for (hint in hints) {
//                    when (hint.lowercase()) {
//                        "username", "emailaddress" -> usernameId = node.autofillId
//                        "password" -> passwordId = node.autofillId
//                    }
//                }
//            }
//            // Fallback for apps that don't use hints
//            node.idEntry?.let {
//                if (it.contains("username", true) || it.contains("email", true)) {
//                    if (usernameId == null) usernameId = node.autofillId
//                }
//                if (it.contains("password", true)) {
//                    if (passwordId == null) passwordId = node.autofillId
//                }
//            }
//            for (i in 0 until node.childCount) {
//                nodes.add(node.getChildAt(i))
//            }
//        }
//        return Pair(usernameId, passwordId)
//    }
//}
