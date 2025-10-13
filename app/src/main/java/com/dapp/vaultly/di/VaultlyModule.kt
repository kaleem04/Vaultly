package com.dapp.vaultly.di

import android.content.Context
import androidx.room.Room
import com.dapp.vaultly.data.local.CredentialsDao
import com.dapp.vaultly.data.local.UserVaultDao
import com.dapp.vaultly.data.local.VaultlyDatabase
import com.dapp.vaultly.data.remote.IpfsGatewayService
import com.dapp.vaultly.data.remote.PinataApiService
import com.dapp.vaultly.data.remote.PolygonApiService
import com.dapp.vaultly.data.repository.CredentialRepository
import com.dapp.vaultly.data.repository.PolygonRepository
import com.dapp.vaultly.data.repository.UserVaultRepository
import com.dapp.vaultly.util.Constants
import com.dapp.vaultly.util.Constants.IPFS_URL
import com.dapp.vaultly.util.Constants.PINATA_URL
import com.dapp.vaultly.util.Constants.POLYGON_URL
import com.dapp.vaultly.util.Constants.TEST_SIGNATURE
import com.dapp.vaultly.util.CryptoUtil
import com.dapp.vaultly.util.IpfsGateway
import com.dapp.vaultly.util.PinataApi
import com.dapp.vaultly.util.PolygonApi
import com.reown.appkit.client.AppKit
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
    ): CredentialRepository {
        return CredentialRepository(
            dao = credentialsDao,
        )
    }

    @PinataApi
    @Singleton
    @Provides
    fun provideRetrofitPinata(@PinataApi client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PINATA_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @PolygonApi
    @Singleton
    @Provides
    fun provideRetrofitPolygon(@PolygonApi client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(POLYGON_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @IpfsGateway
    @Singleton
    @Provides
    fun provideRetrofitIpfs(@IpfsGateway client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(IPFS_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @PinataApi
    @Provides
    @Singleton
    fun providePinataOkHttpClient(): OkHttpClient {
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

    @IpfsGateway
    @Provides
    @Singleton
    fun provideIpfsOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
          //      .addHeader("Authorization", "Bearer ${Constants.JWT_TOKEN}")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
    }

    @PolygonApi
    @Provides
    @Singleton
    fun providePolygonOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
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
        return CryptoUtil.deriveAesKeyFromSignature(AppKit.getAccount()?.address ?: "")
    }


    @Provides
    @Singleton
    fun providePinataApi(@PinataApi retrofit: Retrofit): PinataApiService {
        return retrofit.create(PinataApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePolygonApi(@PolygonApi retrofit: Retrofit): PolygonApiService {
        return retrofit.create(PolygonApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIpfsApi(@IpfsGateway retrofit: Retrofit): IpfsGatewayService {
        return retrofit.create(IpfsGatewayService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserVaultDao(vaultlyDatabase: VaultlyDatabase): UserVaultDao {
        return vaultlyDatabase.userVaultDao()
    }

    @Provides
    @Singleton
    fun provideUserVaultRepository(
        userVaultDao: UserVaultDao,
        pinataApiService: PinataApiService,
        ipfsGatewayService: IpfsGatewayService,
       @ApplicationContext context: Context

    ): UserVaultRepository {
        return UserVaultRepository(
            vaultDao = userVaultDao,
            pinata = pinataApiService,
            ipfsGatewayService = ipfsGatewayService,
            context = context
        )
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context

    }

    @Provides
    fun providePolygonRepo(
        polygonApiService: PolygonApiService
    ): PolygonRepository {
        return PolygonRepository(polygonApiService)
    }
}