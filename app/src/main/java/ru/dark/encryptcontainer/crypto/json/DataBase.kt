package ru.dark.encryptcontainer.crypto.json

class DataBase(val currentKeyPair: Pair<String, String>?, val retiredKeyCache: KeyCache, val messageCache: MessageCache) {
    override fun toString(): String {
        return "DataBase(currentKeyPair=$currentKeyPair, retiredKeyCache=${retiredKeyCache.cache.toSet()}, messageCache=${messageCache.cache.toSet()}"
    }
}