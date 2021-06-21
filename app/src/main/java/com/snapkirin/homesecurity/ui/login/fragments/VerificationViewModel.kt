package com.snapkirin.homesecurity.ui.login.fragments

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class VerificationViewModel : ViewModel() {

    private val periodTime = 500L
    private val elapsedRealTime = MutableLiveData<Long>()
    private var initialTime: Long = 0

    private var countDownRunning = false

    val countDownTime: LiveData<Long> = elapsedRealTime

    fun startCountDown() {
        if (!countDownRunning) {
            countDownRunning = true
            initialTime = SystemClock.elapsedRealtime()
            val timer = Timer()
            val timeTask = object : TimerTask() {
                override fun run() {
                    val newValue = 60 - (SystemClock.elapsedRealtime() - initialTime) / 1000
                    if (newValue > 0) {
                        elapsedRealTime.postValue(newValue)
                    } else {
                        elapsedRealTime.postValue(newValue)
                        timer.cancel()
                        countDownRunning = false
                    }
                }
            }
            timer.scheduleAtFixedRate(timeTask, periodTime, periodTime)
        }
    }
}