package com.snapkirin.homesecurity.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.drakeet.multitype.MultiTypeAdapter
import com.snapkirin.homesecurity.model.*
import com.snapkirin.homesecurity.model.http.PullDevicesResult
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.main.devices.model.DeviceItemViewDelegate
import com.snapkirin.homesecurity.ui.main.model.*
import com.snapkirin.homesecurity.ui.alarmlist.AlarmListViewModel
import kotlinx.coroutines.*
import com.snapkirin.homesecurity.model.http.ResponseCodes as RC

class MainViewModel(val user: User): AlarmListViewModel(user.userId) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _deviceListChange = MutableLiveData<ItemListChangeEvent?>()
    val deviceListChange: LiveData<ItemListChangeEvent?> = _deviceListChange

    private val _deviceItemClicked = MutableLiveData<DeviceItemClickEvent?>()
    val deviceItemClicked: LiveData<DeviceItemClickEvent?> = _deviceItemClicked

    val deviceListAdapter = MultiTypeAdapter().apply {
        register(DeviceItemViewDelegate().apply {
            setDeviceOnClickListener { device: Device, index: Int ->
                lastClickedDeviceItem = index
                _deviceItemClicked.value = DeviceItemClickEvent(true, userId, device)
            }
        })
        items = DeviceList.getList()
    }

    val deviceStatusUpdateCallback = { index: Int, status: DeviceStatus ->
        Log.i(TAG, "Device status update: $status")
        _deviceListChange.value = ItemListChangeEvent(ItemListChangeEvent.UPDATE, index)
        true
    }
    val alarmUpdateCallback = { alarm: Alarm ->
        Log.i(TAG, "Alarm update: $alarm")
        pullNewAlarms()
        true
    }

    var lastClickedDeviceItem = 0
    private var pullingDevices = true

    init {
        initDataSet()
    }

    private fun initDataSet() {
        GlobalScope.launch(Dispatchers.IO) {
            isInitialing = true
            val devicesResultDeferred = async(Dispatchers.IO) {
                requestDeviceList()
            }
            val alarmsResultDeferred = async(Dispatchers.IO) {
                requestAlarmList(alarmsBottomOffset, alarmsPageSize)
            }
            val devicesResult = devicesResultDeferred.await()
            if (devicesResult.success) {
                devicesResult.devices?.let { updateDeviceList(it) }
                val alarmsResult = alarmsResultDeferred.await()
                if (alarmsResult.success) {
                    alarmsResult.offset?.let {
                        alarmsBottomOffset = it
                        alarmsTopOffset = it + (alarmsResult.alarms?.size ?: 0)
                    }
                    alarmsResult.alarms?.let { updateAlarmList(it, PullItemListRequest.MODE_REFRESH) }
                    _requestDataResult.postValue(
                        PullingItemsRequestResult(
                            true,
                            RC.Success,
                            0,
                            PullItemListRequest.CAT_INIT,
                            PullItemListRequest.MODE_REFRESH
                        )
                    )
                } else {
                    _requestDataResult.postValue(
                        pullingItemsFailureHandler(
                            alarmsResult.code,
                            PullItemListRequest.CAT_INIT,
                            PullItemListRequest.MODE_REFRESH
                        )
                    )
                }
            } else {
                _requestDataResult.postValue(
                    pullingItemsFailureHandler(
                        devicesResult.code,
                        PullItemListRequest.CAT_INIT,
                        PullItemListRequest.MODE_REFRESH
                    )
                )
            }
            isInitialing = false
            pullingDevices = false
            pullingNewAlarms = false
            pullingOldAlarms = false
        }
    }

    fun pullDevices() {
        if (!pullingDevices) {
            pullingDevices = true
            GlobalScope.launch(Dispatchers.IO) {
                val result = requestDeviceList()
                if (result.success) {
                    _requestDataResult.postValue(
                        PullingItemsRequestResult(
                            true,
                            result.code,
                            0,
                            PullItemListRequest.CAT_DEVICE,
                            PullItemListRequest.MODE_REFRESH
                        )
                    )
                    result.devices?.let { updateDeviceList(it) }
                } else {
                    _requestDataResult.postValue(
                        pullingItemsFailureHandler(
                            result.code,
                            PullItemListRequest.CAT_DEVICE,
                            PullItemListRequest.MODE_REFRESH
                        )
                    )
                }
                pullingDevices = false
            }
        }
    }

    private suspend fun requestDeviceList(): PullDevicesResult {
        return Http.pullDevices(user.userId)
    }

    /**
     * Need to run on the main thread
     */
    private fun updateDeviceStatus(status: DeviceStatus) {
        val index = DeviceList.updateDeviceStatus(status) ?: return
        Log.e("update", status.toString())
        _deviceListChange.value = ItemListChangeEvent(ItemListChangeEvent.UPDATE, index)
    }

    private suspend fun updateDeviceList(newDevices: List<Device>) {

        withContext(Dispatchers.Main) {
            val devicesToAdd = mutableListOf<Device>()
            val devicesToRemove = mutableSetOf<Int>()

            for (i in 0 until DeviceList.size()) devicesToRemove.add(i)

            for (device in newDevices) {
                val index = DeviceList.updateDeviceInfo(device)
                if (index != null) {
                    devicesToRemove.remove(index)
                    _deviceListChange.value = ItemListChangeEvent(ItemListChangeEvent.UPDATE, index)
                } else {
                    devicesToAdd.add(device)
                }
            }

            for (index in devicesToRemove) {
                Log.e("device", "removed $index")
                DeviceList.remove(index)
                _deviceListChange.value = ItemListChangeEvent(ItemListChangeEvent.DELETE, index)
            }
            devicesToAdd.sortByDescending { device: Device -> device.bindingTime }
            DeviceList.addAll(0, devicesToAdd)
            _deviceListChange.value =
                ItemListChangeEvent(ItemListChangeEvent.ADD, 0, devicesToAdd.size)
        }
    }

    fun devicesListChanged(changeEvent: ItemListChangeEvent) {
        itemListChanged(changeEvent, deviceListAdapter)
    }

    fun devicesClickedItemUpdate() {
        devicesListChanged(ItemListChangeEvent(ItemListChangeEvent.UPDATE, lastClickedDeviceItem))
    }

    override fun clearLiveData() {
        super.clearLiveData()
        _deviceListChange.value = null
        _deviceItemClicked.value = null
    }

}