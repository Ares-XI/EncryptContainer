package ru.dark.encryptcontainer.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    fun isValid(pair: Pair<String, String>): Boolean {
        return pair.first != "" && pair.second != ""
    }

    fun generateKeyPair(): Pair<String, String> {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()

        val publicKey = Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT)
        val privateKey = Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT)
        return Pair(publicKey, privateKey)
    }

     fun encrypt(publicKeyStr: String, message: String): Pair<String, String> {
        val publicKey = stringToPublicKey(publicKeyStr.replace(" ", "").replace("\n", ""))
        val aesKey = generateAESKey()
        val (iv, encryptedMessage) = encryptAES(message.toByteArray(Charsets.UTF_8), aesKey)
        val encryptedAESKey = encryptRSA(aesKey.encoded, publicKey)
        val aesKeyBase64 = Base64.encodeToString(encryptedAESKey, Base64.DEFAULT)
        val messageBase64 = Base64.encodeToString(iv + encryptedMessage, Base64.DEFAULT)
        return Pair(aesKeyBase64, messageBase64)
    }

    fun decrypt(privateKeyStr: String, encryptedAESKeyStr: String, encryptedMessageStr: String): String {
        val privateKey = stringToPrivateKey(privateKeyStr.replace(" ", "").replace("\n", ""))
        val encryptedAESKey = Base64.decode(encryptedAESKeyStr.replace(" ", "").replace("\n", ""), Base64.DEFAULT)
        val aesKeyBytes = decryptRSA(encryptedAESKey, privateKey)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        val fullMessage = Base64.decode(encryptedMessageStr, Base64.DEFAULT)
        val iv = fullMessage.copyOfRange(0, 12)
        val encryptedMessage = fullMessage.copyOfRange(12, fullMessage.size)

        val decryptedBytes = decryptAES(encryptedMessage, iv, aesKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encryptFile(plainText: String, password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val key = deriveKeyFromPassword(password.replace(" ", "").replace("\n", ""), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encryptedData = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val result = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, result, 0, salt.size)
        System.arraycopy(iv, 0, result, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, result, salt.size + iv.size, encryptedData.size)
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    fun decryptFile(encryptedText: String, password: String): String {
        val data = Base64.decode(encryptedText, Base64.DEFAULT)
        val salt = data.copyOfRange(0, 16)
        val iv = data.copyOfRange(16, 16 + 12)
        val encryptedData = data.copyOfRange(16 + 12, data.size)
        val key = deriveKeyFromPassword(password.replace(" ", "").replace("\n", ""), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decryptedData = cipher.doFinal(encryptedData)
        return String(decryptedData, Charsets.UTF_8)
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedKey = factory.generateSecret(spec)
        return SecretKeySpec(derivedKey.encoded, "AES")
    }

    private fun stringToPublicKey(key: String): PublicKey {
        val keyBytes = Base64.decode(key, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private fun stringToPrivateKey(key: String): PrivateKey {
        val keyBytes = Base64.decode(key, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun generateAESKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return generator.generateKey()
    }

    private fun encryptAES(data: ByteArray, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(data)
        return Pair(iv, encrypted)
    }

    private fun decryptAES(encryptedData: ByteArray, iv: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedData)
    }

    private fun encryptRSA(data: ByteArray, key: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    private fun decryptRSA(data: ByteArray, key: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }
}