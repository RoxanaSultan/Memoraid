package com.example.memoraid

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.memoraid.databinding.FragmentCardGameBinding
import com.example.memoraid.models.Card
import android.animation.ObjectAnimator
import android.animation.Animator
import android.animation.AnimatorSet
import android.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import com.example.memoraid.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Timestamp
import java.time.LocalDateTime

class CardGameFragment : Fragment() {
    private var _binding: FragmentCardGameBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String

    private lateinit var cardsGrid: GridLayout
    private var cardImages: List<Int> = listOf()
    private var cardsList = mutableListOf<Card>()

    private var firstCard: Card? = null
    private var secondCard: Card? = null
    private var firstButton: Button? = null
    private var secondButton: Button? = null
    private lateinit var restartButton: Button

    private var matchedPairs = 0

    private lateinit var currentLevel: String
    private var gameScore: Long = 0
    private var levelScore: Long = 0
    private var moveCount = 0
    private var currentTime = 0
    private var currentScore = 0
    private var date: Timestamp = Timestamp(System.currentTimeMillis())
    private var startTime: Long = 0

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {}

    private val easyCardImages = listOf(
        R.drawable.card_apple,
        R.drawable.card_flower,
        R.drawable.card_car,
        R.drawable.card_kite
    )

    private val mediumCardImages = listOf(
        R.drawable.card_dog,
        R.drawable.card_cat,
        R.drawable.card_parrot,
        R.drawable.card_horse,
        R.drawable.card_dolphin,
        R.drawable.card_turtle
    )

    private val hardCardImages = listOf(
        R.drawable.card_chocolate_bar,
        R.drawable.card_ice_cream,
        R.drawable.card_fried_egg,
        R.drawable.card_soup,
        R.drawable.card_burger,
        R.drawable.card_salad,
        R.drawable.card_pizza,
        R.drawable.card_french_fries
    )

    private val expertCardImages = listOf(
        R.drawable.card_painting,
        R.drawable.card_running,
        R.drawable.card_camping,
        R.drawable.card_hiking,
        R.drawable.card_singing,
        R.drawable.card_boxing,
        R.drawable.card_dancing,
        R.drawable.card_cooking,
        R.drawable.card_board_gaming,
        R.drawable.card_canoeing,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardGameBinding.inflate(inflater, container, false)
        val view = binding.root

        database = FirebaseFirestore.getInstance()
        authenticator = FirebaseAuth.getInstance()

        cardsGrid = binding.cardsGrid
        restartButton = binding.restartButton

        restartButton.setOnClickListener { restartGame() }

        currentUser = authenticator.currentUser?.uid ?: ""
        loadScoreFromFirebase()
        currentLevel = requireArguments().getString("difficulty_level").toString()

        setupGame()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exit Game")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun loadScoreFromFirebase() {
        val scoreRef = database.collection("card_matching_game")
            .whereEqualTo("userId", currentUser)

        scoreRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                val totalScore = document.getLong("totalScore")
                gameScore = totalScore!!

                val levelsData = document.get("levels") as? List<Map<String, Any>> ?: listOf()

                for (levelData in levelsData) {
                    if (levelData["level"] == currentLevel) {
                        levelScore = (levelData["totalLevelScore"] as? Long) ?: 0L
                        break
                    }
                }

                binding.scoreDisplay.text = "Score: $levelScore"

            } else {
                Toast.makeText(requireContext(), "Score not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error loading score: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGame() {
        startTime = System.currentTimeMillis()
        startTimer()
        moveCount = 0
        date = Timestamp(System.currentTimeMillis())

        binding.moveDisplay.text = "Moves: $moveCount"

        cardsGrid.removeAllViews()
        cardsList.clear()

        val (levelCardImages, numColumns, cardSize) = when (currentLevel) {
            "Easy" -> Triple(easyCardImages, 2, 250)
            "Medium" -> Triple(mediumCardImages, 3, 250)
            "Hard" -> Triple(hardCardImages, 4, 200)
            "Expert" -> Triple(expertCardImages, 4, 200)
            else -> Triple(easyCardImages, 2, 200)
        }

        cardImages = levelCardImages

        val pairedImages = (cardImages + cardImages).shuffled()

        cardsGrid.columnCount = numColumns

        pairedImages.forEachIndexed { index, image ->
            val card = Card(id = index, imageResId = image)
            cardsList.add(card)
            addCardToGrid(card, cardSize)
        }
    }

    private fun startTimer() {
        runnable = object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / 1000) / 60
                binding.timerDisplay.text = String.format("Time: %02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    private fun addCardToGrid(card: Card, cardSize: Int) {
        val cardButton = Button(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardSize
                height = cardSize
                marginEnd = 40
                bottomMargin = 40
            }
            setBackgroundResource(R.drawable.card)
            setOnClickListener {
                flipCard(card, this)
            }
        }

        cardsGrid.addView(cardButton)
    }

    private fun flipToFront(card: Card, button: Button) {
        if (card.isFlipped) return

        val flipOut = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0f).apply { duration = 200 }
        val flipIn = ObjectAnimator.ofFloat(button, "scaleX", 0f, 1f).apply { duration = 200 }

        flipOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                card.isFlipped = true
                button.setBackgroundResource(card.imageResId)
                flipIn.start()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        AnimatorSet().apply {
            playSequentially(flipOut, flipIn)
            start()
        }
    }

    private fun flipToBack(card: Card, button: Button) {
        if (!card.isFlipped) return  // If already hidden, don't flip again

        val flipOut = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0f).apply { duration = 200 }
        val flipIn = ObjectAnimator.ofFloat(button, "scaleX", 0f, 1f).apply { duration = 200 }

        flipOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                card.isFlipped = false
                button.setBackgroundResource(R.drawable.card)
                flipIn.start()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        AnimatorSet().apply {
            playSequentially(flipOut, flipIn)
            start()
        }
    }

    private fun flipCard(card: Card, button: Button) {
        if (card.isMatched) return
        if (firstCard != null && secondCard != null && firstCard != card && secondCard != card) return

        flipToFront(card, button)
        moveCount++
        binding.moveDisplay.text = "Moves: $moveCount"

        if (firstCard == null) {
            firstCard = card
            firstButton = button
        } else if (secondCard == null && firstCard != card) {
            secondCard = card
            secondButton = button
            checkForMatch()
        }
    }

    private fun resetFlippedCards() {
        firstButton?.let { button ->
            firstCard?.let { card ->
                flipToBack(card, button)
            }
        }
        secondButton?.let { button ->
            secondCard?.let { card ->
                flipToBack(card, button)
            }
        }

        firstButton = null
        secondButton = null
        firstCard = null
        secondCard = null
    }

    private fun checkForMatch() {
        if (firstCard != null && secondCard != null) {
            if (firstCard!!.imageResId == secondCard!!.imageResId) {
                firstCard!!.isMatched = true
                secondCard!!.isMatched = true
                matchedPairs++

                if (matchedPairs == cardImages.size) {
                    stopTimer()
                    Toast.makeText(requireContext(), "You win!", Toast.LENGTH_SHORT).show()
                    updateGameData()
                }
                firstButton = null
                secondButton = null
                firstCard = null
                secondCard = null
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    resetFlippedCards()
                }, 800)
            }
        }
    }

    private fun updateGameData() {
        val elapsedTime = System.currentTimeMillis() - startTime
        currentTime = (elapsedTime / 1000).toInt()
        currentScore = calculateScore(elapsedTime)
        val moves = moveCount

        val gameRef = database.collection("card_matching_game")
            .whereEqualTo("userId", currentUser)

        gameRef.get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.size() > 0) {
                val document = querySnapshot.documents[0]
                val userData = document.data
                val levelsList = userData?.get("levels") as? List<Map<String, Any>> ?: emptyList()

                val levelData = levelsList.find { it["level"] == currentLevel }

                val bestTime = (levelData?.get("bestTime") as? Long) ?: Long.MAX_VALUE
                val leastMoves = (levelData?.get("leastMoves") as? Int) ?: Int.MAX_VALUE
                val lastPlayedGames = (levelData?.get("lastPlayedGames") as? MutableList<Map<String, Any>>) ?: mutableListOf()
                val totalScore = (userData?.get("totalScore") as? Long) ?: 0
                val levelScoreSum = (levelData?.get("totalLevelScore") as? Long) ?: 0

                val newBestTime = if (elapsedTime < bestTime) elapsedTime else bestTime
                val newLeastMoves = if (moves < leastMoves) moves else leastMoves

                val newGameData = mapOf(
                    "date" to date,
                    "time" to currentTime,
                    "moves" to moves,
                    "totalLevelScore" to currentScore
                )

                if (lastPlayedGames.size >= 10) lastPlayedGames.removeAt(0)
                lastPlayedGames.add(newGameData)

                val newTotalScore = totalScore + currentScore
                val newLevelScore = levelScoreSum + currentScore

                val updatedLevels = levelsList.toMutableList()
                val index = updatedLevels.indexOfFirst { it["level"] == currentLevel }
                if (index != -1) {
                    val updatedLevel = updatedLevels[index].toMutableMap()
                    updatedLevel["bestTime"] = newBestTime
                    updatedLevel["leastMoves"] = newLeastMoves
                    updatedLevel["lastPlayedGames"] = lastPlayedGames
                    updatedLevel["totalLevelScore"] = newLevelScore
                    updatedLevels[index] = updatedLevel
                } else {
                    updatedLevels.add(
                        mapOf(
                            "level" to currentLevel,
                            "bestTime" to newBestTime,
                            "leastMoves" to newLeastMoves,
                            "lastPlayedGames" to listOf(newGameData),
                            "totalLevelScore" to newLevelScore
                        )
                    )
                }

                document.reference.update(
                    mapOf(
                        "totalScore" to newTotalScore,
                        "levels" to updatedLevels
                    )
                ).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Game data updated!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error updating data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

            } else {
                val newGameData = mapOf(
                    "userId" to currentUser,
                    "totalScore" to currentScore,
                    "levels" to listOf(
                        mapOf(
                            "level" to currentLevel,
                            "bestTime" to elapsedTime,
                            "leastMoves" to moves,
                            "lastPlayedGames" to listOf(
                                mapOf(
                                    "date" to date,
                                    "time" to currentTime,
                                    "moves" to moves,
                                    "score" to currentScore
                                )
                            ),
                            "totalLevelScore" to currentScore
                        )
                    )
                )

                database.collection("card_matching_game")
                    .add(newGameData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Game data saved!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Error saving data: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateScore(elapsedTime: Long): Int {
        val baseScore = when (currentLevel) {
            "Easy" -> 1
            "Medium" -> 3
            "Hard" -> 6
            "Expert" -> 10
            else -> 0
        }

        val multiplier = when {
            elapsedTime < 60_000 -> 10
            elapsedTime < 120_000 -> 7
            elapsedTime < 300_000 -> 5
            else -> 1
        }

        val movesBonus = when {
            moveCount <= 15 -> 5
            moveCount <= 20 -> 3
            else -> 0
        }

        return (baseScore * multiplier) + movesBonus
    }


    private fun restartGame() {
        matchedPairs = 0
        firstCard = null
        secondCard = null
        setupGame()
    }
}