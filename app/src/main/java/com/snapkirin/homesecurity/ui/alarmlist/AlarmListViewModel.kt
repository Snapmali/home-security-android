package com.snapkirin.homesecurity.ui.alarmlist

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.drakeet.multitype.MultiTypeAdapter
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.http.PullAlarmsResult
import com.snapkirin.homesecurity.network.http.Http
import com.snapkirin.homesecurity.ui.alarmlist.model.AlarmItemClickEvent
import com.snapkirin.homesecurity.ui.alarmlist.model.AlarmItemViewDelegate
import com.snapkirin.homesecurity.ui.alarmlist.model.AlarmListItem
import com.snapkirin.homesecurity.ui.alarmlist.model.LoadingItemViewDelegate
import com.snapkirin.homesecurity.ui.main.model.*
import com.snapkirin.homesecurity.ui.util.BaseItemListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class AlarmListViewModel(open val userId: Long): BaseItemListViewModel() {

    var isInitialing = true

    val alarmItems = mutableListOf<AlarmListItem>()

    protected val _requestDataResult = MutableLiveData<PullingItemsRequestResult?>()
    val requestDataResult: LiveData<PullingItemsRequestResult?> = _requestDataResult

    private val _alarmListChange = MutableLiveData<ItemListChangeEvent?>()
    val alarmListChange: LiveData<ItemListChangeEvent?> = _alarmListChange

    protected val _alarmItemClicked = MutableLiveData<AlarmItemClickEvent?>()
    val alarmItemClicked: LiveData<AlarmItemClickEvent?> = _alarmItemClicked

    open val alarmItemViewDelegate = AlarmItemViewDelegate().apply {
        setAlarmOnClickListener { alarm: Alarm, deviceName: String ->
            _alarmItemClicked.value = AlarmItemClickEvent(true, alarm, deviceName)
            _alarmItemClicked.value = AlarmItemClickEvent()

        }
    }

    val alarmItemListAdapter = MultiTypeAdapter().apply {
        register(AlarmListItem::class).to(
            alarmItemViewDelegate,
            LoadingItemViewDelegate()
        ).withKotlinClassLinker { _, item ->
            if (item.isLoadingItem) {
                LoadingItemViewDelegate::class
            }
            else {
                AlarmItemViewDelegate::class
            }
        }
        items = alarmItems
    }

    protected var alarmsTopOffset = -1
    protected var alarmsBottomOffset = -1
    protected val alarmsPageSize = 15

    protected var pullingNewAlarms = true
    protected var pullingOldAlarms = true

    fun pullNewAlarms() {
        if (!pullingNewAlarms) {
            pullingNewAlarms = true
            pullAlarms(PullItemListRequest.MODE_NEW)
        }
    }

    fun pullOldAlarms() {
        if (!pullingOldAlarms) {
            pullingOldAlarms = true
            pullAlarms(PullItemListRequest.MODE_OLD)
        }
    }

    private fun pullAlarms(mode: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            Log.e("old offset", "Top: $alarmsTopOffset, Bottom: $alarmsBottomOffset")
            val result: PullAlarmsResult =
                when (mode) {
                    PullItemListRequest.MODE_NEW -> {
                        requestAlarmList(alarmsTopOffset, alarmsPageSize)
                    }
                    PullItemListRequest.MODE_OLD -> {
                        if (alarmsBottomOffset - alarmsPageSize > 0)
                            requestAlarmList(alarmsBottomOffset - alarmsPageSize, alarmsPageSize)
                        else
                            requestAlarmList(0, alarmsBottomOffset)
                    }
                    else -> return@launch
                }
            if (result.success) {
                when (mode) {
                    PullItemListRequest.MODE_NEW -> {
                        result.offset?.let {
                            if (alarmsBottomOffset == -1) {
                                alarmsBottomOffset = it
                            }
                            alarmsTopOffset = it + (result.alarms?.size ?: 0)
                        }
                    }
                    PullItemListRequest.MODE_OLD -> {
                        val resultOffset = alarmsBottomOffset - (result.alarms?.size ?: 0)
                        alarmsBottomOffset =
                            if (resultOffset > 0)
                                resultOffset
                            else
                                0
                    }
                    PullItemListRequest.MODE_REFRESH -> {
                        result.offset?.let {
                            alarmsBottomOffset = it
                            alarmsTopOffset = it + (result.alarms?.size ?: 0)
                        }
                    }
                }

                _requestDataResult.postValue(
                    PullingItemsRequestResult(
                        true,
                        result.code,
                        0,
                        PullItemListRequest.CAT_ALARM,
                        mode)
                )
                result.alarms?.let { updateAlarmList(it, mode) }
            } else {
                _requestDataResult.postValue(
                    pullingItemsFailureHandler(
                        result.code,
                        PullItemListRequest.CAT_ALARM,
                        mode
                    )
                )
            }
            when (mode) {
                PullItemListRequest.MODE_NEW -> pullingNewAlarms = false
                PullItemListRequest.MODE_OLD -> pullingOldAlarms = false
            }
            Log.e("new offset", "Top: $alarmsTopOffset, Bottom: $alarmsBottomOffset")
        }
    }

    protected open suspend fun requestAlarmList(
        offset: Int,
        pageSize: Int
    ): PullAlarmsResult {
        return Http.pullAlarms(userId, offset, pageSize)
    }

    protected suspend fun updateAlarmList(newAlarms: List<Alarm>, updateMode: Int) {
        if (newAlarms.isEmpty()) return

        withContext(Dispatchers.Main)
        {
            when (updateMode) {
                PullItemListRequest.MODE_NEW -> {
                    val newAlarmsSorted = newAlarms.sortedBy { alarm: Alarm -> alarm.time }
                    for (alarm in newAlarmsSorted) {
                        alarmItems.add(0, AlarmListItem(alarm))
                    }
                    _alarmListChange.value =
                        ItemListChangeEvent(ItemListChangeEvent.ADD, 0, newAlarms.size)
                }
                PullItemListRequest.MODE_OLD, PullItemListRequest.MODE_REFRESH -> {
                    val oriSize = alarmItems.size
                    val newAlarmsSorted =
                        newAlarms.sortedByDescending { alarm: Alarm -> alarm.time }
                    for (alarm in newAlarmsSorted) {
                        alarmItems.add(AlarmListItem(alarm))
                    }
                    _alarmListChange.value =
                        ItemListChangeEvent(ItemListChangeEvent.ADD, oriSize, newAlarms.size)
                }
            }
        }
    }

    fun alarmsListChanged(changeEvent: ItemListChangeEvent) {
        itemListChanged(changeEvent, alarmItemListAdapter)
    }

    fun addAlarmItemListLoading() {
        if (!(alarmItems.isNotEmpty() && alarmItems.last().isLoadingItem) ) {
            alarmItems.add(AlarmListItem(isLoadingItem = true))
            alarmItemListAdapter.notifyItemInserted(alarmItems.size - 1)
        }
    }

    fun removeAlarmItemListLoading() {
        if (alarmItems.isNotEmpty() && alarmItems.last().isLoadingItem) {
            alarmItems.removeLast()
            alarmItemListAdapter.notifyItemRemoved(alarmItems.size)
        }
    }

    fun noMoreOlderAlarms(): Boolean {
        return alarmsBottomOffset == 0
    }

    override fun clearLiveData() {
        super.clearLiveData()
        _requestDataResult.value = null
        _alarmListChange.value = null
        _alarmItemClicked.value = null
    }

}