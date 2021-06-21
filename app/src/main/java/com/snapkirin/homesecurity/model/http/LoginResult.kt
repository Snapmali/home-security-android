package com.snapkirin.homesecurity.model.http

import com.snapkirin.homesecurity.model.User

data class LoginResult(
        val success: Boolean,
        val code: Int,
        var stringId: Int,
        val user: User? = null
)