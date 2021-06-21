package com.snapkirin.homesecurity.ui.devicebinding.wifi.model

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.snapkirin.homesecurity.R

class WifiItemDelegate : ItemViewBinder<WifiListItem, WifiItemDelegate.ViewHolder>() {
    private var listener: ((ScanResult, Boolean) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemLayout: LinearLayout = view.findViewById(R.id.wifiItemLinearLayout)
        val signalImage: ImageView = view.findViewById(R.id.wifiSignalImage)
        val ssidText: TextView = view.findViewById(R.id.wifiSsidText)
        val encryptedImage: ImageView = view.findViewById(R.id.wifiEncryptedImage)

        @SuppressLint("UseCompatLoadingForDrawables")
        val wifiSignalDrawableList = listOf(
            view.context.getDrawable(R.drawable.ic_wifi_signal_0)!!,
            view.context.getDrawable(R.drawable.ic_wifi_signal_1)!!,
            view.context.getDrawable(R.drawable.ic_wifi_signal_2)!!,
            view.context.getDrawable(R.drawable.ic_wifi_signal_3)!!,
            view.context.getDrawable(R.drawable.ic_wifi_signal_4)!!
        )

        var encrypted = false
    }

    fun setItemOnClickListener(listener: (ScanResult, Boolean) -> Unit) {
        this.listener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, item: WifiListItem) {
        val scanResult = item.wifi
        if (scanResult != null) {
            holder.ssidText.text = scanResult.SSID
            val capabilities = scanResult.capabilities
            when {
                capabilities.contains("WEP", true) ||
                        capabilities.contains("PSK", true) ||
                        capabilities.contains("EAP", true) -> {
                    holder.encryptedImage.visibility = View.VISIBLE
                    holder.encrypted = true
                }
                else -> {
                    holder.encryptedImage.visibility = View.INVISIBLE
                    holder.encrypted = false
                }
            }
            holder.signalImage.setImageDrawable(
                holder.wifiSignalDrawableList[WifiManager.calculateSignalLevel(scanResult.level, 5)]
            )
            holder.itemLayout.setOnClickListener {
                listener?.let { it1 -> it1(scanResult, holder.encrypted) }
            }
        }
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            inflater.inflate(R.layout.item_wifi, parent, false)
        )
    }
}