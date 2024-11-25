package com.example.memoraid
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController

class RegisterAccountInformationFragment : Fragment() {
//    private lateinit var navController: NavController
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register_account_information, container, false)
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        navController = NavHostFragment.findNavController(this)
//
//        val firstContinueButton: Button = view.findViewById(R.id.first_register_continue_button)
//
//        firstContinueButton.setOnClickListener {
//            findNavController().navigate(R.id.fragment_register_optional_account_info)
//        }
//    }
}
