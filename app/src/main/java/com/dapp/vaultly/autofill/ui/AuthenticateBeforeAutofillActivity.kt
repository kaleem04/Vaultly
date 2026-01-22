package com.dapp.vaultly.autofill.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.dapp.vaultly.data.model.AutofillCredential
import com.dapp.vaultly.data.repository.VaultlyAutofillRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AuthenticateBeforeAutofillActivity : FragmentActivity() {

    @Inject
    lateinit var autofillRepository: VaultlyAutofillRepository

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var clientPackageName: String
    private lateinit var fieldMap: Map<String, AutofillId>

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("AuthenticateBeforeAutofillActivity onCreate")

        // Make the activity transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)

        clientPackageName = intent.getStringExtra("CLIENT_PACKAGE_NAME") ?: run {
            Timber.e("No CLIENT_PACKAGE_NAME provided")
            finish()
            return
        }

        val keys = intent.getStringArrayListExtra("FIELD_KEYS") ?: arrayListOf()
        val ids = intent.getParcelableArrayListExtra<AutofillId>("FIELD_IDS") ?: arrayListOf()
        fieldMap = if (keys.size == ids.size) {
            keys.mapIndexed { idx, key -> key to ids[idx] }.toMap()
        } else {
            emptyMap()
        }

        if (fieldMap.isEmpty()) {
            Timber.e("fieldMap is empty")
            finish()
            return
        }

        Timber.d("Setting up biometric for package: $clientPackageName")

        setupBiometricPrompt()

        setContent {
            var credentials by remember { mutableStateOf<List<AutofillCredential>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var showBiometric by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                Timber.d("Loading credentials...")
                try {
                    // Fetch credentials in background thread
                    credentials = withContext(Dispatchers.IO) {
                        autofillRepository.getMatchingCredentials(clientPackageName)
                    }
                    Timber.d("Loaded ${credentials.size} credentials")
                    isLoading = false

                    // Trigger biometric after credentials are loaded
                    if (showBiometric) {
                        biometricPrompt.authenticate(promptInfo)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading credentials")
                    isLoading = false
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }

            // Show loading or content
            if (!isLoading && credentials.isEmpty()) {
                // No credentials found - close immediately
                LaunchedEffect(Unit) {
                    Timber.d("No credentials found, finishing")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Timber.d("Authentication succeeded, showing credentials")

                    runOnUiThread {
                        setContent {
                            var credentials by remember {
                                mutableStateOf<List<AutofillCredential>>(emptyList())
                            }
                            var showContent by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                try {
                                    credentials = withContext(Dispatchers.IO) {
                                        autofillRepository.getMatchingCredentials(clientPackageName)
                                    }
                                    Timber.d("Showing ${credentials.size} credentials")
                                    showContent = true
                                } catch (e: Exception) {
                                    Timber.e(e, "Error loading credentials after auth")
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }
                            }

                            if (showContent && credentials.isNotEmpty()) {
                                CredentialModalDialog(
                                    credentials = credentials,
                                    onCredentialSelected = { credential ->
                                        fillCredentialsAndFinish(credential, fieldMap)
                                    },
                                    onDismiss = {
                                        setResult(Activity.RESULT_CANCELED)
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.d("Authentication error: $errorCode - $errString")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.d("Authentication failed")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to Autofill")
            .setSubtitle("Verify your identity to fill credentials")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun fillCredentialsAndFinish(
        credential: AutofillCredential,
        fieldMap: Map<String, AutofillId>
    ) {
        Timber.d("Filling credentials for: ${credential.username}")

        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        val datasetBuilder = Dataset.Builder()

        var presentationSet = false

        fieldMap["username"]?.let { id ->
            if (!presentationSet) {
                presentation.setTextViewText(android.R.id.text1, credential.username)
                datasetBuilder.setValue(id, AutofillValue.forText(credential.username), presentation)
                presentationSet = true
            } else {
                datasetBuilder.setValue(id, AutofillValue.forText(credential.username))
            }
        }

        fieldMap["email"]?.let { id ->
            if (!presentationSet) {
                presentation.setTextViewText(android.R.id.text1, credential.username)
                datasetBuilder.setValue(id, AutofillValue.forText(credential.username), presentation)
                presentationSet = true
            } else {
                datasetBuilder.setValue(id, AutofillValue.forText(credential.username))
            }
        }

        fieldMap["password"]?.let { id ->
            datasetBuilder.setValue(id, AutofillValue.forText(credential.password))
        }

        val dataset = datasetBuilder.build()
        val response = FillResponse.Builder()
            .addDataset(dataset)
            .build()

        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
        }

        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }
}

@Composable
fun CredentialModalDialog(
    credentials: List<AutofillCredential>,
    onCredentialSelected: (AutofillCredential) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    // Handle bar at top
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Use saved password",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Credentials list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(credentials) { credential ->
                            CredentialModalItem(
                                credential = credential,
                                onClick = { onCredentialSelected(credential) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CredentialModalItem(
    credential: AutofillCredential,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with first letter
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = credential.username.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Credential info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}