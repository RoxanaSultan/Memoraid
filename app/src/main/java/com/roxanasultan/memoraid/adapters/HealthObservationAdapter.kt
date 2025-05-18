package com.roxanasultan.memoraid.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.roxanasultan.memoraid.fragments.HabitsCaretakerFragment
import com.roxanasultan.memoraid.fragments.AppointmentsCaretakerFragment
import com.roxanasultan.memoraid.fragments.MedicineCaretakerFragment

class HealthObservationAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppointmentsCaretakerFragment()
            1 -> MedicineCaretakerFragment()
            2 -> HabitsCaretakerFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
