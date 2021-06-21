package com.snapkirin.homesecurity.ui.devicedetail.fragments

import com.snapkirin.homesecurity.ui.alarmlist.AlarmListFragment
import com.snapkirin.homesecurity.ui.devicedetail.DeviceDetailViewModel

class AlarmsFragment : AlarmListFragment<DeviceDetailViewModel>(DeviceDetailViewModel::class.java)
