package com.snapkirin.homesecurity.ui.devicebinding.bluetooth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingViewModel
import com.snapkirin.homesecurity.ui.devicebinding.model.BluetoothEvent

class ConnectionFragment : Fragment() {

    private lateinit var loadingText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_binding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingText = view.findViewById(R.id.bindLoadingText)

        val bindDeviceViewModel = ViewModelProvider(requireActivity()).get(DeviceBindingViewModel::class.java)
        bindDeviceViewModel.bluetoothEvent.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            when (event.event) {
                BluetoothEvent.EVENT_ENABLED -> {
                    loadingText.text = getString(R.string.connecting_device)
                }
                BluetoothEvent.EVENT_DEVICE_CONNECTING_NETWORK -> {
                    if (event.state)
                        loadingText.text = getString(R.string.device_connecting_to_wifi)
                    else if (event.result == true)
                        loadingText.text = getString(R.string.device_connecting_to_server)
                }
                BluetoothEvent.EVENT_DEVICE_CONNECTING_SERVER -> {
                    if (event.result == true)
                        loadingText.text = getString(R.string.binding_device)
                }
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = ConnectionFragment()
    }
}