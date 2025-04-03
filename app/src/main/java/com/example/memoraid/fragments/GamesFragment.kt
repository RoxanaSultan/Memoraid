
package com.example.memoraid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.R
import com.example.memoraid.databinding.FragmentGamesBinding

class GamesFragment : Fragment() {
    private var _binding: FragmentGamesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPuzzle.setOnClickListener {
            findNavController().navigate(R.id.action_gamesFragment_to_puzzlesFragment)
        }

        binding.btnCards.setOnClickListener {
            findNavController().navigate(R.id.action_gamesFragment_to_cardGamesFragment)
        }
    }
}
