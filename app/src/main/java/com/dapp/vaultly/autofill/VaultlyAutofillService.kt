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
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.dapp.vaultly.autofill.ui.AuthenticateBeforeAutofillActivity
import com.dapp.vaultly.data.repository.VaultlyAutofillRepository
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

                    // Critical flags to prevent launching your app
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_HISTORY or
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

                // Create presentation
                val presentation = RemoteViews(
                    packageName,
                    android.R.layout.simple_list_item_1
                ).apply {
                    setTextViewText(android.R.id.text1, "Use saved passwords")
                }

                val datasetBuilder = android.service.autofill.Dataset.Builder()

                // Set empty values for all fields to trigger authentication
                fieldMap.forEach { (_, autofillId) ->
                    datasetBuilder.setValue(
                        autofillId,
                        AutofillValue.forText(""),
                        presentation
                    )
                }

                val responseBuilder = android.service.autofill.FillResponse.Builder()
                    .setAuthentication(fieldMap.values.toTypedArray(), authIntentSender, presentation)
                    .addDataset(datasetBuilder.build())

                val response = responseBuilder.build()
                callback.onSuccess(response)

            } catch (e: Exception) {
                Timber.e(e, "Error in onFillRequest")
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun parseViewStructure(structure: AssistStructure): Map<String, AutofillId> {
        val fieldMap = mutableMapOf<String, AutofillId>()

        fun traverse(node: AssistStructure.ViewNode) {
            val autofillId = node.autofillId ?: return

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
                val idEntry = node.idEntry?.lowercase() ?: ""
                val hint = node.hint?.lowercase() ?: ""
                val text = node.text?.toString()?.lowercase() ?: ""

                when {
                    listOf(idEntry, hint, text).any { it.contains("user") || it.contains("email") || it.contains("login") }
                            && !fieldMap.containsKey("username") -> fieldMap["username"] = autofillId
                    listOf(idEntry, hint, text).any { it.contains("pass") || it.contains("pwd") }
                            && !fieldMap.containsKey("password") -> fieldMap["password"] = autofillId
                    listOf(idEntry, hint, text).any { it.contains("email") }
                            && !fieldMap.containsKey("email") -> fieldMap["email"] = autofillId
                }

                val variation = node.inputType and android.text.InputType.TYPE_MASK_VARIATION
                if ((variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                    && !fieldMap.containsKey("password")) {
                    fieldMap["password"] = autofillId
                }
            }

            for (i in 0 until node.childCount) {
                node.getChildAt(i)?.let { traverse(it) }
            }
        }

        try {
            traverse(structure.getWindowNodeAt(0).rootViewNode)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing view structure")
        }

        return fieldMap
    }
}