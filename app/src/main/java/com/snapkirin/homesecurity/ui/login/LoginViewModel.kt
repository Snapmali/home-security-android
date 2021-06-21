package com.snapkirin.homesecurity.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.snapkirin.homesecurity.HomeSecurity.IDENTIFIER_EMAIL
import com.snapkirin.homesecurity.HomeSecurity.IDENTIFIER_USERNAME

import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.ResponseCodes
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.login.model.AccountInfoFormState
import com.snapkirin.homesecurity.model.http.LoginResult
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.ui.login.model.FragmentTag
import com.snapkirin.homesecurity.ui.login.model.VerificationCodeState
import com.snapkirin.homesecurity.util.IsStringValid
import com.snapkirin.homesecurity.util.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var currentFragment: FragmentTag? = null

    private val _loginForm = MutableLiveData<AccountInfoFormState?>()
    val loginFormState: LiveData<AccountInfoFormState?> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult?>()
    val loginResult: LiveData<LoginResult?> = _loginResult

    private val _registerForm = MutableLiveData<AccountInfoFormState?>()
    val registerFormState: LiveData<AccountInfoFormState?> = _registerForm

    private val _registerInfoResult = MutableLiveData<BasicRequestResult?>()
    val registerInfoResult: LiveData<BasicRequestResult?> = _registerInfoResult

    private val _verificationCodeForm = MutableLiveData<VerificationCodeState?>()
    val verificationCodeState: LiveData<VerificationCodeState?> = _verificationCodeForm

    private val _registerVerificationResult = MutableLiveData<BasicRequestResult?>()
    val registerVerificationResult: LiveData<BasicRequestResult?> =
        _registerVerificationResult

    private val _resendCodeResult = MutableLiveData<BasicRequestResult?>()
    val resendCodeResult: LiveData<BasicRequestResult?> = _resendCodeResult

    fun clearStates(fragmentTag: FragmentTag) {
        when (fragmentTag) {
            FragmentTag.LOGIN -> {
                _loginForm.postValue(null)
                _loginResult.postValue(null)
            }
            FragmentTag.REGISTER_INFO -> {
                _registerForm.postValue(null)
                _registerInfoResult.postValue(null)
            }
            else -> {
                _verificationCodeForm.postValue(null)
                _registerVerificationResult.postValue(null)
                _resendCodeResult.postValue(null)
            }
        }
    }

    fun login(identifier: String, password: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val isEmail = identifier.contains('@')
            val result = if (isEmail) {
                UserAccount.userLogin(IDENTIFIER_EMAIL, identifier, password)
            } else {
                UserAccount.userLogin(IDENTIFIER_USERNAME, identifier, password)
            }
            if (result.success) {
                _loginResult.postValue(result.apply { stringId = R.string.login_success })
            } else {
                when (result.code) {
                    1, 2 -> {
                        _loginResult.postValue(
                            result.apply {
                                stringId = if (isEmail)
                                    R.string.wrong_email
                                else
                                    R.string.wrong_username
                            }
                        )
                        _loginForm.postValue(
                            AccountInfoFormState(
                                identifierError = if (isEmail)
                                    R.string.wrong_email
                                else
                                    R.string.wrong_username
                            )
                        )
                    }
                    ResponseCodes.Error -> {
                        _loginResult.postValue(
                            result.apply {
                                stringId = R.string.network_error
                            }
                        )
                    }
                    else -> _loginResult.postValue(result.apply {
                        stringId = R.string.login_failed
                    })
                }
            }
        }
    }

    fun loginDataChanged(identifier: String, password: String) {
        val identifierState = IsStringValid.isIdentifierValid(identifier)
        val passwordState = IsStringValid.isPasswordValid(password)
        var identifierError: Int? = null
        var passwordError: Int? = null
        when (identifierState) {
            IsStringValid.INVALID_EMAIL -> identifierError = R.string.invalid_email
            IsStringValid.INVALID_USERNAME -> identifierError = R.string.invalid_username
        }
        when (passwordState) {
            IsStringValid.INVALID_PASSWORD -> passwordError = R.string.invalid_password
        }
        if (identifierState + passwordState == IsStringValid.VALID) {
            _loginForm.value = AccountInfoFormState(isDataValid = true)
        } else {
            _loginForm.value = AccountInfoFormState(
                identifierError = identifierError,
                passwordError = passwordError
            )
        }
    }

    fun registerInfo(username: String, email: String, password: String) {
        sendCode(username, email, password, _registerInfoResult, _registerForm)
    }

    private fun sendCode(
        username: String,
        email: String,
        password: String,
        liveData: MutableLiveData<BasicRequestResult?>,
        formState: MutableLiveData<AccountInfoFormState?>?
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.registerInfo(username, email, password)
            if (result.success) {
                liveData.postValue(
                    result.apply {
                        stringId = R.string.verification_code_sent
                    }
                )
            } else {
                when (result.code) {
                    1 -> {
                        liveData.postValue(
                            result.apply {
                                stringId = R.string.username_registered
                            }
                        )
                        formState?.postValue(
                            AccountInfoFormState(usernameError = R.string.username_registered)
                        )
                    }
                    2 -> {
                        liveData.postValue(
                            result.apply {
                                stringId = R.string.email_registered
                            }
                        )
                        formState?.postValue(
                            AccountInfoFormState(emailError = R.string.email_registered)
                        )
                    }
                    ResponseCodes.Error ->
                        liveData.postValue(
                            result.apply {
                                stringId = R.string.network_error
                            }
                        )
                    else ->
                        liveData.postValue(
                            result.apply {
                                stringId = R.string.register_failed
                            }
                        )
                }
            }
        }
    }

    fun registerDataChanged(username: String, email: String, password: String, confirm: String) {
        var usernameError: Int? = null
        var emailError: Int? = null
        var passwordError: Int? = null
        var confirmError: Int? = null
        val usernameState = IsStringValid.isUsernameValid(username)
        val emailState = IsStringValid.isEmailValid(email)
        val passwordState = IsStringValid.isPasswordValid(password)
        val confirmState =
            when  {
                confirm.isBlank() ->
                    IsStringValid.BLANK
                password != confirm -> {
                    confirmError = R.string.password_inconsistent
                    IsStringValid.INVALID_PASSWORD
                }
                else ->
                    IsStringValid.VALID
            }

        when (usernameState) {
            IsStringValid.INVALID_USERNAME -> usernameError = R.string.invalid_username
        }
        when (emailState) {
            IsStringValid.INVALID_EMAIL -> emailError = R.string.invalid_email
        }
        when (passwordState) {
            IsStringValid.INVALID_PASSWORD -> passwordError = R.string.invalid_password
        }
        if (usernameState + emailState + passwordState + confirmState == IsStringValid.VALID) {
            _registerForm.value = AccountInfoFormState(isDataValid = true)
        } else {
            _registerForm.value = AccountInfoFormState(
                usernameError = usernameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError
            )
        }
    }

    fun registerVerification(username: String, verificationCode: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.registerVerification(username, verificationCode)
            if (result.success) {
                _registerVerificationResult.postValue(
                    result.apply {
                        stringId = R.string.register_success
                    }
                )

            } else {
                when (result.code) {
                    1 -> {
                        _registerVerificationResult.postValue(
                            result.apply {
                                stringId = R.string.register_verification_expired
                            }
                        )
                    }
                    2 -> {
                        _registerVerificationResult.postValue(
                            result.apply {
                                stringId = R.string.wrong_verification_code
                            }
                        )
                        _verificationCodeForm.postValue(
                            VerificationCodeState(error = R.string.wrong_verification_code)
                        )
                    }
                    3 -> {
                        _registerVerificationResult.postValue(
                            result.apply {
                                stringId = R.string.user_registered
                            }
                        )
                    }
                    ResponseCodes.Error ->
                        _registerVerificationResult.postValue(
                            result.apply {
                                stringId = R.string.network_error
                            }
                        )
                    else ->
                        _registerVerificationResult.postValue(
                            result.apply {
                                stringId = R.string.verification_failed
                            }
                        )
                }
            }
        }
    }

    fun verificationCodeChanged(verificationCode: String) {
        return when (IsStringValid.isVerificationCodeValid(verificationCode)) {
            IsStringValid.BLANK ->
                _verificationCodeForm.value = VerificationCodeState()
            IsStringValid.INVALID_VERIFICATION_CODE ->
                _verificationCodeForm.value =
                    VerificationCodeState(error = R.string.invalid_verification_code)
            else ->
                _verificationCodeForm.value = VerificationCodeState(isDataValid = true)
        }
    }

    fun resendCode(username: String, email: String, password: String) {
        sendCode(username, email, password, _resendCodeResult, null)
    }
}