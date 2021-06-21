package com.snapkirin.homesecurity.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptedSP {
    private fun getMasterKey(context: Context) : MasterKey {
        return MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
    }

    fun getEncryptedSharedPreferences(filename: String, context: Context) : SharedPreferences {
        val mainKey = getMasterKey(context)
        return EncryptedSharedPreferences.create(
                context,
                filename,
                mainKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

}