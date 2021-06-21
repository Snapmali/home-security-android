package com.snapkirin.homesecurity.ui.main.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.model.DeviceList

class DevicesViewModelFactory(
        private val devices: DeviceList
        ): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DevicesViewModel::class.java))
            return DevicesViewModel(devices) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}