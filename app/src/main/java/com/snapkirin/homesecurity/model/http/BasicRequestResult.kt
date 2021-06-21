package com.snapkirin.homesecurity.model.http

data class BasicRequestResult (
        val success: Boolean,
        val code: Int,
        var stringId: Int = 0,
        var operation: Int = 0,
        val loginNeeded: Boolean = false
        )