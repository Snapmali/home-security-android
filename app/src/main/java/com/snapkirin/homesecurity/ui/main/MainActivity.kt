package com.snapkirin.homesecurity.ui.main

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.snapkirin.homesecurity.HomeSecurity.DEVICE_ID
import com.snapkirin.homesecurity.HomeSecurity.USER
import com.snapkirin.homesecurity.HomeSecurity.USER_ID
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.network.websocket.WebSocket
import com.snapkirin.homesecurity.service.WebSocketPushService
import com.snapkirin.homesecurity.service.UserTokenHandlerService
import com.snapkirin.homesecurity.ui.login.LoginActivity
import com.snapkirin.homesecurity.ui.util.showToast
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailActivity
import com.snapkirin.homesecurity.util.UserAccount
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingActivity
import com.snapkirin.homesecurity.ui.resetpassword.ResetPasswordActivity
import com.snapkirin.homesecurity.ui.util.showDialog

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var mainViewModel: MainViewModel

    private lateinit var fineLocationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var addDeviceButton: FloatingActionButton

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

    private var webSocketPushBinder: WebSocketPushService.AlarmPushBinder? = null
    private lateinit var webSocketPushServiceIntent: Intent
    private val webSocketPushServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            webSocketPushBinder = service as WebSocketPushService.AlarmPushBinder
            webSocketPushBinder?.getService()?.apply {
                setAlarmCallback(mainViewModel.alarmUpdateCallback)
                setDeviceStatusCallback(mainViewModel.deviceStatusUpdateCallback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "WebSocketService disconnected")
            webSocketPushBinder = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        addDeviceButton = findViewById(R.id.addDeviceButton)

        userTokenHandlerServiceIntent = Intent(this, UserTokenHandlerService::class.java)
        startService(userTokenHandlerServiceIntent)
        bindService(
            userTokenHandlerServiceIntent,
            userTokenHandlerServiceConnection,
            BIND_AUTO_CREATE
        )

        webSocketPushServiceIntent = Intent(this, WebSocketPushService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(webSocketPushServiceIntent)
        } else {
            startService(webSocketPushServiceIntent)
        }
        bindService(webSocketPushServiceIntent, webSocketPushServiceConnection, BIND_AUTO_CREATE)

        mainViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                intent.getParcelableExtra(USER) ?: UserAccount.getUserFromSharedPref(this)!!
            )
        ).get(MainViewModel::class.java)

        initPermissionLaunchers()

        mainViewModel.requestDataResult.observe(this, Observer {
            val result = it ?: return@Observer
            if (!result.success) {
                showToast(result.stringId)
            }
        })

        mainViewModel.loginNeeded.observe(this, Observer {
            it ?: return@Observer
            if (it) {
                userTokenHandlerServiceBinder?.getService()?.reLogin()
            }
        })

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_alarms, R.id.navigation_devices, R.id.navigation_options
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_devices -> {
                    addDeviceButton.show()
                }
                else -> {
                    addDeviceButton.hide()
                }
            }
        }

        addDeviceButton.setOnClickListener {
            startBindDeviceActivityWithCheckingPermission()
        }
    }

    fun reLoginHandler(result: BasicRequestResult): Boolean {
        showToast(result.stringId)
        if (result.loginNeeded) {
            startLoginActivity()
        }
        return true
    }

    private fun initPermissionLaunchers() {
        fineLocationPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    startBindDeviceActivity(mainViewModel.user.userId)
                } else {
                    showDialog(
                        R.string.fine_location_permission_required,
                        R.string.request_fine_location_permission_summary,
                        hasNegativeButton = false
                    )
                }
            }
    }

    /**
     * Grant location permission before starting Bind Device Activity
     */
    fun startBindDeviceActivityWithCheckingPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startBindDeviceActivity(mainViewModel.user.userId)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showDialog(R.string.request_location_permission,
                    R.string.request_location_permission_summary,
                    { _, _ ->
                        fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    })
            }
            else -> {
                fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Start the Device Detail Activity without finishing this activity.
     */
    fun startDeviceDetailActivity(userId: Long, deviceId: Long, bundle: Bundle? = null) {
        val intent = Intent(this, DeviceDetailActivity::class.java).apply {
            putExtra(USER_ID, userId)
            putExtra(DEVICE_ID, deviceId)
            if (bundle != null) {
                putExtras(bundle)
            }
        }
        startActivity(intent)
    }

    /**
     * Start the Login Activity and finish this activity.
     */
    fun startLoginActivity(bundle: Bundle? = null) {
        WebSocket.lifecycle.stop()
        val intent = Intent(this, LoginActivity::class.java).apply {
            if (bundle != null) {
                putExtras(bundle)
            }
        }
        startActivity(intent)
        stopService(userTokenHandlerServiceIntent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    fun startResetPasswordActivity(bundle: Bundle? = null) {
        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
            putExtra(USER_ID, mainViewModel.user.userId)
            if (bundle != null) {
                putExtras(bundle)
            }
        }
        startActivity(intent)
    }

    private fun startBindDeviceActivity(userId: Long, bundle: Bundle? = null) {
        val intent = Intent(this, DeviceBindingActivity::class.java).apply {
            putExtra(USER_ID, userId)
            if (bundle != null) {
                putExtras(bundle)
            }
        }
        startActivity(intent)
    }

    fun startNotificationSettings() {
        val intent = Intent()
        try {
            intent.apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
            }
            startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Start Notification Settings error", e)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.putExtra("package", packageName)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        userTokenHandlerServiceBinder?.getService()?.setLoginCallback { reLoginHandler(it) }
        webSocketPushBinder?.getService()?.apply {
            setAlarmCallback(mainViewModel.alarmUpdateCallback)
            setDeviceStatusCallback(mainViewModel.deviceStatusUpdateCallback)
        }
    }

    override fun onStop() {
        mainViewModel.clearLiveData()
        super.onStop()
    }

    override fun onDestroy() {
        unbindService(userTokenHandlerServiceConnection)
        unbindService(webSocketPushServiceConnection)
        super.onDestroy()
    }
}