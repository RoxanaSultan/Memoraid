package com.roxanasultan.memoraid.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.AuthenticationActivity
import com.roxanasultan.memoraid.databinding.FragmentAccountBinding
import com.roxanasultan.memoraid.services.LocationForegroundService
import com.roxanasultan.memoraid.viewmodel.AccountViewModel
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountViewModel by viewModels()

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001
    private val SEND_SMS_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)

        accountViewModel.loadUser()

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

        if (hasLocationPermission()) {
            startLocationService()
        } else {
            requestLocationPermission()
        }

        binding.emergencyButton.setOnClickListener {
            val emergencyNumber = accountViewModel.user.value?.emergencyNumber
            emergencyNumber?.let {
                getLocationAndSendSms(it)
            } ?: Toast.makeText(requireContext(), "Emergency number unavailable!", Toast.LENGTH_SHORT).show()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return binding.root
    }

    private fun startLocationService() {
        val serviceIntent = Intent(requireContext(), LocationForegroundService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun hasSendSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSendSmsPermission() {
        requestPermissions(arrayOf(Manifest.permission.SEND_SMS), SEND_SMS_PERMISSION_REQUEST_CODE)
    }

    private fun getLocationAndSendSms(emergencyNumber: String) {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }
        if (!hasSendSmsPermission()) {
            requestSendSmsPermission()
            return
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val patient = accountViewModel.user.value?.username
                    val locationUrl = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                    val message = "EMERGENCY ALERT!\nYour patient: $patient needs help! Location: $locationUrl"
                    sendEmergencySms(emergencyNumber, message)
                } ?: Toast.makeText(requireContext(), "Location is not available.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location access denied!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmergencySms(phoneNumber: String, message: String) {
        if (!hasSendSmsPermission()) {
            requestSendSmsPermission()
            return
        }

        val smsManager = android.telephony.SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        Toast.makeText(requireContext(), "Emergency message sent", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService()
                } else {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            SEND_SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Permission to send SMS has been granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ -> logout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        accountViewModel.logout()
        startActivity(Intent(requireContext(), AuthenticationActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}