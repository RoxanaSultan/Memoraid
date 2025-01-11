package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.memoraid.databinding.ItemImageBinding

class ImageAdapter(
    private val imageUris: MutableList<String>
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUri: String, position: Int) {
            Glide.with(binding.root.context)
                .load(imageUri)
                .into(binding.imageView)

            binding.pictureRemoveButton.setOnClickListener {
                imageUris.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = imageUris[position]
        holder.bind(imageUri, position)
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }
}
