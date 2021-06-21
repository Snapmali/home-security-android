package com.snapkirin.homesecurity.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.model.User

class MainViewModelFactory(val user: User) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(user) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}