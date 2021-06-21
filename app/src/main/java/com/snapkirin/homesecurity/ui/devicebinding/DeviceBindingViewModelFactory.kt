package com.snapkirin.homesecurity.ui.devicebinding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DeviceBindingViewModelFactory(val userId: Long) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceBindingViewModel::class.java))
            return DeviceBindingViewModel(userId) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}