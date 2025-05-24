package com.roxanasultan.memoraid.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentRegisterDetailsBinding
import com.roxanasultan.memoraid.viewmodels.RegisterSharedViewModel
import com.roxanasultan.memoraid.viewmodels.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RegisterDetailsFragment : Fragment() {

    private var _binding: FragmentRegisterDetailsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RegisterSharedViewModel by activityViewModels()
    private val registerViewModel: RegisterViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sharedViewModel.clearData()
                findNavController().navigateUp()
            }
        })

        setupDatePickers()
        autofillFromGoogle()

        binding.secondRegisterContinueButton.setOnClickListener {
            val day = binding.dayPicker.value
            val month = binding.monthPicker.value
            val year = binding.yearPicker.value
            handleContinue(year, month, day)
        }

        binding.profilePicture.setOnClickListener {
            showModal()
        }
    }

    private fun autofillFromGoogle() {
        val googleName = arguments?.getString("google_name")
        val googleEmail = arguments?.getString("google_email")
        val googlePhoto = arguments?.getString("google_photo")

        googleName?.let {
            val parts = it.trim().split(" ")
            if (parts.size >= 2) {
                binding.registerFirstname.setText(parts[0])
                binding.registerLastname.setText(parts[1])
            } else if (parts.isNotEmpty()) {
                binding.registerFirstname.setText(parts[0])
            }
        }

        googleEmail?.let {
            val username = it.substringBefore("@")
            binding.registerUsername.setText(username)
            sharedViewModel.setEmail(it)
        }

        googlePhoto?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)
            selectedImageUri = Uri.parse(it)
        }
    }

    private fun handleContinue(year: Int, month: Int, dayOfMonth: Int) {
        val username = binding.registerUsername.text.toString().trim()
        val firstName = binding.registerFirstname.text.toString().trim()
        val lastName = binding.registerLastname.text.toString().trim()
        val phoneNumber = binding.registerPhoneNumber.text.toString().trim()
        val birthdate = String.format("%02d-%02d-%04d", dayOfMonth, month, year)

        sharedViewModel.setFirstName(if (firstName.isEmpty()) "No first name" else firstName)
        sharedViewModel.setLastName(if (lastName.isEmpty()) "No last name" else lastName)
        sharedViewModel.setPhoneNumber(if (phoneNumber.isEmpty()) "No phone number" else phoneNumber)
        sharedViewModel.setBirthdate(birthdate)

        if (selectedImageUri != null) {
            sharedViewModel.setProfilePicture(selectedImageUri.toString())
        }

        lifecycleScope.launch {
            if (validateUsername(username)) {
                findNavController().navigate(R.id.action_registerDetailsFragment_to_registerPatientsFragment)
            }
        }
    }

    private suspend fun validateUsername(username: String): Boolean {
        if (username.trim().contains(" ")) {
            Toast.makeText(requireContext(), "Username cannot contain spaces.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!registerViewModel.isUsernameUnique(username)) {
            Toast.makeText(requireContext(), "Username is already taken.", Toast.LENGTH_SHORT).show()
            return false
        }
        sharedViewModel.setUsername(username)
        return true
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        binding.dayPicker.minValue = 1
        binding.dayPicker.maxValue = 31
        binding.monthPicker.minValue = 1
        binding.monthPicker.maxValue = 12
        val currentYear = calendar.get(Calendar.YEAR)
        binding.yearPicker.minValue = 1900
        binding.yearPicker.maxValue = currentYear
        binding.yearPicker.value = 2000
    }

    private fun showModal() {
        val options = arrayOf("Take a Picture", "Choose from Gallery", "Remove Picture")
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkAndRequestPermissions()
                    1 -> chooseImageFromGallery()
                    2 -> removeProfilePicture()
                }
            }.show()
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

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private fun chooseImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun removeProfilePicture() {
        binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
        selectedImageUri = null
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).placeholder(R.drawable.default_profile_picture).into(binding.profilePicture)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            Glide.with(this).load(photoUri).placeholder(R.drawable.default_profile_picture).into(binding.profilePicture)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}