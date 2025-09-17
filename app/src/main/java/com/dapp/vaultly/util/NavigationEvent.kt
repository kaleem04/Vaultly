package com.dapp.vaultly.util

import android.content.Context
import com.dapp.vaultly.data.local.SessionStorage
import com.dapp.vaultly.data.model.VaultlyRoutes
import com.reown.appkit.client.AppKit
import kotlinx.coroutines.flow.MutableSharedFlow

object NavigationEvent {
    val navigationEvents = MutableSharedFlow<String>()
    val sessionEvent = MutableSharedFlow<Boolean>()

    suspend fun navigateTo(route: String) {
        navigationEvents.emit(route)
    }

    suspend fun setActiveSession(context: Context, isActive: Boolean) {
        sessionEvent.emit(isActive)

        SessionStorage.saveSession(context, isActive)

        if (isActive) {
            navigateTo(VaultlyRoutes.DASHBOARDSCREEN.name)
        } else {
            navigateTo(VaultlyRoutes.WELCOMESCREEN.name)
        }
    }

    fun hasActiveSession(): Boolean {
        return AppKit.getSession() != null
    }
}


