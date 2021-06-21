package com.snapkirin.homesecurity.ui.devicedetail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.snapkirin.homesecurity.HomeSecurity.CAPTURE_ALWAYS_SAVE
import com.snapkirin.homesecurity.HomeSecurity.CAPTURE_SAVE_WHEN_MOVING
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.http.BasicRequestResult
import com.snapkirin.homesecurity.model.Device
import com.snapkirin.homesecurity.model.DeviceStatus
import com.snapkirin.homesecurity.model.http.PullAlarmsResult
import com.snapkirin.homesecurity.model.http.ResponseCodes
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.alarmlist.AlarmListViewModel
import com.snapkirin.homesecurity.ui.main.model.PullingItemsRequestResult
import com.snapkirin.homesecurity.ui.main.model.PullItemListRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class DeviceDetailViewModel(override val userId: Long, val device: Device) :
    AlarmListViewModel(userId) {

    companion object {
        const val OPERATION_MONITOR = 1
        const val OPERATION_UNBIND_DEVICE = 2
        const val OPERATION_RENAME_DEVICE = 3
        const val OPERATION_START_STREAMING = 4

        const val REQUEST_MONITORING = 0b001
        const val REQUEST_STREAMING = 0b010
        const val REQUEST_UNBIND = 0b100

        private const val TAG = "DeviceDetailViewModel"
    }

    var requestState = 0b000

    private val _deviceStatusChanged = MutableLiveData<DeviceStatus?>()
    val deviceStatusChanged: LiveData<DeviceStatus?> = _deviceStatusChanged

    private val _operationRequestResult = MutableLiveData<BasicRequestResult?>()
    val operationRequestResult: LiveData<BasicRequestResult?> = _operationRequestResult

    private val _operationStateChanged = MutableLiveData<Int?>()
    val operationStateChanged: LiveData<Int?> = _operationStateChanged

    private var timer = Timer()

    val deviceStatusUpdateCallback = { _: Int, status: DeviceStatus ->
        if (status.deviceId != device.id) {
            false
        } else {
            Log.i(TAG, "Device status update: $status")
            _deviceStatusChanged.postValue(status)
            if (checkRequestOperationState(REQUEST_MONITORING)) {
                cancelRequestOperation(REQUEST_MONITORING)
                timer.cancel()
            }
            true
        }
    }

    val alarmUpdateCallback = { alarm: Alarm ->
        if (alarm.deviceId == device.id) {
            Log.i(TAG, "Alarm update: $alarm")
            pullNewAlarms()
            true
        } else {
            false
        }
    }

    init {
        initDataSet()
    }

    private fun initDataSet() {
        GlobalScope.launch(Dispatchers.IO) {
            isInitialing = true
            val alarmsResult = requestAlarmList(alarmsBottomOffset, alarmsPageSize)
            if (alarmsResult.success) {
                alarmsResult.offset?.let {
                    alarmsBottomOffset = it
                    alarmsTopOffset = it + (alarmsResult.alarms?.size ?: 0)
                }
                alarmsResult.alarms?.let { updateAlarmList(it, PullItemListRequest.MODE_REFRESH) }
                _requestDataResult.postValue(
                    PullingItemsRequestResult(
                        true,
                        ResponseCodes.Success,
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
            isInitialing = false
            pullingNewAlarms = false
            pullingOldAlarms = false
        }
    }

    fun setRequestOperation(operation: Int) {
        requestState = requestState or operation
        _operationStateChanged.postValue(requestState)
    }

    fun cancelRequestOperation(operation: Int) {
        requestState = requestState and operation.inv()
        _operationStateChanged.postValue(requestState)
    }

    fun checkRequestOperationState(operation: Int): Boolean {
        return requestState and operation > 0
    }

    fun renameDevice(name: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.renameDevice(userId, device.id, name)
            if (result.success) {
                withContext(Dispatchers.Main) {
                    device.name = name
                }
                _operationRequestResult.postValue(
                    result.apply {
                        stringId = R.string.rename_device_success
                        operation = OPERATION_RENAME_DEVICE
                    }
                )
            } else {
                when (result.code) {
                    1 ->
                        _operationRequestResult.postValue(
                            result.apply {
                                stringId = R.string.device_unbound
                                operation = OPERATION_RENAME_DEVICE
                            }
                        )
                    else ->
                        _operationRequestResult.postValue(requestFailureHandler(result.code).apply {
                            operation = OPERATION_RENAME_DEVICE
                        })
                }
            }
        }
    }

    fun startMonitoringAlwaysSave() {
        startMonitoring(CAPTURE_ALWAYS_SAVE)
    }

    fun startMonitoringSaveWhenMoving() {
        startMonitoring(CAPTURE_SAVE_WHEN_MOVING)
    }

    private fun startMonitoring(savingMode: Int) {
        setRequestOperation(REQUEST_MONITORING)
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.startMonitoring(userId, device.id, savingMode)
            monitoringResponseHandler(result)
        }
    }

    fun stopMonitoring() {
        setRequestOperation(REQUEST_MONITORING)
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.stopMonitoring(userId, device.id)
            monitoringResponseHandler(result)
        }
    }

    private fun monitoringResponseHandler(result: BasicRequestResult) {
        if (result.success) {
            startCountDown(10000L)
            _operationRequestResult.postValue(result.apply {
                stringId = R.string.request_successfully_sent
                operation = OPERATION_MONITOR
            })
        } else {
            when (result.code) {
                1 ->
                    _operationRequestResult.postValue(
                        result.apply {
                            stringId = R.string.device_unbound
                            operation = OPERATION_MONITOR
                        }
                    )
                2 ->
                    _operationRequestResult.postValue(
                        result.apply {
                            stringId = R.string.device_offline
                            operation = OPERATION_MONITOR
                        }
                    )
                else ->
                    _operationRequestResult.postValue(requestFailureHandler(result.code).apply {
                        operation = OPERATION_MONITOR
                    })
            }
            cancelRequestOperation(REQUEST_MONITORING)
        }
    }

    private fun startCountDown(delay: Long) {
        val timeTask = object : TimerTask() {
            override fun run() {
                cancelRequestOperation(REQUEST_MONITORING)
                _operationRequestResult.postValue(
                    BasicRequestResult(
                        false,
                        ResponseCodes.Error,
                        R.string.timeout_and_retry,
                        operation = OPERATION_MONITOR
                    )
                )
            }
        }
        this.timer = Timer()
        timer.schedule(timeTask, delay)
    }

    fun startStreaming() {
        setRequestOperation(REQUEST_STREAMING)
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.startStreaming(userId, device.id)
            if (result.success) {
                _operationRequestResult.postValue(result.apply {
                    operation = OPERATION_START_STREAMING
                })
            } else {
                when (result.code) {
                    1 ->
                        _operationRequestResult.postValue(
                            result.apply {
                                stringId = R.string.device_unbound
                                operation = OPERATION_START_STREAMING
                            }
                        )
                    2 ->
                        _operationRequestResult.postValue(
                            result.apply {
                                stringId = R.string.device_offline
                                operation = OPERATION_START_STREAMING
                            }
                        )
                    else ->
                        _operationRequestResult.postValue(requestFailureHandler(result.code).apply {
                            operation = OPERATION_START_STREAMING
                        })
                }
            }
            cancelRequestOperation(REQUEST_STREAMING)
        }
    }

    fun unbindDevice() {
        setRequestOperation(REQUEST_UNBIND)
        GlobalScope.launch(Dispatchers.IO) {
            val result = Http.unbindDevice(userId, device.id)
            if (result.success) {
                _operationRequestResult.postValue(result.apply { operation = OPERATION_UNBIND_DEVICE })
            } else {
                when (result.code) {
                    1 ->
                        _operationRequestResult.postValue(
                            result.apply {
                                stringId = R.string.device_unbound
                                operation = OPERATION_UNBIND_DEVICE
                            }
                        )
                    else ->
                        _operationRequestResult.postValue(requestFailureHandler(result.code).apply {
                            operation = OPERATION_UNBIND_DEVICE
                        })
                }
            }
            cancelRequestOperation(REQUEST_UNBIND)
        }
    }

    override suspend fun requestAlarmList(offset: Int, pageSize: Int): PullAlarmsResult {
        return Http.pullAlarms(userId, offset, pageSize, device.id)
    }

    override fun clearLiveData() {
        super.clearLiveData()
        _deviceStatusChanged.value = null
        _operationRequestResult.value = null
    }
}