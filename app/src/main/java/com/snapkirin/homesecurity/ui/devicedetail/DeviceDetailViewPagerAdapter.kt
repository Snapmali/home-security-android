package com.snapkirin.homesecurity.ui.devicedetail

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.snapkirin.homesecurity.ui.devicedetail.fragments.AlarmsFragment
import com.snapkirin.homesecurity.ui.devicedetail.fragments.DeviceOptionsFragment

class DeviceDetailViewPagerAdapter(fragmentActivity: FragmentActivity)
    : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                AlarmsFragment()
            }
            else -> {
                DeviceOptionsFragment.newInstance()
            }
        }
    }
}