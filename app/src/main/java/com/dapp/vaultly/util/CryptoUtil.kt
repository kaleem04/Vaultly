package com.dapp.vaultly.util

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    private const val AES_ALGO = "AES"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bits
    private val secureRandom = SecureRandom()

    // HKDF (RFC 5869) with HMAC-SHA256
    fun hkdfExtractAndExpand(
        salt: ByteArray?,
        ikm: ByteArray,
        info: ByteArray?,
        length: Int
    ): ByteArray {
        val prk = hkdfExtract(salt, ikm)
        return hkdfExpand(prk, info ?: ByteArray(0), length)
    }

    private fun hkdfExtract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val realSalt = salt ?: ByteArray(32) { 0 }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(realSalt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val okm = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter: Byte = 1
        while (offset < length) {
            mac.reset()
            mac.update(previous)
            mac.update(info)
            mac.update(counter)
            val t = mac.doFinal()
            val copyLen = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, copyLen)
            offset += copyLen
            previous = t
            counter++
        }
        return okm
    }

    // Derive AES key from signature string (hex or utf8)
    fun deriveAesKeyFromSignature(signature: String): SecretKey {
        val sigBytes = try {
            hexToBytesIfHex(signature)
        } catch (e: Exception) {
            signature.toByteArray(Charsets.UTF_8)
        }
        // Optionally include an app-specific salt/info
        val info = "Vaultly AES key v1".toByteArray(Charsets.UTF_8)
        val keyBytes = hkdfExtractAndExpand(null, sigBytes, info, 32) // 32 bytes = 256 bit
        return SecretKeySpec(keyBytes, AES_ALGO)
    }

    // AES-GCM encrypt -> returns Pair(ivBase64, cipherBase64)
    fun encrypt(plainText: String, key: SecretKey): Pair<String, String> {
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv) to Base64.getEncoder()
            .encodeToString(cipherBytes)
    }

    fun decrypt(ivB64: String, cipherB64: String, key: SecretKey): String {
        val iv = Base64.getDecoder().decode(ivB64)
        val cipherBytes = Base64.getDecoder().decode(cipherB64)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plain = cipher.doFinal(cipherBytes)
        return String(plain, Charsets.UTF_8)
    }

    private fun hexToBytesIfHex(s: String): ByteArray {
        val str = if (s.startsWith("0x")) s.substring(2) else s
        if (str.length % 2 != 0) throw IllegalArgumentException("Invalid hex")
        return ByteArray(str.length / 2) { i ->
            ((Character.digit(str[i * 2], 16) shl 4) + Character.digit(str[i * 2 + 1], 16)).toByte()
        }
    }

    fun encryptBlob(plainText: String, key: SecretKey): String {
        val (iv, cipher) = encrypt(plainText, key)
        val obj = JSONObject().apply {
            put("iv", iv)
            put("cipher", cipher)
        }
        return obj.toString()
    }

    fun decryptBlob(blob: String, key: SecretKey): String {
        val obj = JSONObject(blob)
        return decrypt(obj.getString("iv"), obj.getString("cipher"), key)
    }

}
