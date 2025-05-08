package com.example.memoraid.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.memoraid.activities.AuthenticationActivity
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentAccountPatientBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.app.AlertDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Patterns
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.memoraid.viewmodel.AccountCaretakerViewModel
import java.text.ParseException

@AndroidEntryPoint
class AccountPatientFragment : Fragment() {

    private var _binding: FragmentAccountPatientBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountCaretakerViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    private var isEmailChanged = false
    private var isImageRemoved = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountPatientBinding.inflate(inflater, container, false)
        val view = binding.root

        accountViewModel.loadPatient()
        observeSelectedPatient()

        binding.editPictureButton.setOnClickListener {
            showModal()
        }

        binding.switchPatientButton.setOnClickListener {
            showDropDown()
        }

        binding.saveChangesButton.setOnClickListener {
            lifecycleScope.launch {
                saveUpdates()
                if (isEmailChanged) {
                    showReauthenticationDialog()
                }
            }
        }

        binding.profilePicture.setOnClickListener {
            val imageUrl = accountViewModel.selectedPatient.value?.profilePictureUrl
            if (!imageUrl.isNullOrEmpty()) {
                val bundle = Bundle().apply {
                    putString("image", imageUrl)
                }
                findNavController().navigate(R.id.action_accountPatientFragment_to_fullScreenImageFragment, bundle)
            }
        }

        binding.locationButton.setOnClickListener {
            findNavController().navigate(R.id.action_accountPatientFragment_to_patientLocationFragment)
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return view
    }

