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
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentRegisterOptionalAccountInformationBinding
import com.roxanasultan.memoraid.viewmodels.RegisterSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RegisterOptionalAccountInformationFragment : Fragment() {

    private var _binding: FragmentRegisterOptionalAccountInformationBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RegisterSharedViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterOptionalAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editPictureButton.setOnClickListener {
            showModal()
        }

        binding.secondRegisterContinueButton.setOnClickListener {
            val datePicker = binding.registerBirthdate
            val firstName = binding.registerFirstname.text.toString().trim()
            val lastName = binding.registerLastname.text.toString().trim()

            val calendar = Calendar.getInstance().apply { set(datePicker.year, datePicker.month, datePicker.dayOfMonth) }
            val birthdate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

            if (selectedImageUri != null) {
                sharedViewModel.setProfilePicture(selectedImageUri.toString())
            }

            sharedViewModel.setFirstName(if (firstName.isEmpty()) "No first name" else firstName)
            sharedViewModel.setLastName(if (lastName.isEmpty()) "No last name" else lastName)
            sharedViewModel.setBirthdate(if (birthdate.isEmpty()) "No birthdate" else birthdate)

            findNavController().navigate(R.id.action_registerOptionalAccountInfoFragment_to_registerPatientsFragment)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sharedViewModel.clearData()
                findNavController().navigateUp()
            }
        })
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
            Glide.with(this).load(photoUri).placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take pictures.", Toast.LENGTH_SHORT).show()
        }
    }
}