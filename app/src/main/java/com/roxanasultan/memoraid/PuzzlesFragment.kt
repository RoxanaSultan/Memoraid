package com.roxanasultan.memoraid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.patient.adapters.PuzzleAdapter
import com.roxanasultan.memoraid.databinding.FragmentPuzzlesBinding

class PuzzlesFragment : Fragment() {

    private lateinit var puzzlesList: List<Int> // List of resource IDs of puzzle images
    private var _binding: FragmentPuzzlesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPuzzlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        puzzlesList = listOf(R.drawable.puzzle_image_1)

        val recyclerView = view.findViewById<RecyclerView>(R.id.puzzleRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val adapter = PuzzleAdapter(puzzlesList) { imageResId ->
            val action = PuzzlesFragmentDirections.actionPuzzlesFragmentToPuzzleFragment(imageResId)
            findNavController().navigate(action)
        }
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
