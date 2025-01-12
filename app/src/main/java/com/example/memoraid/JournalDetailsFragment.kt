package com.example.memoraid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.adapters.ImageAdapter
import com.example.memoraid.databinding.FragmentJournalDetailsBinding
import com.example.memoraid.models.Journal
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class JournalDetailsFragment : Fragment() {

    private var _binding: FragmentJournalDetailsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var journalId: String? = null
    private var journal: Journal? = null
    private val imageUris = mutableListOf<String>()
    private val storageReference = storage.reference.child("journal_images")
    private val firestoreCollection = db.collection("journals")

    // Adapter with callback for deleting images
    private val imageAdapter by lazy {
        ImageAdapter(imageUris,
            onImageRemoved = { imageUri ->
                removeImageFromFirestore(imageUri)
                removeImageFromStorage(imageUri)
            },
            onImageClicked = { imageUri -> // Handle image click
                val bundle = Bundle().apply {
                    putString("imageUri", imageUri)
                }
                findNavController().navigate(R.id.action_journalDetailsFragment_to_fullScreenImageFragment, bundle)
            }
        )
    }

    private val REQUEST_IMAGE_PICK = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002

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

    private fun removeImageFromFirestore(imageUri: String) {
        firestoreCollection.whereArrayContains("imageUris", imageUri)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val updatedUris = (document["imageUris"] as List<String>).filter { it != imageUri }
                    firestoreCollection.document(document.id).update("imageUris", updatedUris)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to remove image from Firestore", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeImageFromStorage(imageUri: String) {
        val fileReference = storage.getReferenceFromUrl(imageUri)

        if (fileReference == null) {
//            Toast.makeText(requireContext(), "Invalid image URL", Toast.LENGTH_SHORT).show()
            return
        }

        fileReference.delete()
            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Image deleted from storage", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
//                Log.e("JournalDetailsFragment", "Failed to delete image: ${exception.message}")
                Toast.makeText(requireContext(), "Failed to delete image from storage", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadJournalDetails(journalId: String) {
        db.collection("journals").document(journalId).get()
            .addOnSuccessListener { document ->
                journal = document.toObject(Journal::class.java)
                journal?.let {
                    binding.title.setText(it.title)
                    binding.content.setText(it.text)
                    imageUris.clear()
                    it.imageUris?.let { uris -> imageUris.addAll(uris) }
                    imageAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load journal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveJournalDetails() {
        // Show the progress bar
        binding.progressBar.visibility = View.VISIBLE

        deleteAllImagesFromStorage()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        journal?.let { journal ->
            journal.title = binding.title.text.toString()
            journal.entryDate = formattedDate
            journal.text = binding.content.text.toString()

            // List to hold uploaded image URLs
            val uploadedImageUris = mutableListOf<String>()

            // Upload images one by one
            if (imageUris.isNotEmpty()) {
                var imagesUploaded = 0

                // Function to upload an image and add to uploadedImageUris
                val uploadNextImage = { imageUri: Uri ->
                    uploadImageToStorage(imageUri, { uploadedUri ->
                        uploadedImageUris.add(uploadedUri)
                        imagesUploaded++

                        // If all images are uploaded, save the journal
                        if (imagesUploaded == imageUris.size) {
                            journal.imageUris = uploadedImageUris
                            saveJournalToFirestore(journal)
                        }
                    }, { exception ->
                        Toast.makeText(requireContext(), "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    })
                }

                // Iterate through all image URIs and upload them
                for (uriString in imageUris) {
                    val uri = Uri.parse(uriString)
                    uploadNextImage(uri)
                }
            } else {
                // No images selected, save the journal with empty imageUris
                journal.imageUris = mutableListOf()
                saveJournalToFirestore(journal)
            }
        }
    }

    private fun saveJournalToFirestore(journal: Journal) {
        db.collection("journals").document(journalId!!)
            .set(journal)
            .addOnSuccessListener {
                // Hide the progress bar
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Journal saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Hide the progress bar
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to save journal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteAllImagesFromStorage() {
        val storageReference = FirebaseStorage.getInstance().reference.child("journal_images")

        // List all files in the folder
        storageReference.listAll()
            .addOnSuccessListener { listResult ->
                // Loop through the list of files and delete them
                for (item in listResult.items) {
                    item.delete()
                        .addOnSuccessListener {
                            // File deleted successfully
//                            Log.d("DeleteAll", "File deleted: ${item.name}")
                        }
                        .addOnFailureListener { exception ->
                            // Handle any errors that occur
//                            Log.e("DeleteAll", "Failed to delete file: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occur when listing files
//                Log.e("DeleteAll", "Failed to list files: ${exception.message}")
            }
    }


    private fun uploadImageToStorage(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val fileReference = storageReference.child(fileName)

        fileReference.putFile(uri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
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
        val photoFile = File(requireContext().getExternalFilesDir(null), "journal_photo_${UUID.randomUUID()}.jpg")
        photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
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

    private fun handleImage(uri: Uri) {
        // Add image URI to the list but don't upload yet
        imageUris.add(uri.toString())
        imageAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}