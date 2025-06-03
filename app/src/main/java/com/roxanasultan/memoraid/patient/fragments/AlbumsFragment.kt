package com.roxanasultan.memoraid.patient.fragments

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
import com.roxanasultan.memoraid.enums.AlbumType
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.patient.adapters.AlbumAdapter
import com.roxanasultan.memoraid.patient.adapters.AlbumModalAdapter
import com.roxanasultan.memoraid.databinding.FragmentAlbumBinding
import com.roxanasultan.memoraid.models.Album
import com.roxanasultan.memoraid.patient.viewmodels.AlbumViewModel
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

