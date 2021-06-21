package com.snapkirin.homesecurity.ui.devicebinding.bluetooth

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingActivity
import com.snapkirin.homesecurity.ui.devicebinding.DeviceBindingViewModel
import com.snapkirin.homesecurity.ui.devicebinding.model.BluetoothEvent

class BluetoothFragment : Fragment() {

    companion object {
        fun newInstance() = BluetoothFragment()
    }

    private lateinit var refreshButton: ImageButton
    private lateinit var listView: RecyclerView

    private lateinit var deviceBindingViewModel: DeviceBindingViewModel
    private lateinit var parentActivity: DeviceBindingActivity
    private lateinit var rotation: Animation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentActivity = requireActivity() as DeviceBindingActivity
        parentActivity.supportActionBar?.setTitle(R.string.select_device)
        refreshButton = view.findViewById(R.id.bluetoothRefreshButton)
        listView = view.findViewById(R.id.bluetoothListView)
        rotation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_360)

        deviceBindingViewModel = ViewModelProvider(parentActivity).get(DeviceBindingViewModel::class.java)
        deviceBindingViewModel.bluetoothEvent.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            when (event.event) {
                BluetoothEvent.EVENT_REFRESHING -> {
                    refreshButton.isEnabled = !event.state
                    if (event.state) {
                        refreshButton.startAnimation(rotation)
                    }
                    else
                        refreshButton.clearAnimation()
                }
                BluetoothEvent.EVENT_ENABLED -> {
                    refreshButton.isEnabled = event.state
                }
            }
        })

        refreshButton.setOnClickListener {
            parentActivity.refreshBluetooth()
        }
        listView.adapter = deviceBindingViewModel.bluetoothListAdapter
        parentActivity.refreshBluetooth()
    }
}