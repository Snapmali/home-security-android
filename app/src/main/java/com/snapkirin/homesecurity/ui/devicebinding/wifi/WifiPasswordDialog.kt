package com.snapkirin.homesecurity.ui.devicebinding.wifi

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingViewModel
import com.snapkirin.homesecurity.ui.util.afterTextChanged

class WifiPasswordDialog : DialogFragment() {

    private lateinit var passwordText: TextInputEditText
    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button

    private lateinit var deviceBindingViewModel: DeviceBindingViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  inflater.inflate(R.layout.dialog_wifi_password, container, false)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        passwordText = view.findViewById(R.id.wifiPasswordText)
        positiveButton = view.findViewById(R.id.wifiPasswordPositiveButton)
        negativeButton = view.findViewById(R.id.wifiPasswordNegativeButton)
        deviceBindingViewModel = ViewModelProvider(requireActivity()).get(DeviceBindingViewModel::class.java)

        positiveButton.setOnClickListener {
            deviceBindingViewModel.bindDevice(passwordText.text.toString())
            dialog?.dismiss() ?: dismiss()
        }
        negativeButton.setOnClickListener {
            dialog?.dismiss() ?: dismiss()
        }
        passwordText.afterTextChanged {
            positiveButton.isEnabled = it.isNotEmpty()
        }
    }
}