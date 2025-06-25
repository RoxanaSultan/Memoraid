package com.roxanasultan.memoraid.caretaker.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.patient.adapters.CardGameAdapter
import com.roxanasultan.memoraid.databinding.FragmentCardGameCaretakerBinding
import com.roxanasultan.memoraid.models.CardGame
import com.roxanasultan.memoraid.utils.VerticalSpaceItemDecoration
import com.roxanasultan.memoraid.viewmodels.CardGamesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardGameFragment : Fragment() {

    private var _binding: FragmentCardGameCaretakerBinding? = null
    private val binding get() = _binding!!
    private val cardGamesViewModel: CardGamesViewModel by viewModels()
    private val activity = mutableListOf<CardGame>()
    private var activityAdapter = CardGameAdapter(activity)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCardGameCaretakerBinding.inflate(inflater, container, false)

        cardGamesViewModel.loadUser()

        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            cardGamesViewModel.user.collect { user ->
                if (user != null && !user.selectedPatient.isNullOrEmpty()) {
                    loadActivity(user.selectedPatient)
                    loadLevelsFromFirebase()
                }
            }
        }

        return binding.root
    }

    private fun loadActivity(selectedPatient: String) {
        cardGamesViewModel.loadLastPlayedGames(selectedPatient)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            cardGamesViewModel.weeklyActivity.collect { games ->
                activity.clear()
                activity.addAll(games)
                activityAdapter.sortGamesByDateAndTime()
                activityAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.weeklyActivityRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.weeklyActivityRecyclerView.adapter = activityAdapter
        binding.weeklyActivityRecyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))
    }

    private fun loadLevelsFromFirebase() {
        cardGamesViewModel.loadCardGameLevels(cardGamesViewModel.user.value?.selectedPatient ?: "",
            onSuccess = { levels ->
                updateLevelsUI(levels)
            },
            onFailure = { exception ->
                Toast.makeText(requireContext(), "Error loading levels: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }


    private fun updateLevelsUI(levels: List<Map<String, Any>>) {
        levels.forEach { levelData ->
            val levelName = levelData["level"] as? String ?: "Unknown"
            val leastMoves = levelData["totalLevelScore"] as? Long ?: Long.MAX_VALUE
            val bestTime = levelData["bestTime"] as? Long ?: Long.MAX_VALUE
            val totalScore = levelData["leastMoves"] as? Long ?: 0

            val userId = cardGamesViewModel.user.value?.selectedPatient ?: ""

            cardGamesViewModel.getTotalGamesPerLevel(
                userId = userId,
                level = levelName,
                onSuccess = { totalGamesResult ->
                    cardGamesViewModel.getBestScorePerLevel(
                        userId = userId,
                        level = levelName,
                        onSuccess = { bestScoreResult ->
                            addLevelRow(levelName, totalGamesResult, bestScoreResult, leastMoves, bestTime, totalScore)
                        },
                        onFailure = { e ->
                            addLevelRow(levelName, totalGamesResult, 0, leastMoves, bestTime, totalScore)
                        }
                    )
                },
                onFailure = { e ->
                    cardGamesViewModel.getBestScorePerLevel(
                        userId = userId,
                        level = levelName,
                        onSuccess = { bestScoreResult ->
                            addLevelRow(levelName, 0, bestScoreResult, leastMoves, bestTime, totalScore)
                        },
                        onFailure = { e2 ->
                            addLevelRow(levelName, 0, 0, leastMoves, bestTime, totalScore)
                        }
                    )
                }
            )
        }
    }

    private fun addLevelRow(
        levelName: String,
        totalGames: Long,
        bestScore: Long,
        leastMoves: Long,
        bestTime: Long,
        totalScore: Long
    ) {
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

        val totalGamesText = TextView(requireContext()).apply {
            text = totalGames.toString()
            textSize = 16f
            setTextColor(resources.getColor(R.color.blue))
            gravity = Gravity.CENTER
            layoutParams = params
        }

        val bestScoreText = TextView(requireContext()).apply {
            text = bestScore.toString()
            textSize = 16f
            setTextColor(resources.getColor(R.color.blue))
            gravity = Gravity.CENTER
            layoutParams = params
        }

        val movesText = TextView(requireContext()).apply {
            text = if (leastMoves != Long.MAX_VALUE) leastMoves.toString() else "-"
            textSize = 16f
            setTextColor(resources.getColor(R.color.blue))
            gravity = Gravity.CENTER
            layoutParams = params
        }

        val timeText = TextView(requireContext()).apply {
            text = if (bestTime != Long.MAX_VALUE) formatTime(bestTime) else "-"
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

        levelRow.addView(levelText)
        levelRow.addView(totalGamesText)
        levelRow.addView(bestScoreText)
        levelRow.addView(scoreText)
        levelRow.addView(timeText)
        levelRow.addView(movesText)

        binding.levelsTableLayout.addView(levelRow)
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}