package com.yuhai94.awcli.util

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoHelper(context: Context) {
    companion object {
        private const val TAG = "CryptoHelper"
    }
    private val prefs = context.applicationContext.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE)
    private val key: SecretKey

    init {
        val keyString = prefs.getString("secret_key", null)
        key = if (keyString != null) {
            Log.d(TAG, "Loading existing encryption key")
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            Log.d(TAG, "Generating new encryption key")
            val newKey = generateKey()
            prefs.edit().putString("secret_key", Base64.encodeToString(newKey.encoded, Base64.DEFAULT)).apply()
            newKey
        }
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256, SecureRandom())
        return keyGenerator.generateKey()
    }

    fun encrypt(plaintext: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + ciphertext
            val encrypted = Base64.encodeToString(combined, Base64.DEFAULT)
            Log.d(TAG, "Text encrypted successfully")
            encrypted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt text", e)
            throw e
        }
    }

    fun decrypt(ciphertext: String): String? {
        return try {
            val combined = Base64.decode(ciphertext, Base64.DEFAULT)
            val iv = combined.sliceArray(0..11)
            val encrypted = combined.sliceArray(12 until combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plaintext = cipher.doFinal(encrypted)
            val decrypted = String(plaintext, Charsets.UTF_8)
            Log.d(TAG, "Text decrypted successfully")
            decrypted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt text", e)
            null
        }
    }
}