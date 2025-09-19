package com.dapp.vaultly.di

import android.content.Context
import androidx.room.Room
import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.local.VaultlyDatabase
import com.dapp.vaultly.data.remote.PinataApi
import com.dapp.vaultly.data.repository.CredentialRepository
import com.dapp.vaultly.util.Constants
import com.dapp.vaultly.util.Constants.TEST_SIGNATURE
import com.dapp.vaultly.util.CryptoUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.crypto.SecretKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VaultlyModule {

    @Provides
    @Singleton
    fun provideVaultlyDatabase(@ApplicationContext context: Context): VaultlyDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            VaultlyDatabase::class.java,
            name = "vaultly_database"
        )
            .build()

    }

    @Provides
    @Singleton
    fun provideCredentialsDao(vaultlyDatabase: VaultlyDatabase): CredentialsDao {
        return vaultlyDatabase.credentialsDao()
    }
    @Singleton
    @Provides
    fun provideCredentialsRepository(
        credentialsDao: CredentialsDao,
        pinataApi: PinataApi,
        secretKey: SecretKey
    ): CredentialRepository {
        return CredentialRepository(
            dao = credentialsDao,
            pinata = pinataApi,
            secretKey = secretKey
        )
    }
    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.PINATA_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${Constants.JWT_TOKEN}")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }
    @Provides
    @Singleton
    fun provideSecretKey(): SecretKey {
        return CryptoUtil.deriveAesKeyFromSignature(TEST_SIGNATURE)
    }

    @Provides
    @Singleton
    fun providePinataApi(retrofit: Retrofit): PinataApi {
        return retrofit.create(PinataApi::class.java)
    }
}