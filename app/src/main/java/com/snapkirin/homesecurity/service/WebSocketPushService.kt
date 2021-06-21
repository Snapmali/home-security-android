package com.snapkirin.homesecurity.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.snapkirin.homesecurity.HomeSecurity.DEVICE_ID
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.DeviceStatus
import com.snapkirin.homesecurity.model.json.AlarmWebSocketMessage
import com.snapkirin.homesecurity.model.json.DeviceStatusWebSocketMessage
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.ui.main.MainActivity
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailActivity
import com.snapkirin.homesecurity.model.DeviceList
import com.snapkirin.homesecurity.network.NetworkGlobals
import com.snapkirin.homesecurity.util.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

class WebSocketPushService : Service() {

    companion object {
        private const val TAG = "WebSocketPushService"
        private const val ALARM_OBSERVER_TAG = "AlarmFlowableSubscriber"
        private const val DEVICE_OBSERVER_TAG = "DeviceFlowableSubscriber"

        private const val NOTIFICATION_CHANNEL_ONGOING = "on_going"
        private const val NOTIFICATION_CHANNEL_ALARM = "alarm"
        private const val NOTIFICATION_CHANNEL_DEVICE_STATUS = "device_status"
    }

    private val binder = AlarmPushBinder()
    private lateinit var alarmSubscription: Subscription
    private lateinit var deviceStatusSubscription: Subscription

    private var alarmCallback: ((Alarm) -> Boolean)? = null
    private var deviceStatusCallback: ((Int, DeviceStatus) -> Boolean)? = null

    private lateinit var notificationManager: NotificationManager
    private var notificationId = 1

    override fun onCreate() {
        Log.i(TAG, "On create")
        super.onCreate()
        createNotificationChannels()
        startForeground(notificationId, createOngoingNotification())
        notificationId++
        launchWS()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "On bind")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "On unbind")
        return true
    }

    override fun onDestroy() {
        Log.i(TAG, "On destroy")
        alarmSubscription.cancel()
        deviceStatusSubscription.cancel()
        super.onDestroy()
    }

    private fun launchWS() {
        GlobalScope.launch(Dispatchers.IO) {
            if (NetworkGlobals.jwt.isBlank())
                UserAccount.userLogin(this@WebSocketPushService)
            WebSocket.lifecycle.start()
        }
        observeAlarmFromWS()
        observeDeviceStatusFromWS()
    }

    private fun observeAlarmFromWS() {
        val subscriber  = object : Subscriber<AlarmWebSocketMessage> {
            override fun onSubscribe(s: Subscription) {
                Log.i(ALARM_OBSERVER_TAG, "On subscribe")
                s.request(Long.MAX_VALUE)
                alarmSubscription = s
            }

            override fun onNext(message: AlarmWebSocketMessage) {
                Log.d(ALARM_OBSERVER_TAG, "On next: $message")
                pushAlarmNotification(message.payload)
                performAlarmCallback(message.payload)
            }

            override fun onError(t: Throwable) {
                Log.w(ALARM_OBSERVER_TAG, "On error: ", t)
            }

            override fun onComplete() {
                Log.i(ALARM_OBSERVER_TAG, "On complete")
            }
        }
        WebSocket.subscribeAlarm(subscriber)
    }

    fun setAlarmCallback(callback: (Alarm) -> Boolean) {
        Log.d(TAG, "Alarm callback is set")
        alarmCallback = callback
    }

    private fun performAlarmCallback(message: Alarm) {
        try {
            alarmCallback?.let {
                Log.d(TAG, "Performing alarm callback")
                it(message)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to callback for new alarm", e)
        }
    }

    private fun observeDeviceStatusFromWS() {
        val subscriber  = object : Subscriber<DeviceStatusWebSocketMessage> {
            override fun onSubscribe(s: Subscription) {
                Log.d(DEVICE_OBSERVER_TAG, "On subscribe")
                s.request(Long.MAX_VALUE)
                deviceStatusSubscription = s
            }

            override fun onNext(message: DeviceStatusWebSocketMessage) {
                Log.d(DEVICE_OBSERVER_TAG, "On next: $message")
                val device = DeviceList.get(message.payload.deviceId)
                if (device!= null && device.online != message.payload.online) {
                    pushDeviceStatusNotification(message.payload, device.name)
                }
                DeviceList.updateDeviceStatus(message.payload)?.let {
                    performDeviceStatusCallback(it, message.payload)
                }
            }

            override fun onError(t: Throwable) {
                Log.w(DEVICE_OBSERVER_TAG, "On error: ", t)
            }

            override fun onComplete() {
                Log.d(DEVICE_OBSERVER_TAG, "On complete")
            }
        }
        WebSocket.subscribeDeviceStatus(subscriber)
    }

    fun setDeviceStatusCallback(callback: (Int, DeviceStatus) -> Boolean) {
        Log.d(TAG, "Device status callback is set")
        deviceStatusCallback = callback
    }

    private fun performDeviceStatusCallback(index: Int, status: DeviceStatus) {
        try {
            deviceStatusCallback?.let {
                Log.d(TAG, "Performing device status callback")
                it(index, status)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to callback for new device status", e)
        }
    }

    private fun createNotificationChannels() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ONGOING,
                        getString(R.string.ongoing_notification),
                        NotificationManager.IMPORTANCE_LOW
                    ),
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ALARM,
                        getString(R.string.alarm),
                        NotificationManager.IMPORTANCE_HIGH
                    ),
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_DEVICE_STATUS,
                        getString(R.string.device_status),
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            )
        }
    }

    private fun pushAlarmNotification(alarm: Alarm) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ALARM).apply {
            when (alarm.type) {
                Alarm.MOTION_ALARM -> {
                    setContentTitle(getString(R.string.motion_alarm))
                    setSmallIcon(R.drawable.ic_baseline_person_24)
                }
                Alarm.SMOKE_ALARM -> {
                    setContentTitle(getString(R.string.smoke_alarm))
                    setSmallIcon(R.drawable.ic_baseline_local_fire_department_24)
                }
            }
            setContentText("${alarm.timeString} ${DeviceList.getDeviceName(alarm.deviceId)}")
            setAutoCancel(true)
            setContentIntent(pendingIntent)
        }
            .build()
        notificationManager.notify(notificationId, notification)
        notificationId++
    }

    private fun pushDeviceStatusNotification(status: DeviceStatus, deviceName: String) {
        val intent = Intent(this, DeviceDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DEVICE_ID, status.deviceId)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_DEVICE_STATUS).apply {
            if (status.online) {
                setSmallIcon(R.drawable.ic_baseline_wifi_24)
                setContentTitle(getString(R.string.device_online_action))
            } else {
                setSmallIcon(R.drawable.ic_baseline_wifi_off_24)
                setContentTitle(getString(R.string.device_offline_action))
            }
            setContentText("${getString(R.string.device_screen_name)}: $deviceName")
            setAutoCancel(true)
            setContentIntent(pendingIntent)
        }
            .build()
        notificationManager.notify(notificationId, notification)
        notificationId++
    }

    private fun createOngoingNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ONGOING).apply {
            setSmallIcon(R.drawable.ic_baseline_home_24)
            setContentTitle(getString(R.string.app_name))
            setContentText(getString(R.string.ongoing_notification_content))
            setOngoing(true)
            setContentIntent(pendingIntent)
        }
            .build()
    }

    inner class AlarmPushBinder : Binder() {
        fun getService(): WebSocketPushService = this@WebSocketPushService
    }

}