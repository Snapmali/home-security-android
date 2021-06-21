package com.snapkirin.homesecurity.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.model.http.ResponseCodes
import com.snapkirin.homesecurity.network.NetworkGlobals
import com.snapkirin.homesecurity.util.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserTokenHandlerService : Service() {

    companion object {
        private const val TAG = "UserTokenHandlerService"
    }

    private val binder = UserReLoginBinder()
    private var loggingIn = false

    private var loginCallback: ((BasicRequestResult) -> Boolean)? = null

    override fun onCreate() {
        Log.i(TAG, "On create")
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "On bind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "On unbind")
        return true
    }

    override fun onDestroy() {
        Log.i(TAG, "On destroy")
        super.onDestroy()
    }

    fun checkJwt(): Boolean {
        val curTime = System.currentTimeMillis() / 1000
        val expireTime = NetworkGlobals.getJwtPayload()?.exp ?: return true
        if (expireTime - curTime < 7200) {
            return false
        }
        return true
    }

    fun reLogin() {
        if (!loggingIn) {
            Log.w(TAG, "Login performed")
            loggingIn = true
            GlobalScope.launch(Dispatchers.IO) {
                val result = UserAccount.userLogin(this@UserTokenHandlerService)
                launch(Dispatchers.Main) {
                    if (result == null) {
                        callForLogin(
                            BasicRequestResult(
                                false,
                                ResponseCodes.Error,
                                R.string.login_again_needed,
                                loginNeeded = true
                            )
                        )
                    } else if (!result.success) {
                        when (result.code) {
                            1, 2 -> {
                                callForLogin(
                                    BasicRequestResult(
                                        false,
                                        result.code,
                                        R.string.login_again_needed,
                                        loginNeeded = true
                                    )
                                )
                            }
                            ResponseCodes.Error, ResponseCodes.NetworkError -> {
                                BasicRequestResult(
                                    false,
                                    result.code,
                                    R.string.network_error
                                )
                            }
                            else -> {
                                BasicRequestResult(
                                    false,
                                    result.code,
                                    R.string.login_again_needed,
                                    loginNeeded = true
                                )
                            }
                        }
                    } else {
                        callForLogin(
                            BasicRequestResult(
                                true,
                                ResponseCodes.Success,
                                R.string.re_login_success
                            )
                        )
                    }
                }
                loggingIn = false
            }
        }
    }

    fun setLoginCallback(callback: (BasicRequestResult) -> Boolean) {
        Log.d(TAG, "Callback is set")
        loginCallback = callback
    }

    private fun callForLogin(result: BasicRequestResult) {
        try {
            loginCallback?.let {
                Log.d(TAG, "Performing callback")
                it(result)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to callback for login", e)
        }
    }

    inner class UserReLoginBinder : Binder() {
        fun getService() : UserTokenHandlerService {
            return this@UserTokenHandlerService
        }
    }
}