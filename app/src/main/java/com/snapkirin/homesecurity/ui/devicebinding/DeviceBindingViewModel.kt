package com.snapkirin.homesecurity.ui.devicebinding

import android.bluetooth.BluetoothDevice
import android.net.wifi.ScanResult
import android.os.*
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.snapkirin.homesecurity.network.bluetooth.BluetoothConnection
import com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model.BluetoothDisabledItemDelegate
import com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model.BluetoothDeviceItemDelegate
import com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model.BluetoothListItem
import com.snapkirin.homesecurity.ui.devicebinding.model.BluetoothEvent
import com.snapkirin.homesecurity.ui.devicebinding.model.WifiEvent
import com.snapkirin.homesecurity.ui.devicebinding.wifi.model.WifiItemDelegate
import com.snapkirin.homesecurity.ui.devicebinding.wifi.model.WifiListItem
import com.snapkirin.homesecurity.ui.devicebinding.wifi.model.WlanDisabledItemDelegate
import com.snapkirin.homesecurity.util.IsStringValid


class DeviceBindingViewModel(val userId: Long) : ViewModel() {
    val bluetoothItems = mutableListOf<BluetoothListItem>()
    val bluetoothItemDelegate = BluetoothDeviceItemDelegate()
    var bindingBluetoothDevice: BluetoothDevice? = null
    var bluetoothConnection: BluetoothConnection? = null

    private val _bluetoothEvent = MutableLiveData<BluetoothEvent?>()
    val bluetoothEvent: LiveData<BluetoothEvent?> = _bluetoothEvent
    var bluetoothCommunicating: Boolean = false

    val bluetoothListAdapter = MultiTypeAdapter().apply {
        register(BluetoothListItem::class.java).to(
            bluetoothItemDelegate,
            BluetoothDisabledItemDelegate()
        ).withKotlinClassLinker { _, item ->
            if (!item.isDisabledItem)
                BluetoothDeviceItemDelegate::class
            else
                BluetoothDisabledItemDelegate::class
        }
        items = bluetoothItems
    }

    val wifiItems = mutableListOf<WifiListItem>()
    val wifiItemDelegate = WifiItemDelegate()
    var connectingWifi: ScanResult? = null

    private val _wifiEvent = MutableLiveData<WifiEvent?>()
    val wifiEvent: LiveData<WifiEvent?> = _wifiEvent

    val wifiListAdapter = MultiTypeAdapter().apply {
        register(WifiListItem::class.java).to(
            wifiItemDelegate,
            WlanDisabledItemDelegate()
        ).withKotlinClassLinker { _, item ->
            if (!item.isDisabledItem)
                WifiItemDelegate::class
            else
                WlanDisabledItemDelegate::class
        }
        items = wifiItems
    }

