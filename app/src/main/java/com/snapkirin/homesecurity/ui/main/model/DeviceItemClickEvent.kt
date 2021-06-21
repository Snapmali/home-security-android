package com.snapkirin.homesecurity.ui.main.model

import com.snapkirin.homesecurity.model.Device

data class DeviceItemClickEvent(
    val show: Boolean = false,
    val userId: Long? = null,
    val device: Device? = null,
    val index: Int? = null
)
