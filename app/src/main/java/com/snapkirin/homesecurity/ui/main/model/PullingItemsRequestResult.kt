package com.snapkirin.homesecurity.ui.main.model

data class PullingItemsRequestResult (
        val success: Boolean,
        val code: Int,
        var stringId: Int,
        val itemCategory: Int,
        val pullMode: Int,
        var loginNeeded: Boolean = false
)
