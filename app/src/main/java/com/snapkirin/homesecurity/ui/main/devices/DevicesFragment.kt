package com.snapkirin.homesecurity.ui.main.devices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Device
import com.snapkirin.homesecurity.ui.main.MainActivity
import com.snapkirin.homesecurity.ui.main.MainViewModel
import com.snapkirin.homesecurity.ui.main.model.ItemListChangeEvent
import com.snapkirin.homesecurity.ui.main.model.PullItemListRequest

class DevicesFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel

    private lateinit var parentActivity: MainActivity

    private lateinit var deviceListView: RecyclerView
    private lateinit var deviceListLayout: SwipeRefreshLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_devices, container, false)

        deviceListView = root.findViewById(R.id.deviceListView)
        deviceListLayout = root.findViewById(R.id.deviceListRefreshLayout)
        deviceListLayout.setColorSchemeResources(R.color.design_default_color_primary)

        parentActivity = activity as MainActivity

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        if (mainViewModel.isInitialing) {
            deviceListLayout.isRefreshing = true
        }

        deviceListView.adapter = mainViewModel.deviceListAdapter

        mainViewModel.deviceListChange.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            mainViewModel.devicesListChanged(event)
        })

        mainViewModel.requestDataResult.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer
            when (result.itemCategory) {
                PullItemListRequest.CAT_DEVICE -> {
                    when (result.pullMode) {
                        PullItemListRequest.MODE_REFRESH -> {
                            deviceListLayout.isRefreshing = false
                        }
                    }
                }
                PullItemListRequest.CAT_INIT -> {
                    deviceListLayout.isRefreshing = false
                }
            }
        })

        mainViewModel.deviceItemClicked.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            if (event.show && event.userId != null && event.device != null) {
                startDeviceDetailActivity(event.userId, event.device.id)
            }
        })

        deviceListLayout.setOnRefreshListener {
            mainViewModel.pullDevices()
        }

        return root
    }

    private fun startDeviceDetailActivity(userId: Long, deviceId: Long) {
        parentActivity.startDeviceDetailActivity(userId, deviceId)
    }

    override fun onResume() {
        super.onResume()
        Log.d("DevicesFragment", "On resume")
        if (!mainViewModel.isInitialing) {
            mainViewModel.pullDevices()
        }
    }

    override fun onStop() {
        mainViewModel.clearLiveData()
        super.onStop()
    }
}