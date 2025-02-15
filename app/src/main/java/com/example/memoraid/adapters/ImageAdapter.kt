package com.example.memoraid.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memoraid.databinding.ItemImageBinding

class ImageAdapter(
    private val imageUris: MutableList<String>,
    private val onImageRemoved: (String) -> Unit,
    private val onImageClicked: (String) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUri: String) {
            Glide.with(binding.root.context)
                .load(imageUri)
                .into(binding.imageView)

            binding.pictureRemoveButton.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    showDeleteConfirmationDialog(imageUri, currentPosition)
                }
            }

            binding.imageView.setOnClickListener {
                onImageClicked(imageUri)
            }
        }

        private fun showDeleteConfirmationDialog(imageUri: String, position: Int) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Yes") { _, _ ->
                    onImageRemoved(imageUri)
                    imageUris.removeAt(position)
                    notifyItemRemoved(position)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUris[position])
    }

    override fun getItemCount(): Int = imageUris.size
}