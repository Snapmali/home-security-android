package com.snapkirin.homesecurity.ui.devicebinding

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.HomeSecurity
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicebinding.bluetooth.ConnectionFragment
import com.snapkirin.homesecurity.ui.devicebinding.bluetooth.BluetoothFragment
import com.snapkirin.homesecurity.ui.devicebinding.model.BluetoothEvent
import com.snapkirin.homesecurity.ui.devicebinding.wifi.WifiFragment
import com.snapkirin.homesecurity.ui.util.showDialog
import com.snapkirin.homesecurity.ui.util.showToast


class DeviceBindingActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceBindingActivity"
        const val FRAGMENT_BLUETOOTH = "bluetooth"
        const val FRAGMENT_WIFI = "wifi"
        const val FRAGMENT_BINDING = "binding"
    }

    private lateinit var viewModel: DeviceBindingViewModel

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothReceiver: BroadcastReceiver? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    private lateinit var wifiManager: WifiManager
    private var wifiScanReceiver: BroadcastReceiver? = null

    private var backPressedTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_binding)
        val userId = intent.getLongExtra(HomeSecurity.USER_ID, 0L)
        supportActionBar?.apply {
            setTitle(R.string.bind_device)
            setDisplayHomeAsUpEnabled(true)
        }
        viewModel = ViewModelProvider(this, DeviceBindingViewModelFactory(userId))
            .get(DeviceBindingViewModel::class.java)

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            showToast(R.string.bluetooth_needed)
            finish()
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        initBluetooth()

        if (supportFragmentManager.findFragmentById(R.id.deviceBindingFragmentContainer) == null) {
            startFragment(FRAGMENT_BLUETOOTH)
        }

        if (!bluetoothAdapter.isEnabled) {
            viewModel.bluetoothDisabled()
        }

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            viewModel.wifiDisabled()
        }
        initWifi()

        viewModel.bluetoothEvent.observe(this, Observer {
            val event = it ?: return@Observer
            when (event.event) {
                BluetoothEvent.EVENT_ENABLED -> {
                    if (event.state) {
                        refreshBluetooth()
                        viewModel.refreshBluetoothDeviceList()
                    } else {
                        supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                        showEnableBluetoothDialog()
                    }
                }
                BluetoothEvent.EVENT_CONNECTION -> {
                    if (event.state) {
                        startFragment(FRAGMENT_BINDING)
                    } else {
                        if (event.result == true) {
                            showToast(R.string.success)
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            when (event.failureCode) {
                                BluetoothEvent.FAILURE_BLUETOOTH_CONNECTION -> {
                                    showToast(R.string.bluetooth_connection_failure)
                                    supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                                }
                                BluetoothEvent.FAILURE_READ_MESSAGE, BluetoothEvent.FAILURE_WRITE_MESSAGE -> {
                                    showToast(R.string.bluetooth_communication_failure)
                                    supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                                }
                            }
                            viewModel.bluetoothConnection?.cancel()
                        }
                    }
                }
                BluetoothEvent.EVENT_DEVICE_BINDING_STATUS -> {
                    if (event.state && event.result == false) {
                        showToast(R.string.device_has_been_bound)
                        supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                    }
                }
                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK -> {
                    if (event.result == false) {
                        when (event.failureCode) {
                            BluetoothEvent.FAILURE_NETWORK_NOT_FOUND -> {
                                showToast(R.string.device_cannot_find_wifi)
                                supportFragmentManager.popBackStack(FRAGMENT_WIFI, 0)
                            }
                            BluetoothEvent.FAILURE_NETWORK_CONNECTION_FAILURE -> {
                                showToast(R.string.device_wifi_connection_failure)
                                supportFragmentManager.popBackStack(FRAGMENT_WIFI, 0)
                            }
                        }
                    }
                }
                BluetoothEvent.EVENT_DEVICE_CONNECTING_SERVER -> {
                    if (event.result == false) {
                        showToast(R.string.device_server_connection_failure)
                        supportFragmentManager.popBackStack(FRAGMENT_WIFI, 0)
                    }
                }
                BluetoothEvent.EVENT_DEVICE_BINDING -> {
                    if (event.result == false) {
                        when (event.failureCode) {
                            BluetoothEvent.FAILURE_BINDING_BOUND -> {
                                showToast(R.string.device_has_been_bound)
                                supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                            }
                            BluetoothEvent.FAILURE_BINDING_FAILURE -> {
                                showToast(R.string.bind_device_failed)
                                supportFragmentManager.popBackStack(FRAGMENT_BLUETOOTH, 0)
                            }
                        }
                    }
                }
            }
        })

        viewModel.bluetoothItemDelegate.setItemOnClickListener {
            viewModel.bindingBluetoothDevice = it
            startFragment(FRAGMENT_WIFI)
        }

        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            when (it.resultCode) {
                RESULT_OK -> {
                }
                else -> {
                    showToast(R.string.enable_bluetooth_failed)
                }
            }
        }
    }

    private fun startFragment(fragmentTag: String, addToBackStack: Boolean = true) {
        val newFragment = when (fragmentTag) {
            FRAGMENT_BLUETOOTH -> {
                supportActionBar?.setTitle(R.string.select_device)
                BluetoothFragment.newInstance()
            }
            FRAGMENT_WIFI -> {
                WifiFragment.newInstance()
            }
            else -> ConnectionFragment.newInstance()
        }
        supportFragmentManager
            .beginTransaction().apply {
                setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                replace(R.id.deviceBindingFragmentContainer, newFragment, fragmentTag)
                if (addToBackStack)
                    addToBackStack(fragmentTag)
            }
            .commit()
    }

    private fun showEnableBluetoothDialog() {
        showDialog(R.string.enable_bluetooth, R.string.enable_bluetooth_summary, { _, _ ->
            enableBluetooth()
        }, true, { _, _ ->
            showToast(R.string.enable_bluetooth_failed)
        })
    }

    private fun enableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(intent)
    }

    fun refreshBluetooth() {
        bluetoothAdapter.startDiscovery()
    }

    private fun initBluetooth() {
        val intentFilter = IntentFilter() //创建一个IntentFilter对象
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND) //获得扫描结果
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //扫描结束
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { viewModel.addBluetoothDevice(device) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        viewModel.refreshBluetoothDeviceList()
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        viewModel.refreshBluetoothDeviceListFinished()
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                            BluetoothAdapter.STATE_ON -> viewModel.bluetoothEnabled()
                            BluetoothAdapter.STATE_OFF -> viewModel.bluetoothDisabled()
                        }
                    }
                }
            }
        }
        registerReceiver(bluetoothReceiver, intentFilter)
    }

    fun scanWifi() {
        val success = wifiManager.startScan()
        viewModel.wifiScanStarted()
        if (!success)
            wifiScanFailure()
    }

    fun wifiScanSuccess() {
        viewModel.wifiScanFinished(true)
        viewModel.refreshWifiList(wifiManager.scanResults)
    }

    fun wifiScanFailure() {
        viewModel.wifiScanFinished(false)
        viewModel.refreshWifiList(wifiManager.scanResults)
    }

    private fun initWifi() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                        val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        if (success) {
                            wifiScanSuccess()
                        } else {
                            wifiScanFailure()
                        }
                    }
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        when (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)) {
                            WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_UNKNOWN ->
                                viewModel.wifiDisabled()
                            WifiManager.WIFI_STATE_ENABLED ->
                                viewModel.wifiEnabled()
                        }
                    }
                }
            }
        }
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (!viewModel.bluetoothCommunicating) {
            super.onBackPressed()
            if (supportFragmentManager.findFragmentById(R.id.deviceBindingFragmentContainer) == null)
                super.onBackPressed()
        } else {
            // 蓝牙通信中，按两次返回键退至wifi fragment
            val backPressedTime = System.currentTimeMillis()
            if (backPressedTime - this.backPressedTime > 2000) {
                Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_LONG).show()
                this.backPressedTime = backPressedTime
            } else {
                viewModel.bluetoothConnection?.cancel()
                super.onBackPressed()
                this.backPressedTime = 0
            }
        }
    }

    override fun onDestroy() {
        bluetoothReceiver?.let { unregisterReceiver(it) }
        viewModel.clearLiveData()
        super.onDestroy()
    }

}