package com.example.memoraid

import AccountViewModel
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val accountViewModel: AccountViewModel by activityViewModels()
//    private var selectedImageUri: Uri? = null
//    private var isImageDeleted = false
//    private var photoUri: Uri? = null
//    private val storage = FirebaseStorage.getInstance()
//    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val view = binding.root
        db = FirebaseFirestore.getInstance()

        if (accountViewModel.username.value.isNullOrEmpty()) {
            val currentUser = auth.currentUser?.uid ?: ""
            if (currentUser.isNotEmpty()) {
                fetchUserData(currentUser)
            }
        } else {
            binding.username.setText(accountViewModel.username.value ?: "")
            binding.email.setText(accountViewModel.email.value ?: "")
            binding.firstName.setText(accountViewModel.firstName.value ?: "")
            binding.lastName.setText(accountViewModel.lastName.value ?: "")
            binding.phoneNumber.setText(accountViewModel.phoneNumber.value ?: "")
            binding.birthdate.setText(accountViewModel.birthdate.value ?: "")

            val profilePictureUrl = accountViewModel.profilePictureUrl.value
            Glide.with(this).load(profilePictureUrl)
                .placeholder(R.drawable.default_profile_picture)
                .into(binding.profilePicture)
        }
//        if (user == null)
//        {
//            val currentUser = auth.currentUser?.uid ?: ""
//            if (currentUser.isNotEmpty()) {
//                fetchUserData(currentUser)
//            }
//        }
//        else
//        {
//            binding.username.text = user!!.username
//            binding.email.text = user!!.email
//            binding.firstName.text = user!!.firstName
//            binding.lastName.text = user!!.lastName
//            binding.phoneNumber.text = user!!.phoneNumber
//            binding.birthdate.text = user!!.birthdate
//
//            // Load profile picture using Glide (if stored in ViewModel)
//            val profilePictureUrl = user!!.profilePictureUrl
//            Glide.with(this).load(profilePictureUrl)
//                .placeholder(R.drawable.default_profile_picture)
//                .into(binding.profilePicture)
//        }

        binding.notificationsButton.setOnClickListener {

        }

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
//            findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
            val intent = Intent(requireContext(), AuthenticationActivity::class.java)
            startActivity(intent)
        }
//
//        binding.saveChangesButton.setOnClickListener {
//            saveUpdates()
//        }
//
//        binding.changePasswordButton.setOnClickListener {
//            findNavController().navigate(R.id.action_accountFragment_to_changePasswordFragment)
//        }
//
//        binding.changePictureButton.setOnClickListener {
//            showImageSourceDialog()
//        }
//
//        binding.deletePictureButton.setOnClickListener {
//            isImageDeleted = true
//            binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
//        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
    }
//
//    private fun update(userId: String) {
//        binding.accountProgressBar.visibility = View.VISIBLE
//        val defaultProfilePictureUrl = "https://firebasestorage.googleapis.com/v0/b/memoraid-application.firebasestorage.app/o/profile_pictures%2Fdefault_profile_picture.png?alt=media&token=fa0aea7d-b11e-49f9-95f7-4b9f820e3942"
//
//        db.collection("users").document(userId)
//            .update("profilePictureUrl", defaultProfilePictureUrl)
//            .addOnSuccessListener {
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to update Firestore", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun deleteProfilePictureFromStorage(imageUrl: String) {
//        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
//        storageRef.delete()
//    }
//
//
//    private fun saveUpdates() {
//        val username = binding.username.text.toString().trim()
//        val email = binding.email.text.toString().trim()
//        val firstName = binding.firstName.text.toString().trim()
//        val lastName = binding.lastName.text.toString().trim()
//        val phoneNumber = binding.phoneNumber.text.toString().trim()
//        val birthdate = binding.birthdate.text.toString().trim()
//        var didAnythingChange = false
//
//        if (username != accountViewModel.username.value) {
//            lifecycleScope.launch {
//                if (validateUsername(username)) {
//                    accountViewModel.setUsername(username)
//                    didAnythingChange = true
//                }
//            }
//        }
//        if (firstName != accountViewModel.firstName.value) {
//            accountViewModel.setFirstName(firstName)
//            didAnythingChange = true
//        }
//        if (lastName != accountViewModel.lastName.value) {
//            accountViewModel.setLastName(lastName)
//            didAnythingChange = true
//        }
//        if (phoneNumber != accountViewModel.phoneNumber.value) {
//            accountViewModel.setPhoneNumber(phoneNumber)
//            didAnythingChange = true
//        }
//        if (birthdate != accountViewModel.birthdate.value) {
//            accountViewModel.setBirthdate(birthdate)
//            didAnythingChange = true
//        }
//
//        if (selectedImageUri != null) {
//            didAnythingChange = true
//        }
//
//        if (isImageDeleted) {
//            didAnythingChange = true
//        }
//
//        if (!didAnythingChange) {
//            Toast.makeText(requireContext(), "No changes detected", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        saveToFirestore(username, email, firstName, lastName, phoneNumber, birthdate)
//    }

