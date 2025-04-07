package com.example.memoraid.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.adapters.ImageAdapter
import com.example.memoraid.databinding.FragmentJournalDetailsBinding
import com.example.memoraid.models.Journal
import com.example.memoraid.viewmodel.JournalViewModel
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class JournalDetailsFragment : Fragment(R.layout.fragment_journal_details) {

    private var _binding: FragmentJournalDetailsBinding? = null
    private val binding get() = _binding!!

    private val journalViewModel: JournalViewModel by viewModels()

    private var journalId: String? = null
    private var journal: Journal? = null
    private val imageUris = mutableListOf<String>()
    private var imagesToRemove = mutableListOf<String>()
    private val localToFirebaseUriMap = mutableMapOf<String, String>()

    private val imageAdapter by lazy {
        ImageAdapter(imageUris,
            onImageRemoved = { image ->
                val uriToRemove = localToFirebaseUriMap[image] ?: image
                imagesToRemove.add(uriToRemove)
            },
            onImageClicked = { image ->
                val bundle = Bundle().apply {
                    putString("image", image)
                }
                findNavController().navigate(R.id.action_journalDetailsFragment_to_fullScreenImageFragment, bundle)
            }
        )
    }

    private val REQUEST_IMAGE_PICK = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002
    private val REQUEST_CAMERA_PERMISSION = 1003

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        journalId = arguments?.getString("journalId")
        if (journalId != null) {
            loadJournalDetails(journalId!!)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.picture_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = imageAdapter

        binding.saveButton.setOnClickListener {
            saveJournalDetails()
        }

        binding.pictureButton.setOnClickListener {
            showImageOptions()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

//    private fun removeImageFromFirestore(imageUri: String) {
//        firestoreCollection.whereArrayContains("imageUris", imageUri)
//            .get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val updatedUris = (document["imageUris"] as List<String>).filter { it != imageUri }
//                    firestoreCollection.document(document.id).update("imageUris", updatedUris)
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to remove image from Firestore", Toast.LENGTH_SHORT).show()
//            }
//    }

//    private fun removeImageFromStorage(imageUri: String) {
//        val fileReference = storage.getReferenceFromUrl(imageUri)
//
//        fileReference.delete()
//            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Image deleted from storage", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(requireContext(), "Failed to delete image from storage", Toast.LENGTH_SHORT).show()
//            }
//    }

//    private fun loadJournalDetails(journalId: String) {
//        db.collection("journals").document(journalId).get()
//            .addOnSuccessListener { document ->
//                journal = document.toObject(Journal::class.java)
//                journal?.let {
//                    binding.title.setText(it.title)
//                    binding.content.setText(it.text)
//                    imageUris.clear()
//                    it.imageUris?.let { uris -> imageUris.addAll(uris) }
//                    imageAdapter.notifyDataSetChanged()
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to load journal", Toast.LENGTH_SHORT).show()
//            }
//    }

    private fun loadJournalDetails(journalId: String) {
        binding.progressContainer.visibility = View.VISIBLE

        journalViewModel.loadJournalDetails(journalId)

        viewLifecycleOwner.lifecycleScope.launch {
            journalViewModel.journalDetails.collect { loadedJournal ->
                if (loadedJournal != null) {
                    binding.title.setText(loadedJournal.title)
                    binding.content.setText(loadedJournal.text)
                    imageUris.clear()
                    loadedJournal.imageUris?.let { uris -> imageUris.addAll(uris) }
                    imageAdapter.notifyDataSetChanged()
                }
                binding.progressContainer.visibility = View.GONE
            }
        }
    }

    private fun saveJournalDetails() {
        binding.progressContainer.visibility = View.VISIBLE

        if (imagesToRemove.isNotEmpty()) {
            for (imageUri in imagesToRemove) {
                val firebaseUri = localToFirebaseUriMap[imageUri] ?: imageUri

                if (isFirebaseStorageUri(firebaseUri)) {
                    journalViewModel.checkIfImageExistsInStorage(firebaseUri) { exists ->
                        if (exists) {
                            journalViewModel.removeImageFromFirestore(firebaseUri)
                            journalViewModel.removeImageFromStorage(firebaseUri)
                        }
                    }
                }
            }
            imagesToRemove.clear()
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date())

        journal?.let { journal ->
            journal.title = binding.title.text.toString()
            journal.entryDate = formattedDate
            journal.text = binding.content.text.toString()

            val uploadedImageUris = mutableListOf<String>()
            var imagesUploaded = 0
            val totalImages = imageUris.size

            if (imageUris.isNotEmpty()) {
                for (uriString in imageUris) {
                    if (isFirebaseStorageUri(uriString)) {
                        uploadedImageUris.add(uriString)
                        imagesUploaded++

                        if (imagesUploaded == totalImages) {
                            journal.imageUris = uploadedImageUris
                            saveJournalToFirestore(journal)
                        }
                    } else {
                        val uri = Uri.parse(uriString)
                        journalViewModel.uploadImageToStorage(
                            uri,
                            onSuccess = { uploadedUri ->
                                uploadedImageUris.add(uploadedUri)
                                localToFirebaseUriMap[uriString] = uploadedUri
                                val index = imageUris.indexOf(uriString)
                                if (index != -1) {
                                    imageUris[index] = uploadedUri
                                    imageAdapter.notifyItemChanged(index)
                                }

                                imagesUploaded++
                                if (imagesUploaded == totalImages) {
                                    journal.imageUris = uploadedImageUris
                                    saveJournalToFirestore(journal)
                                }
                            },
                            onFailure = { exception ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to upload image: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                binding.progressContainer.visibility = View.GONE
                            }
                        )
                    }
                }
            } else {
                journal.imageUris = mutableListOf()
                saveJournalToFirestore(journal)
            }
        }
    }

    private fun handleImage(uri: Uri) {
        val uriString = uri.toString()
        imageUris.add(uriString)
        localToFirebaseUriMap.remove(uriString)
        imageAdapter.notifyDataSetChanged()
    }

    private fun saveJournalToFirestore(journal: Journal) {
        binding.progressContainer.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val isSaved = journalViewModel.saveJournalDetails(journal)

            binding.progressContainer.visibility = View.GONE

            if (isSaved) {
                Toast.makeText(requireContext(), "Journal saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to save journal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private lateinit var photoUri: Uri

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun startCamera() {
        val photoFile = File(requireContext().getExternalFilesDir(null), "journal_photo_${UUID.randomUUID()}.jpg")
        photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(requireContext(), "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> handleImage(photoUri)
                REQUEST_IMAGE_PICK -> data?.data?.let { handleImage(it) }
            }
        }
    }

//    private fun handleImage(uri: Uri) {
//        imageUris.add(uri.toString())
//        imageAdapter.notifyDataSetChanged()
//    }

    private fun isFirebaseStorageUri(uri: String): Boolean {
        return uri.startsWith("http://") || uri.startsWith("https://")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}