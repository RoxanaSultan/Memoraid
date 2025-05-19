package com.roxanasultan.memoraid.caretaker.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.roxanasultan.memoraid.caretaker.fragments.HabitsFragment
import com.roxanasultan.memoraid.caretaker.fragments.AppointmentsFragment
import com.roxanasultan.memoraid.caretaker.fragments.MedicationFragment

class HealthAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppointmentsFragment()
            1 -> MedicationFragment()
            2 -> HabitsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
