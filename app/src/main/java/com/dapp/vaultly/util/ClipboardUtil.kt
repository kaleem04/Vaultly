package com.dapp.vaultly.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast

/**
 * Utility object for secure clipboard operations.
 * Handles sensitive data (passwords) with proper Android 13+ flags.
 */
object ClipboardUtil {

    /**
     * Copies text to clipboard with optional sensitive data flag.
     * On Android 13+, sensitive data is marked to prevent it from appearing
     * in clipboard history or being read by other apps.
     *
     * @param context The context
     * @param label A user-visible label for the clip data
     * @param text The text to copy
     * @param isSensitive Whether the data is sensitive (e.g., passwords)
     * @param showToast Whether to show a confirmation toast
     */
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = false,
        showToast: Boolean = true
    ) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)

        // Mark as sensitive on Android 13+ (API 33)
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clipData.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboardManager.setPrimaryClip(clipData)

        // On Android 13+, the system shows its own toast for clipboard copies
        // Only show our toast on older versions
        if (showToast && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val message = if (isSensitive) "Copied securely" else "Copied to clipboard"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies a password to clipboard with sensitive flag enabled.
     */
    fun copyPassword(context: Context, password: String) {
        copyToClipboard(
            context = context,
            label = "Password",
            text = password,
            isSensitive = true
        )
    }

    /**
     * Copies a username to clipboard (not marked as sensitive).
     */
    fun copyUsername(context: Context, username: String) {
        copyToClipboard(
            context = context,
            label = "Username",
            text = username,
            isSensitive = false
        )
    }
}
