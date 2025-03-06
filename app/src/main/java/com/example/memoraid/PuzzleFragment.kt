package com.example.memoraid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.memoraid.databinding.FragmentPuzzleBinding

class PuzzleFragment : Fragment() {
    private lateinit var puzzleContainer: FrameLayout
    private lateinit var puzzlePieces: List<Bitmap>
    private val rows = 3
    private val cols = 4

    companion object {
        private const val ARG_IMAGE_RES_ID = "image_res_id"

        fun newInstance(imageResId: Int): PuzzleFragment {  // ✅ Fix: Accept Int, not Bitmap
            val fragment = PuzzleFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES_ID, imageResId)  // ✅ Fix: Store Int instead of Bitmap
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPuzzleBinding.inflate(inflater, container, false)
        puzzleContainer = binding.puzzleContainer

        val imageResId = arguments?.getInt("imageResId")!!

        puzzlePieces = splitImage(requireContext(), imageResId, rows, cols)
        displayPuzzlePieces()

        return binding.root
    }

    private fun displayPuzzlePieces() {
        puzzlePieces.forEach { piece ->
            val imageView = ImageView(requireContext())
            imageView.setImageBitmap(piece)

            imageView.x = (50..450).random().toFloat()
            imageView.y = (50..750).random().toFloat()

            imageView.setOnTouchListener(PuzzlePieceTouchListener())
            puzzleContainer.addView(imageView)
        }
    }

    private fun splitImage(context: Context, imageResId: Int, rows: Int, cols: Int): List<Bitmap> {
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId)
            ?: throw IllegalArgumentException("Could not decode image resource")

        val pieceWidth = bitmap.width / cols
        val pieceHeight = bitmap.height / rows

        val pieces = mutableListOf<Bitmap>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val piece = Bitmap.createBitmap(bitmap, col * pieceWidth, row * pieceHeight, pieceWidth, pieceHeight)
                pieces.add(piece)
            }
        }
        return pieces
    }

    class PuzzlePieceTouchListener : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX + dX
                    view.y = event.rawY + dY
                }
            }
            return true
        }
    }
}
