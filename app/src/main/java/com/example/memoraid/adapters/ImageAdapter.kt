package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memoraid.databinding.ItemImageBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.storage.StorageReference

class ImageAdapter(
    private val imageUris: MutableList<String>,
    private val onImageRemoved: (String) -> Unit // Callback for image removal
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUri: String) {
            Glide.with(binding.root.context)
                .load(imageUri)
                .into(binding.imageView)

            binding.pictureRemoveButton.setOnClickListener {
                val currentPosition = adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val uriToRemove = imageUris[currentPosition]
                    onImageRemoved(uriToRemove)
                    imageUris.removeAt(currentPosition)
                    notifyItemRemoved(currentPosition)
                }
            }
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
