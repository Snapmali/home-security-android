package com.snapkirin.homesecurity.ui.devicebinding.model

import android.net.wifi.ScanResult

data class WifiEvent(
    val event: Int,
    val state: Boolean,
    val result: Boolean? = null,
    val wifi: ScanResult? = null
) {
    companion object {
        const val REFRESHING = 1
        const val ENABLED = 2
    }
}