//    private fun saveToFirestore(
//        username: String,
//        email: String,
//        firstName: String,
//        lastName: String,
//        phoneNumber: String,
//        birthdate: String,
//    ) {
//        binding.accountProgressBar.visibility = View.VISIBLE
//        val userId = auth.currentUser?.uid ?: ""
//        if (userId.isNotEmpty()) {
//            val userUpdates = hashMapOf(
//                "username" to username,
//                "email" to email,
//                "firstName" to firstName,
//                "lastName" to lastName,
//                "phoneNumber" to phoneNumber,
//                "birthdate" to birthdate
//            )
//
//            if (selectedImageUri != null) {
//                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(accountViewModel.profilePictureUrl.value!!)
//
//                storageRef.delete()
//                    .addOnSuccessListener {
//                        update(userId)
//                    }
//                    .addOnFailureListener {
//                        Toast.makeText(requireContext(), "Failed to delete profile picture", Toast.LENGTH_SHORT).show()
//                    }
//
//                uploadImageToFirebase { imageUrl ->
//                    accountViewModel.setProfilePicture(imageUrl)
//                    val userId = auth.currentUser?.uid ?: ""
//                    db.collection("users").document(userId)
//                        .update("profilePictureUrl", imageUrl)
//                }
//            }
//
//            db.collection("users").document(userId)
//                .update(userUpdates as MutableMap<String, Any>)
//                .addOnSuccessListener {
//                    binding.accountProgressBar.visibility = View.GONE
//                    Toast.makeText(requireContext(), "Changes saved successfully", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener { exception ->
//                    binding.accountProgressBar.visibility = View.GONE
//                    Toast.makeText(requireContext(), "Error saving changes: ${exception.message}", Toast.LENGTH_LONG).show()
//                }
//        }
//    }
//
//    private fun saveToFirestore(
//        username: String,
//        email: String,
//        firstName: String,
//        lastName: String,
//        phoneNumber: String,
//        birthdate: String,
//    ) {
//        val userId = auth.currentUser?.uid ?: ""
//
//        if (userId.isNotEmpty()) {
//            val userUpdates = hashMapOf(
//                "username" to username,
//                "email" to email,
//                "firstName" to firstName,
//                "lastName" to lastName,
//                "phoneNumber" to phoneNumber,
//                "birthdate" to birthdate
//            )
//
//            binding.progressContainer.visibility = View.VISIBLE
//            binding.accountProgressBar.visibility = View.VISIBLE
//
//            if (selectedImageUri != null) {
//                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(accountViewModel.profilePictureUrl.value!!)
//
//                if (accountViewModel.profilePictureUrl.value != "https://firebasestorage.googleapis.com/v0/b/memoraid-application.firebasestorage.app/o/profile_pictures%2Fdefault_profile_picture.png?alt=media&token=fa0aea7d-b11e-49f9-95f7-4b9f820e3942") {
//                    storageRef.delete()
//                        .addOnSuccessListener {
//                            uploadImageToFirebase { imageUrl ->
//                                accountViewModel.setProfilePicture(imageUrl)
//                                db.collection("users").document(userId)
//                                    .update("profilePictureUrl", imageUrl)
//                                    .addOnSuccessListener {
//                                        updateUserFields(userId, userUpdates)
//                                    }
//                                    .addOnFailureListener { exception ->
//                                        Toast.makeText(
//                                            requireContext(),
//                                            "Error updating image: ${exception.message}",
//                                            Toast.LENGTH_LONG
//                                        ).show()
//                                    }
//                            }
//                        }
//                        .addOnFailureListener {
//                            Toast.makeText(
//                                requireContext(),
//                                "Failed to delete profile picture",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                } else {
//                    uploadImageToFirebase { imageUrl ->
//                        accountViewModel.setProfilePicture(imageUrl)
//                        db.collection("users").document(userId)
//                            .update("profilePictureUrl", imageUrl)
//                            .addOnSuccessListener {
//                                updateUserFields(userId, userUpdates)
//                            }
//                            .addOnFailureListener { exception ->
//                                Toast.makeText(
//                                    requireContext(),
//                                    "Error updating image: ${exception.message}",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            }
//                    }
//                }
//            } else if (isImageDeleted) {
//                binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
//
//                val currentProfilePictureUrl = accountViewModel.profilePictureUrl.value
//                if (!currentProfilePictureUrl.isNullOrEmpty()) {
//                    deleteProfilePictureFromStorage(currentProfilePictureUrl)
//                }
//
//                db.collection("users").document(userId)
//                    .update("profilePictureUrl", "https://firebasestorage.googleapis.com/v0/b/memoraid-application.firebasestorage.app/o/profile_pictures%2Fdefault_profile_picture.png?alt=media&token=fa0aea7d-b11e-49f9-95f7-4b9f820e3942")
//                    .addOnSuccessListener {
//                        updateUserFields(userId, userUpdates)
//                    }
//                    .addOnFailureListener { exception ->
//                        Toast.makeText(requireContext(), "Error updating profile picture URL: ${exception.message}", Toast.LENGTH_LONG).show()
//                        binding.progressContainer.visibility = View.GONE
//                        binding.accountProgressBar.visibility = View.GONE
//                    }
//            } else {
//                updateUserFields(userId, userUpdates)
//            }
//        }
//    }
//
//    private fun updateUserFields(userId: String, userUpdates: HashMap<String, String>) {
//        db.collection("users").document(userId)
//            .update(userUpdates as Map<String, Any>)
//            .addOnSuccessListener {
//                binding.progressContainer.visibility = View.GONE
//                binding.accountProgressBar.visibility = View.GONE
//                Toast.makeText(requireContext(), "Changes saved successfully", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { exception ->
//                binding.progressContainer.visibility = View.GONE
//                binding.accountProgressBar.visibility = View.GONE
//                Toast.makeText(requireContext(), "Error saving changes: ${exception.message}", Toast.LENGTH_LONG).show()
//            }
//    }
//
//
//    private suspend fun validateUsername(
//        username: String
//    ): Boolean {
//        if (username.isEmpty()) {
//            Toast.makeText(requireContext(), "All fields are required.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        if (username.trim().contains(" ")) {
//            Toast.makeText(requireContext(), "Username cannot contain spaces.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        if (!isUsernameUnique(username)) {
//            Toast.makeText(requireContext(), "Username is already taken.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//
//        return true
//    }
//
//    private suspend fun isUsernameUnique(username: String): Boolean {
//        val querySnapshot = db.collection("users")
//            .whereEqualTo("username", username)
//            .get()
//            .await()
//        return querySnapshot.isEmpty  // true if no documents found, meaning email is unique
//    }
//
//    private val imagePickerLauncher = registerForActivityResult(
//        ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let {
//            selectedImageUri = it
//            binding.profilePicture.setImageURI(it)
//        }
//    }
//
//    private val cameraLauncher = registerForActivityResult(
//        ActivityResultContracts.TakePicture()
//    ) { success: Boolean ->
//        if (success && photoUri != null) {
//            selectedImageUri = photoUri
//            binding.profilePicture.setImageURI(photoUri)
//        }
//    }
//
//    private fun showImageSourceDialog() {
//        val options = arrayOf("Take a Picture", "Choose from Gallery")
//        AlertDialog.Builder(requireContext())
//            .setItems(options) { _, which ->
//                when (which) {
//                    0 -> checkAndRequestPermissions()
//                    1 -> chooseImageFromGallery()
//                }
//            }
//            .show()
//    }
//
//    private fun checkAndRequestPermissions() {
//        val cameraPermission = android.Manifest.permission.CAMERA
//        if (requireContext().checkSelfPermission(cameraPermission) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(arrayOf(cameraPermission), 1001)
//        } else {
//            takePicture()
//        }
//    }
//
//    private fun takePicture() {
//        val photoFile = createImageFile()
//        photoUri = FileProvider.getUriForFile(requireContext(), "com.example.memoraid.fileprovider", photoFile)
//        cameraLauncher.launch(photoUri)
//    }
//
//    private fun chooseImageFromGallery() {
//        imagePickerLauncher.launch("image/*")
//    }
//
//    private fun createImageFile(): File {
//        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
//    }
//
//    private fun uploadImageToFirebase(onSuccess: (String) -> Unit) {
//        binding.accountProgressBar.visibility = View.VISIBLE
//        val storageRef = storage.reference.child("profile_pictures/${System.currentTimeMillis()}.jpg")
//        selectedImageUri?.let { uri ->
//            storageRef.putFile(uri)
//                .addOnSuccessListener {
//                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
//                        onSuccess(downloadUrl.toString())
//                    }
//                    binding.accountProgressBar.visibility = View.GONE
//                }
//                .addOnFailureListener { e ->
//                    binding.accountProgressBar.visibility = View.GONE
//                    Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }

