package com.example.memoraid

import RegisterViewModel
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentRegisterOptionalAccountInformationBinding
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterOptionalAccountInformationFragment : Fragment() {
    private lateinit var binding: FragmentRegisterOptionalAccountInformationBinding
    private val sharedViewModel: RegisterViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.profilePicture.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize view binding
        binding = FragmentRegisterOptionalAccountInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var selectedYear = 0
        var selectedMonth = 0
        var selectedDayOfMonth = 0

        // Capture the date from the CalendarView
        binding.registerBirthdate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDayOfMonth = dayOfMonth
        }

        binding.addPictureButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Handle the continue button click
        binding.secondRegisterContinueButton.setOnClickListener {
            val firstName = binding.registerFirstname.text.toString().trim()
            val lastName = binding.registerLastname.text.toString().trim()
            val phoneNumber = binding.registerPhoneNumber.text.toString().trim()

            // Format the selected date
            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthdate = dateFormat.format(calendar.time)

            // Upload the image or use the default image
            if (selectedImageUri != null) {
                uploadImageToFirebase()  // Upload the selected image
            } else {
                sharedViewModel.setProfilePicture("default_image_url")  // Set default image URL if no image is selected
            }

            // Pass data to the ViewModel
            sharedViewModel.setFirstName(if (firstName.isEmpty()) "No first name" else firstName)
            sharedViewModel.setLastName(if (lastName.isEmpty()) "No last name" else lastName)
            sharedViewModel.setPhoneNumber(if (phoneNumber.isEmpty()) "No phone number" else phoneNumber)
            sharedViewModel.setBirthdate(if (birthdate.isEmpty()) "No birthdate" else birthdate)

            findNavController().navigate(R.id.fragment_register_patients)
        }
    }

    private fun uploadImageToFirebase() {
        val storageRef = storage.reference.child("profile_pictures/${System.currentTimeMillis()}.jpg")
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Set the download URL in the ViewModel after the upload is successful
                        sharedViewModel.setProfilePicture(downloadUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}