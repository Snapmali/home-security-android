package com.snapkirin.homesecurity.ui.alarmlist.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.model.DeviceList

class AlarmItemViewDelegate : ItemViewBinder<AlarmListItem, AlarmItemViewDelegate.ViewHolder>() {

    private val deviceInfo = mutableMapOf<Long, Pair<String, Boolean>>()
    private var listener: ((Alarm, String) -> Unit)? = null

    class ViewHolder(view : View): RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.alarmCardView)
        val alarmTimeText: TextView = view.findViewById(R.id.alarmTimeText)
        val alarmTypeText: TextView = view.findViewById(R.id.alarmTypeText)
        val deviceNameText: TextView = view.findViewById(R.id.alarmDeviceNameText)
        var deviceBound = false

        @SuppressLint("UseCompatLoadingForDrawables")
        val motionDrawable = view.context.getDrawable(R.drawable.ic_baseline_directions_walk_24)
        @SuppressLint("UseCompatLoadingForDrawables")
        val fireDrawable = view.context.getDrawable(R.drawable.ic_baseline_local_fire_department_24)
    }

    fun setAlarmOnClickListener(listener: (Alarm, String) -> Unit) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AlarmListItem) {
        val alarm = item.alarm
        if (alarm != null) {
            holder.alarmTimeText.text = alarm.dateTimeString
            holder.alarmTypeText.apply {
                when (alarm.type) {
                    Alarm.MOTION_ALARM -> {
                        setText(R.string.motion_alarm)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(holder.motionDrawable, null, null, null)
                    }
                    Alarm.SMOKE_ALARM -> {
                        setText(R.string.smoke_alarm)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(holder.fireDrawable, null, null, null)
                    }
                }
            }

            // 获取设备名及绑定信息
            var deviceInfo = deviceInfo[alarm.deviceId]
            if (deviceInfo == null) {
                var name = DeviceList.getDeviceName(alarm.deviceId)
                var status = true
                if (name == null) {
                    name = alarm.deviceId.toString()
                    status = false
                }
                deviceInfo = Pair(name, status)
                this.deviceInfo[alarm.deviceId] = deviceInfo
            }
            holder.deviceNameText.text = deviceInfo.first
            holder.deviceBound = deviceInfo.second
            holder.deviceNameText.isClickable = deviceInfo.second


            holder.cardView.setOnClickListener {
                listener?.let { it1 -> it1(alarm, deviceInfo.first) }
            }
        }
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_alarm, parent, false))
    }

    fun clearDevicesInfo() {
        deviceInfo.clear()
    }

}