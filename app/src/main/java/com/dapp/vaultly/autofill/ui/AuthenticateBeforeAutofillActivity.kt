package com.dapp.vaultly.autofill.ui

import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Shared state to control UI
    private lateinit var loadedCredentials: List<AutofillCredential>
    private val shouldShowCredentials = mutableStateOf(false)
    private var isAuthenticationInProgress = false

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d(">>> AuthenticateBeforeAutofillActivity onCreate")

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

        Timber.d("Setting up biometric for package: $clientPackageName, fields: ${fieldMap.keys}")

        setupBiometricPrompt()
        setupUI()
    }

    override fun onStop() {
        super.onStop()
        Timber.d(">>> onStop called, isAuthenticationInProgress=$isAuthenticationInProgress")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d(">>> onDestroy called")
    }

    override fun onPause() {
        super.onPause()
        Timber.d(">>> onPause called, isAuthenticationInProgress=$isAuthenticationInProgress")
    }

    override fun onResume() {
        super.onResume()
        Timber.d(">>> onResume called")
    }

    private fun setupUI() {
        setContent {
            MaterialTheme {
                var showCredentials by remember { shouldShowCredentials }
                var credentials by remember { mutableStateOf<List<AutofillCredential>?>(null) }

                LaunchedEffect(Unit) {
                    Timber.d(">>> LAUNCHED EFFECT: Loading credentials...")
                    try {
                        // Fetch credentials in background thread
                        val loaded = withContext(Dispatchers.IO) {
                            autofillRepository.getMatchingCredentials(clientPackageName)
                        }
                        Timber.d(">>> LOADED ${loaded.size} credentials")

                        if (loaded.isEmpty()) {
                            Timber.d(">>> No credentials found, finishing")
                            setResult(RESULT_CANCELED)
                            finish()
                            return@LaunchedEffect
                        }

                        credentials = loaded
                        loadedCredentials = loaded

                        // Add a small delay to ensure activity is fully visible
                        kotlinx.coroutines.delay(100)

                        // Show biometric prompt
                        Timber.d(">>> Showing biometric prompt")
                        isAuthenticationInProgress = true
                        biometricPrompt.authenticate(promptInfo)
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error loading credentials")
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }

                // Show credentials dialog after successful authentication
                Timber.tag("AUTOFILL_UI").d(">>> COMPOSE: showCredentials=$showCredentials, credentials=${credentials?.size}")
                if (showCredentials && credentials != null) {
                    Timber.tag("AUTOFILL_UI").d(">>> SHOWING DIALOG with ${credentials!!.size} credentials")
                    Timber.tag("AUTOFILL_UI").d(">>> fieldMap has ${fieldMap.size} fields: ${fieldMap.keys.joinToString()}")
                    CredentialModalDialog(
                        credentials = credentials!!,
                        onCredentialSelected = { credential ->
                            android.util.Log.e("AUTOFILL_CLICK", "========== CALLBACK START ==========")
                            android.util.Log.e("AUTOFILL_CLICK", "onCredentialSelected lambda called")
                            android.util.Log.e("AUTOFILL_CLICK", "Selected: ${credential.username}")

                            Timber.tag("AUTOFILL_CLICK").d(">>> ============================================")
                            Timber.tag("AUTOFILL_CLICK").d(">>> CREDENTIAL SELECTED CALLBACK TRIGGERED")
                            Timber.tag("AUTOFILL_CLICK").d(">>> Selected: ${credential.username}")
                            Timber.tag("AUTOFILL_CLICK").d(">>> Website: ${credential.website}")
                            Timber.tag("AUTOFILL_CLICK").d(">>> Thread: ${Thread.currentThread().name}")
                            Timber.tag("AUTOFILL_CLICK").d(">>> About to call fillCredentialsAndFinish")
                            Timber.tag("AUTOFILL_CLICK").d(">>> ============================================")

                            android.util.Log.e("AUTOFILL_CLICK", "About to call fillCredentialsAndFinish()")
                            fillCredentialsAndFinish(credential, fieldMap)
                            android.util.Log.e("AUTOFILL_CLICK", "fillCredentialsAndFinish() returned")
                        },
                        onDismiss = {
                            Timber.tag("AUTOFILL_CLICK").d(">>> DIALOG DISMISSED BY USER")
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
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
                    Timber.d(">>> AUTHENTICATION SUCCEEDED")
                    isAuthenticationInProgress = false
                    Timber.d(">>> Setting shouldShowCredentials = true")
                    // Trigger credential list display
                    shouldShowCredentials.value = true
                    Timber.d(">>> shouldShowCredentials is now: ${shouldShowCredentials.value}")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.d(">>> AUTHENTICATION ERROR: $errorCode - $errString")
                    isAuthenticationInProgress = false
                    setResult(RESULT_CANCELED)
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Timber.d(">>> AUTHENTICATION FAILED")
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
        try {
            // IMMEDIATE LOG - First line of method
            android.util.Log.e("AUTOFILL_INJECT", "========== METHOD CALLED ==========")
            android.util.Log.e("AUTOFILL_INJECT", "fillCredentialsAndFinish() STARTED")
            android.util.Log.e("AUTOFILL_INJECT", "Thread: ${Thread.currentThread().name}")
            android.util.Log.e("AUTOFILL_INJECT", "Username: ${credential.username}")
            android.util.Log.e("AUTOFILL_INJECT", "Website: ${credential.website}")
            android.util.Log.e("AUTOFILL_INJECT", "Password length: ${credential.password.length}")
            android.util.Log.e("AUTOFILL_INJECT", "Fields in map: ${fieldMap.keys.joinToString()}")
            android.util.Log.e("AUTOFILL_INJECT", "Field IDs: ${fieldMap.values.joinToString { it.toString() }}")
            android.util.Log.e("AUTOFILL_INJECT", "Has 'username' key: ${fieldMap.containsKey("username")}")
        android.util.Log.e("AUTOFILL_INJECT", "Has 'email' key: ${fieldMap.containsKey("email")}")
        android.util.Log.e("AUTOFILL_INJECT", "Has 'password' key: ${fieldMap.containsKey("password")}")

        // Create a presentation for the dataset - THIS IS REQUIRED!
        // Even for authenticated datasets, Android requires a presentation
        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Vaultly: ${credential.username}")
        }
        android.util.Log.e("AUTOFILL_INJECT", "Created RemoteViews presentation")

        // Build dataset with presentation
        android.util.Log.e("AUTOFILL_INJECT", "Building Dataset WITH presentation...")
        val datasetBuilder = Dataset.Builder(presentation)

        var fieldsSet = 0

        // Try to fill username field
        android.util.Log.e("AUTOFILL_INJECT", "Checking for 'username' field...")
        fieldMap["username"]?.let { id ->
            android.util.Log.e("AUTOFILL_INJECT", ">>> Adding USERNAME field to dataset (ID: $id)")
            android.util.Log.e("AUTOFILL_INJECT", ">>> Username value: '${credential.username}'")
            datasetBuilder.setValue(id, AutofillValue.forText(credential.username))
            fieldsSet++
            android.util.Log.e("AUTOFILL_INJECT", ">>> Username field added, fieldsSet=$fieldsSet")
        } ?: android.util.Log.e("AUTOFILL_INJECT", ">>> No username field in fieldMap")

        // Try to fill email field (same value as username)
        android.util.Log.e("AUTOFILL_INJECT", "Checking for 'email' field...")
        fieldMap["email"]?.let { id ->
            android.util.Log.e("AUTOFILL_INJECT", ">>> Adding EMAIL field to dataset (ID: $id)")
            android.util.Log.e("AUTOFILL_INJECT", ">>> Email value: '${credential.username}'")
            datasetBuilder.setValue(id, AutofillValue.forText(credential.username))
            fieldsSet++
            android.util.Log.e("AUTOFILL_INJECT", ">>> Email field added, fieldsSet=$fieldsSet")
        } ?: android.util.Log.e("AUTOFILL_INJECT", ">>> No email field in fieldMap")

        // Fill password field
        android.util.Log.e("AUTOFILL_INJECT", "Checking for 'password' field...")
        fieldMap["password"]?.let { id ->
            android.util.Log.e("AUTOFILL_INJECT", ">>> Adding PASSWORD field to dataset (ID: $id)")
            android.util.Log.e("AUTOFILL_INJECT", ">>> Password length: ${credential.password.length}")
            datasetBuilder.setValue(id, AutofillValue.forText(credential.password))
            fieldsSet++
            android.util.Log.e("AUTOFILL_INJECT", ">>> Password field added, fieldsSet=$fieldsSet")
        } ?: android.util.Log.e("AUTOFILL_INJECT", ">>> No password field in fieldMap")

        android.util.Log.e("AUTOFILL_INJECT", "Total fields set in dataset: $fieldsSet")

        if (fieldsSet == 0) {
            android.util.Log.e("AUTOFILL_INJECT", "=======================================================")
            android.util.Log.e("AUTOFILL_INJECT", "ERROR: No fields were added to dataset!")
            android.util.Log.e("AUTOFILL_INJECT", "Available field keys: ${fieldMap.keys.joinToString()}")
            android.util.Log.e("AUTOFILL_INJECT", "=======================================================")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        android.util.Log.e("AUTOFILL_INJECT", ">>> Total fields set in dataset: $fieldsSet")

        // Build the dataset
        val dataset = try {
            android.util.Log.e("AUTOFILL_INJECT", ">>> Building dataset object...")
            val built = datasetBuilder.build()
            android.util.Log.e("AUTOFILL_INJECT", ">>> Dataset built successfully: $built")
            built
        } catch (e: Exception) {
            android.util.Log.e("AUTOFILL_INJECT", ">>> EXCEPTION building dataset: ${e.message}")
            e.printStackTrace()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // For RESPONSE-LEVEL authentication, we need to return a FillResponse, not just a Dataset
        android.util.Log.e("AUTOFILL_INJECT", ">>> Building FillResponse...")
        val fillResponse = try {
            FillResponse.Builder()
                .addDataset(dataset)
                .build()
        } catch (e: Exception) {
            android.util.Log.e("AUTOFILL_INJECT", ">>> EXCEPTION building FillResponse: ${e.message}")
            e.printStackTrace()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        android.util.Log.e("AUTOFILL_INJECT", ">>> FillResponse built successfully: $fillResponse")

        // Return the FillResponse
        android.util.Log.e("AUTOFILL_INJECT", ">>> Creating reply Intent with FillResponse...")
        val replyIntent = Intent().apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
        }

        android.util.Log.e("AUTOFILL_INJECT", "=======================================================")
        android.util.Log.e("AUTOFILL_INJECT", "=== SENDING FILL RESPONSE WITH $fieldsSet FIELDS ===")
        android.util.Log.e("AUTOFILL_INJECT", "Intent extras: ${replyIntent.extras}")
        android.util.Log.e("AUTOFILL_INJECT", "=======================================================")

        android.util.Log.e("AUTOFILL_INJECT", ">>> Calling setResult(RESULT_OK)...")
        setResult(RESULT_OK, replyIntent)

        android.util.Log.e("AUTOFILL_INJECT", ">>> Calling finish()...")
        finish()
        android.util.Log.e("AUTOFILL_INJECT", ">>> Activity finish() called - method complete")
        } catch (e: Exception) {
            android.util.Log.e("AUTOFILL_INJECT", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            android.util.Log.e("AUTOFILL_INJECT", "EXCEPTION CAUGHT IN fillCredentialsAndFinish!")
            android.util.Log.e("AUTOFILL_INJECT", "Exception: ${e.javaClass.simpleName}")
            android.util.Log.e("AUTOFILL_INJECT", "Message: ${e.message}")
            android.util.Log.e("AUTOFILL_INJECT", "Stack trace:")
            e.printStackTrace()
            android.util.Log.e("AUTOFILL_INJECT", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            setResult(RESULT_CANCELED)
            finish()
        }
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
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            Timber.d(">>> Background tapped - dismissing")
                            onDismiss()
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    // Handle bar at top
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Header with icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lock icon
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🔐",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose account",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${credentials.size} account${if (credentials.size != 1) "s" else ""} available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Credentials list with better styling
                    Timber.tag("AUTOFILL_UI").d(">>> Rendering LazyColumn with ${credentials.size} items")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(credentials) { credential ->
                            Timber.tag("AUTOFILL_UI").d(">>> Creating list item for: ${credential.username}")
                            EnhancedCredentialItem(
                                credential = credential,
                                onClick = {
                                    android.util.Log.e("AUTOFILL_CLICK", "========== ITEM CLICK ==========")
                                    android.util.Log.e("AUTOFILL_CLICK", "LazyColumn item onClick for: ${credential.username}")

                                    Timber.tag("AUTOFILL_CLICK").d(">>> LAZY COLUMN ITEM onClick for: ${credential.username}")

                                    android.util.Log.e("AUTOFILL_CLICK", "About to call onCredentialSelected()")
                                    onCredentialSelected(credential)
                                    android.util.Log.e("AUTOFILL_CLICK", "onCredentialSelected() returned")
                                }
                            )
                        }
                    }

                    // Divider before cancel
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cancel button with better styling
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCredentialItem(
    credential: AutofillCredential,
    onClick: () -> Unit
) {
    Timber.tag("AUTOFILL_UI").d(">>> Rendering credential item: ${credential.username}")
    Surface(
        onClick = {
            android.util.Log.e("AUTOFILL_CLICK", "========== SURFACE CLICK ==========")
            android.util.Log.e("AUTOFILL_CLICK", "Surface onClick for: ${credential.username}")

            Timber.tag("AUTOFILL_CLICK").d(">>> ==========================================")
            Timber.tag("AUTOFILL_CLICK").d(">>> SURFACE CLICKED FOR: ${credential.username}")
            Timber.tag("AUTOFILL_CLICK").d(">>> Calling onClick lambda...")
            Timber.tag("AUTOFILL_CLICK").d(">>> ==========================================")

            android.util.Log.e("AUTOFILL_CLICK", "About to call onClick()")
            onClick()
            android.util.Log.e("AUTOFILL_CLICK", "onClick() returned")

            Timber.tag("AUTOFILL_CLICK").d(">>> onClick lambda completed")
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top  // Changed from CenterVertically to Top for better text wrapping
        ) {
            // Enhanced Avatar
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .padding(top = 4.dp),  // Slight top padding to align with text
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = credential.username.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Credential info - now properly constrained
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)  // Don't force fill, let it wrap
                    .widthIn(max = 240.dp)     // Maximum width to prevent overflow
            ) {
                // Username - allow ellipsis for long text
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,  // Allow 2 lines for long usernames
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Website badge - separate row for better control
                if (credential.website.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = credential.website,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Password indicator - always on its own line
                Text(
                    text = "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Arrow indicator - always visible
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_send),
                contentDescription = "Select",
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 8.dp),  // Align with first line of text
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
