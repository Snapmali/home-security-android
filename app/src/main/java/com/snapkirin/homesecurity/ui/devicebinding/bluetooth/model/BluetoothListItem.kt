package com.snapkirin.homesecurity.ui.devicebinding.bluetooth.model

import android.bluetooth.BluetoothDevice

data class BluetoothListItem(
    val device: BluetoothDevice? = null,
    val isDisabledItem: Boolean = false
)
