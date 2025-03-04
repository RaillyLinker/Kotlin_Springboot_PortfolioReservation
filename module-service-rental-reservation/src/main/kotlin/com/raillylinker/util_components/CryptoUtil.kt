package com.raillylinker.util_components

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// [암호화, 복호화 관련 유틸]
@Component
class CryptoUtil(private val customUtil: CustomUtil) {
    // [암호화 / 복호화]
    // (AES256 암호화)
    fun encryptAES256(
        text: String, // 암호화하려는 평문
        alg: String, // 암호화 알고리즘 (ex : "AES/CBC/PKCS5Padding")
        initializationVector: String, // 초기화 벡터 16byte = 16char
        encryptionKey: String // 암호화 키 32byte = 32char
    ): String {
        if (encryptionKey.length != 32 || initializationVector.length != 16) {
            try {
                throw RuntimeException("encryptionKey length must be 32 and initializationVector length must be 16")
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val cipher = Cipher.getInstance(alg)
        val keySpec = SecretKeySpec(encryptionKey.toByteArray(), "AES")
        val ivParamSpec = IvParameterSpec(initializationVector.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec)
        val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    // (AES256 복호화)
    fun decryptAES256(
        cipherText: String, // 복호화하려는 암호문
        alg: String, // 암호화 알고리즘 (ex : "AES/CBC/PKCS5Padding")
        initializationVector: String, // 초기화 벡터 16byte = 16char
        encryptionKey: String // 암호화 키 32byte = 32char
    ): String {
        if (encryptionKey.length != 32 || initializationVector.length != 16) {
            try {
                throw RuntimeException("encryptionKey length must be 32 and initializationVector length must be 16")
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val cipher = Cipher.getInstance(alg)
        val keySpec = SecretKeySpec(encryptionKey.toByteArray(), "AES")
        val ivParamSpec = IvParameterSpec(initializationVector.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec)
        val decodedBytes = Base64.getDecoder().decode(cipherText)
        val decrypted = cipher.doFinal(decodedBytes)
        return String(decrypted, StandardCharsets.UTF_8)
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // [인코딩 / 디코딩]
    // (Base64 인코딩)
    fun base64Encode(str: String): String {
        return Base64.getEncoder().encodeToString(str.toByteArray(StandardCharsets.UTF_8))
    }

    // (Base64 디코딩)
    fun base64Decode(str: String): String {
        return String(Base64.getDecoder().decode(str))
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // [해싱]
    // (SHA256 해싱)
    fun hashSHA256(str: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(str.toByteArray(StandardCharsets.UTF_8))
        return customUtil.bytesToHex(messageDigest.digest())
    }

    // (HmacSHA256)
    fun hmacSha256(data: String, secret: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        sha256Hmac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8)))
    }
}