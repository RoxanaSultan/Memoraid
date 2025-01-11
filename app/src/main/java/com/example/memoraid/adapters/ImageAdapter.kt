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
    private val isSaved: Boolean, // Add isSaved flag to the constructor
    private val storageReference: StorageReference, // Reference to Firebase Storage
    private val firestoreCollection: CollectionReference // Reference to Firestore collection
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUri: String, position: Int) {
            Glide.with(binding.root.context)
                .load(imageUri)
                .into(binding.imageView)

            binding.pictureRemoveButton.setOnClickListener {
                if (isSaved) {
                    // Remove from Firestore and Firebase Storage only if 'Save' was pressed
                    removeImageFromFirestore(imageUri)
                    removeImageFromStorage(imageUri)
                }

                // Remove from the local list (UI update)
                imageUris.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        private fun removeImageFromFirestore(imageUri: String) {
            // Assuming imageUri contains the document ID or path to the image in Firestore
            val imageDocument = firestoreCollection.document(imageUri) // Example: using imageUri as the document ID
            imageDocument.delete().addOnSuccessListener {
                // Successfully deleted from Firestore
            }.addOnFailureListener {
                // Handle error in deleting from Firestore
            }
        }

        private fun removeImageFromStorage(imageUri: String) {
            val fileReference = storageReference.child("images/$imageUri") // Adjust based on your storage path
            fileReference.delete().addOnSuccessListener {
                // Successfully deleted from Storage
            }.addOnFailureListener {
                // Handle error in deleting from Storage
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
