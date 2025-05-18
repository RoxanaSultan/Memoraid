package com.roxanasultan.memoraid.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.roxanasultan.memoraid.fragments.HabitsFragment
import com.roxanasultan.memoraid.fragments.AppointmentsFragment
import com.roxanasultan.memoraid.fragments.MedicineFragment

class HealthPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppointmentsFragment()
            1 -> MedicineFragment()
            2 -> HabitsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
