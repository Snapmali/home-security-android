package com.snapkirin.homesecurity.ui.resetpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.resetpassword.model.ResetPasswordFormState
import com.snapkirin.homesecurity.ui.util.BaseNetworkRequestViewModel
import com.snapkirin.homesecurity.util.IsStringValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ResetPasswordViewModel : BaseNetworkRequestViewModel() {

    private val _resetPasswordForm = MutableLiveData<ResetPasswordFormState?>()
    val resetPasswordFormState: LiveData<ResetPasswordFormState?> = _resetPasswordForm

    private val _resetPasswordResult = MutableLiveData<BasicRequestResult>()
    val resetPasswordResult: LiveData<BasicRequestResult> = _resetPasswordResult

    fun resetPassword(userId: Long, oldPassword: String, newPassword: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.resetPassword(userId, oldPassword, newPassword)
            if (result.success) {
                _resetPasswordResult.postValue(result.apply { stringId = R.string.login_again_needed })
            } else {
                when (result.code) {
                    1 ->
                        _resetPasswordResult.postValue(result.apply {
                            stringId = R.string.login_again_needed
                        })
                    2 ->
                        _resetPasswordResult.postValue(result.apply {
                            stringId = R.string.wrong_password
                        })
                    else ->
                        _resetPasswordResult.postValue(requestFailureHandler(result.code))
                }
            }
        }
    }

    fun checkFormState(oldPassword: String, newPassword: String, confirmPassword: String) {
        var oldPasswordError: Int? = null
        var newPasswordError: Int? = null
        var confirmPasswordError: Int? = null
        val oldPasswordState = IsStringValid.isPasswordValid(oldPassword)
        val newPasswordState = IsStringValid.isPasswordValid(newPassword)
        val confirmPasswordState =
            when {
                confirmPassword.isBlank() ->
                    IsStringValid.BLANK
                newPassword != confirmPassword -> {
                    confirmPasswordError = R.string.password_inconsistent
                    IsStringValid.INVALID_PASSWORD
                }
                else ->
                    IsStringValid.VALID
            }
        when (oldPasswordState) {
            IsStringValid.INVALID_PASSWORD -> oldPasswordError = R.string.invalid_password
        }
        when (newPasswordState) {
            IsStringValid.INVALID_PASSWORD -> newPasswordError = R.string.invalid_password
        }
        if (oldPasswordState + newPasswordState + confirmPasswordState == IsStringValid.VALID) {
            _resetPasswordForm.value = ResetPasswordFormState(isDataValid = true)
        } else {
            _resetPasswordForm.value = ResetPasswordFormState(
                oldPasswordError = oldPasswordError,
                newPasswordError = newPasswordError,
                confirmError = confirmPasswordError
            )
        }
    }

    override fun clearLiveData() {
        super.clearLiveData()
        _resetPasswordForm.value = null
    }
}