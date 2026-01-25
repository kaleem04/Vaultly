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
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import com.dapp.vaultly.R
import com.dapp.vaultly.autofill.ui.AuthenticateBeforeAutofillActivity
import com.dapp.vaultly.data.model.Credential
import com.dapp.vaultly.data.repository.VaultlyAutofillRepository
import com.dapp.vaultly.data.repository.UserVaultRepository
import com.reown.appkit.client.AppKit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VaultlyAutofillService : AutofillService() {
    @Inject
    lateinit var autofillRepository: VaultlyAutofillRepository

    @Inject
    lateinit var vaultRepository: UserVaultRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

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

                Timber.d("Fill request from: %s", clientPackageName)

                val fieldMap = parseViewStructure(structure)

                if (fieldMap.isEmpty()) {
                    Timber.d("No autofill fields found in structure")
                    callback.onSuccess(null)
                    return@launch
                }

                // Create authentication intent with proper flags
                val authIntent = Intent(this@VaultlyAutofillService, AuthenticateBeforeAutofillActivity::class.java).apply {
                    putExtra("CLIENT_PACKAGE_NAME", clientPackageName)
                    putStringArrayListExtra("FIELD_KEYS", ArrayList(fieldMap.keys))
                    putParcelableArrayListExtra("FIELD_IDS", ArrayList(fieldMap.values))

                    // Flags to launch activity properly for autofill
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }

                val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
                val pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT or immutableFlag

                val authIntentSender = PendingIntent.getActivity(
                    this@VaultlyAutofillService,
                    request.hashCode(),
                    authIntent,
                    pendingFlags
                ).intentSender

                // Create presentation for the autofill suggestion
                val presentation = RemoteViews(
                    packageName,
                    android.R.layout.simple_list_item_1
                ).apply {
                    setTextViewText(android.R.id.text1, "🔐 Vaultly passwords")
                }

                android.util.Log.e("VaultlyAutofillService", "========== USING RESPONSE-LEVEL AUTHENTICATION ==========")
                android.util.Log.e("VaultlyAutofillService", "fieldMap size: ${fieldMap.size}")
                fieldMap.forEach { (fieldType, autofillId) ->
                    android.util.Log.e("VaultlyAutofillService", "Field: $fieldType -> $autofillId")
                }

                // Use RESPONSE-level authentication
                // This requires the user to tap on the suggestion, then authenticate,
                // then the returned FillResponse will provide the actual credentials
                val responseBuilder = android.service.autofill.FillResponse.Builder()
                    .setAuthentication(
                        fieldMap.values.toTypedArray(),
                        authIntentSender,
                        presentation
                    )

                android.util.Log.e("VaultlyAutofillService", "Response-level auth set with ${fieldMap.size} field IDs")

                // Add SaveInfo to enable save request when user logs in
                val usernameId = fieldMap["username"] ?: fieldMap["email"]
                val passwordId = fieldMap["password"]

                if (usernameId != null && passwordId != null) {
                    val saveInfoBuilder = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                        arrayOf(usernameId, passwordId)
                    )
                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                }

                val response = responseBuilder.build()
                callback.onSuccess(response)

            } catch (e: Exception) {
                Timber.e(e, "Error in onFillRequest")
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        serviceScope.launch {
            try {
                val context = request.fillContexts
                val structure = context.last().structure
                val clientPackageName = context.last().structure.activityComponent?.packageName
                    ?: "unknown"

                Timber.d("Save request from: %s", clientPackageName)

                val fieldMap = parseViewStructure(structure)

                if (fieldMap.isEmpty()) {
                    Timber.d("No autofill fields found in structure")
                    callback.onSuccess()
                    return@launch
                }

                // Extract credentials from the request
                var username: String? = null
                var password: String? = null

                fieldMap["username"]?.let { id ->
                    username = request.fillContexts.lastOrNull()?.structure?.let { struct ->
                        findValueForAutofillId(struct, id)
                    }
                }

                if (username == null) {
                    fieldMap["email"]?.let { id ->
                        username = request.fillContexts.lastOrNull()?.structure?.let { struct ->
                            findValueForAutofillId(struct, id)
                        }
                    }
                }

                fieldMap["password"]?.let { id ->
                    password = request.fillContexts.lastOrNull()?.structure?.let { struct ->
                        findValueForAutofillId(struct, id)
                    }
                }

                Timber.d("Extracted - Username: $username, Password exists: ${password != null}")

                if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                    val userId = AppKit.getAccount()?.address
                    if (userId != null) {
                        // Extract domain from package name for website field
                        val website = extractDomainFromPackage(clientPackageName)

                        val credential = Credential(
                            website = website,
                            username = username!!,
                            password = password!!,
                            note = "Saved via Autofill from $clientPackageName"
                        )

                        // Save to vault
                        vaultRepository.addOrUpdateCredential(userId, credential)

                        // Sync to autofill cache
                        autofillRepository.syncCredentials(userId)

                        Toast.makeText(
                            this@VaultlyAutofillService,
                            "Password saved to Vaultly",
                            Toast.LENGTH_SHORT
                        ).show()

                        Timber.d("Credential saved successfully for $website")
                    } else {
                        Timber.w("User not logged in, cannot save credential")
                        Toast.makeText(
                            this@VaultlyAutofillService,
                            "Please login to Vaultly to save passwords",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Timber.d("Username or password is null/blank, cannot save")
                }

                callback.onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Error in onSaveRequest")
                callback.onFailure(e.message)
            }
        }
    }

    /**
     * Helper function to find the value for a given AutofillId in the structure
     */
    private fun findValueForAutofillId(structure: AssistStructure, targetId: AutofillId): String? {
        fun traverse(node: AssistStructure.ViewNode): String? {
            if (node.autofillId == targetId) {
                return node.autofillValue?.textValue?.toString()
            }
            for (i in 0 until node.childCount) {
                node.getChildAt(i)?.let { child ->
                    traverse(child)?.let { return it }
                }
            }
            return null
        }

        return try {
            for (i in 0 until structure.windowNodeCount) {
                traverse(structure.getWindowNodeAt(i).rootViewNode)?.let { return it }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Error finding value for autofill ID")
            null
        }
    }

    /**
     * Extract a friendly domain name from package name
     */
    private fun extractDomainFromPackage(packageName: String): String {
        return when {
            packageName.contains("com.google") -> "Google"
            packageName.contains("com.facebook") -> "Facebook"
            packageName.contains("com.twitter") -> "Twitter"
            packageName.contains("com.instagram") -> "Instagram"
            packageName.contains("com.whatsapp") -> "WhatsApp"
            packageName.contains("com.linkedin") -> "LinkedIn"
            packageName.contains("com.netflix") -> "Netflix"
            packageName.contains("com.amazon") -> "Amazon"
            packageName.contains("com.spotify") -> "Spotify"
            packageName.contains("com.microsoft") -> "Microsoft"
            else -> {
                // Extract readable name from package (e.g., com.example.app -> example.app)
                val parts = packageName.split(".")
                if (parts.size >= 2) {
                    "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
                } else {
                    packageName
                }
            }
        }
    }

    private fun parseViewStructure(structure: AssistStructure): Map<String, AutofillId> {
        val fieldMap = mutableMapOf<String, AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            val autofillId = node.autofillId ?: return

            // First check autofill hints (most reliable)
            val hints = node.autofillHints
            if (!hints.isNullOrEmpty()) {
                for (hint in hints) {
                    when {
                        hint.contains("username", ignoreCase = true) -> fieldMap["username"] = autofillId
                        hint.contains("email", ignoreCase = true) -> fieldMap["email"] = autofillId
                        hint.contains("password", ignoreCase = true) -> fieldMap["password"] = autofillId
                    }
                }
            } else {
                // Fallback to heuristics
                val idEntry = node.idEntry?.lowercase() ?: ""
                val hint = node.hint?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""
                val htmlInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    node.htmlInfo?.attributes?.mapNotNull { it.second }?.joinToString(" ")?.lowercase() ?: ""
                } else ""

                when {
                    listOf(idEntry, hint, text, htmlInfo).any {
                        it.contains("user") || it.contains("email") || it.contains("login")
                    } && !fieldMap.containsKey("username") -> fieldMap["username"] = autofillId

                    listOf(idEntry, hint, text, htmlInfo).any {
                        it.contains("pass") || it.contains("pwd")
                    } && !fieldMap.containsKey("password") -> fieldMap["password"] = autofillId

                    listOf(idEntry, hint, text, htmlInfo).any {
                        it.contains("email")
                    } && !fieldMap.containsKey("email") -> fieldMap["email"] = autofillId
                }

                // Check input type for password fields
                val variation = node.inputType and android.text.InputType.TYPE_MASK_VARIATION
                if ((variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                    && !fieldMap.containsKey("password")) {
                    fieldMap["password"] = autofillId
                }
            }

            // Recursively traverse children
            for (i in 0 until node.childCount) {
                node.getChildAt(i)?.let { traverse(it) }
            }
        }

        try {
            for (i in 0 until structure.windowNodeCount) {
                traverse(structure.getWindowNodeAt(i).rootViewNode)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing view structure")
        }

        return fieldMap
    }
}
