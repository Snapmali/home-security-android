package com.snapkirin.homesecurity.ui.devicebinding.wifi.model

import android.net.wifi.ScanResult

data class WifiListItem(
    val wifi: ScanResult? = null,
    val isDisabledItem: Boolean = false
)
