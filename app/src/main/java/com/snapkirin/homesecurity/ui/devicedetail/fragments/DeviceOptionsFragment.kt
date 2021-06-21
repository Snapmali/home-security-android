package com.snapkirin.homesecurity.ui.devicedetail.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Device
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailActivity
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel.Companion.REQUEST_UNBIND

class DeviceOptionsFragment : PreferenceFragmentCompat() {

    private lateinit var streamingPreference: Preference
    private lateinit var unbindPreference: Preference

    private lateinit var deviceDetailViewModel: DeviceDetailViewModel
    private lateinit var parentActivity: DeviceDetailActivity

    private lateinit var device: Device

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_device_options, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =  super.onCreateView(inflater, container, savedInstanceState)
        parentActivity = activity as DeviceDetailActivity
        streamingPreference = findPreference("start_streaming")!!
        unbindPreference = findPreference("unbind_device") !!
        deviceDetailViewModel = ViewModelProvider(requireActivity()).get(DeviceDetailViewModel::class.java)
        device = deviceDetailViewModel.device

        deviceDetailViewModel.deviceStatusChanged.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            if (deviceDetailViewModel.requestState == 0)
                streamingPreference.isEnabled = device.monitoring
        })

        deviceDetailViewModel.operationStateChanged.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            if (it == 0)
                restoreOptionsEnabled()
            else if (deviceDetailViewModel.checkRequestOperationState(REQUEST_UNBIND)) {
                disableOptions()
            } else {
                streamingPreference.isEnabled = false
                unbindPreference.isEnabled = true
            }
        })

        streamingPreference.setOnPreferenceClickListener {
            deviceDetailViewModel.startStreaming()
            true
        }

        unbindPreference.setOnPreferenceClickListener {
            showUnbindDialog()
            true
        }
        if (deviceDetailViewModel.requestState == 0)
            streamingPreference.isEnabled = device.monitoring
        return root
    }

    private fun restoreOptionsEnabled() {
        streamingPreference.isEnabled = device.monitoring
        unbindPreference.isEnabled = true
    }

    private fun disableOptions() {
        streamingPreference.isEnabled = false
        unbindPreference.isEnabled = false
    }

    private fun showUnbindDialog() {
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
        dialogBuilder.apply {
            setTitle(R.string.unbind_device_confirm)
            setMessage(R.string.unbind_device_confirm_summary)
            setPositiveButton(R.string.ok) { _, _ ->
                deviceDetailViewModel.unbindDevice()
            }
            setNegativeButton(R.string.cancel) { _, _ ->
            }
        }
        dialogBuilder.show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = DeviceOptionsFragment()
    }
}