package ru.dark.encryptcontainer.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.dark.encryptcontainer.crypto.json.KeyCache
import ru.dark.encryptcontainer.crypto.json.MessageCache

object CryptoManager {
    private const val PUBLIC_KEY = "public_key"
    private const val PRIVATE_KEY = "private_key"
    private const val RETIRED_KEYS = "retired_keys"
    private const val ENCRYPT_TO_MESSAGES = "encrypt_to_messages"

    private val GSON = Gson()
    private lateinit var prefs: EncryptedSharedPreferences
    private lateinit var keyCache: KeyCache
    private lateinit var messageCache: MessageCache

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        prefs = EncryptedSharedPreferences.create(context, "secure_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM) as EncryptedSharedPreferences
        keyCache = serialiseKeys()
        messageCache = serialiseMessages()
    }

    fun getCurrentKeyPair(): Pair<String, String>? {
        val pair = Pair(prefs.getString(PUBLIC_KEY, null)?.replace(" ", "")?.replace("\n", "") ?: "", prefs.getString(PRIVATE_KEY, null)?.replace(" ", "")?.replace("\n", "") ?: "")
        if(!CryptoUtils.isValid(pair)) return null
        return pair
    }

    fun setCurrentKeyPair(publicKey: String, privateKey: String) {
        addKeyPair(Pair(prefs.getString(PUBLIC_KEY, null) ?: "", prefs.getString(PRIVATE_KEY, null) ?: ""))
        prefs.edit().putString(PUBLIC_KEY, publicKey).putString(PRIVATE_KEY, privateKey).apply()
    }

    fun getRetiredKeyPairs(): Array<Pair<String, String>> {
        return keyCache.cache.toTypedArray()
    }

    fun addRetiredKeyPair(pair: Pair<String, String>) {
        addKeyPair(pair)
    }

    fun getEncryptToMessagePairs(): Array<Pair<String, String>> {
        return messageCache.cache.toTypedArray()
    }

    fun addEncryptToMessagePair(pair: Pair<String, String>) {
        addMessagePair(pair)
    }

    private fun serialiseKeys(): KeyCache {
        val default = KeyCache()
        val json = prefs.getString(RETIRED_KEYS, null) ?: return default

        return try {
            val type = object : TypeToken<KeyCache>() {}.type
            GSON.fromJson(json, type)
        } catch (e: Throwable) {
            e.printStackTrace(System.out)
            default
        }
    }

    private fun serialiseMessages(): MessageCache {
        val default = MessageCache()
        val json = prefs.getString(ENCRYPT_TO_MESSAGES, null) ?: return default

        return try {
            val type = object : TypeToken<MessageCache>() {}.type
            GSON.fromJson(json, type)
        } catch (e: Throwable) {
            e.printStackTrace(System.out)
            default
        }
    }

    private fun addKeyPair(pair: Pair<String, String>) {
        if (CryptoUtils.isValid(pair)) {
            keyCache.cache.add(pair)
            prefs.edit().putString(RETIRED_KEYS, GSON.toJson(keyCache)).apply()
        }
    }

    private fun addMessagePair(pair: Pair<String, String>) {
        if (CryptoUtils.isValid(pair)) {
            messageCache.cache.add(pair)
            prefs.edit().putString(ENCRYPT_TO_MESSAGES, GSON.toJson(messageCache)).apply()
        }
    }
}