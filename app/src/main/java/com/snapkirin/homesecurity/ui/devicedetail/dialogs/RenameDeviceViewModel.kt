package com.snapkirin.homesecurity.ui.devicedetail.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicedetail.dialogs.model.DeviceScreenNameFormState
import com.snapkirin.homesecurity.util.IsStringValid

class RenameDeviceViewModel: ViewModel() {

    private val _deviceScreenNameForm = MutableLiveData<DeviceScreenNameFormState>()
    val deviceScreenNameFormState: LiveData<DeviceScreenNameFormState> = _deviceScreenNameForm

    fun deviceScreenNameTextChanged(screenName: String) {
        when (IsStringValid.isDeviceScreenNameValid(screenName)) {
            IsStringValid.VALID ->
                _deviceScreenNameForm.value = DeviceScreenNameFormState(isDataValid = true)
            IsStringValid.INVALID_DEVICE_SCREEN_NAME ->
                _deviceScreenNameForm.value = DeviceScreenNameFormState(error = R.string.invalid_device_screen_name)
            else ->
                _deviceScreenNameForm.value = DeviceScreenNameFormState()
        }
    }
}