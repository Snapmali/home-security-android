package com.snapkirin.homesecurity.ui.player

import androidx.lifecycle.ViewModel
import com.snapkirin.homesecurity.network.http.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VideoPlayerViewModel(val userId: Long, val deviceId: Long) : ViewModel() {
    private fun stopStreaming() {
        GlobalScope.launch(Dispatchers.IO) {
            delay(5000L)
            val result = Http.stopStreaming(userId, deviceId)
        }
    }

    override fun onCleared() {
        stopStreaming()
        super.onCleared()
    }
}