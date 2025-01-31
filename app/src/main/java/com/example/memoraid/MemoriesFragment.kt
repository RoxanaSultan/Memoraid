package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentMemoriesBinding

class MemoriesFragment : Fragment() {

    // Declare the binding variable
    private var _binding: FragmentMemoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the binding object
        _binding = FragmentMemoriesBinding.inflate(inflater, container, false)

        // Use the binding object to access the views
        binding.btnJournal.setOnClickListener {
            findNavController().navigate(R.id.action_memoriesFragment_to_journalFragment)
        }

        binding.btnAlbums.setOnClickListener {
            findNavController().navigate(R.id.action_memoriesFragment_to_journalFragment)
        }

        // Return the root view of the binding object
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by setting the binding to null
        _binding = null
    }
}
