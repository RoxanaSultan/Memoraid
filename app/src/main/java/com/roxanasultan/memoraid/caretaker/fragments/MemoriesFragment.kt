package com.roxanasultan.memoraid.caretaker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.FragmentMemoriesBinding

class MemoriesFragment : Fragment() {
    private var _binding: FragmentMemoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMemoriesBinding.inflate(inflater, container, false)

        binding.btnJournal.setOnClickListener {
            findNavController().navigate(R.id.action_memoriesFragment_to_journalFragment)
        }

        binding.btnAlbums.setOnClickListener {
            findNavController().navigate(R.id.action_memoriesFragment_to_albumsFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
