package com.dapp.vaultly.util

import kotlinx.coroutines.flow.MutableSharedFlow

object NavigationEvent {
    val navigationEvents = MutableSharedFlow<String>()
}