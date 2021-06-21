package com.snapkirin.homesecurity.util

import android.content.Context
import androidx.core.content.edit
import com.snapkirin.homesecurity.HomeSecurity
import com.snapkirin.homesecurity.HomeSecurity.USER_ID
import com.snapkirin.homesecurity.HomeSecurity.EMAIL
import com.snapkirin.homesecurity.HomeSecurity.IDENTIFIER_USERNAME
import com.snapkirin.homesecurity.HomeSecurity.JWT
import com.snapkirin.homesecurity.HomeSecurity.PASSWORD
import com.snapkirin.homesecurity.HomeSecurity.SHARED_PREF_ACCOUNT
import com.snapkirin.homesecurity.HomeSecurity.USERNAME
import com.snapkirin.homesecurity.model.User
import com.snapkirin.homesecurity.model.http.LoginResult
import com.snapkirin.homesecurity.network.NetworkGlobals
import com.snapkirin.homesecurity.network.http.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserAccount {

    fun getUserFromSharedPref(context: Context): User? {
        val sp = EncryptedSP.getEncryptedSharedPreferences(SHARED_PREF_ACCOUNT, context)
        val userId = sp.getLong(USER_ID, 0)
        val username = sp.getString(USERNAME, null)
        val jwt = sp.getString(JWT, null)
        return if (userId == 0L || username?.isNotBlank() != true || jwt?.isNotBlank() != true) {
             null
        } else {
            User(userId, username, jwt).apply {
                email = sp.getString(EMAIL, null)
            }
        }
    }



    /**
     * Login user with the account info in shared preference.
     * It will also update the shared preference and
     * the jwt in Network Module if the request is successful.
     */
    suspend fun userLogin(context: Context): LoginResult? {
        val sp = EncryptedSP.getEncryptedSharedPreferences(SHARED_PREF_ACCOUNT, context)
        val username = sp.getString(USERNAME, null)
        val password = sp.getString(PASSWORD, null)
        return if (username?.isBlank() == false && password?.isBlank() == false)
            Http.login(IDENTIFIER_USERNAME, username, password).apply {
                if (success && user != null) {
                    NetworkGlobals.jwt = user.jwt
                    withContext(Dispatchers.Main) { saveUserToSharedPref(user, context, password) }
                }
            }
        else
            null
    }

    /**
     * Login user with the provided identifier and password.
     * It will update the jwt in Network Module if the request is successful
     * and also update the shared preference if the context is provided.
     */
    suspend fun userLogin(
        identifierType: Int, identifier: String, password: String, context: Context? = null
    ): LoginResult {
        if (identifierType != HomeSecurity.IDENTIFIER_EMAIL && identifierType != IDENTIFIER_USERNAME) {
        throw IllegalArgumentException("Invalid identifier type")
        }
        return Http.login(identifierType, identifier, password).apply {
            if (success && user != null) {
                NetworkGlobals.jwt = user.jwt
                context?.let {
                    withContext(Dispatchers.Main) { saveUserToSharedPref(user, context, password) }
                }
            }
        }
    }

    fun saveUserToSharedPref(user: User, context: Context, password: String? = null) {
        val sp = EncryptedSP.getEncryptedSharedPreferences(SHARED_PREF_ACCOUNT, context)
        sp.edit {
            putString(JWT, user.jwt)
            putLong(USER_ID, user.userId)
            putString(USERNAME, user.username)
            if (password?.isBlank() == false) {
                putString(PASSWORD, password)
            }
            apply()
        }
    }

    fun clearUserFromSharedPref(context: Context) {
        val sp = EncryptedSP.getEncryptedSharedPreferences(SHARED_PREF_ACCOUNT, context)
        sp.edit {
            remove(JWT)
            remove(USER_ID)
            remove(USERNAME)
            remove(PASSWORD)
            apply()
        }
    }
}