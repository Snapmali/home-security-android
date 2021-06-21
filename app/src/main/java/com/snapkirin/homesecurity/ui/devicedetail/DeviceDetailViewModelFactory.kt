package com.snapkirin.homesecurity.ui.devicedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.model.Device

class DeviceDetailViewModelFactory(val userId: Long, val device: Device) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceDetailViewModel::class.java))
            return DeviceDetailViewModel(userId, device) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}