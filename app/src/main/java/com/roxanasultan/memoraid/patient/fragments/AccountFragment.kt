package com.roxanasultan.memoraid.patient.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.activities.AuthenticationActivity
import com.roxanasultan.memoraid.databinding.FragmentAccountBinding
import com.roxanasultan.memoraid.services.LocationForegroundService
import com.roxanasultan.memoraid.patient.viewmodels.AccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 2001
    private val SEND_SMS_PERMISSION_REQUEST_CODE = 1001

    private val prefs by lazy {
        requireContext().getSharedPreferences("memoraid_prefs", MODE_PRIVATE)
    }

    private fun isBiometricEnabledForUser(userId: String): Boolean {
        return prefs.getBoolean("biometric_enabled_for_$userId", false)
    }

    private fun setBiometricEnabledForUser(userId: String, enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled_for_$userId", enabled).apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)

        accountViewModel.loadUser()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

                        binding.checkboxBiometricLogin.isChecked = isBiometricEnabledForUser(it.email)
                    }
                }
            }
        }

        if (hasLocationPermission()) {
            startLocationService()
        } else {
            requestLocationPermission()
        }

        binding.checkboxBiometricLogin.setOnCheckedChangeListener { _, isChecked ->
            val userId = accountViewModel.user.value?.email
            if (userId != null) {
                setBiometricEnabledForUser(userId, isChecked)
            }
        }

        binding.editPictureButton.setOnClickListener {
            showModal()
        }

        binding.profilePicture.setOnClickListener {
            val imageUrl = accountViewModel.user.value?.profilePictureUrl
            if (!imageUrl.isNullOrEmpty()) {
                val bundle = Bundle().apply {
                    putString("image", imageUrl)
                }
                findNavController().navigate(R.id.action_accountFragment_to_fullScreenImageFragment, bundle)
            }
        }

        binding.emergencyButton.setOnClickListener {
            val emergencyNumbers = accountViewModel.user.value?.emergencyNumbers
            if (!emergencyNumbers.isNullOrEmpty()) {
                // Dialog confirmare
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Emergency Alert")
                    .setMessage("Are you sure you want to send emergency messages to your contacts?")
                    .setPositiveButton("Yes") { _, _ ->
                        getLocationAndSendSms(emergencyNumbers)
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Emergency numbers unavailable!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return binding.root
    }

    private fun showModal() {
        val hasPicture = accountViewModel.user.value?.profilePictureUrl != null

        val options = if (hasPicture) {
            arrayOf("Take a Picture", "Choose from Gallery", "Remove Picture")
        } else {
            arrayOf("Take a Picture", "Choose from Gallery")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Picture")
            .setItems(options) { _, which ->
                val userId = accountViewModel.user.value?.id
                val profilePictureUrl = accountViewModel.user.value?.profilePictureUrl

                when (which) {
                    0 -> {
                        checkAndRequestPermissions()
                    }
                    1 -> {
                        chooseImageFromGallery()
                    }
                    2 -> if (hasPicture && userId != null) {
                        removeProfilePicture()
                        accountViewModel.removeProfilePicture(userId)
                    }
                }
            }
            .show()
    }

    private fun removeProfilePicture() {
        binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
        selectedImageUri = null
    }

    private fun checkAndRequestPermissions() {
        val cameraPermission = Manifest.permission.CAMERA
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), cameraPermission)

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(cameraPermission), 1001)
        } else {
            takePicture()
        }
    }

    private fun takePicture() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.roxanasultan.memoraid.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoUri)
    }

    private fun chooseImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it

            // Afișare în UI
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)

            // Upload doar dacă există user
            accountViewModel.user.value?.id?.let { userId ->
                accountViewModel.user.value?.profilePictureUrl?.let { url ->
                    accountViewModel.deleteImageFromStorage(url)
                }

                accountViewModel.uploadAndSaveProfilePicture(it, userId)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri

            Glide.with(this)
                .load(photoUri)
                .placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)

            accountViewModel.user.value?.id?.let { userId ->
                accountViewModel.user.value?.profilePictureUrl?.let { url ->
                    accountViewModel.deleteImageFromStorage(url)
                }

                accountViewModel.uploadAndSaveProfilePicture(photoUri!!, userId)
            }
        }
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
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

    private fun getLocationAndSendSms(emergencyNumbers: List<String>) {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }
        if (!hasSendSmsPermission()) {
            requestSendSmsPermission()
            return
        }

        val userId = accountViewModel.user.value?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }
        accountViewModel.fetchLastRouteLocation(userId)

        viewLifecycleOwner.lifecycleScope.launch {
            accountViewModel.lastRouteLocation
                .filter { it != null }
                .first()
                .let { geoPoint ->
                    val patient = accountViewModel.user.value?.username ?: "Unknown"
                    val locationUrl = "https://maps.google.com/?q=${geoPoint!!.latitude},${geoPoint.longitude}"
                    val message = "EMERGENCY ALERT!\nYour patient: $patient needs help! Location: $locationUrl"

                    emergencyNumbers.forEach { number ->
                        sendEmergencySms(number, message)
                        Log.d("AccountFragment", "Sent SMS to $number: $message")
                    }
                }
        }
    }

    private fun sendEmergencySms(phoneNumber: String, message: String) {
        if (!hasSendSmsPermission()) {
            requestSendSmsPermission()
            return
        }

        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        Toast.makeText(requireContext(), "Emergency message sent to $phoneNumber", Toast.LENGTH_SHORT).show()
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