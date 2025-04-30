package com.example.memoraid.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.memoraid.fragments.HabitsFragment
import com.example.memoraid.fragments.AppointmentsCaretakerFragment
import com.example.memoraid.fragments.MedicineFragment

class HealthObservationAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AppointmentsCaretakerFragment()
            1 -> MedicineFragment()
            2 -> HabitsFragment()
            else -> throw IllegalArgumentException("Invalid tab position")
        }
    }
}
