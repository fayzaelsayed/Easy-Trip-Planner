package com.example.tripplanner.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tripplanner.ui.pasttrips.PastTripsFragment
import com.example.tripplanner.ui.upcoming.UpcomingTripsFragment

class HomeViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0)
            UpcomingTripsFragment()
        else
            PastTripsFragment()
    }
}