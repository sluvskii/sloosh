package com.slooshfilm.app.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MoviesFragment()
            1 -> SeriesFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}