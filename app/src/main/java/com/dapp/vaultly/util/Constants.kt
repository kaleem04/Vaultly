package com.dapp.vaultly.util

import com.reown.appkit.client.AppKit
import org.web3j.crypto.Hash
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import javax.inject.Qualifier

object Constants {
    const val PINATA_URL = "https://api.pinata.cloud/"
    const val POLYGON_URL = "https://api.etherscan.io/v2/"
    const val IPFS_URL = "https://gateway.pinata.cloud/"
    const val CONTRACT_ADDRESS = "0x65CEA2972EDbF230E6E526eE86e17b63274aE368"
    const val TEST_SIGNATURE = "vaultly-dev-fallback"
    const val JWT_TOKEN =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySW5mb3JtYXRpb24iOnsiaWQiOiI0ZjM2ODQyYS1iY2I1LTQ0ZGQtYjE4My1lZWIyZDNhMzA1MjAiLCJlbWFpbCI6InliYWRzaGFoZ2FtaW5nQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJwaW5fcG9saWN5Ijp7InJlZ2lvbnMiOlt7ImRlc2lyZWRSZXBsaWNhdGlvbkNvdW50IjoxLCJpZCI6IkZSQTEifSx7ImRlc2lyZWRSZXBsaWNhdGlvbkNvdW50IjoxLCJpZCI6Ik5ZQzEifV0sInZlcnNpb24iOjF9LCJtZmFfZW5hYmxlZCI6ZmFsc2UsInN0YXR1cyI6IkFDVElWRSJ9LCJhdXRoZW50aWNhdGlvblR5cGUiOiJzY29wZWRLZXkiLCJzY29wZWRLZXlLZXkiOiIxYTlkYzc1YjE0MTNhMzBlY2E2YiIsInNjb3BlZEtleVNlY3JldCI6IjQ1MjhmMTZlZmY1OWUwZjRkZjYzMGQxMzY4OWY1NTFlOWJmZTgwZjJmYzlhNzFkOGUwYTIyODUzMDFmZWRkZTMiLCJleHAiOjE3ODg4NzAyODh9.-HMMgGTQcO126PPboVrq7P0h0upZR0MHpyPvJ0hXTE8"
    const val API_KEY = "U67KSQT7TU38T83K4ENRUVZ8315IF7MGXJ"

    val WALLET_ADDRESS = AppKit.getAccount()?.address ?: ""
    fun formatDate(createdAt: Long): String {
        val instant = Date(createdAt).toInstant()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault()) // Use the system's default time zone
        val formattedDate = formatter.format(instant)
        return formattedDate
    }


    fun getFunctionSelector(signature: String): String {
        val hash = Hash.sha3String(signature)   // keccak256("setCID(string)")
        return hash.substring(0, 10)            // "0x" + first 8 hex chars
    }

}


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PinataApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PolygonApi
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IpfsGateway