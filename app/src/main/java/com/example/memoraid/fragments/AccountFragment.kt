package com.example.memoraid.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.memoraid.activities.AuthenticationActivity
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentAccountBinding
import com.example.memoraid.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountFragment : Fragment(R.layout.fragment_account) {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val view = binding.root

        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        currentUser?.let {
            accountViewModel.loadUser(it)
        }

        lifecycleScope.launch {
            accountViewModel.user.collectLatest { user ->
                user?.let {
                    binding.username.text = it.username
                    binding.email.text = it.email
                    binding.firstName.text = it.firstName
                    binding.lastName.text = it.lastName
                    binding.phoneNumber.text = it.phoneNumber
                    binding.birthdate.text = it.birthdate

                    Glide.with(this@AccountFragment)
                        .load(it.profilePictureUrl)
                        .placeholder(R.drawable.default_profile_picture)
                        .into(binding.profilePicture)
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthenticationActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}