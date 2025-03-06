package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R

class PuzzleAdapter(
    private val puzzles: List<Int>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PuzzleAdapter.PuzzleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_puzzle, parent, false)
        return PuzzleViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val puzzleImage = puzzles[position]
        holder.bind(puzzleImage, onClick)
    }

    override fun getItemCount() = puzzles.size

    class PuzzleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.puzzleImage)

        fun bind(imageResId: Int, onClick: (Int) -> Unit) {
            imageView.setImageResource(imageResId)
            itemView.setOnClickListener {
                onClick(imageResId)
            }
        }
    }
}
