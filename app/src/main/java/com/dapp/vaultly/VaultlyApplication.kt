package com.dapp.vaultly

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VaultlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}