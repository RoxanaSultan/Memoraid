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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.adapters.ImageAdapter
import com.example.memoraid.databinding.FragmentJournalDetailsBinding
import com.example.memoraid.models.Journal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
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
    private val imageAdapter by lazy { ImageAdapter(imageUris, isSaved, storageReference, firestoreCollection) }
    private var isSaved = false

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
        val layoutManager = GridLayoutManager(context, 3)
        recyclerView.layoutManager = layoutManager
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
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load journal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveJournalDetails() {
        journal?.let {
            // Update journal data from UI
            it.title = binding.title.text.toString()
            it.text = binding.content.text.toString()
            it.imageUris = imageUris

            // Save the updated journal to Firestore
            db.collection("journals").document(journalId!!)
                .set(it)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Journal saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to save journal: ${e.message}", Toast.LENGTH_SHORT).show()
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
                REQUEST_IMAGE_CAPTURE -> {
                    handleImage(photoUri)
                }
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        handleImage(uri)
                    }
                }
            }
        }
    }

    private fun handleImage(uri: Uri) {
        val imageName = UUID.randomUUID().toString()

        // Upload the image to Firebase Storage
        val imageRef = storage.reference.child("journal_images/$imageName")
        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Add the image URI to the list and update RecyclerView
                    imageUris.add(downloadUri.toString())
                    imageAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
