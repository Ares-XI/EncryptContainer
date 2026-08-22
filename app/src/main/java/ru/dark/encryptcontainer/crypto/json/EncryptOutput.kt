package ru.dark.encryptcontainer.crypto.json

import com.google.gson.annotations.SerializedName

class EncryptOutput {
    @SerializedName("k")
    var encryptedAesKey: String = ""

    @SerializedName("v")
    var encryptedAesValue: String = ""

    override fun equals(other: Any?): Boolean {
        try {
            val casted = other as EncryptOutput
            return casted.encryptedAesKey == this.encryptedAesKey && casted.encryptedAesValue == this.encryptedAesValue
        } catch (_: Throwable) {
            return false
        }
    }

    override fun hashCode(): Int {
        var result = encryptedAesKey.hashCode()
        result = 31 * result + encryptedAesValue.hashCode()
        return result
    }
}