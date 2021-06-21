package com.snapkirin.homesecurity.ui.devicedetail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.snapkirin.homesecurity.HomeSecurity.DEVICE_ID
import com.snapkirin.homesecurity.HomeSecurity.USER_ID
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Device
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.service.UserTokenHandlerService
import com.snapkirin.homesecurity.service.WebSocketPushService
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.devicedetail.dialogs.RenameDeviceDialog
import com.snapkirin.homesecurity.model.DeviceList
import com.snapkirin.homesecurity.util.UserAccount
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.OPERATION_MONITOR
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.OPERATION_RENAME_DEVICE
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.OPERATION_START_STREAMING
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.OPERATION_UNBIND_DEVICE
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.REQUEST_MONITORING
import com.snapkirin.homesecurity.ui.player.VideoPlayerActivity
import com.snapkirin.homesecurity.ui.util.showToast

class DeviceDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceDetailActivity"
    }

    private lateinit var device: Device
    private var userId: Long = 0L

    private lateinit var nameText: TextView
    private lateinit var timeText: TextView
    private lateinit var onlineText: TextView
    private lateinit var monitoringText: TextView
    private lateinit var streamingText: TextView
    private lateinit var monitorButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private lateinit var deviceDetailViewModel: DeviceDetailViewModel

    private var userTokenHandlerServiceBinder: UserTokenHandlerService.UserReLoginBinder? = null
    private lateinit var userTokenHandlerServiceIntent: Intent
    private val userTokenHandlerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userTokenHandlerServiceBinder = service as UserTokenHandlerService.UserReLoginBinder
            userTokenHandlerServiceBinder?.getService()?.setLoginCallback {
                reLoginHandler(it)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "UserTokenHandlerService disconnected")
            userTokenHandlerServiceBinder = null
        }

    }

    private var webSocketPushServiceBinder: WebSocketPushService.AlarmPushBinder? = null
    private lateinit var webSocketPushServiceIntent: Intent
    private val webSocketPushServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            webSocketPushServiceBinder = service as WebSocketPushService.AlarmPushBinder
            webSocketPushServiceBinder?.getService()?.apply {
                setAlarmCallback(deviceDetailViewModel.alarmUpdateCallback)
                setDeviceStatusCallback(deviceDetailViewModel.deviceStatusUpdateCallback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "WebSocketService disconnected")
            webSocketPushServiceBinder = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_detail)
        userId = intent.getLongExtra(USER_ID, 0L)
        if (userId == 0L) {
            userId = UserAccount.getUserFromSharedPref(this)!!.userId
        }
        val deviceId = intent.getLongExtra(DEVICE_ID, 0L)
        device = DeviceList.get(deviceId)!!

        supportActionBar?.apply {
            setTitle(R.string.device_detail)
            setDisplayHomeAsUpEnabled(true)
        }

        nameText = findViewById(R.id.deviceDetailNameText)
        timeText = findViewById(R.id.deviceDetailTimeText)
        onlineText = findViewById(R.id.deviceDetailIsOnlineText)
        monitoringText = findViewById(R.id.deviceDetailIsMonitoringText)
        streamingText = findViewById(R.id.deviceDetailIsStreamingText)
        monitorButton = findViewById(R.id.deviceDetailMonitorButton)
        tabLayout = findViewById(R.id.deviceDetailTabLayout)
        viewPager = findViewById(R.id.deviceDetailViewPager)

        userTokenHandlerServiceIntent = Intent(this, UserTokenHandlerService::class.java)
        startService(userTokenHandlerServiceIntent)
        bindService(userTokenHandlerServiceIntent, userTokenHandlerServiceConnection, BIND_AUTO_CREATE)

        webSocketPushServiceIntent = Intent(this, WebSocketPushService::class.java)
        startService(webSocketPushServiceIntent)
        bindService(webSocketPushServiceIntent, webSocketPushServiceConnection,BIND_AUTO_CREATE)

        deviceDetailViewModel = ViewModelProvider(
            this,
            DeviceDetailViewModelFactory(userId, device)
        ).get(DeviceDetailViewModel::class.java)
        observeLiveData()

        refreshViews(device)

        viewPager.adapter = DeviceDetailViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.alarm)
                1 -> tab.text = getString(R.string.options)
            }
        }.attach()

        nameText.setOnClickListener {
            showRenameDialog()
        }

        monitorButton.setOnClickListener {
            if (!device.monitoring) {
                showMonitorDialog()
            } else {
                deviceDetailViewModel.stopMonitoring()
                setMonitorLoading()
            }
        }
    }

    private fun observeLiveData() {
        deviceDetailViewModel.apply {
            deviceStatusChanged.observe(this@DeviceDetailActivity, Observer {
                it ?: return@Observer
                refreshViews(device)

            })

            operationRequestResult.observe(this@DeviceDetailActivity, Observer {
                val result = it ?: return@Observer
                when (result.operation) {
                    OPERATION_RENAME_DEVICE -> {
                        showToast(result.stringId)
                        if (result.success) {
                            nameText.text = device.name
                        }
                    }
                    OPERATION_MONITOR -> {
                        if (!result.success) {
                            showToast(result.stringId)
                        }
                    }
                    OPERATION_START_STREAMING -> {
                        if (result.success) {
                            startPlayerActivity(userId, device.id)
                        } else {
                            startPlayerActivity(userId, device.id)
                            showToast(result.stringId)
                        }
                    }
                    OPERATION_UNBIND_DEVICE -> {
                        if (result.success) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            showToast(result.stringId)
                        }
                    }
                }
            })

            loginNeeded.observe(this@DeviceDetailActivity, Observer {
                it ?: return@Observer
                if (it) {
                    userTokenHandlerServiceBinder?.getService()?.reLogin()
                }
            })

            operationStateChanged.observe(this@DeviceDetailActivity, Observer {
                it ?: return@Observer
                if (it == 0) {
                    cancelMonitorLoading()
                } else if (deviceDetailViewModel.checkRequestOperationState(REQUEST_MONITORING)){
                    setMonitorLoading()
                } else {
                    monitorButton.isEnabled = false
                }
            })
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun refreshViews(device: Device) {
        nameText.text = device.name
        timeText.text = device.bindingTimeString
        onlineText.apply {
            if (device.online) {
                setText(R.string.is_online)
                setTextAppearance(R.style.DeviceStatusActivated)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    getDrawable(R.drawable.ic_baseline_wifi_24),
                    null, null, null
                )
            } else {
                setText(R.string.is_offline)
                setTextAppearance(R.style.DeviceStatusDefault)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    getDrawable(R.drawable.ic_baseline_wifi_off_24),
                    null, null, null
                )
            }
        }
        monitoringText.apply {
            if (device.monitoring) {
                setText(R.string.is_monitoring)
                setTextAppearance(R.style.DeviceStatusActivated)
            } else {
                setText(R.string.is_not_monitoring)
                setTextAppearance(R.style.DeviceStatusDefault)
            }
        }
        streamingText.apply {
            if (device.streaming) {
                setText(R.string.is_streaming)
                setTextAppearance(R.style.DeviceStatusActivated)
            } else {
                setText(R.string.is_not_streaming)
                setTextAppearance(R.style.DeviceStatusDefault)
            }
        }
        monitorButton.apply {
            if (deviceDetailViewModel.requestState == 0) {
                isEnabled = device.online
            }
            text = if (!device.monitoring) {
                getString(R.string.start_monitoring)
            } else {
                getString(R.string.stop_monitoring)
            }
        }
    }

    private fun setMonitorLoading() {
        monitorButton.isEnabled = false
    }

    private fun cancelMonitorLoading() {
        monitorButton.isEnabled = true
    }

    private fun showMonitorDialog() {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.apply {
            setTitle(R.string.select_capture_saving_mode)
            setItems(R.array.capture_saving_modes) { _, which ->
                when (which) {
                    0 -> {
                        setMonitorLoading()
                        deviceDetailViewModel.startMonitoringAlwaysSave()
                    }
                    1 -> {
                        setMonitorLoading()
                        deviceDetailViewModel.startMonitoringSaveWhenMoving()
                    }
                }
            }
        }
        dialogBuilder.show()
    }

    private fun showRenameDialog() {
        val dialog = RenameDeviceDialog()
        dialog.show(supportFragmentManager, "rename_device_dialog")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun reLoginHandler(result: BasicRequestResult): Boolean {
        showToast(result.stringId)
        if (result.loginNeeded) {
            startLoginActivity()
        }
        return true
    }

    private fun startPlayerActivity(userId: Long, deviceId: Long, bundle: Bundle? = null) {
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra(USER_ID, userId)
            putExtra(DEVICE_ID, deviceId)
            if (bundle != null)
                putExtras(bundle)
        }
        startActivity(intent)
    }

    /**
     * Start the Login Activity and finish all other activities.
     */
    private fun startLoginActivity(bundle: Bundle? = null) {
        WebSocket.lifecycle.stop()
        val intent = Intent(this, LoginActivity::class.java).apply {
            if (bundle != null) {
                putExtras(bundle)
            }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        userTokenHandlerServiceBinder?.getService()?.setLoginCallback {
            reLoginHandler(it)
        }
        webSocketPushServiceBinder?.getService()?.apply {
            setAlarmCallback(deviceDetailViewModel.alarmUpdateCallback)
            setDeviceStatusCallback(deviceDetailViewModel.deviceStatusUpdateCallback)
        }
    }

    override fun onStop() {
        deviceDetailViewModel.clearLiveData()
        super.onStop()
    }

    override fun onDestroy() {
        unbindService(userTokenHandlerServiceConnection)
        unbindService(webSocketPushServiceConnection)
        super.onDestroy()
    }
}