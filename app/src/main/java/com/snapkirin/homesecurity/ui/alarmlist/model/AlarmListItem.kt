package com.snapkirin.homesecurity.ui.alarmlist.model

import com.snapkirin.homesecurity.model.Alarm

data class AlarmListItem (
    val alarm: Alarm? = null,
    var isLoadingItem: Boolean = false
)