package com.snapkirin.homesecurity.ui.resetpassword.model

data class ResetPasswordFormState(
    val oldPasswordError: Int? = null,
    val newPasswordError: Int? = null,
    val confirmError: Int? = null,
    val isDataValid: Boolean = false
)
