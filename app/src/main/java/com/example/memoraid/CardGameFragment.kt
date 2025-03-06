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

class CardGameFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_card_game, container, false)

        cardsGrid = view.findViewById(R.id.cards_grid)
        restartButton = view.findViewById(R.id.restart_button)

        restartButton.setOnClickListener { restartGame() }

        setupGame()
        return view
    }

    private fun setupGame() {
        cardsGrid.removeAllViews()
        cardsList.clear()

        // Duplicate images to create pairs
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
                marginEnd = 16
                bottomMargin = 16
            }
            setBackgroundResource(R.drawable.card) // Default card back
            setOnClickListener {
                flipCard(card, this)
            }
        }

        cardsGrid.addView(cardButton)
    }

    private fun flipCard(card: Card, button: Button) {
        if (card.isMatched) return
        if (firstCard != null && secondCard != null && firstCard != card && secondCard != card) return


        val flipOut = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0f).apply { duration = 150 }
        val flipIn = ObjectAnimator.ofFloat(button, "scaleX", 0f, 1f).apply { duration = 150 }

        flipOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                card.isFlipped = !card.isFlipped
                button.setBackgroundResource(if (card.isFlipped) card.imageResId else R.drawable.card)
                flipIn.start()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        flipIn.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                if (firstCard == null) {
                    firstCard = card
                    firstButton = button
                } else if (secondCard == null && firstCard != card) {
                    secondCard = card
                    secondButton = button
                    checkForMatch()
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        val flip = AnimatorSet()
        flip.playSequentially(flipOut, flipIn)
        flip.start()
    }

    private fun resetFlippedCards() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (firstButton != null && secondButton != null) {
                firstButton?.let { button ->
                    val card = firstCard!!
                    flipCard(card, button)
                }
                secondButton?.let { button ->
                    val card = secondCard!!
                    flipCard(card, button)
                }
            }
            firstButton = null
            secondButton = null
            firstCard = null
            secondCard = null
        }, 100)
    }

    private fun checkForMatch() {
        if (firstCard != null && secondCard != null) {
            if (firstCard!!.imageResId == secondCard!!.imageResId) {
                firstCard!!.isMatched = true
                secondCard!!.isMatched = true
                matchedPairs++

                if (matchedPairs == cardImages.size) {
                    Toast.makeText(requireContext(), "You win!", Toast.LENGTH_SHORT).show()
                    firstCard = null
                    secondCard = null
                }
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    resetFlippedCards()
                }, 100)
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