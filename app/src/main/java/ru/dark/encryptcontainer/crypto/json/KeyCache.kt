package ru.dark.encryptcontainer.crypto.json

import com.google.gson.annotations.SerializedName

class KeyCache {
    @SerializedName("cache_of_retired")
    var cache = mutableSetOf<Pair<String, String>>()
}