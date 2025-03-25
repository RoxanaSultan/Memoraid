package com.example.memoraid

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HealthPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppointmentsFragment()
            1 -> PillsFragment()
            2 -> HabitsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
