package com.snapkirin.homesecurity.network.websocket

import com.snapkirin.homesecurity.model.json.AlarmWebSocketMessage
import com.snapkirin.homesecurity.model.json.DeviceStatusWebSocketMessage
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface WebSocketApis {
    @Send
    fun sendTest(deviceStatusWebSocketMessage: DeviceStatusWebSocketMessage)

    @Receive
    fun observeDeviceStatus(): Flowable<DeviceStatusWebSocketMessage>

    @Receive
    fun observeAlarm(): Flowable<AlarmWebSocketMessage>
}