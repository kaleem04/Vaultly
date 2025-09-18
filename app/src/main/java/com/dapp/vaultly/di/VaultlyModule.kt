package com.dapp.vaultly.di

import android.content.Context
import androidx.room.Room
import com.dapp.vaultly.data.local.VaultlyDatabase
import com.dapp.vaultly.data.local.CredentialsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VaultlyModule {

    @Provides
    @Singleton
    fun provideVaultlyDatabase(@ApplicationContext context: Context) : VaultlyDatabase{
        return Room.databaseBuilder(
            context.applicationContext,
            VaultlyDatabase::class.java,
            name = "vaultly_database"
        )
            .build()

    }

    @Provides
    @Singleton
    fun provideCredentialsDao(vaultlyDatabase: VaultlyDatabase) : CredentialsDao{
        return vaultlyDatabase.credentialsDao()
    }
}