//    private fun updateEmailWithReauth(newEmail: String) {
//        val currentUser = auth.currentUser
//
//        if (currentUser != null && currentUser.email != newEmail) {
//            // Show password prompt
//            val passwordInput = EditText(requireContext())
//            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//
//            AlertDialog.Builder(requireContext())
//                .setTitle("Re-authentication Required")
//                .setMessage("Please enter your password to confirm email change.")
//                .setView(passwordInput)
//                .setPositiveButton("Confirm") { _, _ ->
//                    val password = passwordInput.text.toString()
//
//                    // Re-authenticate user
//                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
//                    currentUser.reauthenticate(credential)
//                        .addOnSuccessListener {
//                            // Proceed with email update
//                            currentUser.updateEmail(newEmail)
//                                .addOnSuccessListener {
//                                    // Update Firestore with the new email
//                                    db.collection("users").document(currentUser.uid)
//                                        .update("email", newEmail)
//                                        .addOnSuccessListener {
//                                            Toast.makeText(requireContext(), "Email updated. Please log in again.", Toast.LENGTH_SHORT).show()
//                                            redirectToLogin()
//                                        }
//                                        .addOnFailureListener { e ->
//                                            Toast.makeText(requireContext(), "Error updating Firestore: ${e.message}", Toast.LENGTH_LONG).show()
//                                        }
//                                }
//                                .addOnFailureListener { e ->
//                                    Toast.makeText(requireContext(), "Error updating email: ${e.message}", Toast.LENGTH_LONG).show()
//                                }
//                        }
//                        .addOnFailureListener { e ->
//                            Toast.makeText(requireContext(), "Re-authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
//                        }
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        }
//    }
//
//    private fun redirectToLogin() {
//        auth.signOut()  // Log out the user
//        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        requireActivity().finish()
//    }


    private fun fetchUserData(userId: String) {
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val userData = userDoc.data

                if (userData != null) {
                    val username = userData["username"] as? String ?: ""
                    val email = userData["email"] as? String ?: ""
                    val firstName = userData["firstName"] as? String ?: ""
                    val lastName = userData["lastName"] as? String ?: ""
                    val phoneNumber = userData["phoneNumber"] as? String ?: ""
                    val birthdate = userData["birthdate"] as? String ?: ""
                    val profilePictureUrl = userData["profilePictureUrl"] as? String ?: ""

                    // Verificăm dacă fragmentul este încă atașat
                    if (!isAdded || _binding == null) return@launch

                    binding.username.setText(username)
                    binding.email.setText(email)
                    binding.firstName.setText(firstName)
                    binding.lastName.setText(lastName)
                    binding.phoneNumber.setText(phoneNumber)
                    binding.birthdate.setText(birthdate)

                    Glide.with(this@AccountFragment)
                        .load(profilePictureUrl)
                        .placeholder(R.drawable.default_profile_picture)
                        .into(binding.profilePicture)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Eroare la încărcarea datelor: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}