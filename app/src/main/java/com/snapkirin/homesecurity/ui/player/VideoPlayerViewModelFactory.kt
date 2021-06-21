package com.snapkirin.homesecurity.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.model.Device
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel

class VideoPlayerViewModelFactory(val userId: Long, val deviceId: Long) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoPlayerViewModel::class.java))
            return VideoPlayerViewModel(userId, deviceId) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
