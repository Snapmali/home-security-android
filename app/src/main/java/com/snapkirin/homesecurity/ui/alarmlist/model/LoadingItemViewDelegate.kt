package com.snapkirin.homesecurity.ui.alarmlist.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R

class LoadingItemViewDelegate: ItemViewBinder<AlarmListItem, LoadingItemViewDelegate.ViewHolder>() {

    class ViewHolder(view : View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_loading, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AlarmListItem) {

    }
}