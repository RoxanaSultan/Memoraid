package com.example.memoraid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memoraid.AlbumType
import com.example.memoraid.R
import com.example.memoraid.adapters.AlbumAdapter
import com.example.memoraid.adapters.AlbumModalAdapter
import com.example.memoraid.databinding.FragmentAlbumBinding
import com.example.memoraid.models.Album
import com.example.memoraid.viewmodel.AlbumViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlbumsFragment : Fragment(R.layout.fragment_album) {

    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var albumModalAdapter: AlbumModalAdapter

    private val albumViewModel: AlbumViewModel by viewModels()

    private val albums = mutableListOf<Album>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupModal()

        albumViewModel.loadAlbums()

//        binding.newAlbumImageButton.setOnClickListener {
//            if (userId == null) {
//                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            binding.modalContainer.visibility = View.VISIBLE
//            setupModal()
//        }

        lifecycleScope.launchWhenStarted {
            albumViewModel.albums.collect { updatedAlbums ->
                albums.clear()
                albums.addAll(updatedAlbums)
                albumAdapter.notifyDataSetChanged()
            }
        }

        binding.newAlbumImageButton.setOnClickListener {
            binding.modalContainer.visibility = View.VISIBLE
        }

        binding.modalContainer.setOnClickListener {
            binding.modalContainer.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener{
            findNavController().navigate(R.id.action_albumModal_to_albumFragment)
        }
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumAdapter(requireContext(), albums,
            onAlbumClick = { album ->
                val bundle = Bundle().apply {
                    putString("albumId", album.id)
                }
                findNavController().navigate(R.id.action_albumsFragment_to_albumDetailsFragment, bundle)
            },
            onAlbumDelete = { deletedAlbum ->
                binding.progressContainer.visibility = View.VISIBLE
                albumViewModel.deleteAlbum(deletedAlbum.id) {
                    binding.progressContainer.visibility = View.GONE
                }
            }
        )

        binding.albumRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = albumAdapter
        }
    }

//    private fun checkIfAlbumDeleted(albumId: String) {
//        database.collection("albums").document(albumId).get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    val imageUris = document.get("images") as? List<String> ?: emptyList()
//
//                    if (imageUris.isNotEmpty()) {
//                        deleteImagesFromStorage(imageUris) {
//                            deleteAlbumFromFirestore(albumId)
//                        }
//                    } else {
//                        deleteAlbumFromFirestore(albumId)
//                    }
//                } else {
//                    binding.progressContainer.visibility = View.GONE
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Error checking album deletion", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun deleteImagesFromStorage(imageUris: List<String>, onComplete: () -> Unit) {
//        var deletedCount = 0
//        for (imageUri in imageUris) {
//            val storageRef = storage.getReferenceFromUrl(imageUri)
//            storageRef.delete()
//                .addOnSuccessListener {
//                    deletedCount++
//                    if (deletedCount == imageUris.size) {
//                        onComplete()
//                    }
//                }
//                .addOnFailureListener {
//                    Toast.makeText(requireContext(), "Failed to delete an image", Toast.LENGTH_SHORT).show()
//                }
//        }
//
//        if (imageUris.isEmpty()) {
//            onComplete()
//        }
//    }
//
//    private fun deleteAlbumFromFirestore(albumId: String) {
//        database.collection("albums").document(albumId).delete()
//            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Album deleted successfully", Toast.LENGTH_SHORT).show()
//                binding.progressContainer.visibility = View.GONE
//                loadAlbums()
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to delete album", Toast.LENGTH_SHORT).show()
//                binding.progressContainer.visibility = View.GONE
//            }
//    }

//    private fun createAlbum(selectedImageIndex: Int) {
//        userId?.let { uid ->
//            val albumRef = database.collection("albums").document()
//            val albumType = when (selectedImageIndex) {
//                0 -> AlbumType.ALBUM_GREEN.type
//                1 -> AlbumType.ALBUM_BLUE.type
//                2 -> AlbumType.ALBUM_PINK.type
//                else -> {
//                    Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
//                    return
//                }
//            }
//
//            val albumInfo = hashMapOf(
//                "userId" to uid,
//                "createdAt" to FieldValue.serverTimestamp(),
//                "title" to "Untitled",
//                "description" to "",
//                "images" to listOf<String>(),
//                "type" to albumType,
//                "updatedAt" to FieldValue.serverTimestamp()
//            )
//
//            albumRef.set(albumInfo).addOnSuccessListener {
//                Toast.makeText(requireContext(), "Album created successfully", Toast.LENGTH_SHORT).show()
//
//                val bundle = Bundle().apply {
//                    putString("albumId", albumRef.id)
//                }
//                findNavController().navigate(R.id.action_albumsFragment_to_albumDetailsFragment, bundle)
//
//                binding.modalContainer.visibility = View.GONE
//            }.addOnFailureListener { e ->
//                e.printStackTrace()
//                Toast.makeText(requireContext(), "Failed to create new album: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        } ?: Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
//    }

    //    private fun setupModal() {
//        albumModalAdapter = AlbumModalAdapter { position ->
//            createAlbum(position)
//        }
//        binding.modalRecyclerView.apply {
//            layoutManager = GridLayoutManager(requireContext(), 3)
//            adapter = albumModalAdapter
//        }
//    }

    private fun setupModal() {
        albumModalAdapter = AlbumModalAdapter { position ->
            val albumType = when (position) {
                0 -> AlbumType.ALBUM_GREEN.type
                1 -> AlbumType.ALBUM_BLUE.type
                2 -> AlbumType.ALBUM_PINK.type
                else -> {
                    Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                    return@AlbumModalAdapter
                }
            }

            albumViewModel.createAlbum(albumType, onSuccess = { id ->
                val bundle = Bundle().apply {
                    putString("albumId", id)
                }
                findNavController().navigate(R.id.action_albumsFragment_to_albumDetailsFragment, bundle)
                binding.modalContainer.visibility = View.GONE
            }, onFailure = {
                Toast.makeText(requireContext(), "Failed to create album", Toast.LENGTH_SHORT).show()
            })
        }

        binding.modalRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.modalRecyclerView.adapter = albumModalAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        albums.clear()
    }
}

