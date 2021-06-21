package com.snapkirin.homesecurity.ui.main.devices.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Device

class DeviceItemViewDelegate: ItemViewBinder<Device, DeviceItemViewDelegate.ViewHolder>() {

    private var listener: ((Device, Int) -> Unit)? = null

    class ViewHolder(view : View): RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.deviceCardView)
        val deviceNameText: TextView = view.findViewById(R.id.deviceNameText)
        val deviceIsOnlineText: TextView = view.findViewById(R.id.deviceIsOnlineText)
        val deviceIsMonitoringText: TextView = view.findViewById(R.id.deviceIsMonitoringText)
        val deviceIsStreamingText: TextView = view.findViewById(R.id.deviceIsStreamingText)

        @SuppressLint("UseCompatLoadingForDrawables")
        val onlineDrawable = view.context.getDrawable(R.drawable.ic_baseline_wifi_24)
        @SuppressLint("UseCompatLoadingForDrawables")
        val offlineDrawable = view.context.getDrawable(R.drawable.ic_baseline_wifi_off_24)
    }

    fun setDeviceOnClickListener(listener: (Device, Int) -> Unit) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Device) {
        holder.deviceNameText.text = item.name
        holder.deviceIsOnlineText.apply {
            if (item.online) {
                setText(R.string.is_online)
                setTextAppearance(R.style.DeviceStatusActivated)
                setCompoundDrawablesRelativeWithIntrinsicBounds(holder.onlineDrawable, null, null, null)
            } else {
                setText(R.string.is_offline)
                setTextAppearance(R.style.DeviceStatusDefault)
                setCompoundDrawablesRelativeWithIntrinsicBounds(holder.offlineDrawable, null, null, null)
            }
        }
        holder.deviceIsMonitoringText.apply {
            if (item.monitoring) {
                setText(R.string.is_monitoring)
                setTextAppearance(R.style.DeviceStatusActivated)
            } else {
                setText(R.string.is_not_monitoring)
                setTextAppearance(R.style.DeviceStatusDefault)
            }
        }
        holder.deviceIsStreamingText.apply {
            if (item.streaming) {
                setText(R.string.is_streaming)
                setTextAppearance(R.style.DeviceStatusActivated)
            } else {
                setText(R.string.is_not_streaming)
                setTextAppearance(R.style.DeviceStatusDefault)
            }
        }
        holder.cardView.setOnClickListener {
            listener?.let { it1 -> it1(item, holder.adapterPosition) }
        }
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_device, parent, false))
    }
}