package com.snapkirin.homesecurity.network.websocket

import android.util.Log
import com.snapkirin.homesecurity.model.json.AlarmWebSocketMessage
import com.snapkirin.homesecurity.model.json.DeviceStatusWebSocketMessage
import com.snapkirin.homesecurity.network.NetworkGlobals
import com.snapkirin.homesecurity.network.NetworkGlobals.webSocketUrl
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import org.reactivestreams.Subscriber
import java.util.concurrent.TimeUnit

object WebSocket {

    private const val HOST_STATUS = 1
    private const val ALARM = 2

    private const val PING_INTERVAL_SECONDS = 10L

    private const val TAG = "WebSocket"

    val lifecycle = WebSocketLifecycle()
    private val scarlet = Scarlet.Builder()
        .webSocketFactory(
            NetworkGlobals.okHttpClient
                .newBuilder()
                .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .build()
                .newWebSocketFactory(webSocketUrl)
        )
        .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
        .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
        .backoffStrategy(ExponentialWithJitterBackoffStrategy(1000L, 30000L))
        .lifecycle(lifecycle)
        .build()
    private val webSocketService = scarlet.create<WebSocketApis>()
    private val deviceStatusFlowable = webSocketService.observeDeviceStatus()
        .filter {
            Log.d(TAG, "Device status received: ${it.payload}")
            it.type == HOST_STATUS
        }
    private val alarmFlowable = webSocketService.observeAlarm()
        .filter {
            Log.d(TAG, "Alarm received: ${it.payload}")
            it.type == ALARM
        }

    fun subscribeDeviceStatus(subscriber: Subscriber<DeviceStatusWebSocketMessage>) {
        deviceStatusFlowable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscriber)
    }

    fun subscribeAlarm(subscriber: Subscriber<AlarmWebSocketMessage>) {
        alarmFlowable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscriber)
    }
}