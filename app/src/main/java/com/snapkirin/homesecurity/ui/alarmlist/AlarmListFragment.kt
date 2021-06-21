package com.snapkirin.homesecurity.ui.alarmlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.snapkirin.homesecurity.R
import com.snapkirin.homesecurity.model.Alarm
import com.snapkirin.homesecurity.ui.main.model.PullItemListRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val IS_BOTTOM_LOADING = "is_bottom_loading"

open class AlarmListFragment<T : AlarmListViewModel>(
    private val alarmListViewModelClass: Class<T>
) : Fragment() {

    private var isBottomLoading = false

    private lateinit var listViewModel: T

    private lateinit var alarmListView: RecyclerView
    private lateinit var alarmListLayout: SwipeRefreshLayout

    private lateinit var parentActivity: AppCompatActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_alarms, container, false)

        parentActivity = activity as AppCompatActivity

        alarmListView = root.findViewById(R.id.alarmListView)
        alarmListLayout = root.findViewById(R.id.alarmListRefreshLayout)
        alarmListLayout.setColorSchemeResources(R.color.design_default_color_primary)

        listViewModel = ViewModelProvider(requireActivity()).get(alarmListViewModelClass)

        if (listViewModel.isInitialing) {
            alarmListLayout.isRefreshing = true
        } else {
            listViewModel.pullNewAlarms()
        }

        listViewModel.alarmItemViewDelegate.clearDevicesInfo()

        alarmListView.adapter = listViewModel.alarmItemListAdapter

        listViewModel.alarmListChange.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            listViewModel.alarmsListChanged(event)
        })

        listViewModel.requestDataResult.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer
            when (result.itemCategory) {
                PullItemListRequest.CAT_ALARM -> {
                    when (result.pullMode) {
                        PullItemListRequest.MODE_NEW -> {
                            alarmListLayout.isRefreshing = false
                        }
                        PullItemListRequest.MODE_OLD -> {
                            isBottomLoading = false
                            listViewModel.removeAlarmItemListLoading()
                        }
                    }
                }
                PullItemListRequest.CAT_INIT -> {
                    alarmListLayout.isRefreshing = false
                }
            }
        })

        if (savedInstanceState != null) {
            isBottomLoading = savedInstanceState.getBoolean(IS_BOTTOM_LOADING)
            Log.e("loading", "$isBottomLoading")
        }

        listViewModel.alarmItemClicked.observe(viewLifecycleOwner, Observer {
            val event = it ?: return@Observer
            if (event.show) {
                showAlarmDetailDialog(event.alarm!!, event.deviceName!!)
            }
        })

        alarmListLayout.setOnRefreshListener {
            listViewModel.pullNewAlarms()
        }

        alarmListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var lastVisibleItemPosition = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                recyclerView.layoutManager?.let {
                    if (dy < 0)
                        return
                    lastVisibleItemPosition =
                        (it as LinearLayoutManager).findLastVisibleItemPosition()
                    if (!isBottomLoading
                        && !listViewModel.noMoreOlderAlarms()
                        && lastVisibleItemPosition + 1 == recyclerView.adapter?.itemCount
                    ) {
                        isBottomLoading = true
                        GlobalScope.launch(Dispatchers.Main) {
                            listViewModel.addAlarmItemListLoading()
                        }
                        listViewModel.pullOldAlarms()
                    }
                }
            }
        })

        return root
    }

    private fun showAlarmDetailDialog(alarm: Alarm, deviceName: String) {
        val dialog = AlarmDetailDialog.newInstance(listViewModel.userId, alarm, deviceName)
        dialog.show(parentActivity.supportFragmentManager, "alarm_detail_dialog")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_BOTTOM_LOADING, isBottomLoading)
    }

    override fun onStop() {
        listViewModel.clearLiveData()
        super.onStop()
    }
}