    private fun observeSelectedPatient() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.selectedPatient.collectLatest { patient ->
                    patient?.let {
                        binding.username.setText(it.username ?: "")
                        binding.email.setText(it.email ?: "")
                        binding.firstName.setText(it.firstName ?: "")
                        binding.lastName.setText(it.lastName ?: "")
                        binding.phoneNumber.setText(it.phoneNumber ?: "")
                        binding.birthdate.setText(it.birthdate ?: "")

                        Glide.with(this@AccountPatientFragment)
                            .load(it.profilePictureUrl)
                            .placeholder(R.drawable.default_profile_picture)
//                            .circleCrop()
                            .into(binding.profilePicture)
                    }
                }
            }
        }
    }


    private fun showDropDown() {
        val popupMenu = PopupMenu(requireContext(), binding.switchPatientButton)

        lifecycleScope.launch {
            accountViewModel.getOtherPatients()
            accountViewModel.availablePatients.collectLatest { patients ->
                popupMenu.menu.clear()
                patients.forEachIndexed { index, patient ->
                    if (patient != null) {
                        popupMenu.menu.add(0, index, index, patient.username ?: "Unknown")
                    }
                }

                popupMenu.setOnMenuItemClickListener { menuItem ->
                    val selectedPatient = patients[menuItem.itemId]
                    if (selectedPatient != null) {
                        accountViewModel.selectPatient(selectedPatient.id)
                    }
                    true
                }

                popupMenu.show()
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showReauthenticationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Re-login Required")
            .setMessage("Your email has been changed. You need to log in again.")
            .setPositiveButton("OK") { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        accountViewModel.logout()
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.default_profile_picture)
//                .circleCrop()
                .into(binding.profilePicture)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            Glide.with(this)
                .load(selectedImageUri)
                .placeholder(R.drawable.default_profile_picture)
//                .circleCrop()
                .into(binding.profilePicture)
        }
    }

    private fun showModal() {
        val hasPicture = accountViewModel.selectedPatient.value?.profilePictureUrl != null

        val options = if (hasPicture) {
            arrayOf("Take a Picture", "Choose from Gallery", "Remove Picture")
        } else {
            arrayOf("Take a Picture", "Choose from Gallery")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkAndRequestPermissions()
                    1 -> chooseImageFromGallery()
                    2 -> if (hasPicture) removeProfilePicture()
                }
            }
            .show()
    }

    private fun removeProfilePicture() {
        binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
        selectedImageUri = null
        isImageRemoved = true
    }

    private fun checkAndRequestPermissions() {
        val cameraPermission = android.Manifest.permission.CAMERA
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
            "com.example.memoraid.fileprovider",
            photoFile
        )
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

    private fun saveUpdates() {
        val username = binding.username.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val phoneNumber = binding.phoneNumber.text.toString().trim()
        val birthdate = binding.birthdate.text.toString().trim()
        var didAnythingChange = false

        lifecycleScope.launch {
            if (username != accountViewModel.selectedPatient.value?.username) {
                if (validateUsername(username)) {
                    didAnythingChange = true
                }
            }
            if (email != accountViewModel.selectedPatient.value?.email) {
                if (validateEmailAddress(email)) {
                    didAnythingChange = true
                    isEmailChanged = true
                }
            }
            if (firstName != accountViewModel.selectedPatient.value?.firstName) {
                didAnythingChange = true
            }
            if (lastName != accountViewModel.selectedPatient.value?.lastName) {
                didAnythingChange = true
            }
            if (phoneNumber != accountViewModel.selectedPatient.value?.phoneNumber) {
                if (validatePhone(phoneNumber)) {
                    didAnythingChange = true
                }
            }
            if (birthdate != accountViewModel.selectedPatient.value?.birthdate ) {
                if (validateBirthdate(birthdate)) {
                    didAnythingChange = true
                }
            }

            if (selectedImageUri != null && !isImageRemoved) {
                accountViewModel.selectedPatient.value?.profilePictureUrl?.let { profilePictureUrl ->
                    accountViewModel.deleteImageFromStorage(profilePictureUrl)
                }

                accountViewModel.uploadAndSaveProfilePicture(selectedImageUri!!, accountViewModel.selectedPatient.value?.id!!)
                didAnythingChange = true
            }

            if (isImageRemoved) {
                accountViewModel.removeProfilePicture(accountViewModel.selectedPatient.value?.id!!)
                isImageRemoved = false
                didAnythingChange = true
            }

            if (!didAnythingChange) {
                Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show()
                return@launch
            }

            saveToFirestore(username, email, firstName, lastName, phoneNumber, birthdate)
        }
    }

    private fun saveToFirestore(
        username: String,
        email: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        birthdate: String,
    ) {
        val userUpdates = mapOf(
            "username" to username,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "birthdate" to birthdate
        )

        lifecycleScope.launch {
            val success = accountViewModel.selectedPatient.value?.let { accountViewModel.saveUserDetails(userUpdates, it.id) }
            if (success == true) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun validateUsername(username: String): Boolean {
        return if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
            false
        } else if (username.contains(" ")) {
            Toast.makeText(requireContext(), "Username cannot contain spaces", Toast.LENGTH_SHORT).show()
            false
        } else if (!accountViewModel.isUsernameUnique(username)) {
            Toast.makeText(requireContext(), "Username is already used", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validatePhone(phoneNumber: String): Boolean {
        return if (phoneNumber.contains(" ")) {
            Toast.makeText(requireContext(), "Phone number cannot contain spaces", Toast.LENGTH_SHORT).show()
            false
        } else if (!Patterns.PHONE.matcher(phoneNumber).matches()){
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validateBirthdate(birthdate: String): Boolean {
        if (birthdate.contains(" ")) {
            Toast.makeText(requireContext(), "Birthdate cannot contain spaces", Toast.LENGTH_SHORT).show()
            return false
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        dateFormat.isLenient = false

        return try {
            val parsedDate = dateFormat.parse(birthdate)
            val currentDate = Date()

            if (parsedDate != null && parsedDate.after(currentDate)) {
                Toast.makeText(requireContext(), "Birthdate cannot be in the future", Toast.LENGTH_SHORT).show()
                false
            } else if (parsedDate == null) {
                Toast.makeText(requireContext(), "Invalid birthdate", Toast.LENGTH_SHORT).show()
                false
            } else {
                true
            }
        } catch (e: ParseException) {
            Toast.makeText(requireContext(), "Invalid birthdate", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private suspend fun validateEmailAddress(email: String): Boolean {
        return if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email address", Toast.LENGTH_SHORT).show()
            false
        } else if (!accountViewModel.isEmailUnique(email)) {
            Toast.makeText(requireContext(), "Username is already used", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}