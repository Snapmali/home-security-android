package com.snapkirin.homesecurity.ui.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.model.http.ResponseCodes
import com.snapkirin.homesecurity.network.websocket.WebSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class BaseNetworkRequestViewModel : ViewModel() {

    private val _loginNeeded = MutableLiveData<Boolean?>()
    val loginNeeded: LiveData<Boolean?> = _loginNeeded

    protected fun requestFailureHandler(code: Int): BasicRequestResult {
        return when (code) {
            ResponseCodes.InvalidToken, ResponseCodes.LoginAgainNeeded -> {
                _loginNeeded.postValue(true)
                BasicRequestResult(
                    false,
                    code,
                    R.string.blank,
                    loginNeeded = true
                )
            }
            ResponseCodes.Error, ResponseCodes.NetworkError -> {
                BasicRequestResult(
                    false,
                    code,
                    R.string.network_error
                )
            }
            ResponseCodes.InvalidPassword -> {
                BasicRequestResult(
                    false,
                    code,
                    R.string.invalid_password
                )
            }
            ResponseCodes.InvalidEmail -> {
                BasicRequestResult(
                    false,
                    code,
                    R.string.invalid_email
                )
            }
            else -> {
                BasicRequestResult(
                    false,
                    code,
                    R.string.request_failure
                )
            }
        }
    }

    open fun clearLiveData() {
        _loginNeeded.value = null
    }
}