package com.snapkirin.homesecurity.ui.alarmlist.model

import com.snapkirin.homesecurity.model.Alarm

data class AlarmItemClickEvent(
    var show: Boolean = false,
    var alarm: Alarm? = null,
    var deviceName: String? = null
)
