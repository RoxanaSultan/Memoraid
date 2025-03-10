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
import com.example.memoraid.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CardGameFragment : Fragment() {
    private var _binding: FragmentCardGameBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseFirestore
    private lateinit var authenticator: FirebaseAuth
    private lateinit var currentUser: String

    private lateinit var cardsGrid: GridLayout
    private lateinit var restartButton: Button
    private val cardImages = listOf(
        R.drawable.card_apple, R.drawable.card_flower,
        R.drawable.card_car, R.drawable.card_kite
    )

    private var cardsList = mutableListOf<Card>()
    private var firstCard: Card? = null
    private var secondCard: Card? = null
    private var firstButton: Button? = null
    private var secondButton: Button? = null
    private var matchedPairs = 0
    private lateinit var currentLevel: String
    private var gameScore: Long = 0

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
        loadScoreFromFirebase(currentUser)
        currentLevel = requireArguments().getString("difficulty_level").toString()

        setupGame()
        return view
    }

    private fun loadScoreFromFirebase(userId: String) {
        val scoreRef = database.collection("card_matching_game")
            .whereEqualTo("userId", userId)

        scoreRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                val score = document.getLong("score")
                gameScore = score!!
                binding.scoreDisplay.text = "Score: $score"
            } else {
                Toast.makeText(requireContext(), "Score not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error loading score: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGame() {
        cardsGrid.removeAllViews()
        cardsList.clear()

        val pairedImages = (cardImages + cardImages).shuffled()

        pairedImages.forEachIndexed { index, image ->
            val card = Card(id = index, imageResId = image)
            cardsList.add(card)
            addCardToGrid(card)
        }
    }

    private fun addCardToGrid(card: Card) {
        val cardButton = Button(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 250
                height = 250
                marginEnd = 60
                bottomMargin = 60
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
                button.setBackgroundResource(R.drawable.card) // Back of the card
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
                    Toast.makeText(requireContext(), "You win!", Toast.LENGTH_SHORT).show()
                    updateScore()
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

    private fun updateScore() {
        val score = when (currentLevel) {
            "Easy" -> 1
            "Medium" -> 3
            "Hard" -> 6
            "Expert" -> 10
            else -> 0
        }

        gameScore += score
        binding.scoreDisplay.text = "Score: $gameScore"
        saveScoreToFirebase(currentUser, gameScore.toInt())
    }

    private fun saveScoreToFirebase(currentUser: String, newScore: Int) {
        val scoreRef = database.collection("card_matching_game").whereEqualTo("userId", currentUser)

        scoreRef.get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                val userScore = hashMapOf(
                    "userId" to currentUser,
                    "score" to newScore
                )

                database.collection("card_matching_game")
                    .add(userScore)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Score saved!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Error saving score: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val document = querySnapshot.documents[0]
                val documentId = document.id
                val updatedScore = hashMapOf("score" to newScore)

                database.collection("card_matching_game")
                    .document(documentId)
                    .update(updatedScore as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Score updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Error updating score: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun restartGame() {
        matchedPairs = 0
        firstCard = null
        secondCard = null
        setupGame()
    }
}