package ru.ksolowtools.telegram.client.security

import java.lang.System.arraycopy
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

class MessageEncryptionService(
    keyValue: String
) {
    private val key = SecretKeySpec(decodeKey(keyValue), "AES")
    private val secureRandom = SecureRandom()

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }

        val cipherText = cipher.doFinal(plainText.toByteArray(UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        arraycopy(iv, 0, combined, 0, iv.size)
        arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(payloadBase64: String): String {
        val decoded = Base64.getDecoder().decode(payloadBase64)
        require(decoded.size > 12) { "Invalid payload length" }

        val iv = decoded.copyOfRange(0, 12)
        val cipherText = decoded.copyOfRange(12, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }

        return String(cipher.doFinal(cipherText), UTF_8)
    }

    private fun decodeKey(keyValue: String): ByteArray {
        val base64Bytes = decodeBase64(keyValue)
        val keyBytes = if (isValidKeySize(base64Bytes?.size)) {
            base64Bytes!!
        } else {
            keyValue.toByteArray(UTF_8)
        }

        require(isValidKeySize(keyBytes.size)) {
            "messagesEncryptionKey must be Base64 or raw string of 16, 24, or 32 bytes"
        }

        return keyBytes
    }

    private fun decodeBase64(value: String): ByteArray? =
        kotlin.runCatching { Base64.getDecoder().decode(value) }.getOrNull()

    private fun isValidKeySize(size: Int?) = size == 16 || size == 24 || size == 32
}
