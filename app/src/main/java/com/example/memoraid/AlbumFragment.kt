package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memoraid.adapters.AlbumAdapter
import com.example.memoraid.adapters.AlbumModalAdapter
import com.example.memoraid.databinding.FragmentAlbumBinding
import com.example.memoraid.models.Album
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue

class AlbumFragment : Fragment() {

    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var albumAdapter: AlbumAdapter
    private val albumList = mutableListOf<Album>()

    private lateinit var albumModalAdapter: AlbumModalAdapter

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
        loadAlbums()

        binding.newAlbumImageButton.setOnClickListener {
            if (userId == null) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.modalContainer.visibility = View.VISIBLE
            setupModal()
        }

        binding.modalContainer.setOnClickListener {
            binding.modalContainer.visibility = View.GONE
        }

        binding.cancelButton.setOnClickListener{
            findNavController().navigate(R.id.action_albumModal_to_albumFragment)
        }
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumAdapter(requireContext(), albumList,
            onAlbumClick = { album ->
                val bundle = Bundle().apply {
                    putString("albumId", album.id)
                }
                findNavController().navigate(R.id.action_albumsFragment_to_albumDetailsFragment, bundle)
            },
            onAlbumDelete = { deletedAlbum ->
                binding.progressContainer.visibility = View.VISIBLE
                checkIfAlbumDeleted(deletedAlbum.id)
            }
        )

        binding.albumRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = albumAdapter
        }
    }

    private fun checkIfAlbumDeleted(albumId: String) {
        database.collection("albums").document(albumId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageUris = document.get("images") as? List<String> ?: emptyList()

                    if (imageUris.isNotEmpty()) {
                        deleteImagesFromStorage(imageUris) {
                            deleteAlbumFromFirestore(albumId)
                        }
                    } else {
                        deleteAlbumFromFirestore(albumId)
                    }
                } else {
                    binding.progressContainer.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error checking album deletion", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteImagesFromStorage(imageUris: List<String>, onComplete: () -> Unit) {
        var deletedCount = 0
        for (imageUri in imageUris) {
            val storageRef = storage.getReferenceFromUrl(imageUri)
            storageRef.delete()
                .addOnSuccessListener {
                    deletedCount++
                    if (deletedCount == imageUris.size) {
                        onComplete()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete an image", Toast.LENGTH_SHORT).show()
                }
        }

        if (imageUris.isEmpty()) {
            onComplete()
        }
    }

    private fun deleteAlbumFromFirestore(albumId: String) {
        database.collection("albums").document(albumId).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Album deleted successfully", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
                loadAlbums()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete album", Toast.LENGTH_SHORT).show()
                binding.progressContainer.visibility = View.GONE
            }
    }


    private fun setupModal() {
        albumModalAdapter = AlbumModalAdapter { position ->
            createAlbum(position)
        }
        binding.modalRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = albumModalAdapter
        }
    }

    private fun createAlbum(selectedImageIndex: Int) {
        userId?.let { uid ->
            val albumRef = database.collection("albums").document()
            val albumType = when (selectedImageIndex) {
                0 -> AlbumType.ALBUM_GREEN.type
                1 -> AlbumType.ALBUM_BLUE.type
                2 -> AlbumType.ALBUM_PINK.type
                else -> {
                    Toast.makeText(requireContext(), "Invalid selection", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val albumInfo = hashMapOf(
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "title" to "Untitled",
                "description" to "",
                "images" to listOf<String>(),
                "type" to albumType,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            albumRef.set(albumInfo).addOnSuccessListener {
                Toast.makeText(requireContext(), "Album created successfully", Toast.LENGTH_SHORT).show()

                val bundle = Bundle().apply {
                    putString("albumId", albumRef.id)
                }
                findNavController().navigate(R.id.action_albumsFragment_to_albumDetailsFragment, bundle)

                binding.modalContainer.visibility = View.GONE
            }.addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to create new album: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
    }

    private fun loadAlbums() {
        albumList.clear()
        database.collection("albums")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val album = document.toObject(Album::class.java).copy(id = document.id)
                    albumList.add(album)
                }
                albumAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        albumList.clear()
    }
}

