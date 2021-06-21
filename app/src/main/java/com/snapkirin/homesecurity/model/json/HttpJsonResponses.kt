package com.snapkirin.homesecurity.model.json

import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.Device
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

interface JsonResponse {
    val message: String
    val code: Int
}

@JsonClass(generateAdapter = true)
data class BasicJsonResponse(
        override val message: String,
        override val code: Int,
) : JsonResponse

@JsonClass(generateAdapter = true)
data class LoginJsonResponse(
        override val message: String,
        override val code: Int,
        val username: String?,
        @Json(name = "user_id") val userId: Long?,
        val token: String?) : JsonResponse

@JsonClass(generateAdapter = true)
data class PullDevicesJsonResponse(
        override val message: String,
        override val code: Int,
        val hosts: List<Device>?
) : JsonResponse

@JsonClass(generateAdapter = true)
data class PullAlarmsJsonResponse(
        override val message: String,
        override val code: Int,
        val offset: Int?,
        val alarms: List<Alarm>?
) : JsonResponse
