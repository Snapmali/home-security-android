package com.snapkirin.homesecurity.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceStatus(
    @Json(name = "host_id")
    val deviceId: Long,
    var online: Boolean,
    var streaming: Boolean,
    var monitoring: Boolean
)
