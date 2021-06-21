package com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R

class BluetoothDisabledItemDelegate
    : ItemViewBinder<BluetoothListItem, BluetoothDisabledItemDelegate.ViewHolder>() {
    class ViewHolder(view : View): RecyclerView.ViewHolder(view)

    override fun onBindViewHolder(holder: ViewHolder, item: BluetoothListItem) {
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_bluetooth_disabled, parent, false))
    }
}