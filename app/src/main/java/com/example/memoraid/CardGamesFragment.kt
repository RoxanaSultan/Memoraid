package com.example.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentCardGamesBinding

class CardGamesFragment : Fragment() {

    private var _binding: FragmentCardGamesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCardGamesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Spinner setup for difficulty levels
        val difficultySpinner: Spinner = binding.difficultySpinner
        val difficulties = arrayOf("Easy", "Medium", "Hard", "Expert")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter

        // Button to start game
        val startGameButton: Button = binding.startGameButton
        startGameButton.setOnClickListener {
            val selectedDifficulty = difficultySpinner.selectedItem.toString()
            val action = CardGamesFragmentDirections.actionCardGamesFragmentToCardGameFragment(selectedDifficulty)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
