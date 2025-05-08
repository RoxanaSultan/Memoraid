package com.example.memoraid.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.memoraid.R
import com.example.memoraid.adapters.ImageAdapter
import com.example.memoraid.databinding.FragmentAlbumDetailsBinding
import com.example.memoraid.models.Album
import com.example.memoraid.viewmodel.AlbumViewModel
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.getValue

@AndroidEntryPoint
class AlbumDetailsFragment : Fragment(R.layout.fragment_album_details) {

    private var _binding: FragmentAlbumDetailsBinding? = null
    private val binding get() = _binding!!

    private val albumViewModel: AlbumViewModel by viewModels()
    private var albumId: String? = null

    private var album: Album? = null
    private val images = mutableListOf<String>()
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
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
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

    private fun loadAlbumDetails(albumId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.root.alpha = 0.5f
        binding.root.setEnabledRecursively(false)

        albumViewModel.loadAlbumDetails(albumId)

        viewLifecycleOwner.lifecycleScope.launch {
            albumViewModel.albumDetails.collect { loadedAlbum ->
                if (loadedAlbum != null) {
                    album = loadedAlbum
                    binding.title.setText(loadedAlbum.title)
                    binding.description.setText(loadedAlbum.description)
                    images.clear()
                    loadedAlbum.images?.let { uris -> images.addAll(uris) }
                    imageAdapter.notifyDataSetChanged()
                }
                binding.progressBar.visibility = View.GONE
                binding.root.alpha = 1f
                binding.root.setEnabledRecursively(true)
            }
        }
    }

    private fun saveAlbumDetails() {
        binding.progressBar.visibility = View.VISIBLE
        binding.root.alpha = 0.5f
        binding.root.setEnabledRecursively(false)

        if (imagesToRemove.isNotEmpty()) {
            for (image in imagesToRemove) {
                val firebaseUri = localToFirebaseUriMap[image] ?: image

                if (isFirebaseStorageUri(firebaseUri)) {
                    albumViewModel.checkIfImageExistsInStorage(firebaseUri) { exists ->
                        if (exists) {
                            albumViewModel.removeImageFromFirestore(firebaseUri)
                            albumViewModel.removeImageFromStorage(firebaseUri)
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
                        albumViewModel.uploadImageToStorage(
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
                                binding.progressBar.visibility = View.GONE
                                binding.root.alpha = 1f
                                binding.root.setEnabledRecursively(true)
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
        binding.progressBar.visibility = View.VISIBLE
//        binding.blockingView.visibility = View.VISIBLE
        binding.root.alpha = 0.5f
        binding.root.setEnabledRecursively(false)

        viewLifecycleOwner.lifecycleScope.launch {
            val isSaved = albumViewModel.saveAlbumDetails(album)

            binding.progressBar.visibility = View.GONE
//            binding.blockingView.visibility = View.GONE
            binding.root.alpha = 1f
            binding.root.setEnabledRecursively(true)

            if (isSaved) {
                Toast.makeText(requireContext(), "Album saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to save album", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
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

    fun View.setEnabledRecursively(enabled: Boolean) {
        this.isEnabled = enabled
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).setEnabledRecursively(enabled)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}