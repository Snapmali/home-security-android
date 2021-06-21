package com.snapkirin.homesecurity.ui.login.model

/**
 * Data validation state of the login form.
 */
data class AccountInfoFormState(
        val identifierError: Int? = null,
        val usernameError: Int? = null,
        val emailError: Int? = null,
        val passwordError: Int? = null,
        val confirmError: Int? = null,
        val isDataValid: Boolean = false
)