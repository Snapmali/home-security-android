package com.snapkirin.homesecurity.ui.devicebinding.wifi.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R

class WlanDisabledItemDelegate : ItemViewBinder<WifiListItem, WlanDisabledItemDelegate.ViewHolder>() {
    class ViewHolder(view : View): RecyclerView.ViewHolder(view) {
    }

    override fun onBindViewHolder(holder: ViewHolder, item: WifiListItem) {
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_wlan_disabled, parent, false))
    }
}