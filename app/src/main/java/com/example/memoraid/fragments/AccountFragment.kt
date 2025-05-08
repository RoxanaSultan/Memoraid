package com.example.memoraid.fragments

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.memoraid.activities.AuthenticationActivity
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentAccountBinding
import com.example.memoraid.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AccountFragment : Fragment(R.layout.fragment_account) {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val accountViewModel: AccountViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val view = binding.root

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
//                        .circleCrop()
                        .into(binding.profilePicture)
                }
            }
        }

        binding.changePasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_changePasswordFragment)
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

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return view
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

    private fun logout() {
        accountViewModel.logout()
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
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
                when (which) {
                    0 -> checkAndRequestPermissions()
                    1 -> chooseImageFromGallery()
                    2 -> if (hasPicture) removeProfilePicture()
                }
            }
            .show()
    }

    private fun saveNewProfilePicture(uri: Uri) {
        val user = accountViewModel.user.value ?: return

        user.profilePictureUrl?.let { profilePictureUrl ->
            accountViewModel.deleteImageFromStorage(profilePictureUrl)
        }

        accountViewModel.uploadAndSaveProfilePicture(
            uri,
            user.id
        )
    }


    private fun removeProfilePicture() {
        showDeletePictureModal()
    }

    private fun showDeletePictureModal()
    {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Profile Picture")
            .setMessage("Are you sure you want to remove your profile picture?")
            .setPositiveButton("Yes") { _, _ ->
                binding.profilePicture.setImageResource(R.drawable.default_profile_picture)
                accountViewModel.removeProfilePicture(accountViewModel.user.value?.id!!)
                selectedImageUri = null
            }
            .setNegativeButton("No", null)
            .show()
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

            saveNewProfilePicture(it)
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
//                .circleCrop()
                .into(binding.profilePicture)

            saveNewProfilePicture(photoUri!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}