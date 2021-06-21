package com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R

class BluetoothDeviceItemDelegate :
    ItemViewBinder<BluetoothListItem, BluetoothDeviceItemDelegate.ViewHolder>() {
    private var listener: ((BluetoothDevice) -> Unit)? = null

    class ViewHolder(view : View): RecyclerView.ViewHolder(view) {
        val itemLayout: LinearLayout = view.findViewById(R.id.bluetoothDeviceLinearLayout)
        val deviceName: TextView = view.findViewById(R.id.blueToothDeviceNameText)
    }

    fun setItemOnClickListener(listener: (BluetoothDevice) -> Unit) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, item: BluetoothListItem) {
        val device = item.device
        if (device != null) {
            holder.deviceName.text = device.name ?: device.address
            holder.itemLayout.setOnClickListener {
                listener?.let { it1 -> it1(device) }
            }
        }
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            inflater.inflate(R.layout.item_bluetooth_device, parent, false)
        )
    }
}