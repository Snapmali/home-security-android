package com.snapkirin.homesecurity.model.json

import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.DeviceStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

interface WebSocketMessage<T: Any>{
    val type: Int
    val payload: T
}

@JsonClass(generateAdapter = true)
data class BaseWebSocketMessage(
        override val type: Int,
        override val payload: Any
): WebSocketMessage<Any>

@JsonClass(generateAdapter = true)
data class DeviceStatusWebSocketMessage(
        override val type: Int,
        override val payload: DeviceStatus
): WebSocketMessage<DeviceStatus>

@JsonClass(generateAdapter = true)
data class AlarmWebSocketMessage(
        override val type: Int,
        override val payload: Alarm
): WebSocketMessage<Alarm>


