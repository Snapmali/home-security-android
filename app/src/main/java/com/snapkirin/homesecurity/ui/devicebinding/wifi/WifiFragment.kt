package com.snapkirin.homesecurity.ui.devicebinding.wifi

import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingActivity
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingViewModel
import com.snapkirin.homesecurity.ui.devicebinding.model.WifiEvent
import com.snapkirin.homesecurity.ui.util.showToast

class WifiFragment : Fragment() {

    companion object {
        fun newInstance() = WifiFragment()
    }

    private lateinit var refreshButton: ImageButton
    private lateinit var listView: RecyclerView
    private lateinit var throttlingText: TextView

    private lateinit var deviceBindingViewModel: DeviceBindingViewModel
    private lateinit var parentActivity: DeviceBindingActivity
    private lateinit var rotation: Animation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wifi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as DeviceBindingActivity
        parentActivity.supportActionBar?.setTitle(R.string.select_wlan)
        refreshButton = view.findViewById(R.id.wifiRefreshButton)
        listView = view.findViewById(R.id.wifiListView)
        throttlingText = view.findViewById(R.id.wifiScanningThrottlingText)
        rotation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_360)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            throttlingText.visibility = View.VISIBLE
        }

        deviceBindingViewModel = ViewModelProvider(parentActivity).get(DeviceBindingViewModel::class.java)
        deviceBindingViewModel.wifiEvent.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            when (event.event) {
                WifiEvent.REFRESHING -> {
                    if (event.state) {
                        refreshButton.startAnimation(rotation)
                    } else {
                        refreshButton.clearAnimation()
                        if (event.result == false) {
                            parentActivity.showToast(R.string.refresh_failed)
                        }
                    }
                }
                WifiEvent.ENABLED -> {
                    refreshButton.isEnabled = event.state
                }
            }
        })
        deviceBindingViewModel.wifiItemDelegate.setItemOnClickListener { scanResult, encrypted ->
            deviceBindingViewModel.connectingWifi = scanResult
            if (encrypted)
                showWifiPasswordDialog()
            else
                deviceBindingViewModel.bindDevice()
        }
        listView.adapter = deviceBindingViewModel.wifiListAdapter

        refreshButton.setOnClickListener {
            parentActivity.scanWifi()
        }
        parentActivity.scanWifi()
    }

    private fun showWifiPasswordDialog() {
        val dialog = WifiPasswordDialog()
        dialog.show(childFragmentManager, "wifi_password_dialog")
    }

}