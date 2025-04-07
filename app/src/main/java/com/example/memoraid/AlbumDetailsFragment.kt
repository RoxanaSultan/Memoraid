package com.example.memoraid

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.adapters.ImageAdapter
import com.example.memoraid.databinding.FragmentAlbumDetailsBinding
import com.example.memoraid.models.Album
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

class AlbumDetailsFragment : Fragment() {

    private var _binding: FragmentAlbumDetailsBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var albumId: String? = null
    private var album: Album? = null
    private val images = mutableListOf<String>()
    private val storageReference = storage.reference.child("album_images")
    private val firestoreCollection = database.collection("albums")
    private var imagesToRemove = mutableListOf<String>()
    private val localToFirebaseUriMap = mutableMapOf<String, String>()

    private val imageAdapter by lazy {
        ImageAdapter(images,
            onImageRemoved = { image ->
                val uriToRemove = localToFirebaseUriMap[image] ?: image
                imagesToRemove.add(uriToRemove)
            },
            onImageClicked = { image ->
                val bundle = Bundle().apply {
                    putString("image", image)
                }
                findNavController().navigate(R.id.action_albumDetailsFragment_to_fullScreenImageFragment, bundle)
            }
        )
    }

    private val REQUEST_IMAGE_PICK = 1001
    private val REQUEST_IMAGE_CAPTURE = 1002
    private val REQUEST_CAMERA_PERMISSION = 1003

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumId = arguments?.getString("albumId")
        if (albumId != null) {
            loadAlbumDetails(albumId!!)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.picture_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = imageAdapter

        binding.saveButton.setOnClickListener {
            saveAlbumDetails()
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

    private fun removeImageFromFirestore(image: String) {
        firestoreCollection.whereArrayContains("images", image)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val updatedUris = (document["images"] as List<String>).filter { it != image }
                    firestoreCollection.document(document.id).update("images", updatedUris)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to remove image from Firestore", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeImageFromStorage(image: String) {
        val fileReference = storage.getReferenceFromUrl(image)

        fileReference.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Image deleted from storage", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to delete image from storage", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAlbumDetails(albumId: String) {
        database.collection("albums").document(albumId).get()
            .addOnSuccessListener { document ->
                album = document.toObject(Album::class.java)
                album?.let {
                    binding.title.setText(it.title)
                    binding.description.setText(it.description)
                    images.clear()
                    it.images?.let { uris -> images.addAll(uris) }
                    imageAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load album", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAlbumDetails() {
        binding.progressContainer.visibility = View.VISIBLE

        if (imagesToRemove.isNotEmpty()) {
            for (image in imagesToRemove) {
                val firebaseUri = localToFirebaseUriMap[image] ?: image

                if (isFirebaseStorageUri(firebaseUri)) {
                    checkIfImageExistsInStorage(firebaseUri) { exists ->
                        if (exists) {
                            removeImageFromFirestore(firebaseUri)
                            removeImageFromStorage(firebaseUri)
                        }
                    }
                }
            }
            imagesToRemove.clear()
        }

        album?.let { album ->
            album.title = binding.title.text.toString()
            album.description = binding.description.text.toString()
            album.updatedAt = Timestamp.now()

            val uploadedImages = mutableListOf<String>()
            var imagesUploaded = 0
            val totalImages = images.size

            if (images.isNotEmpty()) {
                for (uriString in images) {
                    if (isFirebaseStorageUri(uriString)) {
                        uploadedImages.add(uriString)
                        imagesUploaded++

                        if (imagesUploaded == totalImages) {
                            album.images = uploadedImages
                            saveAlbumToFirestore(album)
                        }
                    } else {
                        val uri = Uri.parse(uriString)
                        uploadImageToStorage(
                            uri,
                            onSuccess = { uploadedUri ->
                                uploadedImages.add(uploadedUri)
                                localToFirebaseUriMap[uriString] = uploadedUri
                                val index = images.indexOf(uriString)
                                if (index != -1) {
                                    images[index] = uploadedUri
                                    imageAdapter.notifyItemChanged(index)
                                }

                                imagesUploaded++
                                if (imagesUploaded == totalImages) {
                                    album.images = uploadedImages
                                    saveAlbumToFirestore(album)
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
                album.images = mutableListOf()
                saveAlbumToFirestore(album)
            }
        }
    }

    private fun handleImage(uri: Uri) {
        val uriString = uri.toString()
        images.add(uriString)
        localToFirebaseUriMap.remove(uriString)
        imageAdapter.notifyDataSetChanged()
    }

    private fun saveAlbumToFirestore(album: Album) {
        database.collection("albums").document(albumId!!)
            .set(album)
            .addOnSuccessListener {
                binding.progressContainer.visibility = View.GONE
                Toast.makeText(requireContext(), "Album saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.progressContainer.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to save album: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIfImageExistsInStorage(image: String, onResult: (Boolean) -> Unit) {
        val fileReference = storage.getReferenceFromUrl(image)

        fileReference.downloadUrl
            .addOnSuccessListener { uri ->
                onResult(true)
            }
            .addOnFailureListener { exception ->
                onResult(false)
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun startCamera() {
        val photoFile = File(requireContext().getExternalFilesDir(null), "album_photo_${UUID.randomUUID()}.jpg")
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