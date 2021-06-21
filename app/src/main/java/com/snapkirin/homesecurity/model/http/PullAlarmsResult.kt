package com.snapkirin.homesecurity.model.http

import com.snapkirin.homesecurity.model.Alarm

data class PullAlarmsResult(
        val success: Boolean,
        val code: Int,
        var stringId: Int,
        val offset: Int? = null,
        val alarms: List<Alarm>? = null
)
