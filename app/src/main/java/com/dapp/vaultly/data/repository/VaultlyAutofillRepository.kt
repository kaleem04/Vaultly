package com.dapp.vaultly.data.repository

import android.content.Context
import android.util.Log
import com.dapp.vaultly.data.model.AutofillCredential
import com.dapp.vaultly.data.model.Credential
import com.reown.appkit.client.AppKit
import kotlinx.coroutines.flow.firstOrNull
import java.net.URI
import javax.inject.Inject
class VaultlyAutofillRepository @Inject constructor(
    private val vaultRepository: UserVaultRepository,
    private val context: Context
) {
    suspend fun getMatchingCredentials(packageName: String): List<AutofillCredential> {
        return try {
            val userId = AppKit.getAccount()?.address ?: return emptyList()

            // Get all credentials from your existing flow
            val credentials = vaultRepository.getCredentials(userId).firstOrNull()
                ?: return emptyList()

            Log.d("VaultlyAutofill", "Found ${credentials.size} credentials")

            // Return all credentials - user will see all and pick manually
            // Since you don't store package names, we can't intelligently filter
            credentials.map { credential ->
                AutofillCredential(
                    id = credential.id.toString(),
                    website = credential.website,
                    username = credential.username,
                    password = credential.password,
                    note = credential.note
                )
            }
        } catch (e: Exception) {
            Log.e("VaultlyAutofill", "Error getting credentials", e)
            emptyList()
        }
    }

    fun getAppLabel(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}