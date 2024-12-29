package com.example.memoraid

import RegisterViewModel
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterOptionalAccountInformationBinding
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterOptionalAccountInformationFragment : Fragment() {

    private lateinit var binding: FragmentRegisterOptionalAccountInformationBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.profilePicture.setImageURI(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            binding.profilePicture.setImageURI(photoUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterOptionalAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var selectedYear = 0
        var selectedMonth = 0
        var selectedDayOfMonth = 0

        // CalendarView date selection
        binding.registerBirthdate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDayOfMonth = dayOfMonth
        }

        binding.addPictureButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.secondRegisterContinueButton.setOnClickListener {
            handleContinue(selectedYear, selectedMonth, selectedDayOfMonth)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take a Picture", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkAndRequestPermissions()
                    1 -> chooseImageFromGallery()
                }
            }
            .show()
    }

    private fun checkAndRequestPermissions() {
        val cameraPermission = android.Manifest.permission.CAMERA
        if (requireContext().checkSelfPermission(cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(cameraPermission), 1001)
        } else {
            takePicture()
        }
    }

    private fun takePicture() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(requireContext(), "com.example.memoraid.fileprovider", photoFile)
        cameraLauncher.launch(photoUri)
    }

    private fun chooseImageFromGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private fun handleContinue(year: Int, month: Int, dayOfMonth: Int) {
        val firstName = binding.registerFirstname.text.toString().trim()
        val lastName = binding.registerLastname.text.toString().trim()
        val phoneNumber = binding.registerPhoneNumber.text.toString().trim()

        val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
        val birthdate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

        if (selectedImageUri != null) {
            uploadImageToFirebase { imageUrl ->
                sharedViewModel.setProfilePicture(imageUrl)
                proceedToNextStep(firstName, lastName, phoneNumber, birthdate)
            }
        } else {
            sharedViewModel.setProfilePicture("default_image_url")
            proceedToNextStep(firstName, lastName, phoneNumber, birthdate)
        }
    }

    private fun uploadImageToFirebase(onSuccess: (String) -> Unit) {
        val storageRef = storage.reference.child("profile_pictures/${System.currentTimeMillis()}.jpg")
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        onSuccess(downloadUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun proceedToNextStep(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        birthdate: String
    ) {
        sharedViewModel.setFirstName(if (firstName.isEmpty()) "No first name" else firstName)
        sharedViewModel.setLastName(if (lastName.isEmpty()) "No last name" else lastName)
        sharedViewModel.setPhoneNumber(if (phoneNumber.isEmpty()) "No phone number" else phoneNumber)
        sharedViewModel.setBirthdate(if (birthdate.isEmpty()) "No birthdate" else birthdate)

        findNavController().navigate(R.id.fragment_register_patients)
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