    init {
        val handler = object : Handler(Looper.getMainLooper()) {
            var binding = true
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Log.e("bind_message", "${msg.what} ${msg.arg1} ${msg.arg2} ${msg.obj}")
                when (msg.what) {
                    BluetoothConnection.MESSAGE_CONNECTION_SUCCESS -> {
                        _bluetoothEvent.postValue(
                            BluetoothEvent(
                                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK,
                                true
                            )
                        )
                    }
                    BluetoothConnection.MESSAGE_READ -> {
                        when (msg.arg1) {
                            BluetoothConnection.RECV_MSG_BINDING_STATUS -> {
                                when (msg.arg2) {
                                    // Device not bound to any users
                                    0 -> {
                                        binding = true
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_BINDING_STATUS,
                                                state = false,
                                                result = false
                                            )
                                        )
                                    }
                                    // Device bound to this user
                                    1 -> {
                                        binding = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_BINDING_STATUS,
                                                state = true,
                                                result = true
                                            )
                                        )
                                    }
                                    // Device bound to another user
                                    2 -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_BINDING_STATUS,
                                                state = true,
                                                result = false,
                                                failureCode = BluetoothEvent.FAILURE_BINDING_BOUND
                                            )
                                        )
                                        bluetoothConnection?.cancel()
                                    }
                                }
                            }
                            BluetoothConnection.RECV_MSG_NETWORK -> {
                                when (msg.arg2) {
                                    // Device successfully connected to the wifi
                                    0 -> {
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK,
                                                false,
                                                result = true
                                            )
                                        )
                                    }
                                    // Device failed to connect to the wifi
                                    1 -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK,
                                                false,
                                                result = false,
                                                failureCode = BluetoothEvent.FAILURE_NETWORK_CONNECTION_FAILURE
                                            )
                                        )
                                        bluetoothConnection?.cancel()
                                    }
                                    // Wifi not found
                                    2 -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK,
                                                false,
                                                result = false,
                                                failureCode = BluetoothEvent.FAILURE_NETWORK_NOT_FOUND
                                            )
                                        )
                                        bluetoothConnection?.cancel()
                                    }
                                }
                            }
                            BluetoothConnection.RECV_MSG_LOGIN -> {
                                when (msg.arg2) {
                                    // Device successfully connected to the server
                                    0 -> {
                                        if (!binding) {
                                            bluetoothCommunicating = false
                                            _bluetoothEvent.postValue(
                                                BluetoothEvent(
                                                    BluetoothEvent.EVENT_CONNECTION,
                                                    false,
                                                    result = true
                                                )
                                            )
                                            bluetoothConnection?.cancel()
                                        }
                                        else {
                                            _bluetoothEvent.postValue(
                                                BluetoothEvent(
                                                    BluetoothEvent.EVENT_DEVICE_CONNECTING_SERVER,
                                                    false,
                                                    result = true
                                                )
                                            )
                                        }
                                    }
                                    // Device failed to connect to the server
                                    else -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_CONNECTING_SERVER,
                                                false,
                                                result = false
                                            )
                                        )
                                        bluetoothConnection?.cancel()
                                    }
                                }
                            }
                            BluetoothConnection.RECV_MSG_BINDING -> {
                                when (msg.arg2) {
                                    // Bind successfully
                                    0 -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_CONNECTION,
                                                false,
                                                result = true
                                            )
                                        )
                                    }
                                    // Device bound to another user
                                    1 -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_BINDING,
                                                false,
                                                result = false,
                                                failureCode = BluetoothEvent.FAILURE_BINDING_BOUND
                                            )
                                        )
                                    }
                                    // Other failure
                                    else -> {
                                        bluetoothCommunicating = false
                                        _bluetoothEvent.postValue(
                                            BluetoothEvent(
                                                BluetoothEvent.EVENT_DEVICE_BINDING,
                                                false,
                                                result = false,
                                                failureCode = BluetoothEvent.FAILURE_BINDING_FAILURE
                                            )
                                        )
                                    }
                                }
                                bluetoothConnection?.cancel()
                            }
                        }
                    }
                    BluetoothConnection.MESSAGE_CONNECTION_FAILURE -> {
                        bluetoothCommunicating = false
                        _bluetoothEvent.postValue(
                            BluetoothEvent(
                                BluetoothEvent.EVENT_CONNECTION,
                                false,
                                result = false,
                                failureCode = BluetoothEvent.FAILURE_BLUETOOTH_CONNECTION
                            )
                        )
                        bluetoothConnection?.cancel()
                    }
                    BluetoothConnection.MESSAGE_READ_FAILURE -> {
                        bluetoothCommunicating = false
                        _bluetoothEvent.postValue(
                            BluetoothEvent(
                                BluetoothEvent.EVENT_CONNECTION,
                                false,
                                result = false,
                                failureCode = BluetoothEvent.FAILURE_READ_MESSAGE
                            )
                        )
                        bluetoothConnection?.cancel()
                    }
                    BluetoothConnection.MESSAGE_WRITE_FAILURE -> {
                        bluetoothCommunicating = false
                        _bluetoothEvent.postValue(
                            BluetoothEvent(
                                BluetoothEvent.EVENT_CONNECTION,
                                false,
                                result = false,
                                failureCode = BluetoothEvent.FAILURE_WRITE_MESSAGE
                            )
                        )
                        bluetoothConnection?.cancel()
                    }
                }
            }
        }
        bluetoothConnection = BluetoothConnection(handler)
    }

    fun bluetoothEnabled() {
        clearBluetoothDeviceList()
        _bluetoothEvent.value = BluetoothEvent(BluetoothEvent.EVENT_ENABLED, true)
    }

    fun bluetoothDisabled() {
        bluetoothItems.clear()
        bluetoothItems.add(BluetoothListItem(isDisabledItem = true))
        bluetoothListAdapter.notifyDataSetChanged()
        _bluetoothEvent.value = BluetoothEvent(BluetoothEvent.EVENT_ENABLED, false)
    }

    fun refreshBluetoothDeviceList() {
        _bluetoothEvent.value = BluetoothEvent(BluetoothEvent.EVENT_REFRESHING, true)
        clearBluetoothDeviceList()
    }

    fun refreshBluetoothDeviceListFinished() {
        _bluetoothEvent.value = BluetoothEvent(BluetoothEvent.EVENT_REFRESHING, false)
    }

    private fun clearBluetoothDeviceList() {
        bluetoothItems.clear()
        bluetoothListAdapter.notifyDataSetChanged()
    }

    fun addBluetoothDevice(device: BluetoothDevice) {
        if (IsStringValid.isBluetoothNameValid(device.name) != IsStringValid.VALID)
            return
        device.fetchUuidsWithSdp()
        val deviceItem = BluetoothListItem(device)
        if (!bluetoothItems.contains(deviceItem)) {
            bluetoothItems.add(deviceItem)
            bluetoothListAdapter.notifyItemInserted(bluetoothItems.size - 1)
        }
    }

    fun wifiEnabled() {
        clearWifiList()
        _wifiEvent.value = WifiEvent(WifiEvent.ENABLED, true)
    }

    fun wifiDisabled() {
        wifiItems.clear()
        wifiItems.add(WifiListItem(isDisabledItem = true))
        wifiListAdapter.notifyDataSetChanged()
        _wifiEvent.value = WifiEvent(WifiEvent.ENABLED, false)
    }

    fun wifiScanStarted() {
        _wifiEvent.value = WifiEvent(WifiEvent.REFRESHING, true)
    }

    fun wifiScanFinished(success: Boolean) {
        _wifiEvent.value = WifiEvent(WifiEvent.REFRESHING, false, result = success)
    }

    private fun clearWifiList() {
        wifiItems.clear()
        wifiListAdapter.notifyDataSetChanged()
    }

    fun refreshWifiList(newWifiList: List<ScanResult>) {
        val newWifiItems = mutableListOf<WifiListItem>()
        val ssids = mutableSetOf<String>()
        for (wifi in newWifiList) {
            if (!ssids.contains(wifi.SSID)) {
                newWifiItems.add(WifiListItem(wifi))
                ssids.add(wifi.SSID)
            }
        }
        wifiItems.clear()
        wifiItems.addAll(newWifiItems)
        wifiItems.sortByDescending {
            it.wifi?.level
        }
        wifiListAdapter.notifyDataSetChanged()
    }

    fun bindDevice(password: String? = null) {
        bluetoothCommunicating = true
        _bluetoothEvent.value = BluetoothEvent(BluetoothEvent.EVENT_CONNECTION, true)
        bluetoothConnection!!.connect(bindingBluetoothDevice!!)
        bluetoothConnection!!.sendBindMessage(
            connectingWifi!!.SSID,
            password,
            userId,
            IsStringValid.getWifiAuthenticationType(connectingWifi!!.capabilities))

    }

    fun clearLiveData() {
        _bluetoothEvent.value = null
        _wifiEvent.value = null
    }

    override fun onCleared() {
        bluetoothConnection?.cancel()
        super.onCleared()
    }
}