package com.snapkirin.homesecurity.model.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterJsonRequest(val username: String, val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RegisterVerificationJsonRequest(val username: String, val code: String)

@JsonClass(generateAdapter = true)
data class LoginJsonRequest (@Json(name = "idtf_type") val idtfType: Int, val identifier: String, val password: String)

@JsonClass(generateAdapter = true)
data class StartMonitoringJsonRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "host_id") val deviceId: Long,
    @Json(name = "saving_mode") val savingMode: Int
)

@JsonClass(generateAdapter = true)
data class UserDeviceIdPairJsonRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "host_id") val deviceId: Long
)

@JsonClass(generateAdapter = true)
data class RenameDeviceJsonRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "host_id") val deviceId: Long,
    @Json(name = "screen_name") val screenName: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordJsonRequest(
    @Json(name = "user_id") val userId: Long,
    val old: String,
    val new: String
)
