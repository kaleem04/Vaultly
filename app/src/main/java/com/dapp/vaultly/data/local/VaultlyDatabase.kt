package com.dapp.vaultly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dapp.vaultly.data.model.CredentialEntity

@Database(entities = [CredentialEntity::class], version = 1, exportSchema = false)
abstract class VaultlyDatabase : RoomDatabase() {
    abstract fun credentialsDao() : CredentialsDao
}