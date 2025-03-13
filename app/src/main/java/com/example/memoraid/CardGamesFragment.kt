package com.example.memoraid

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentCardGamesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CardGamesFragment : Fragment() {

    private var _binding: FragmentCardGamesBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String

    private var totalGameScore: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardGamesBinding.inflate(inflater, container, false)
        database = FirebaseFirestore.getInstance()
        authenticator = FirebaseAuth.getInstance()
        currentUser = authenticator.currentUser?.uid ?: ""

        loadLevelsFromFirebase()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val difficultySpinner: Spinner = binding.difficultySpinner
        val difficulties = arrayOf("Easy", "Medium", "Hard", "Expert")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter

        val startGameButton: Button = binding.startGameButton
        startGameButton.setOnClickListener {
            val selectedDifficulty = difficultySpinner.selectedItem.toString()
            val action = CardGamesFragmentDirections.actionCardGamesFragmentToCardGameFragment(selectedDifficulty)
            findNavController().navigate(action)
        }
    }

    private fun loadLevelsFromFirebase() {
        val levelsRef = database.collection("card_matching_game")
            .whereEqualTo("userId", currentUser)

        levelsRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                totalGameScore = document.get("totalScore") as? Long ?: 0
                binding.totalScoreTextView.text = "Total Game Score: $totalGameScore"
                val levels = document.get("levels") as? List<Map<String, Any>>

                levels?.forEach { levelData ->
                    val levelName = levelData["level"] as? String ?: "Unknown"
                    val totalScore = levelData["totalLevelScore"] as? Long ?: 0
                    val bestTime = levelData["bestTime"] as? Long ?: Long.MAX_VALUE
                    val leastMoves = levelData["leastMoves"] as? Long ?: Long.MAX_VALUE

                    val levelRow = TableRow(requireContext()).apply {
                        layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)

                    val levelText = TextView(requireContext()).apply {
                        text = levelName
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.blue))
                        gravity = Gravity.CENTER
                        layoutParams = params
                    }

                    val scoreText = TextView(requireContext()).apply {
                        text = totalScore.toString()
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.blue))
                        gravity = Gravity.CENTER
                        layoutParams = params
                    }

                    val timeText = TextView(requireContext()).apply {
                        text = formatTime(bestTime)
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.blue))
                        gravity = Gravity.CENTER
                        layoutParams = params
                    }

                    val movesText = TextView(requireContext()).apply {
                        text = leastMoves.toString()
                        textSize = 16f
                        setTextColor(resources.getColor(R.color.blue))
                        gravity = Gravity.CENTER
                        layoutParams = params
                    }

                    levelRow.addView(levelText)
                    levelRow.addView(scoreText)
                    levelRow.addView(timeText)
                    levelRow.addView(movesText)

                    binding.levelsTableLayout.addView(levelRow)
                }
            } else {
                Toast.makeText(requireContext(), "No levels found for this user", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error loading levels: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
