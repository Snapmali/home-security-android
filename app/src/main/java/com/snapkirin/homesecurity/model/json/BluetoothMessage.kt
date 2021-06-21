package com.snapkirin.homesecurity.model.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BindDeviceBTMessage(
    val type: Int,
    val ssid: String,
    @Json(name = "key_management") val keyManagement: List<String>,
    val cipher: String,
    @Json(name = "wifi_password") val wifiPassword: String?,
    @Json(name = "user_id") val userId: Long
)

@JsonClass(generateAdapter = true)
data class DeviceBTResponse(
    val type: Int,
    val message: String,
    val code: Int
)