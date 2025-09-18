package com.dapp.vaultly.util

import android.content.Context
import com.dapp.vaultly.data.local.SessionStorage
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.reown.appkit.client.AppKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object NavigationEvent {
    val navigationEvents = MutableSharedFlow<String>()
    val sessionEvent = MutableSharedFlow<Boolean>()

    suspend fun navigateTo(route: String) {
        navigationEvents.emit(route)
    }

    fun setActiveSession(context: Context, isActive: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            sessionEvent.emit(isActive)
            SessionStorage.saveSession(context, isActive)

            if (isActive) {
                navigateTo(VaultlyRoutes.DASHBOARDSCREEN.name)
            } else {
                navigateTo(VaultlyRoutes.WELCOMESCREEN.name)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        return AppKit.getSession() != null
    }
}


