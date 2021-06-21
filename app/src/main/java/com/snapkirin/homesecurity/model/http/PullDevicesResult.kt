package com.snapkirin.homesecurity.model.http

import com.snapkirin.homesecurity.model.Device

data class PullDevicesResult (
        val success: Boolean,
        val code: Int,
        var stringId: Int,
        val devices: List<Device>? = null
        )